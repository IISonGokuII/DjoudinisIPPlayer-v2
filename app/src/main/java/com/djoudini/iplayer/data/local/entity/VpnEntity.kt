package com.djoudini.iplayer.data.local.entity

/**
 * VPN Server entity representing an available VPN endpoint.
 */
data class VpnServer(
    val id: String,
    val name: String,
    val country: String,
    val city: String,
    val hostname: String,
    val port: Int,
    val protocol: VpnProtocol = VpnProtocol.WIREGUARD,
    val isFree: Boolean = false,
    val isPremium: Boolean = false,
    val speed: Int? = null,
    val ping: Int? = null,
    val load: Int? = null,
    val configUrl: String? = null,
) {
    val displayName: String get() = "$country - $city"

    val statusText: String
        get() {
            val parts = listOfNotNull(
                ping?.let { "${it}ms" },
                speed?.let { "${it}Mbps" },
                load?.let { "${it}% Last" },
            )
            return parts.joinToString(" • ").ifEmpty { hostname }
        }
}

/**
 * VPN Protocol enum.
 */
enum class VpnProtocol {
    WIREGUARD,
    OPENVPN_UDP,
    OPENVPN_TCP,
    IKEV2,
    CUSTOM,
}

/**
 * VPN Provider type.
 */
enum class VpnProviderType {
    FREE_BUILTIN,
    CUSTOM_PROVIDER,
    MANUAL_CONFIG,
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
    val supportedProtocols: List<VpnProtocol> = listOf(VpnProtocol.WIREGUARD),
    val serversUrl: String? = null,
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
    val connectedSince: Long? = null,
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
