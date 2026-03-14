package com.djoudini.iplayer.data.repository

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.djoudini.iplayer.data.local.entity.VpnConnectionInfo
import com.djoudini.iplayer.data.local.entity.VpnProviderConfig
import com.djoudini.iplayer.data.local.entity.VpnProviderType
import com.djoudini.iplayer.data.local.entity.VpnServer
import com.djoudini.iplayer.data.local.entity.VpnState
import com.djoudini.iplayer.data.local.entity.WireGuardConfig
import com.djoudini.iplayer.data.local.entity.WireGuardConfigParser
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.data.service.IpCheckService
import com.djoudini.iplayer.data.service.SpeedTestService
import com.djoudini.iplayer.data.service.VpnPermissionManager
import com.djoudini.iplayer.data.service.VpnService
import com.djoudini.iplayer.data.service.WireGuardTunnel
import com.djoudini.iplayer.domain.repository.VpnRepository
import com.wireguard.android.backend.GoBackend
import com.wireguard.config.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val connectivityManager: ConnectivityManager,
    private val vpnPermissionManager: VpnPermissionManager,
    private val speedTestService: SpeedTestService,
    private val ipCheckService: IpCheckService,
) : VpnRepository {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val goBackend: GoBackend by lazy { GoBackend(context) }

    @Volatile
    private var currentTunnel: WireGuardTunnel? = null

    private val _connectionState = MutableStateFlow(VpnConnectionInfo())
    override val connectionInfo: Flow<VpnConnectionInfo> = _connectionState.asStateFlow()

    override val vpnEnabled: Flow<Boolean> = appPreferences.vpnEnabled
    override val autoConnectEnabled: Flow<Boolean> = appPreferences.vpnAutoConnect

    private val connectionMutex = Mutex()
    private var lastServerId: String? = null

    override val freeServers: Flow<List<VpnServer>> = flowOf(emptyList())
    override val premiumServers: Flow<List<VpnServer>> = flowOf(emptyList())
    override val customProviderServers: Flow<List<VpnServer>> = flowOf(emptyList())

    override suspend fun getAllServers(): List<VpnServer> {
        return getImportedWireGuardConfig()?.let { listOf(buildServerFromConfig(it)) } ?: emptyList()
    }

    override suspend fun getServerById(serverId: String): VpnServer? {
        return getAllServers().find { it.id == serverId }
    }

    override suspend fun connect(serverId: String): Result<Unit> {
        return connectionMutex.withLock {
            try {
                val config = getImportedWireGuardConfig()
                    ?: return Result.failure(
                        IllegalStateException("Keine WireGuard-Konfiguration importiert. Bitte zuerst eine .conf-Datei importieren.")
                    )

                val server = buildServerFromConfig(config)
                if (serverId.isNotBlank() && serverId != server.id) {
                    return Result.failure(
                        IllegalArgumentException("Die importierte Konfiguration passt nicht zum angeforderten Endpunkt.")
                    )
                }

                _connectionState.update { it.copy(server = server, state = VpnState.Connecting) }
                lastServerId = server.id
                appPreferences.setVpnServerId(server.id)
                appPreferences.setVpnProtocol(server.protocol.name)
                appPreferences.setVpnProviderType(VpnProviderType.MANUAL_CONFIG.name)

                val permissionIntent = android.net.VpnService.prepare(context)
                if (permissionIntent != null) {
                    vpnPermissionManager.requestPermission {
                        repoScope.launch { connect(server.id) }
                    }
                    _connectionState.update { it.copy(state = VpnState.Disconnected) }
                    return Result.failure(VpnPermissionRequiredException())
                }

                val wireGuardConfig = withContext(Dispatchers.IO) {
                    Config.parse(getRequiredConfigText().reader().buffered())
                }

                val localIp = config.tunnelAddress
                val tunnel = WireGuardTunnel(server.name) { newState ->
                    repoScope.launch {
                        when (newState) {
                            com.wireguard.android.backend.Tunnel.State.UP -> {
                                val remoteIp = ipCheckService.getIpAddress().ip.ifEmpty { null }
                                _connectionState.update {
                                    VpnConnectionInfo(
                                        server = server,
                                        state = VpnState.Connected,
                                        connectedSince = System.currentTimeMillis(),
                                        localIp = localIp,
                                        remoteIp = remoteIp,
                                    )
                                }
                                context.startForegroundService(
                                    Intent(context, VpnService::class.java).apply {
                                        action = VpnService.ACTION_SHOW
                                        putExtra(VpnService.EXTRA_TUNNEL_NAME, server.name)
                                    }
                                )
                            }

                            com.wireguard.android.backend.Tunnel.State.DOWN -> {
                                _connectionState.update {
                                    VpnConnectionInfo(server = it.server, state = VpnState.Disconnected)
                                }
                                context.startService(
                                    Intent(context, VpnService::class.java).apply {
                                        action = VpnService.ACTION_HIDE
                                    }
                                )
                            }
                            else -> Unit
                        }
                    }
                }

                currentTunnel = tunnel
                withContext(Dispatchers.IO) {
                    goBackend.setState(
                        tunnel,
                        com.wireguard.android.backend.Tunnel.State.UP,
                        wireGuardConfig,
                    )
                }

                Result.success(Unit)
            } catch (e: VpnPermissionRequiredException) {
                Result.failure(e)
            } catch (e: Exception) {
                Timber.e(e, "VPN connection failed")
                _connectionState.update { it.copy(state = VpnState.Error(e.message ?: "Unknown error")) }
                Result.failure(e)
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return connectionMutex.withLock {
            try {
                _connectionState.update { it.copy(state = VpnState.Disconnecting) }
                currentTunnel?.let { tunnel ->
                    withContext(Dispatchers.IO) {
                        goBackend.setState(
                            tunnel,
                            com.wireguard.android.backend.Tunnel.State.DOWN,
                            null,
                        )
                    }
                }
                currentTunnel = null
                _connectionState.value = VpnConnectionInfo(state = VpnState.Disconnected)
                context.startService(
                    Intent(context, VpnService::class.java).apply {
                        action = VpnService.ACTION_HIDE
                    }
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "VPN disconnect failed")
                Result.failure(e)
            }
        }
    }

    override suspend fun reconnect(): Result<Unit> {
        val serverId = lastServerId ?: appPreferences.vpnServerId.first()
        if (serverId.isBlank()) {
            return Result.failure(IllegalStateException("Keine importierte WireGuard-Konfiguration gespeichert."))
        }
        return connect(serverId)
    }

    override fun isConnected(): Boolean = _connectionState.value.state is VpnState.Connected

    override fun getCurrentState(): VpnState = _connectionState.value.state

    override suspend fun testServerSpeed(serverId: String): Result<Int> {
        return try {
            ensureMatchingImportedServer(serverId)
            val speedTestResult = speedTestService.performSpeedTest()
            if (speedTestResult.success && speedTestResult.downloadSpeedMbps != null) {
                _connectionState.update { it.copy(currentPing = speedTestResult.pingMs) }
                Result.success(speedTestResult.downloadSpeedMbps)
            } else {
                Result.failure(IllegalStateException(speedTestResult.errorMessage ?: "Geschwindigkeitstest fehlgeschlagen"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pingServer(serverId: String): Result<Int> {
        return try {
            ensureMatchingImportedServer(serverId)
            val ping = speedTestService.quickPingTest()
            _connectionState.update { it.copy(currentPing = ping) }
            Result.success(ping)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCustomProvider(config: VpnProviderConfig): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Automatischer Provider-Import ist derzeit nicht implementiert. Bitte importiere eine WireGuard-.conf-Datei.")
        )
    }

    override suspend fun getCustomProvider(): VpnProviderConfig? = null

    override suspend fun removeCustomProvider(): Result<Unit> {
        return try {
            appPreferences.setVpnCustomConfig("")
            appPreferences.setVpnServerId("")
            appPreferences.setVpnEnabled(false)
            currentTunnel = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importConfigFile(filePath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext Result.failure(FileNotFoundException("Config file not found: $filePath"))
                }

                val content = file.readText()
                val config = parseRequiredWireGuardConfig(content)
                val server = buildServerFromConfig(config)

                appPreferences.setVpnCustomConfig(content)
                appPreferences.setVpnProviderType(VpnProviderType.MANUAL_CONFIG.name)
                appPreferences.setVpnServerId(server.id)
                appPreferences.setVpnProtocol(server.protocol.name)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun fetchCustomProviderServers(): Result<List<VpnServer>> {
        return Result.failure(
            UnsupportedOperationException("Provider-Serverlisten werden nicht künstlich erzeugt. Bitte importiere eine echte WireGuard-Konfiguration.")
        )
    }

    private suspend fun ensureMatchingImportedServer(serverId: String) {
        if (serverId.isBlank()) {
            throw IllegalArgumentException("Kein WireGuard-Endpunkt ausgewählt.")
        }
        val imported = getAllServers().firstOrNull()
            ?: throw IllegalStateException("Keine WireGuard-Konfiguration importiert.")
        if (imported.id != serverId) {
            throw IllegalArgumentException("Die importierte WireGuard-Konfiguration passt nicht zum ausgewählten Endpunkt.")
        }
    }

    private suspend fun getImportedWireGuardConfig(): WireGuardConfig? {
        val configText = appPreferences.getVpnCustomConfig()
        if (configText.isBlank()) return null
        return parseRequiredWireGuardConfig(configText)
    }

    private fun parseRequiredWireGuardConfig(content: String): WireGuardConfig {
        if (!WireGuardConfigParser.looksLikeWireGuard(content)) {
            throw IllegalArgumentException("Nur echte WireGuard-.conf-Dateien werden aktuell unterstützt.")
        }
        val parsed = WireGuardConfigParser.parse(content)
        if (!parsed.isValid) {
            throw IllegalArgumentException("Die WireGuard-Konfiguration ist unvollständig: PrivateKey, Peer oder Endpoint fehlen.")
        }
        return parsed
    }

    private suspend fun getRequiredConfigText(): String {
        val content = appPreferences.getVpnCustomConfig()
        if (content.isBlank()) {
            throw IllegalStateException("Keine WireGuard-Konfiguration gespeichert.")
        }
        return content
    }

    private fun buildServerFromConfig(config: WireGuardConfig): VpnServer {
        val host = config.serverHost.ifBlank { "wireguard-endpoint" }
        val city = host.substringBefore(".").replace("-", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        return VpnServer(
            id = host.lowercase(Locale.ROOT),
            name = host,
            country = "Importiert",
            city = city,
            hostname = host,
            port = config.serverPort,
        )
    }
}

class VpnPermissionRequiredException : Exception("VPN permission required - please accept the system dialog")
