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
import timber.log.Timber
import androidx.compose.runtime.Immutable
import javax.inject.Inject

@Immutable
data class AudioTrackInfo(
    val index: Int,
    val language: String,
    val label: String?,
    val isSelected: Boolean,
)

@Immutable
data class SubtitleTrackInfo(
    val index: Int,
    val language: String,
    val label: String?,
    val isSelected: Boolean,
)

/**
 * Aspect ratio modes for video playback.
 */
enum class AspectRatio(val label: String, val scale: Float) {
    FIT_16_9("16:9", 1.0f),
    FIT_4_3("4:3", 0.75f),
    ZOOM("Zoom", 1.33f),
    STRETCH("Stretch", 1.0f),
    ORIGINAL("Original", 1.0f)
}

/**
 * Sleep timer presets in minutes.
 */
enum class SleepTimerPreset(val minutes: Int, val label: String) {
    OFF(0, "Aus"),
    MIN_15(15, "15 Min"),
    MIN_30(30, "30 Min"),
    MIN_45(45, "45 Min"),
    MIN_60(60, "60 Min"),
    MIN_90(90, "90 Min"),
}

@Immutable
data class PlayerUiState(
    val title: String = "",
    val streamUrl: String = "",
    val logoUrl: String? = null,
    val contentType: WatchContentType = WatchContentType.CHANNEL,
    val contentId: Long = 0L,
    val playlistId: Long = 0L,
    val categoryId: Long? = null, // For channel zapping
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
    // Series info for next episode
    val seriesId: Long? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val hasNextEpisode: Boolean = false,
    // Audio/Subtitle tracks
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val subtitleTracks: List<SubtitleTrackInfo> = emptyList(),
    val showTrackSelection: Boolean = false,
    // Favorite status
    val isFavorite: Boolean = false,
    // Recent channels (last 5)
    val recentChannels: List<Long> = emptyList(),
    // Channel number input
    val channelNumberInput: String = "",
    val showChannelNumberInput: Boolean = false,
    // NEW: Auto-play for next episode
    val showAutoPlayCountdown: Boolean = false,
    val autoPlayCountdownSeconds: Int = 5,
    // NEW: Sleep timer
    val sleepTimerActive: Boolean = false,
    val sleepTimerRemainingSeconds: Int = 0,
    // NEW: Aspect ratio
    val aspectRatio: AspectRatio = AspectRatio.FIT_16_9,
    // NEW: Pinch-to-zoom scale
    val videoScale: Float = 1.0f,
    // NEW: VOD description
    val description: String? = null,
    // NEW: Audio delay (ms) for sync
    val audioDelayMs: Int = 0,
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
    // NEW: Sleep timer job
    private var sleepTimerJob: Job? = null
    // NEW: Auto-play job
    private var autoPlayJob: Job? = null

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

        // Load recent channels (last 5 watched), excluding current
        val recentChannels = channelDao.getRecentlyWatchedIds(playlistId, excludeId = contentId, limit = 5)

        _uiState.update {
            it.copy(
                title = channel.name,
                streamUrl = channel.streamUrl,
                logoUrl = channel.logoUrl,
                contentType = contentType,
                contentId = contentId,
                playlistId = playlistId,
                categoryId = channel.categoryId,
                isLoading = false,
                currentProgram = currentProgram,
                nextProgram = nextProgram,
                userAgent = channel.userAgent,
                fallbackUrls = fallbackUrls,
                currentFallbackIndex = 0,
                isFavorite = channel.isFavorite,
                recentChannels = recentChannels,
            )
        }
    }

    /**
     * Build alternative stream URLs by trying different extensions/formats.
     * Viele IPTV-Provider unterstützen mehrere Formate pro Kanal.
     * OPTIMIERUNG: String-Operationen statt Regex für bessere Performance.
     */
    private fun buildStreamFallbacks(originalUrl: String, containerExtension: String?): List<String> {
        val urls = mutableListOf(originalUrl)

        // OPTIMIERUNG: String-Operationen statt Regex für Xtream-URLs
        // Pattern: http://server/live/user/pass/12345.ts
        val liveIndex = originalUrl.indexOf("/live/")
        if (liveIndex != -1) {
            val lastSlash = originalUrl.lastIndexOf('/')
            if (lastSlash > liveIndex) {
                val dotIndex = originalUrl.lastIndexOf('.')
                if (dotIndex > lastSlash) {
                    val base = originalUrl.substring(0, dotIndex)
                    // Fallbacks in Prioritätsreihenfolge
                    val extensions = listOf("ts", "m3u8", "mpegts")
                    for (ext in extensions) {
                        val alt = "$base.$ext"
                        if (alt != originalUrl && alt !in urls) {
                            urls.add(alt)
                        }
                    }
                }
            }
        } else {
            // Für generische URLs
            val lastSlash = originalUrl.lastIndexOf('/')
            val dotIndex = originalUrl.lastIndexOf('.')
            if (dotIndex > lastSlash) {
                val base = originalUrl.substring(0, dotIndex)
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
     * If null is returned, caller should invoke onPlaybackError().
     */
    fun tryNextFallback(): String? {
        val state = _uiState.value
        if (state.contentType != WatchContentType.CHANNEL) return null

        val nextIndex = state.currentFallbackIndex + 1
        if (nextIndex >= state.fallbackUrls.size) {
            // No more fallbacks available - set error state
            _uiState.update {
                it.copy(
                    error = "Keine weiteren Stream-URLs verfügbar. Bitte überprüfen Sie Ihre Internetverbindung.",
                    isLoading = false,
                )
            }
            return null
        }

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
                description = vod.plot, // NEW: Load description
            )
        }
    }

    private suspend fun loadEpisode(playlistId: Long, contentType: WatchContentType) {
        val episode = episodeDao.getById(contentId)
            ?: throw IllegalStateException("Episode not found")

        val progress = watchProgressRepository.getProgress(playlistId, contentType, contentId)
        val hasProgress = (progress?.positionMs ?: 0L) > 10_000 && !(progress?.isCompleted ?: true)

        // Check if there's a next episode
        val hasNext = episodeDao.hasNextEpisode(
            episode.seriesId,
            episode.seasonNumber,
            episode.episodeNumber
        )

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
                seriesId = episode.seriesId,
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber,
                hasNextEpisode = hasNext,
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
        sleepTimerJob?.cancel()
        autoPlayJob?.cancel()
    }

    // ==================== SLEEP TIMER ====================

    /**
     * Set sleep timer to stop playback after specified minutes.
     */
    fun setSleepTimer(preset: SleepTimerPreset) {
        sleepTimerJob?.cancel()
        
        if (preset == SleepTimerPreset.OFF) {
            _uiState.update { it.copy(sleepTimerActive = false, sleepTimerRemainingSeconds = 0) }
            return
        }

        val totalSeconds = preset.minutes * 60
        _uiState.update { 
            it.copy(
                sleepTimerActive = true,
                sleepTimerRemainingSeconds = totalSeconds
            )
        }

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
            }
            // Time's up - stop playback
            stopPlayback()
            _uiState.update { it.copy(sleepTimerActive = false, sleepTimerRemainingSeconds = 0) }
        }
    }

    /**
     * Cancel active sleep timer.
     */
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _uiState.update { it.copy(sleepTimerActive = false, sleepTimerRemainingSeconds = 0) }
    }

    // ==================== ASPECT RATIO ====================

    /**
     * Cycle through aspect ratio modes.
     */
    fun cycleAspectRatio() {
        val current = _uiState.value.aspectRatio
        _uiState.update {
            it.copy(
                aspectRatio = when (current) {
                    AspectRatio.FIT_16_9 -> AspectRatio.FIT_4_3
                    AspectRatio.FIT_4_3 -> AspectRatio.ZOOM
                    AspectRatio.ZOOM -> AspectRatio.STRETCH
                    AspectRatio.STRETCH -> AspectRatio.ORIGINAL
                    AspectRatio.ORIGINAL -> AspectRatio.FIT_16_9
                }
            )
        }
    }

    /**
     * Set specific aspect ratio mode.
     */
    fun setAspectRatio(ratio: AspectRatio) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    /**
     * Update video scale for pinch-to-zoom.
     */
    fun setVideoScale(scale: Float) {
        _uiState.update { it.copy(videoScale = scale.coerceIn(0.5f, 3.0f)) }
    }

    /**
     * Reset video scale to default.
     */
    fun resetVideoScale() {
        _uiState.update { it.copy(videoScale = 1.0f) }
    }

    // ==================== AUDIO DELAY ====================

    /**
     * Set audio delay for sync (in milliseconds).
     */
    fun setAudioDelay(delayMs: Int) {
        _uiState.update { it.copy(audioDelayMs = delayMs.coerceIn(-5000, 5000)) }
    }

    /**
     * Adjust audio delay by delta.
     */
    fun adjustAudioDelay(deltaMs: Int) {
        val current = _uiState.value.audioDelayMs
        _uiState.update { it.copy(audioDelayMs = (current + deltaMs).coerceIn(-5000, 5000)) }
    }

    /**
     * Reset audio delay to zero.
     */
    fun resetAudioDelay() {
        _uiState.update { it.copy(audioDelayMs = 0) }
    }

    // ==================== AUTO-PLAY NEXT EPISODE ====================

    /**
     * Start countdown for auto-playing next episode.
     */
    fun startAutoPlayCountdown(seconds: Int = 5) {
        autoPlayJob?.cancel()
        
        _uiState.update { 
            it.copy(
                showAutoPlayCountdown = true,
                autoPlayCountdownSeconds = seconds
            )
        }

        autoPlayJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.update { it.copy(autoPlayCountdownSeconds = remaining) }
            }
            // Auto-play next episode
            _uiState.update { it.copy(showAutoPlayCountdown = false) }
            loadNextEpisode()
        }
    }

    /**
     * Cancel auto-play countdown.
     */
    fun cancelAutoPlay() {
        autoPlayJob?.cancel()
        autoPlayJob = null
        _uiState.update { it.copy(showAutoPlayCountdown = false) }
    }

    // ==================== STOP PLAYBACK ====================

    /**
     * Stop playback (used by sleep timer).
     */
    private fun stopPlayback() {
        // This will be called from the sleep timer
        // The PlayerScreen should observe sleepTimerActive and stop the player
    }

    // ==================== CHANNEL ZAPPING (Live TV) ====================

    /**
     * Play the previous channel in the current category.
     * Used for D-PAD_UP channel zapping.
     * Returns true if a previous channel was found and loaded.
     */
    fun playPreviousChannel(): Boolean {
        val state = _uiState.value
        if (state.contentType != WatchContentType.CHANNEL || state.categoryId == null) {
            return false
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val currentChannel = channelDao.getById(state.contentId) ?: return@launch
                val previousChannel = channelDao.getPreviousChannelInCategory(
                    state.categoryId,
                    state.contentId,
                    currentChannel.sortOrder
                )

                if (previousChannel != null) {
                    // Update state with new channel info
                    channelRepository.updateLastWatched(previousChannel.id)
                    
                    val currentProgram = previousChannel.tvgId?.let { epgRepository.getCurrentProgram(it) }
                    val nextProgram = previousChannel.tvgId?.let { epgRepository.getNextProgram(it) }
                    val fallbackUrls = buildStreamFallbacks(previousChannel.streamUrl, previousChannel.containerExtension)

                    _uiState.update {
                        it.copy(
                            title = previousChannel.name,
                            streamUrl = previousChannel.streamUrl,
                            logoUrl = previousChannel.logoUrl,
                            contentId = previousChannel.id,
                            isLoading = false,
                            currentProgram = currentProgram,
                            nextProgram = nextProgram,
                            userAgent = previousChannel.userAgent,
                            fallbackUrls = fallbackUrls,
                            currentFallbackIndex = 0,
                            error = null,
                        )
                    }
                } else {
                    // No previous channel - wrap around to the last channel in category
                    val channelsInCategory = channelDao.getByCategoryOrdered(state.categoryId)
                    if (channelsInCategory.isNotEmpty()) {
                        val lastChannel = channelsInCategory.last()
                        channelRepository.updateLastWatched(lastChannel.id)
                        
                        val currentProgram = lastChannel.tvgId?.let { epgRepository.getCurrentProgram(it) }
                        val nextProgram = lastChannel.tvgId?.let { epgRepository.getNextProgram(it) }
                        val fallbackUrls = buildStreamFallbacks(lastChannel.streamUrl, lastChannel.containerExtension)

                        _uiState.update {
                            it.copy(
                                title = lastChannel.name,
                                streamUrl = lastChannel.streamUrl,
                                logoUrl = lastChannel.logoUrl,
                                contentId = lastChannel.id,
                                isLoading = false,
                                currentProgram = currentProgram,
                                nextProgram = nextProgram,
                                userAgent = lastChannel.userAgent,
                                fallbackUrls = fallbackUrls,
                                currentFallbackIndex = 0,
                                error = null,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
        return true
    }

    /**
     * Play the next channel in the current category.
     * Used for D-PAD_DOWN channel zapping.
     * Returns true if a next channel was found and loaded.
     */
    fun playNextChannel(): Boolean {
        val state = _uiState.value
        if (state.contentType != WatchContentType.CHANNEL || state.categoryId == null) {
            return false
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val currentChannel = channelDao.getById(state.contentId) ?: return@launch
                val nextChannel = channelDao.getNextChannelInCategory(
                    state.categoryId,
                    state.contentId,
                    currentChannel.sortOrder
                )

                if (nextChannel != null) {
                    // Update state with new channel info
                    channelRepository.updateLastWatched(nextChannel.id)
                    
                    val currentProgram = nextChannel.tvgId?.let { epgRepository.getCurrentProgram(it) }
                    val nextEpgProgram = nextChannel.tvgId?.let { epgRepository.getNextProgram(it) }
                    val fallbackUrls = buildStreamFallbacks(nextChannel.streamUrl, nextChannel.containerExtension)

                    _uiState.update {
                        it.copy(
                            title = nextChannel.name,
                            streamUrl = nextChannel.streamUrl,
                            logoUrl = nextChannel.logoUrl,
                            contentId = nextChannel.id,
                            isLoading = false,
                            currentProgram = currentProgram,
                            nextProgram = nextEpgProgram,
                            userAgent = nextChannel.userAgent,
                            fallbackUrls = fallbackUrls,
                            currentFallbackIndex = 0,
                            error = null,
                        )
                    }
                } else {
                    // No next channel - wrap around to the first channel in category
                    val channelsInCategory = channelDao.getByCategoryOrdered(state.categoryId)
                    if (channelsInCategory.isNotEmpty()) {
                        val firstChannel = channelsInCategory.first()
                        channelRepository.updateLastWatched(firstChannel.id)
                        
                        val currentProgram = firstChannel.tvgId?.let { epgRepository.getCurrentProgram(it) }
                        val nextEpgProgram = firstChannel.tvgId?.let { epgRepository.getNextProgram(it) }
                        val fallbackUrls = buildStreamFallbacks(firstChannel.streamUrl, firstChannel.containerExtension)

                        _uiState.update {
                            it.copy(
                                title = firstChannel.name,
                                streamUrl = firstChannel.streamUrl,
                                logoUrl = firstChannel.logoUrl,
                                contentId = firstChannel.id,
                                isLoading = false,
                                currentProgram = currentProgram,
                                nextProgram = nextEpgProgram,
                                userAgent = firstChannel.userAgent,
                                fallbackUrls = fallbackUrls,
                                currentFallbackIndex = 0,
                                error = null,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
        return true
    }

    // ==================== NEXT EPISODE (Binge Watching) ====================

    /**
     * Load and play the next episode in the series.
     * Used for binge-watching TV series.
     * Returns true if a next episode was found and loaded.
     */
    fun loadNextEpisode(): Boolean {
        val state = _uiState.value
        if (state.contentType != WatchContentType.EPISODE || 
            state.seriesId == null || 
            state.seasonNumber == null || 
            state.episodeNumber == null) {
            return false
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val nextEpisode = episodeDao.getNextEpisode(
                    state.seriesId,
                    state.seasonNumber,
                    state.episodeNumber
                )

                if (nextEpisode != null) {
                    // Save progress of current episode first
                    saveProgress()

                    // Check for progress on next episode
                    val progress = watchProgressRepository.getProgress(
                        state.playlistId, 
                        WatchContentType.EPISODE, 
                        nextEpisode.id
                    )
                    val hasProgress = (progress?.positionMs ?: 0L) > 10_000 && !(progress?.isCompleted ?: true)

                    // Check if there's another episode after this one
                    val hasNext = episodeDao.hasNextEpisode(
                        nextEpisode.seriesId,
                        nextEpisode.seasonNumber,
                        nextEpisode.episodeNumber
                    )

                    _uiState.update {
                        it.copy(
                            title = nextEpisode.name,
                            streamUrl = nextEpisode.streamUrl,
                            logoUrl = nextEpisode.coverUrl,
                            contentId = nextEpisode.id,
                            isLoading = false,
                            resumePositionMs = progress?.positionMs ?: 0L,
                            showResumeDialog = hasProgress,
                            seasonNumber = nextEpisode.seasonNumber,
                            episodeNumber = nextEpisode.episodeNumber,
                            hasNextEpisode = hasNext,
                            error = null,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
        return true
    }

    // ==================== FAVORITES ====================

    /**
     * Toggle favorite status for current channel.
     */
    fun toggleFavorite() {
        val state = _uiState.value
        if (state.contentType != WatchContentType.CHANNEL) return

        viewModelScope.launch {
            try {
                val newFavoriteStatus = !state.isFavorite
                channelDao.setFavorite(state.contentId, newFavoriteStatus)
                _uiState.update { it.copy(isFavorite = newFavoriteStatus) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle favorite")
            }
        }
    }

    // ==================== AUDIO & SUBTITLE TRACKS ====================

    /**
     * Update available audio and subtitle tracks from ExoPlayer.
     */
    fun updateTracks(audioTracks: List<AudioTrackInfo>, subtitleTracks: List<SubtitleTrackInfo>) {
        _uiState.update {
            it.copy(
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
            )
        }
    }

    fun showTrackSelection(show: Boolean) {
        _uiState.update { it.copy(showTrackSelection = show) }
    }

    // ==================== CHANNEL NUMBER INPUT ====================

    /**
     * Add a digit to the channel number input.
     */
    fun inputChannelNumber(digit: String) {
        val current = _uiState.value.channelNumberInput
        if (current.length < 4) {
            _uiState.update { 
                it.copy(
                    channelNumberInput = current + digit,
                    showChannelNumberInput = true,
                )
            }
        }
    }

    /**
     * Clear the channel number input.
     */
    fun clearChannelNumber() {
        _uiState.update { 
            it.copy(
                channelNumberInput = "",
                showChannelNumberInput = false,
            )
        }
    }

    /**
     * Play the channel with the entered number.
     */
    fun playChannelByNumber() {
        val number = _uiState.value.channelNumberInput.toIntOrNull() ?: return
        val state = _uiState.value
        
        viewModelScope.launch {
            try {
                // Find channel by sort order (assuming channel number = sort order)
                val channels = state.categoryId?.let { 
                    channelDao.getByCategoryOrdered(it)
                } ?: emptyList()
                
                // Try to find channel by number (1-indexed)
                val channel = channels.getOrNull(number - 1)
                
                if (channel != null && channel.id != state.contentId) {
                    // Load the channel
                    channelRepository.updateLastWatched(channel.id)
                    
                    val currentProgram = channel.tvgId?.let { epgRepository.getCurrentProgram(it) }
                    val nextProgram = channel.tvgId?.let { epgRepository.getNextProgram(it) }
                    val fallbackUrls = buildStreamFallbacks(channel.streamUrl, channel.containerExtension)

                    _uiState.update {
                        it.copy(
                            title = channel.name,
                            streamUrl = channel.streamUrl,
                            logoUrl = channel.logoUrl,
                            contentId = channel.id,
                            currentProgram = currentProgram,
                            nextProgram = nextProgram,
                            userAgent = channel.userAgent,
                            fallbackUrls = fallbackUrls,
                            currentFallbackIndex = 0,
                            channelNumberInput = "",
                            showChannelNumberInput = false,
                            isFavorite = channel.isFavorite,
                            error = null,
                        )
                    }
                } else {
                    clearChannelNumber()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to play channel by number")
                clearChannelNumber()
            }
        }
    }
}
