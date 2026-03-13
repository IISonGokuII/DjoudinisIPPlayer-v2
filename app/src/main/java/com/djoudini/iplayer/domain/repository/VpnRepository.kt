package com.djoudini.iplayer.domain.repository

import com.djoudini.iplayer.data.local.entity.VpnConnectionInfo
import com.djoudini.iplayer.data.local.entity.VpnProviderConfig
import com.djoudini.iplayer.data.local.entity.VpnServer
import com.djoudini.iplayer.data.local.entity.VpnState
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for VPN operations.
 */
interface VpnRepository {

    /**
     * Flow of available VPN servers (free built-in servers).
     */
    val freeServers: Flow<List<VpnServer>>

    /**
     * Flow of premium VPN servers.
     */
    val premiumServers: Flow<List<VpnServer>>

    /**
     * Flow of custom provider servers (if configured).
     */
    val customProviderServers: Flow<List<VpnServer>>

    /**
     * Flow of current VPN connection state.
     */
    val connectionInfo: Flow<VpnConnectionInfo>

    /**
     * Flow of VPN enabled state.
     */
    val vpnEnabled: Flow<Boolean>

    /**
     * Flow of auto-connect setting.
     */
    val autoConnectEnabled: Flow<Boolean>

    /**
     * Get all available servers (combined).
     */
    suspend fun getAllServers(): List<VpnServer>

    /**
     * Get server by ID.
     */
    suspend fun getServerById(serverId: String): VpnServer?

    /**
     * Connect to a VPN server.
     */
    suspend fun connect(serverId: String): Result<Unit>

    /**
     * Disconnect from VPN.
     */
    suspend fun disconnect(): Result<Unit>

    /**
     * Check if VPN is currently connected.
     */
    fun isConnected(): Boolean

    /**
     * Get current connection state.
     */
    fun getCurrentState(): VpnState

    /**
     * Reconnect to the last used server.
     */
    suspend fun reconnect(): Result<Unit>

    /**
     * Test connection speed to a server.
     */
    suspend fun testServerSpeed(serverId: String): Result<Int>

    /**
     * Ping a server to get latency.
     */
    suspend fun pingServer(serverId: String): Result<Int>

    /**
     * Add custom VPN provider configuration.
     */
    suspend fun addCustomProvider(config: VpnProviderConfig): Result<Unit>

    /**
     * Get custom provider configuration.
     */
    suspend fun getCustomProvider(): VpnProviderConfig?

    /**
     * Remove custom provider configuration.
     */
    suspend fun removeCustomProvider(): Result<Unit>

    /**
     * Import VPN configuration from file (WireGuard .conf or OpenVPN .ovpn).
     */
    suspend fun importConfigFile(filePath: String): Result<Unit>

    /**
     * Fetch servers from custom provider API.
     */
    suspend fun fetchCustomProviderServers(): Result<List<VpnServer>>
}
