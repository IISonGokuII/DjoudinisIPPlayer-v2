package com.djoudini.iplayer.data.local.entity

/**
 * Parsed WireGuard configuration.
 */
data class WireGuardConfig(
    val privateKey: String = "",
    val addresses: List<String> = emptyList(),
    val dns: List<String> = emptyList(),
    val mtu: Int = 1420,
    val peers: List<WireGuardPeer> = emptyList(),
) {
    val isValid: Boolean
        get() = privateKey.isNotEmpty() && peers.any { it.publicKey.isNotEmpty() && it.endpoint.isNotEmpty() }

    /** First peer's endpoint host, e.g. "de-fra.mullvad.net" */
    val serverHost: String
        get() = peers.firstOrNull()?.endpoint?.substringBefore(":")?.trim() ?: ""

    /** First peer's endpoint port, defaults to 51820 */
    val serverPort: Int
        get() = peers.firstOrNull()?.endpoint?.substringAfter(":", "51820")
            ?.toIntOrNull() ?: 51820

    /** Primary tunnel address (first entry). */
    val tunnelAddress: String
        get() = addresses.firstOrNull()?.substringBefore("/")?.trim() ?: "10.0.0.2"

    /** Prefix length for tunnel address. */
    val tunnelPrefix: Int
        get() = addresses.firstOrNull()?.substringAfter("/", "32")
            ?.toIntOrNull() ?: 32

    /** Primary DNS server. */
    val primaryDns: String
        get() = dns.firstOrNull()?.trim() ?: "1.1.1.1"
}

/**
 * WireGuard peer (= VPN server endpoint).
 */
data class WireGuardPeer(
    val publicKey: String = "",
    val preSharedKey: String = "",
    val endpoint: String = "",
    val allowedIps: List<String> = listOf("0.0.0.0/0", "::/0"),
    val persistentKeepalive: Int = 25,
)

/**
 * Parser for WireGuard .conf files.
 *
 * Format:
 * ```
 * [Interface]
 * PrivateKey = ...
 * Address = 10.0.0.2/32
 * DNS = 1.1.1.1
 * MTU = 1420
 *
 * [Peer]
 * PublicKey = ...
 * Endpoint = server:51820
 * AllowedIPs = 0.0.0.0/0
 * PersistentKeepalive = 25
 * ```
 */
object WireGuardConfigParser {

    fun parse(content: String): WireGuardConfig {
        var privateKey = ""
        val addresses = mutableListOf<String>()
        val dns = mutableListOf<String>()
        var mtu = 1420
        val peers = mutableListOf<WireGuardPeer>()

        // Current section: "interface" or "peer"
        var section = ""
        // Accumulate peer fields
        var peerFields = mutableMapOf<String, String>()

        for (raw in content.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            when {
                line.equals("[Interface]", ignoreCase = true) -> {
                    if (section == "peer") peers.add(buildPeer(peerFields))
                    section = "interface"
                    peerFields = mutableMapOf()
                }
                line.equals("[Peer]", ignoreCase = true) -> {
                    if (section == "peer") peers.add(buildPeer(peerFields))
                    section = "peer"
                    peerFields = mutableMapOf()
                }
                line.contains("=") -> {
                    val eqIdx = line.indexOf('=')
                    val key = line.substring(0, eqIdx).trim().lowercase()
                    val value = line.substring(eqIdx + 1).trim()

                    if (section == "interface") {
                        when (key) {
                            "privatekey" -> privateKey = value
                            "address" -> addresses += value.split(",").map { it.trim() }
                            "dns" -> dns += value.split(",").map { it.trim() }
                            "mtu" -> mtu = value.toIntOrNull() ?: 1420
                        }
                    } else if (section == "peer") {
                        peerFields[key] = value
                    }
                }
            }
        }

        if (section == "peer") peers.add(buildPeer(peerFields))

        return WireGuardConfig(
            privateKey = privateKey,
            addresses = addresses,
            dns = dns,
            mtu = mtu,
            peers = peers,
        )
    }

    private fun buildPeer(fields: Map<String, String>): WireGuardPeer {
        val allowedIps = fields["allowedips"]
            ?.split(",")
            ?.map { it.trim() }
            ?: listOf("0.0.0.0/0")

        return WireGuardPeer(
            publicKey = fields["publickey"] ?: "",
            preSharedKey = fields["presharedkey"] ?: "",
            endpoint = fields["endpoint"] ?: "",
            allowedIps = allowedIps,
            persistentKeepalive = fields["persistentkeepalive"]?.toIntOrNull() ?: 25,
        )
    }

    /** Quick check: does the string look like a WireGuard config? */
    fun looksLikeWireGuard(content: String): Boolean =
        content.contains("[Interface]", ignoreCase = true) &&
                content.contains("PrivateKey", ignoreCase = true)

    /** Quick check: does the string look like an OpenVPN config? */
    fun looksLikeOpenVpn(content: String): Boolean =
        content.contains("client", ignoreCase = true) &&
                content.contains("remote ", ignoreCase = true)
}
