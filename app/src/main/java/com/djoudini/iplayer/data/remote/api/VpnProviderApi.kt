package com.djoudini.iplayer.data.remote.api

import com.djoudini.iplayer.data.local.entity.VpnServer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * VPN Provider API services.
 */

// ==================== Mullvad API ====================

/**
 * Mullvad API service.
 * Note: Mullvad doesn't have a public API for configs.
 * Configs must be downloaded from their website.
 */
interface MullvadApiService {
    
    // This is a placeholder - Mullvad configs are downloaded via website
    @GET("relays")
    suspend fun getRelays(): MullvadRelaysResponse
}

@Serializable
data class MullvadRelaysResponse(
    @SerialName("wiresguard")
    val wireguardRelays: List<WireguardRelay>? = null,
    @SerialName("openvpn")
    val openvpnRelays: List<OpenvpnRelay>? = null,
)

@Serializable
data class WireguardRelay(
    @SerialName("hostname")
    val hostname: String,
    @SerialName("location")
    val location: RelayLocation,
    @SerialName("endpoints")
    val endpoints: List<String>,
    @SerialName("ipv4_addr_in")
    val ipv4AddrIn: String,
)

@Serializable
data class OpenvpnRelay(
    @SerialName("hostname")
    val hostname: String,
    @SerialName("location")
    val location: RelayLocation,
)

@Serializable
data class RelayLocation(
    @SerialName("country")
    val country: String,
    @SerialName("city")
    val city: String? = null,
    @SerialName("latitude")
    val latitude: Double,
    @SerialName("longitude")
    val longitude: Double,
)

// ==================== ProtonVPN API ====================

/**
 * ProtonVPN API service.
 * Requires OAuth 2.0 authentication.
 */
interface ProtonVpnApiService {
    
    @GET("vpn/logicals")
    suspend fun getServers(
        @Query("Limit") limit: Int = 100,
        @Query("Page") page: Int = 1,
    ): ProtonVpnServersResponse
}

@Serializable
data class ProtonVpnServersResponse(
    @SerialName("Code")
    val code: Int,
    @SerialName("Servers")
    val servers: List<ProtonVpnServer>,
)

@Serializable
data class ProtonVpnServer(
    @SerialName("ID")
    val id: String,
    @SerialName("Name")
    val name: String,
    @SerialName("Status")
    val status: Int,
    @SerialName("Load")
    val load: Int,
    @SerialName("Location")
    val location: ServerLocation,
    @SerialName("Features")
    val features: Int,
)

@Serializable
data class ServerLocation(
    @SerialName("Country")
    val country: String,
    @SerialName("City")
    val city: String? = null,
    @SerialName("Lat")
    val latitude: Double,
    @SerialName("Long")
    val longitude: Double,
)

// ==================== VPN Config Generator ====================

/**
 * Helper object to generate WireGuard configs from provider data.
 */
object VpnConfigGenerator {
    
    /**
     * Generate WireGuard config for Mullvad server.
     */
    fun generateMullvadConfig(
        accountNumber: String,
        relay: WireguardRelay,
        privateKey: String,
        publicKey: String,
    ): String {
        return buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = $privateKey")
            appendLine("Address = ${relay.ipv4AddrIn}/32")
            appendLine("DNS = 10.64.0.1")
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = $publicKey")
            appendLine("Endpoint = ${relay.ipv4AddrIn}:51820")
            appendLine("AllowedIPs = 0.0.0.0/0")
        }
    }
    
    /**
     * Generate WireGuard config from raw parameters.
     */
    fun generateWireGuardConfig(
        privateKey: String,
        address: String,
        dns: String,
        publicKey: String,
        endpoint: String,
        allowedIPs: String = "0.0.0.0/0",
    ): String {
        return buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = $privateKey")
            appendLine("Address = $address")
            appendLine("DNS = $dns")
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = $publicKey")
            appendLine("Endpoint = $endpoint")
            appendLine("AllowedIPs = $allowedIPs")
        }
    }
    
    /**
     * Convert VpnServer to WireGuard config.
     */
    fun serverToConfig(server: VpnServer, privateKey: String): String {
        return generateWireGuardConfig(
            privateKey = privateKey,
            address = "10.0.0.2/24",
            dns = "8.8.8.8",
            publicKey = "SERVER_PUBLIC_KEY", // Would come from provider
            endpoint = "${server.hostname}:${server.port}",
        )
    }
}
