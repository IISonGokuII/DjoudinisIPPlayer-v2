package com.djoudini.iplayer.data.repository

import android.content.Context
import android.net.Uri
import com.djoudini.iplayer.data.local.entity.KnownVpnProviders
import com.djoudini.iplayer.data.local.entity.VpnConnectionTestResult
import com.djoudini.iplayer.data.local.entity.VpnProviderInfo
import com.djoudini.iplayer.data.local.entity.VpnSetupState
import com.djoudini.iplayer.data.local.entity.VpnSetupStep
import com.djoudini.iplayer.data.local.entity.WireGuardConfigParser
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.data.service.IpCheckService
import com.djoudini.iplayer.data.service.SpeedTestService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Repository for VPN Setup Wizard state and operations.
 */
class VpnSetupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val speedTestService: SpeedTestService,
    private val ipCheckService: IpCheckService,
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
     * Test VPN connection using real speed and IP checks.
     */
    suspend fun testConnection(): Result<VpnConnectionTestResult> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _setupState.update { it.copy(isLoading = true) }

                // Real speed test
                val speedResult = speedTestService.performSpeedTest()

                // Real IP check (shows current public IP — VPN changes it when real tunnel is up)
                val ipInfo = ipCheckService.getIpAddressWithRetries(retries = 2)

                val result = VpnConnectionTestResult(
                    success = speedResult.success,
                    pingMs = speedResult.pingMs,
                    downloadSpeedMbps = speedResult.downloadSpeedMbps,
                    uploadSpeedMbps = speedResult.uploadSpeedMbps,
                    serverLocation = ipInfo.displayLocation,
                    newIpAddress = ipInfo.ip.ifEmpty { null },
                    errorMessage = speedResult.errorMessage,
                )

                _setupState.update {
                    it.copy(
                        connectionTestResult = result,
                        isLoading = false,
                        currentStep = VpnSetupStep.Complete,
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
                        errorMessage = e.message,
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
     * Import VPN config from a content URI (file picker result).
     * Reads via ContentResolver so it works with any content:// URI.
     */
    suspend fun importVpnConfig(uri: Uri): Result<Unit> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: return@withContext Result.failure(Exception("Cannot read config file"))

                if (!WireGuardConfigParser.looksLikeWireGuard(content) &&
                    !WireGuardConfigParser.looksLikeOpenVpn(content)
                ) {
                    return@withContext Result.failure(
                        Exception("Ungültige VPN-Config (kein WireGuard / OpenVPN Format erkannt)")
                    )
                }

                // Validate WireGuard config more strictly
                if (WireGuardConfigParser.looksLikeWireGuard(content)) {
                    val parsed = WireGuardConfigParser.parse(content)
                    if (!parsed.isValid) {
                        return@withContext Result.failure(
                            Exception("WireGuard-Config unvollständig: PrivateKey oder Peer fehlt")
                        )
                    }
                }

                appPreferences.setVpnCustomConfig(content)
                _setupState.update { it.copy(currentStep = VpnSetupStep.ServerSelection) }

                Result.success(Unit)

            } catch (e: Exception) {
                Timber.e(e, "Failed to import VPN config from URI")
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
