package com.djoudini.iplayer.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPlaybackLogicTest {

    @Test
    fun `buildStreamFallbacks appends alternate live extensions once`() {
        val fallbacks = PlayerPlaybackLogic.buildStreamFallbacks(
            "https://example.com/live/user/pass/12345.ts",
        )

        assertEquals(
            listOf(
                "https://example.com/live/user/pass/12345.ts",
                "https://example.com/live/user/pass/12345.m3u8",
                "https://example.com/live/user/pass/12345.mpegts",
            ),
            fallbacks,
        )
    }

    @Test
    fun `nextAspectRatio cycles through all modes and wraps`() {
        assertEquals(AspectRatio.FIT_4_3, PlayerPlaybackLogic.nextAspectRatio(AspectRatio.FIT_16_9))
        assertEquals(AspectRatio.ZOOM, PlayerPlaybackLogic.nextAspectRatio(AspectRatio.FIT_4_3))
        assertEquals(AspectRatio.STRETCH, PlayerPlaybackLogic.nextAspectRatio(AspectRatio.ZOOM))
        assertEquals(AspectRatio.ORIGINAL, PlayerPlaybackLogic.nextAspectRatio(AspectRatio.STRETCH))
        assertEquals(AspectRatio.FIT_16_9, PlayerPlaybackLogic.nextAspectRatio(AspectRatio.ORIGINAL))
    }

    @Test
    fun `clamp helpers keep values inside supported bounds`() {
        assertEquals(0.5f, PlayerPlaybackLogic.clampVideoScale(0.1f))
        assertEquals(3.0f, PlayerPlaybackLogic.clampVideoScale(9.0f))
        assertEquals(1.5f, PlayerPlaybackLogic.clampVideoScale(1.5f))
        assertEquals(-5000, PlayerPlaybackLogic.clampAudioDelay(-9000))
        assertEquals(5000, PlayerPlaybackLogic.clampAudioDelay(9000))
        assertEquals(250, PlayerPlaybackLogic.clampAudioDelay(250))
        assertTrue(PlayerPlaybackLogic.buildStreamFallbacks("plain-url").single() == "plain-url")
    }
}
