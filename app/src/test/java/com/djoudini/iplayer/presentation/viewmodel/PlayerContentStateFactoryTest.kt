package com.djoudini.iplayer.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerContentStateFactoryTest {

    @Test
    fun `shouldOfferResume only returns true above threshold for incomplete items`() {
        assertFalse(PlayerContentStateFactory.shouldOfferResume(positionMs = 10_000L, isCompleted = false))
        assertFalse(PlayerContentStateFactory.shouldOfferResume(positionMs = 45_000L, isCompleted = true))
        assertTrue(PlayerContentStateFactory.shouldOfferResume(positionMs = 45_001L, isCompleted = false))
    }

    @Test
    fun `channelPlaybackState resets fallback index and clears stale error`() {
        val initialState = PlayerUiState(
            currentFallbackIndex = 2,
            error = "old",
            fallbackUrls = listOf("a", "b"),
        )

        val updatedState = PlayerContentStateFactory.channelPlaybackState(
            currentState = initialState,
            channel = ChannelPlaybackSnapshot(
                id = 77L,
                name = "Sports HD",
                streamUrl = "https://example.com/live/u/p/77.ts",
                logoUrl = "https://example.com/logo.png",
                userAgent = "TestAgent",
            ),
            currentProgram = null,
            nextProgram = null,
        )

        assertEquals(77L, updatedState.contentId)
        assertEquals("Sports HD", updatedState.title)
        assertEquals(0, updatedState.currentFallbackIndex)
        assertEquals(
            listOf(
                "https://example.com/live/u/p/77.ts",
                "https://example.com/live/u/p/77.m3u8",
                "https://example.com/live/u/p/77.mpegts",
            ),
            updatedState.fallbackUrls,
        )
        assertEquals(null, updatedState.error)
    }

    @Test
    fun `episodePlaybackState carries resume data and episode metadata`() {
        val updatedState = PlayerContentStateFactory.episodePlaybackState(
            currentState = PlayerUiState(error = "old"),
            episode = EpisodePlaybackSnapshot(
                id = 15L,
                name = "Episode 15",
                streamUrl = "https://example.com/episode.mp4",
                coverUrl = "https://example.com/cover.jpg",
                seasonNumber = 2,
                episodeNumber = 5,
            ),
            resumePositionMs = 55_000L,
            showResumeDialog = true,
            hasNextEpisode = true,
        )

        assertEquals(15L, updatedState.contentId)
        assertEquals("Episode 15", updatedState.title)
        assertEquals(55_000L, updatedState.resumePositionMs)
        assertTrue(updatedState.showResumeDialog)
        assertEquals(2, updatedState.seasonNumber)
        assertEquals(5, updatedState.episodeNumber)
        assertTrue(updatedState.hasNextEpisode)
        assertEquals(null, updatedState.error)
    }
}
