package com.djoudini.iplayer.data.repository

import android.content.Context
import com.djoudini.iplayer.data.local.entity.KnownVpnProviders
import com.djoudini.iplayer.data.local.entity.VpnConnectionTestResult
import com.djoudini.iplayer.data.local.entity.VpnProviderInfo
import com.djoudini.iplayer.data.local.entity.VpnSetupState
import com.djoudini.iplayer.data.local.entity.VpnSetupStep
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Repository for VPN Setup Wizard state and operations.
 */
class VpnSetupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
) {

    private val _setupState = MutableStateFlow(VpnSetupState())
    val setupState: StateFlow<VpnSetupState> = _setupState.asStateFlow()

    /**
     * Get all available VPN providers.
     */
    fun getAvailableProviders(): List<VpnProviderInfo> {
        return KnownVpnProviders.ALL_PROVIDERS
    }

    /**
     * Get popular VPN providers.
     */
    fun getPopularProviders(): List<VpnProviderInfo> {
        return KnownVpnProviders.POPULAR_PROVIDERS
    }

    /**
     * Select a VPN provider.
     */
    fun selectProvider(provider: VpnProviderInfo) {
        _setupState.update { currentState ->
            currentState.copy(
                selectedProvider = provider,
                authMethod = provider.authType,
                currentStep = when (provider.authType) {
                    com.djoudini.iplayer.data.local.entity.VpnAuthType.NONE -> {
                        // Built-in servers, skip to server selection
                        VpnSetupStep.ServerSelection
                    }
                    com.djoudini.iplayer.data.local.entity.VpnAuthType.MANUAL_CONFIG -> {
                        // Manual config, go to config import
                        VpnSetupStep.ConfigImport
                    }
                    else -> {
                        // Go to login
                        VpnSetupStep.Login
                    }
                }
            )
        }
    }

    /**
     * Set login credentials.
     */
    fun setCredentials(
        username: String? = null,
        password: String? = null,
        accountNumber: String? = null,
        apiKey: String? = null,
    ) {
        _setupState.update { currentState ->
            currentState.copy(
                username = username,
                password = password,
                accountNumber = accountNumber,
                apiKey = apiKey,
                currentStep = VpnSetupStep.ServerSelection
            )
        }
    }

    /**
     * Set config file path.
     */
    fun setConfigFile(filePath: String) {
        _setupState.update { currentState ->
            currentState.copy(
                configFilePath = filePath,
                currentStep = VpnSetupStep.ServerSelection
            )
        }
    }

    /**
     * Select a VPN server.
     */
    fun selectServer(serverId: String) {
        _setupState.update { currentState ->
            currentState.copy(
                selectedServerId = serverId,
                currentStep = VpnSetupStep.ConnectionTest
            )
        }
    }

    /**
     * Test VPN connection.
     */
    suspend fun testConnection(): Result<VpnConnectionTestResult> {
        return withContext(kotlinx.coroutines.Dispatchers.Default) {
            try {
                _setupState.update { it.copy(isLoading = true) }

                // Simulate connection test
                kotlinx.coroutines.delay(3000)

                val currentState = _setupState.value
                val serverId = currentState.selectedServerId ?: currentState.selectedProvider?.id

                // Simulate test results
                val result = VpnConnectionTestResult(
                    success = true,
                    pingMs = (20..80).random(),
                    downloadSpeedMbps = (50..200).random(),
                    uploadSpeedMbps = (20..100).random(),
                    serverLocation = "Frankfurt, Germany",
                    newIpAddress = "185.234.${(1..255).random()}.${(1..255).random()}",
                )

                _setupState.update {
                    it.copy(
                        connectionTestResult = result,
                        isLoading = false,
                        currentStep = VpnSetupStep.Complete
                    )
                }

                Result.success(result)

            } catch (e: Exception) {
                Timber.e(e, "VPN connection test failed")
                val errorResult = VpnConnectionTestResult(
                    success = false,
                    errorMessage = e.message ?: "Unknown error",
                )
                _setupState.update {
                    it.copy(
                        connectionTestResult = errorResult,
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
                Result.failure(e)
            }
        }
    }

    /**
     * Complete setup and save configuration.
     */
    suspend fun completeSetup(enableAutoConnect: Boolean): Result<Unit> {
        return try {
            val currentState = _setupState.value
            
            // Save VPN settings
            appPreferences.setVpnEnabled(true)
            appPreferences.setVpnAutoConnect(enableAutoConnect)
            
            if (currentState.selectedServerId != null) {
                appPreferences.setVpnServerId(currentState.selectedServerId)
            }
            
            // Save provider config if applicable
            currentState.selectedProvider?.let { provider ->
                appPreferences.setVpnProviderType(
                    when (provider.authType) {
                        com.djoudini.iplayer.data.local.entity.VpnAuthType.NONE ->
                            com.djoudini.iplayer.data.local.entity.VpnProviderType.FREE_BUILTIN.name
                        com.djoudini.iplayer.data.local.entity.VpnAuthType.MANUAL_CONFIG ->
                            com.djoudini.iplayer.data.local.entity.VpnProviderType.MANUAL_CONFIG.name
                        else ->
                            com.djoudini.iplayer.data.local.entity.VpnProviderType.CUSTOM_PROVIDER.name
                    }
                )
            }

            Timber.d("VPN setup completed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to complete VPN setup")
            Result.failure(e)
        }
    }

    /**
     * Reset setup state.
     */
    fun resetSetup() {
        _setupState.value = VpnSetupState()
    }

    /**
     * Go to previous step.
     */
    fun goToPreviousStep() {
        val currentState = _setupState.value
        val previousStep = when (currentState.currentStep) {
            is VpnSetupStep.AuthMethod -> VpnSetupStep.ProviderSelection
            is VpnSetupStep.Login -> VpnSetupStep.AuthMethod
            is VpnSetupStep.ConfigImport -> VpnSetupStep.ProviderSelection
            is VpnSetupStep.ServerSelection -> {
                if (currentState.selectedProvider?.authType == 
                    com.djoudini.iplayer.data.local.entity.VpnAuthType.MANUAL_CONFIG
                ) {
                    VpnSetupStep.ConfigImport
                } else {
                    VpnSetupStep.Login
                }
            }
            is VpnSetupStep.ConnectionTest -> VpnSetupStep.ServerSelection
            is VpnSetupStep.Complete -> VpnSetupStep.ConnectionTest
            else -> VpnSetupStep.ProviderSelection
        }
        
        _setupState.update { it.copy(currentStep = previousStep) }
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        _setupState.update { it.copy(isLoading = loading) }
    }

    /**
     * Set error message.
     */
    fun setError(message: String?) {
        _setupState.update { it.copy(errorMessage = message) }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _setupState.update { it.copy(errorMessage = null) }
    }

    /**
     * Import VPN config from file.
     */
    suspend fun importVpnConfig(filePath: String): Result<Unit> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("File not found"))
                }

                val content = file.readText()
                
                // Validate config (basic check)
                val isValid = content.contains("[Interface]") || // WireGuard
                             content.contains("client") ||      // OpenVPN
                             content.contains("remote")         // OpenVPN
                
                if (!isValid) {
                    return@withContext Result.failure(Exception("Invalid VPN config file"))
                }

                // Save config
                appPreferences.setVpnCustomConfig(content)
                
                setConfigFile(filePath)
                Result.success(Unit)

            } catch (e: Exception) {
                Timber.e(e, "Failed to import VPN config")
                Result.failure(e)
            }
        }
    }

    /**
     * Authenticate with VPN provider (placeholder).
     */
    suspend fun authenticateWithProvider(): Result<Unit> {
        return withContext(kotlinx.coroutines.Dispatchers.Default) {
            try {
                _setupState.update { it.copy(isLoading = true) }
                
                // Simulate network delay
                kotlinx.coroutines.delay(2000)
                
                val currentState = _setupState.value
                
                // Basic validation
                val isValid = when (currentState.selectedProvider?.authType) {
                    com.djoudini.iplayer.data.local.entity.VpnAuthType.USERNAME_PASSWORD -> {
                        !currentState.username.isNullOrBlank() && 
                        !currentState.password.isNullOrBlank()
                    }
                    com.djoudini.iplayer.data.local.entity.VpnAuthType.ACCOUNT_NUMBER -> {
                        !currentState.accountNumber.isNullOrBlank()
                    }
                    else -> true
                }
                
                _setupState.update { it.copy(isLoading = false) }
                
                if (isValid) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Invalid credentials"))
                }

            } catch (e: Exception) {
                Timber.e(e, "Authentication failed")
                _setupState.update { it.copy(isLoading = false) }
                Result.failure(e)
            }
        }
    }
}
