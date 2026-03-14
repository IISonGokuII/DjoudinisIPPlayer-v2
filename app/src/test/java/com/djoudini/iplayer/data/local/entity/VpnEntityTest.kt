package com.djoudini.iplayer.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnEntityTest {

    @Test
    fun `statusText falls back to hostname when metrics are missing`() {
        val server = VpnServer(
            id = "fra",
            name = "fra",
            country = "Importiert",
            city = "Frankfurt",
            hostname = "de-fra.example.net",
            port = 51820,
        )

        assertEquals("de-fra.example.net", server.statusText)
    }

    @Test
    fun `manual setup flow uses condensed real wireguard steps`() {
        assertEquals(4, VpnSetupStep.ProviderSelection.totalSteps)
        assertEquals(2, VpnSetupStep.ConfigImport.stepNumber)
        assertEquals(3, VpnSetupStep.ConnectionTest.stepNumber)
        assertEquals(4, VpnSetupStep.Complete.stepNumber)
        assertTrue(KnownVpnProviders.ALL_PROVIDERS.all { it.authType == VpnAuthType.MANUAL_CONFIG })
    }
}
