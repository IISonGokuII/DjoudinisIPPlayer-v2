package com.djoudini.iplayer.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WireGuardConfigParserTest {

    @Test
    fun `parse extracts core WireGuard fields`() {
        val config = WireGuardConfigParser.parse(
            """
            [Interface]
            PrivateKey = test-private-key
            Address = 10.10.0.2/32
            DNS = 1.1.1.1, 9.9.9.9

            [Peer]
            PublicKey = test-public-key
            Endpoint = de-fra.example.net:51820
            AllowedIPs = 0.0.0.0/0, ::/0
            PersistentKeepalive = 25
            """.trimIndent(),
        )

        assertTrue(config.isValid)
        assertEquals("test-private-key", config.privateKey)
        assertEquals("de-fra.example.net", config.serverHost)
        assertEquals(51820, config.serverPort)
        assertEquals("10.10.0.2", config.tunnelAddress)
        assertEquals("1.1.1.1", config.primaryDns)
        assertEquals(2, config.dns.size)
    }

    @Test
    fun `looksLikeWireGuard rejects non wireguard content`() {
        val openVpnStyle = """
            client
            dev tun
            remote vpn.example.net 1194
        """.trimIndent()

        assertFalse(WireGuardConfigParser.looksLikeWireGuard(openVpnStyle))
        assertTrue(WireGuardConfigParser.looksLikeOpenVpn(openVpnStyle))
    }
}
