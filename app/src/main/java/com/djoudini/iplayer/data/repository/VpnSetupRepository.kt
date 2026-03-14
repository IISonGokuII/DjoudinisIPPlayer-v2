package com.djoudini.iplayer.data.repository

import android.content.Context
import android.net.Uri
import com.djoudini.iplayer.data.local.entity.KnownVpnProviders
import com.djoudini.iplayer.data.local.entity.VpnConnectionTestResult
import com.djoudini.iplayer.data.local.entity.VpnProviderInfo
import com.djoudini.iplayer.data.local.entity.VpnProviderType
import com.djoudini.iplayer.data.local.entity.VpnSetupState
import com.djoudini.iplayer.data.local.entity.VpnSetupStep
import com.djoudini.iplayer.data.local.entity.WireGuardConfigParser
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.data.service.IpCheckService
import com.djoudini.iplayer.data.service.SpeedTestService
import com.djoudini.iplayer.domain.repository.VpnRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * Repository for the production-ready VPN setup flow.
 * The supported path is importing a real WireGuard config and validating it
 * through an actual tunnel connection test.
 */
class VpnSetupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val speedTestService: SpeedTestService,
    private val ipCheckService: IpCheckService,
    private val vpnRepository: VpnRepository,
) {

    private val _setupState = MutableStateFlow(VpnSetupState())
    val setupState: StateFlow<VpnSetupState> = _setupState.asStateFlow()

    fun getAvailableProviders(): List<VpnProviderInfo> = KnownVpnProviders.ALL_PROVIDERS

    fun getPopularProviders(): List<VpnProviderInfo> = KnownVpnProviders.POPULAR_PROVIDERS

    fun selectProvider(provider: VpnProviderInfo) {
        _setupState.update {
            it.copy(
                selectedProvider = provider,
                authMethod = provider.authType,
                currentStep = VpnSetupStep.ConfigImport,
                errorMessage = null,
            )
        }
    }

    fun setCredentials(
        username: String? = null,
        password: String? = null,
        accountNumber: String? = null,
        apiKey: String? = null,
    ) {
        _setupState.update {
            it.copy(
                username = username,
                password = password,
                accountNumber = accountNumber,
                apiKey = apiKey,
            )
        }
    }

    fun setConfigFile(filePath: String) {
        _setupState.update { it.copy(configFilePath = filePath) }
    }

    fun selectServer(serverId: String) {
        _setupState.update {
            it.copy(
                selectedServerId = serverId,
                currentStep = VpnSetupStep.ConnectionTest,
                errorMessage = null,
            )
        }
    }

    suspend fun testConnection(): Result<VpnConnectionTestResult> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val serverId = _setupState.value.selectedServerId
                    ?: return@withContext Result.failure(IllegalStateException("Keine WireGuard-Konfiguration importiert."))

                _setupState.update { it.copy(isLoading = true, errorMessage = null) }

                vpnRepository.connect(serverId).getOrThrow()
                val speedResult = speedTestService.performSpeedTest()
                val ipInfo = ipCheckService.getIpAddressWithRetries(retries = 2)

                val result = VpnConnectionTestResult(
                    success = speedResult.success && ipInfo.error == null,
                    pingMs = speedResult.pingMs,
                    downloadSpeedMbps = speedResult.downloadSpeedMbps,
                    uploadSpeedMbps = speedResult.uploadSpeedMbps,
                    serverLocation = ipInfo.displayLocation,
                    newIpAddress = ipInfo.ip.ifEmpty { null },
                    errorMessage = speedResult.errorMessage ?: ipInfo.error,
                )

                _setupState.update {
                    it.copy(
                        connectionTestResult = result,
                        isLoading = false,
                        currentStep = if (result.success) VpnSetupStep.Complete else VpnSetupStep.ConnectionTest,
                        errorMessage = result.errorMessage,
                    )
                }

                if (result.success) Result.success(result)
                else Result.failure(IllegalStateException(result.errorMessage ?: "VPN-Test fehlgeschlagen"))
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
                        currentStep = VpnSetupStep.ConnectionTest,
                        errorMessage = e.message,
                    )
                }
                Result.failure(e)
            }
        }
    }

    suspend fun completeSetup(enableAutoConnect: Boolean): Result<Unit> {
        return try {
            val currentState = _setupState.value
            if (currentState.selectedServerId.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Keine importierte WireGuard-Konfiguration gespeichert."))
            }

            appPreferences.setVpnEnabled(true)
            appPreferences.setVpnAutoConnect(enableAutoConnect)
            appPreferences.setVpnProviderType(VpnProviderType.MANUAL_CONFIG.name)
            appPreferences.setVpnServerId(currentState.selectedServerId)
            appPreferences.setVpnProtocol("WIREGUARD")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to complete VPN setup")
            Result.failure(e)
        }
    }

    fun resetSetup() {
        _setupState.value = VpnSetupState()
    }

    fun goToPreviousStep() {
        val previousStep = when (_setupState.value.currentStep) {
            is VpnSetupStep.ConfigImport -> VpnSetupStep.ProviderSelection
            is VpnSetupStep.ConnectionTest -> VpnSetupStep.ConfigImport
            is VpnSetupStep.Complete -> VpnSetupStep.ConnectionTest
            else -> VpnSetupStep.ProviderSelection
        }
        _setupState.update { it.copy(currentStep = previousStep, errorMessage = null) }
    }

    fun setLoading(loading: Boolean) {
        _setupState.update { it.copy(isLoading = loading) }
    }

    fun setError(message: String?) {
        _setupState.update { it.copy(errorMessage = message) }
    }

    fun clearError() {
        _setupState.update { it.copy(errorMessage = null) }
    }

    suspend fun importVpnConfig(uri: Uri): Result<Unit> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: return@withContext Result.failure(Exception("Cannot read config file"))

                if (!WireGuardConfigParser.looksLikeWireGuard(content)) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Aktuell werden nur echte WireGuard-.conf-Dateien unterstuetzt.")
                    )
                }

                val parsed = WireGuardConfigParser.parse(content)
                if (!parsed.isValid) {
                    return@withContext Result.failure(
                        IllegalArgumentException("WireGuard-Konfiguration unvollstaendig: PrivateKey, Peer oder Endpoint fehlen.")
                    )
                }

                val host = parsed.serverHost.ifBlank { "wireguard-endpoint" }.lowercase(Locale.ROOT)
                appPreferences.setVpnCustomConfig(content)
                appPreferences.setVpnProviderType(VpnProviderType.MANUAL_CONFIG.name)
                appPreferences.setVpnServerId(host)
                appPreferences.setVpnProtocol("WIREGUARD")

                _setupState.update {
                    it.copy(
                        currentStep = VpnSetupStep.ConnectionTest,
                        selectedServerId = host,
                        connectionTestResult = null,
                        errorMessage = null,
                    )
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to import VPN config from URI")
                Result.failure(e)
            }
        }
    }

    suspend fun authenticateWithProvider(): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Provider-Logins werden nicht simuliert. Bitte importiere eine echte WireGuard-.conf-Datei.")
        )
    }
}
