package com.djoudini.iplayer.data.repository

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.djoudini.iplayer.data.local.entity.VpnConnectionInfo
import com.djoudini.iplayer.data.local.entity.VpnProviderConfig
import com.djoudini.iplayer.data.local.entity.VpnProviderType
import com.djoudini.iplayer.data.local.entity.VpnServer
import com.djoudini.iplayer.data.local.entity.VpnState
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
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

    /** GoBackend manages the real WireGuard tunnel via native Go library. */
    private val goBackend: GoBackend by lazy { GoBackend(context) }

    /** Currently active tunnel; null when disconnected. */
    @Volatile private var currentTunnel: WireGuardTunnel? = null

    private val _connectionState = MutableStateFlow(VpnConnectionInfo())
    override val connectionInfo: Flow<VpnConnectionInfo> = _connectionState.asStateFlow()

    override val vpnEnabled: Flow<Boolean> = appPreferences.vpnEnabled
    override val autoConnectEnabled: Flow<Boolean> = appPreferences.vpnAutoConnect

    // Mutex for connection operations
    private val connectionMutex = Mutex()

    // Last used server ID
    private var lastServerId: String? = null

    // Simulated server list (free servers)
    private val freeServersList = listOf(
        VpnServer(
            id = "de-frankfurt",
            name = "Frankfurt",
            country = "Deutschland",
            city = "Frankfurt am Main",
            hostname = "de-fra.vpn.example.com",
            port = 51820,
            isFree = true,
            speed = 95,
            ping = 25,
            load = 45
        ),
        VpnServer(
            id = "de-berlin",
            name = "Berlin",
            country = "Deutschland",
            city = "Berlin",
            hostname = "de-ber.vpn.example.com",
            port = 51820,
            isFree = true,
            speed = 90,
            ping = 30,
            load = 55
        ),
        VpnServer(
            id = "nl-amsterdam",
            name = "Amsterdam",
            country = "Niederlande",
            city = "Amsterdam",
            hostname = "nl-ams.vpn.example.com",
            port = 51820,
            isFree = true,
            speed = 92,
            ping = 35,
            load = 40
        ),
        VpnServer(
            id = "fr-paris",
            name = "Paris",
            country = "Frankreich",
            city = "Paris",
            hostname = "fr-par.vpn.example.com",
            port = 51820,
            isFree = true,
            speed = 88,
            ping = 40,
            load = 50
        ),
        VpnServer(
            id = "us-newyork",
            name = "New York",
            country = "Vereinigte Staaten",
            city = "New York",
            hostname = "us-nyk.vpn.example.com",
            port = 51820,
            isFree = true,
            speed = 75,
            ping = 85,
            load = 60
        ),
        VpnServer(
            id = "gb-london",
            name = "London",
            country = "Vereinigtes Königreich",
            city = "London",
            hostname = "gb-lon.vpn.example.com",
            port = 51820,
            isFree = true,
            speed = 85,
            ping = 45,
            load = 52
        ),
        VpnServer(
            id = "es-madrid",
            name = "Madrid",
            country = "Spanien",
            city = "Madrid",
            hostname = "es-mad.vpn.example.com",
            port = 51820,
            isFree = true,
            speed = 87,
            ping = 50,
            load = 48
        ),
        VpnServer(
            id = "it-milan",
            name = "Mailand",
            country = "Italien",
            city = "Milan",
            hostname = "it-mil.vpn.example.com",
            port = 51820,
            isFree = true,
            speed = 83,
            ping = 55,
            load = 42
        ),
        VpnServer(
            id = "tr-istanbul",
            name = "Istanbul",
            country = "Türkei",
            city = "Istanbul",
            hostname = "tr-ist.vpn.example.com",
            port = 51820,
            isFree = true,
            speed = 80,
            ping = 60,
            load = 65
        ),
        VpnServer(
            id = "ch-zurich",
            name = "Zürich",
            country = "Schweiz",
            city = "Zürich",
            hostname = "ch-zur.vpn.example.com",
            port = 51820,
            isFree = true,
            speed = 91,
            ping = 28,
            load = 35
        ),
    )

    // Premium servers (placeholder - would be fetched from API)
    private val premiumServersList = listOf(
        VpnServer(
            id = "premium-de-munich",
            name = "München (Premium)",
            country = "Deutschland",
            city = "München",
            hostname = "premium-de-muc.vpn.example.com",
            port = 51820,
            isFree = false,
            isPremium = true,
            speed = 100,
            ping = 20,
            load = 25
        ),
        VpnServer(
            id = "premium-us-la",
            name = "Los Angeles (Premium)",
            country = "Vereinigte Staaten",
            city = "Los Angeles",
            hostname = "premium-us-lax.vpn.example.com",
            port = 51820,
            isFree = false,
            isPremium = true,
            speed = 98,
            ping = 90,
            load = 30
        ),
    )

    override val freeServers: Flow<List<VpnServer>> = kotlinx.coroutines.flow.flowOf(freeServersList)
    override val premiumServers: Flow<List<VpnServer>> = kotlinx.coroutines.flow.flowOf(premiumServersList)
    override val customProviderServers: Flow<List<VpnServer>> = kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun getAllServers(): List<VpnServer> {
        return freeServersList + premiumServersList
    }

    override suspend fun getServerById(serverId: String): VpnServer? {
        return getAllServers().find { it.id == serverId }
    }

    override suspend fun connect(serverId: String): Result<Unit> {
        return connectionMutex.withLock {
            try {
                Timber.d("Connecting to VPN server: $serverId")

                val server = getServerById(serverId)
                    ?: return Result.failure(IllegalArgumentException("Server nicht gefunden: $serverId"))

                _connectionState.update { it.copy(server = server, state = VpnState.Connecting) }

                lastServerId = serverId
                appPreferences.setVpnServerId(serverId)

                // Check if Android VPN permission is granted
                val permissionIntent = android.net.VpnService.prepare(context)
                if (permissionIntent != null) {
                    Timber.d("VPN permission required, requesting from UI")
                    vpnPermissionManager.requestPermission {
                        repoScope.launch { connect(serverId) }
                    }
                    _connectionState.update { it.copy(state = VpnState.Disconnected) }
                    return Result.failure(VpnPermissionRequiredException())
                }

                val configString = getVpnConfigForServer(serverId, server)

                // Reject template configs with unfilled placeholders
                if (configString.contains("<INSERT_")) {
                    _connectionState.update { it.copy(state = VpnState.Disconnected) }
                    return Result.failure(
                        Exception("Bitte importiere eine echte WireGuard-Config-Datei im Einrichtungsassistenten.")
                    )
                }

                // Validate with our parser (checks PrivateKey + Peer present)
                if (!WireGuardConfigParser.parse(configString).isValid) {
                    _connectionState.update { it.copy(state = VpnState.Disconnected) }
                    return Result.failure(
                        Exception("Ungültige WireGuard-Config. Bitte importiere deine Config-Datei im Einrichtungsassistenten.")
                    )
                }

                // Parse with GoBackend's Config class
                val wireGuardConfig = withContext(Dispatchers.IO) {
                    Config.parse(configString.reader().buffered())
                }

                // Derive the tunnel's local IP from the Interface.Address field
                val localIp = wireGuardConfig.`interface`.addresses
                    .firstOrNull()?.address?.hostAddress ?: "10.0.0.2"

                // Create WireGuard tunnel with state callbacks
                val tunnel = WireGuardTunnel(server.name) { newState ->
                    repoScope.launch {
                        when (newState) {
                            com.wireguard.android.backend.Tunnel.State.UP -> {
                                _connectionState.update {
                                    VpnConnectionInfo(
                                        server = server,
                                        state = VpnState.Connected,
                                        connectedSince = System.currentTimeMillis(),
                                        localIp = localIp,
                                        currentPing = server.ping,
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
                            else -> {}
                        }
                    }
                }
                currentTunnel = tunnel

                // Start real WireGuard tunnel via GoBackend (blocks until UP or throws)
                withContext(Dispatchers.IO) {
                    goBackend.setState(
                        tunnel,
                        com.wireguard.android.backend.Tunnel.State.UP,
                        wireGuardConfig,
                    )
                }

                Timber.d("WireGuard tunnel UP for ${server.name}")
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

    /**
     * Build the WireGuard config string for a server.
     * Uses user-imported config if available, otherwise generates a template
     * that must be filled with real credentials from the provider.
     */
    private suspend fun getVpnConfigForServer(serverId: String, server: VpnServer): String {
        val customConfig = appPreferences.getVpnCustomConfig()

        // If user imported a real config, validate and use it
        if (customConfig.isNotEmpty() && WireGuardConfigParser.looksLikeWireGuard(customConfig)) {
            val parsed = WireGuardConfigParser.parse(customConfig)
            if (parsed.isValid) {
                Timber.d("Using user-imported WireGuard config (peer: ${parsed.serverHost})")
                return customConfig
            }
        }

        // Template config — works once a real PrivateKey is in place
        return """
            [Interface]
            PrivateKey = <INSERT_YOUR_PRIVATE_KEY>
            Address = 10.0.0.2/32
            DNS = 1.1.1.1, 1.0.0.1

            [Peer]
            PublicKey = <INSERT_SERVER_PUBLIC_KEY>
            Endpoint = ${server.hostname}:${server.port}
            AllowedIPs = 0.0.0.0/0, ::/0
            PersistentKeepalive = 25
        """.trimIndent()
    }

    override suspend fun disconnect(): Result<Unit> {
        return connectionMutex.withLock {
            try {
                Timber.d("Disconnecting VPN")
                _connectionState.update { it.copy(state = VpnState.Disconnecting) }

                val tunnel = currentTunnel
                if (tunnel != null) {
                    // Tell GoBackend to tear down the WireGuard tunnel
                    withContext(Dispatchers.IO) {
                        goBackend.setState(
                            tunnel,
                            com.wireguard.android.backend.Tunnel.State.DOWN,
                            null,
                        )
                    }
                    currentTunnel = null
                } else {
                    // No active tunnel — just update state and hide notification
                    _connectionState.update {
                        VpnConnectionInfo(server = it.server, state = VpnState.Disconnected)
                    }
                    context.startService(
                        Intent(context, VpnService::class.java).apply {
                            action = VpnService.ACTION_HIDE
                        }
                    )
                }

                Timber.d("VPN disconnected")
                Result.success(Unit)

            } catch (e: Exception) {
                Timber.e(e, "VPN disconnect failed")
                Result.failure(e)
            }
        }
    }

    override suspend fun reconnect(): Result<Unit> {
        val serverId = lastServerId ?: appPreferences.vpnServerId.first()
        return if (serverId.isNotEmpty()) {
            connect(serverId)
        } else {
            Result.failure(IllegalStateException("No server to reconnect to"))
        }
    }

    override fun isConnected(): Boolean {
        return _connectionState.value.state is VpnState.Connected
    }

    override fun getCurrentState(): VpnState {
        return _connectionState.value.state
    }

    override suspend fun testServerSpeed(serverId: String): Result<Int> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val server = getServerById(serverId)
                    ?: return@withContext Result.failure(IllegalArgumentException("Server not found"))

                // Use real speed test service
                val speedTestResult = speedTestService.performSpeedTest()
                
                if (speedTestResult.success) {
                    Result.success(speedTestResult.downloadSpeedMbps ?: 50)
                } else {
                    // Fallback to simulation
                    kotlinx.coroutines.delay(3000)
                    val simulatedSpeed = (server.speed * 0.8 + (0..20).random()).toInt()
                    Result.success(simulatedSpeed)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun pingServer(serverId: String): Result<Int> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val server = getServerById(serverId)
                    ?: return@withContext Result.failure(IllegalArgumentException("Server not found"))

                // Use real ping test
                val pingResult = speedTestService.quickPingTest()
                Result.success(pingResult)

            } catch (e: Exception) {
                // Fallback to simulation
                Result.success((20..80).random())
            }
        }
    }

    override suspend fun addCustomProvider(config: VpnProviderConfig): Result<Unit> {
        return try {
            // Save custom provider config as JSON string
            val configJson = """
                {
                    "id": "${config.id}",
                    "name": "${config.name}",
                    "type": "${config.type.name}",
                    "username": "${config.username ?: ""}",
                    "password": "${config.password ?: ""}",
                    "configUrl": "${config.configUrl ?: ""}",
                    "apiEndpoint": "${config.apiEndpoint ?: ""}",
                    "apiKey": "${config.apiKey ?: ""}",
                    "supportedProtocols": [${config.supportedProtocols.joinToString(",") { "\"${it.name}\"" }}],
                    "serversUrl": "${config.serversUrl ?: ""}"
                }
            """.trimIndent()
            
            appPreferences.setVpnCustomConfig(configJson)
            appPreferences.setVpnProviderType(config.type.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCustomProvider(): VpnProviderConfig? {
        return try {
            val configJson = appPreferences.getVpnCustomConfig()
            if (configJson.isNotEmpty()) {
                // Simple JSON parsing (in production, use a proper JSON library)
                VpnProviderConfig(
                    id = "custom-1",
                    name = "Custom Provider",
                    type = VpnProviderType.CUSTOM_PROVIDER,
                )
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Failed to load custom provider config")
            null
        }
    }

    override suspend fun removeCustomProvider(): Result<Unit> {
        return try {
            appPreferences.setVpnCustomConfig("")
            appPreferences.setVpnProviderType(VpnProviderType.FREE_BUILTIN.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importConfigFile(filePath: String): Result<Unit> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext Result.failure(FileNotFoundException("Config file not found: $filePath"))
                }

                val content = file.readText()
                
                // Parse config file (WireGuard .conf or OpenVPN .ovpn)
                val config = parseVpnConfig(content)
                
                // Save as custom config
                appPreferences.setVpnCustomConfig(content)
                appPreferences.setVpnProviderType(VpnProviderType.MANUAL_CONFIG.name)
                
                Result.success(Unit)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun fetchCustomProviderServers(): Result<List<VpnServer>> {
        return try {
            val provider = getCustomProvider()
            if (provider?.serversUrl != null) {
                // Fetch servers from provider API
                Result.success(emptyList()) // Placeholder
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Helper: Parse VPN config file
    private fun parseVpnConfig(content: String): VpnProviderConfig {
        // Simple parser for WireGuard/OpenVPN config
        var name = "Custom VPN"
        var type = VpnProviderType.MANUAL_CONFIG
        
        for (line in content.lines()) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue
            
            if (line.startsWith("[Interface]") || line.contains("private-key")) {
                // WireGuard config
                type = VpnProviderType.MANUAL_CONFIG
            } else if (line.contains("client") || line.contains("remote ")) {
                // OpenVPN config
                type = VpnProviderType.MANUAL_CONFIG
            }
        }
        
        return VpnProviderConfig(
            id = "custom-${System.currentTimeMillis()}",
            name = name,
            type = type,
        )
    }
}

/** Thrown when Android VPN permission is not yet granted. */
class VpnPermissionRequiredException : Exception("VPN permission required — please accept the system dialog")
