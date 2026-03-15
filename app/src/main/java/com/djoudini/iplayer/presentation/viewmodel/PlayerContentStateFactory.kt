package com.djoudini.iplayer.presentation.viewmodel

import com.djoudini.iplayer.data.local.entity.EpgProgramEntity

internal data class ChannelPlaybackSnapshot(
    val id: Long,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val userAgent: String?,
)

internal data class EpisodePlaybackSnapshot(
    val id: Long,
    val name: String,
    val streamUrl: String,
    val coverUrl: String?,
    val seasonNumber: Int,
    val episodeNumber: Int,
)

internal data class RecordingPlaybackSnapshot(
    val id: Long,
    val name: String,
    val filePath: String,
)

internal object PlayerContentStateFactory {
    private const val RESUME_THRESHOLD_MS = 10_000L

    fun shouldOfferResume(positionMs: Long, isCompleted: Boolean): Boolean {
        return positionMs > RESUME_THRESHOLD_MS && !isCompleted
    }

    fun channelPlaybackState(
        currentState: PlayerUiState,
        channel: ChannelPlaybackSnapshot,
        currentProgram: EpgProgramEntity?,
        nextProgram: EpgProgramEntity?,
    ): PlayerUiState {
        return currentState.copy(
            title = channel.name,
            streamUrl = channel.streamUrl,
            logoUrl = channel.logoUrl,
            contentId = channel.id,
            isLoading = false,
            currentProgram = currentProgram,
            nextProgram = nextProgram,
            userAgent = channel.userAgent,
            fallbackUrls = PlayerPlaybackLogic.buildStreamFallbacks(channel.streamUrl),
            currentFallbackIndex = 0,
            error = null,
        )
    }

    fun episodePlaybackState(
        currentState: PlayerUiState,
        episode: EpisodePlaybackSnapshot,
        resumePositionMs: Long,
        showResumeDialog: Boolean,
        hasNextEpisode: Boolean,
    ): PlayerUiState {
        return currentState.copy(
            title = episode.name,
            streamUrl = episode.streamUrl,
            logoUrl = episode.coverUrl,
            contentId = episode.id,
            isLoading = false,
            resumePositionMs = resumePositionMs,
            showResumeDialog = showResumeDialog,
            seasonNumber = episode.seasonNumber,
            episodeNumber = episode.episodeNumber,
            hasNextEpisode = hasNextEpisode,
            error = null,
        )
    }

    fun recordingPlaybackState(
        currentState: PlayerUiState,
        recording: RecordingPlaybackSnapshot,
    ): PlayerUiState {
        return currentState.copy(
            title = recording.name,
            streamUrl = recording.filePath,
            contentId = recording.id,
            isLoading = false,
            error = null,
        )
    }
}
