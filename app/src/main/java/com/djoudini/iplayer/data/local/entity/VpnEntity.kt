package com.djoudini.iplayer.data.local.entity

/**
 * VPN Server entity representing a available VPN server.
 */
data class VpnServer(
    val id: String,
    val name: String,
    val country: String,
    val city: String,
    val hostname: String,
    val port: Int,
    val protocol: VpnProtocol = VpnProtocol.WIREGUARD,
    val isFree: Boolean = true,
    val isPremium: Boolean = false,
    val speed: Int = 100, // 1-100
    val ping: Int = 50,   // in ms
    val load: Int = 50,   // 1-100 (server load percentage)
    val configUrl: String? = null, // For custom config downloads
) {
    val displayName: String get() = "$country - $city"
    val statusText: String get() = "${ping}ms • ${speed}Mbps • ${load}% Last"
}

/**
 * VPN Protocol enum.
 */
enum class VpnProtocol {
    WIREGUARD,
    OPENVPN_UDP,
    OPENVPN_TCP,
    IKEV2,
    CUSTOM
}

/**
 * VPN Provider type.
 */
enum class VpnProviderType {
    FREE_BUILTIN,      // Built-in free servers
    CUSTOM_PROVIDER,   // User's own VPN provider
    MANUAL_CONFIG      // Manual WireGuard/OpenVPN config
}

/**
 * Custom VPN Provider configuration.
 */
data class VpnProviderConfig(
    val id: String,
    val name: String,
    val type: VpnProviderType = VpnProviderType.CUSTOM_PROVIDER,
    val username: String? = null,
    val password: String? = null,
    val configUrl: String? = null,
    val apiEndpoint: String? = null,
    val apiKey: String? = null,
    val supportedProtocols: List<VpnProtocol> = listOf(VpnProtocol.WIREGUARD, VpnProtocol.OPENVPN_UDP),
    val serversUrl: String? = null, // URL to fetch server list
)

/**
 * VPN Connection state.
 */
sealed class VpnState {
    object Disconnected : VpnState()
    object Connecting : VpnState()
    object Connected : VpnState()
    object Disconnecting : VpnState()
    data class Error(val message: String) : VpnState()
    
    val isConnected: Boolean get() = this is Connected
    val isConnecting: Boolean get() = this is Connecting || this is Disconnecting
}

/**
 * VPN Connection info.
 */
data class VpnConnectionInfo(
    val server: VpnServer? = null,
    val state: VpnState = VpnState.Disconnected,
    val connectedSince: Long? = null, // Timestamp when connected
    val bytesUploaded: Long = 0,
    val bytesDownloaded: Long = 0,
    val currentPing: Int? = null,
    val localIp: String? = null,
    val remoteIp: String? = null,
) {
    val connectionDuration: Long?
        get() = connectedSince?.let { (System.currentTimeMillis() - it) / 1000 }
    
    val formattedDuration: String?
        get() = connectionDuration?.let {
            val hours = it / 3600
            val minutes = (it % 3600) / 60
            val seconds = it % 60
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
}
