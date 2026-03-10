package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.ChannelDao
import com.djoudini.iplayer.data.local.dao.EpisodeDao
import com.djoudini.iplayer.data.local.dao.VodDao
import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import com.djoudini.iplayer.domain.model.WatchContentType
import com.djoudini.iplayer.domain.repository.ChannelRepository
import com.djoudini.iplayer.domain.repository.EpgRepository
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import com.djoudini.iplayer.presentation.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val title: String = "",
    val streamUrl: String = "",
    val logoUrl: String? = null,
    val contentType: WatchContentType = WatchContentType.CHANNEL,
    val contentId: Long = 0L,
    val playlistId: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null,
    // EPG
    val currentProgram: EpgProgramEntity? = null,
    val nextProgram: EpgProgramEntity? = null,
    // Resume
    val resumePositionMs: Long = 0L,
    val showResumeDialog: Boolean = false,
    // Controls visibility
    val controlsVisible: Boolean = true,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    // User-Agent override
    val userAgent: String? = null,
    // Stream fallback URLs for LiveTV retry
    val fallbackUrls: List<String> = emptyList(),
    val currentFallbackIndex: Int = 0,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val channelRepository: ChannelRepository,
    private val channelDao: ChannelDao,
    private val vodDao: VodDao,
    private val episodeDao: EpisodeDao,
    private val epgRepository: EpgRepository,
    private val watchProgressRepository: WatchProgressRepository,
    val playerFactory: PlayerFactory,
) : ViewModel() {

    private val contentTypeArg: String = savedStateHandle.get<String>(NavArgs.CONTENT_TYPE) ?: "channel"
    private val contentId: Long = savedStateHandle.get<Long>(NavArgs.CONTENT_ID) ?: 0L

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressSaveJob: Job? = null

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            try {
                val playlist = playlistRepository.getActive()
                val playlistId = playlist?.id ?: 0L

                val contentType = WatchContentType.fromValue(contentTypeArg)

                when (contentType) {
                    WatchContentType.CHANNEL -> loadChannel(playlistId, contentType)
                    WatchContentType.VOD -> loadVod(playlistId, contentType)
                    WatchContentType.EPISODE -> loadEpisode(playlistId, contentType)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load content") }
            }
        }
    }

    private suspend fun loadChannel(playlistId: Long, contentType: WatchContentType) {
        val channel = channelDao.getById(contentId)
            ?: throw IllegalStateException("Channel not found")

        channelRepository.updateLastWatched(contentId)

        // Load EPG data
        val currentProgram = channel.tvgId?.let { epgRepository.getCurrentProgram(it) }
        val nextProgram = channel.tvgId?.let { epgRepository.getNextProgram(it) }

        // Build fallback URLs for LiveTV streams
        val fallbackUrls = buildStreamFallbacks(channel.streamUrl, channel.containerExtension)

        _uiState.update {
            it.copy(
                title = channel.name,
                streamUrl = channel.streamUrl,
                logoUrl = channel.logoUrl,
                contentType = contentType,
                contentId = contentId,
                playlistId = playlistId,
                isLoading = false,
                currentProgram = currentProgram,
                nextProgram = nextProgram,
                userAgent = channel.userAgent,
                fallbackUrls = fallbackUrls,
                currentFallbackIndex = 0,
            )
        }
    }

    /**
     * Build alternative stream URLs by trying different extensions/formats.
     * Many IPTV providers support multiple formats per channel.
     */
    private fun buildStreamFallbacks(originalUrl: String, containerExtension: String?): List<String> {
        val urls = mutableListOf(originalUrl)

        // For Xtream-style URLs like http://server/live/user/pass/12345.ts
        val xtreamPattern = Regex("""(.+/live/.+/\d+)\.(ts|m3u8|mpegts)$""")
        val match = xtreamPattern.find(originalUrl)
        if (match != null) {
            val base = match.groupValues[1]
            val extensions = listOf("ts", "m3u8", "mpegts")
            for (ext in extensions) {
                val alt = "$base.$ext"
                if (alt != originalUrl && alt !in urls) {
                    urls.add(alt)
                }
            }
        } else {
            // For generic URLs, try appending /live.m3u8 or changing extension
            val dotIdx = originalUrl.lastIndexOf('.')
            if (dotIdx > originalUrl.lastIndexOf('/')) {
                val base = originalUrl.substring(0, dotIdx)
                val extensions = listOf(".ts", ".m3u8", ".mpegts")
                for (ext in extensions) {
                    val alt = "$base$ext"
                    if (alt != originalUrl && alt !in urls) {
                        urls.add(alt)
                    }
                }
            }
        }

        return urls
    }

    /**
     * Called when playback fails. Tries the next fallback URL for LiveTV.
     * Returns the next URL to try, or null if all fallbacks exhausted.
     */
    fun tryNextFallback(): String? {
        val state = _uiState.value
        if (state.contentType != WatchContentType.CHANNEL) return null

        val nextIndex = state.currentFallbackIndex + 1
        if (nextIndex >= state.fallbackUrls.size) return null

        val nextUrl = state.fallbackUrls[nextIndex]
        _uiState.update {
            it.copy(
                streamUrl = nextUrl,
                currentFallbackIndex = nextIndex,
                error = null,
            )
        }
        return nextUrl
    }

    private suspend fun loadVod(playlistId: Long, contentType: WatchContentType) {
        val vod = vodDao.getById(contentId)
            ?: throw IllegalStateException("VOD item not found")

        val progress = watchProgressRepository.getProgress(playlistId, contentType, contentId)
        val hasProgress = (progress?.positionMs ?: 0L) > 10_000 && !(progress?.isCompleted ?: true)

        _uiState.update {
            it.copy(
                title = vod.name,
                streamUrl = vod.streamUrl,
                logoUrl = vod.logoUrl,
                contentType = contentType,
                contentId = contentId,
                playlistId = playlistId,
                isLoading = false,
                resumePositionMs = progress?.positionMs ?: 0L,
                showResumeDialog = hasProgress,
            )
        }
    }

    private suspend fun loadEpisode(playlistId: Long, contentType: WatchContentType) {
        val episode = episodeDao.getById(contentId)
            ?: throw IllegalStateException("Episode not found")

        val progress = watchProgressRepository.getProgress(playlistId, contentType, contentId)
        val hasProgress = (progress?.positionMs ?: 0L) > 10_000 && !(progress?.isCompleted ?: true)

        _uiState.update {
            it.copy(
                title = episode.name,
                streamUrl = episode.streamUrl,
                logoUrl = episode.coverUrl,
                contentType = contentType,
                contentId = contentId,
                playlistId = playlistId,
                isLoading = false,
                resumePositionMs = progress?.positionMs ?: 0L,
                showResumeDialog = hasProgress,
            )
        }
    }

    fun onResumeChoice(resume: Boolean) {
        if (!resume) {
            _uiState.update { it.copy(resumePositionMs = 0L) }
        }
        _uiState.update { it.copy(showResumeDialog = false) }
    }

    fun updatePlaybackState(isPlaying: Boolean, positionMs: Long, durationMs: Long) {
        _uiState.update {
            it.copy(isPlaying = isPlaying, currentPositionMs = positionMs, durationMs = durationMs)
        }
    }

    /**
     * Start periodic save of watch progress (every 10 seconds).
     */
    fun startProgressTracking() {
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                saveProgress()
            }
        }
    }

    fun stopProgressTracking() {
        progressSaveJob?.cancel()
        progressSaveJob = null
        // Final save
        viewModelScope.launch { saveProgress() }
    }

    private suspend fun saveProgress() {
        val state = _uiState.value
        if (state.playlistId == 0L || state.contentId == 0L) return
        if (state.contentType == WatchContentType.CHANNEL) return // Skip live

        watchProgressRepository.saveProgress(
            playlistId = state.playlistId,
            contentType = state.contentType,
            contentId = state.contentId,
            positionMs = state.currentPositionMs,
            durationMs = state.durationMs,
            contentName = state.title,
            posterUrl = state.logoUrl ?: "",
        )
    }

    fun toggleControls() {
        _uiState.update { it.copy(controlsVisible = !it.controlsVisible) }
    }

    fun hideControls() {
        _uiState.update { it.copy(controlsVisible = false) }
    }

    fun showControls() {
        _uiState.update { it.copy(controlsVisible = true) }
    }

    fun onPlaybackError(message: String) {
        _uiState.update { it.copy(error = message, isLoading = false) }
    }

    override fun onCleared() {
        super.onCleared()
        progressSaveJob?.cancel()
    }
}
