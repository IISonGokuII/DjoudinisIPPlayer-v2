package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.djoudini.iplayer.data.local.entity.PlayerConfig
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.data.repository.TraktRepository
import com.djoudini.iplayer.data.worker.SyncScheduler
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val playlistRepository: PlaylistRepository,
    private val traktRepository: TraktRepository,
    private val syncScheduler: SyncScheduler,
    private val watchProgressRepository: WatchProgressRepository,
) : ViewModel() {

    // Language preferences (not persisted yet - would need AppPreferences extension)
    var preferredAudioLanguage by mutableStateOf("")
        private set

    var preferredSubtitleLanguage by mutableStateOf("")
        private set

    // Make them readable from UI (different method names to avoid JVM signature clash)
    fun audioLanguage(): String = preferredAudioLanguage
    fun subtitleLanguage(): String = preferredSubtitleLanguage

    // OPTIMIERUNG: SharingStarted.Lazily für persistenten Cache
    val playerConfig: StateFlow<PlayerConfig> = appPreferences.playerConfig
        .stateIn(viewModelScope, SharingStarted.Lazily, PlayerConfig())

    val autoSyncEnabled: StateFlow<Boolean> = appPreferences.autoSyncEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val traktEnabled: StateFlow<Boolean> = appPreferences.traktEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val theme: StateFlow<String> = appPreferences.theme
        .stateIn(viewModelScope, SharingStarted.Lazily, "dark")

    fun setUserAgent(userAgent: String) {
        viewModelScope.launch {
            appPreferences.setUserAgent(userAgent)
        }
    }

    fun setBufferSizes(minMs: Int, maxMs: Int, playbackMs: Int, rebufferMs: Int) {
        viewModelScope.launch {
            val current = playerConfig.value
            appPreferences.updatePlayerConfig(
                current.copy(
                    minBufferMs = minMs,
                    maxBufferMs = maxMs,
                    bufferForPlaybackMs = playbackMs,
                    bufferForPlaybackAfterRebufferMs = rebufferMs,
                )
            )
        }
    }

    fun toggleSoftwareDecoding(prefer: Boolean) {
        viewModelScope.launch {
            val current = playerConfig.value
            appPreferences.updatePlayerConfig(current.copy(preferSoftwareDecoding = prefer))
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setAutoSyncEnabled(enabled)
            if (enabled) {
                syncScheduler.schedulePeriodicPlaylistSync()
                syncScheduler.schedulePeriodicEpgSync()
            } else {
                syncScheduler.cancelAll()
            }
        }
    }

    fun syncPlaylistNow() {
        viewModelScope.launch {
            val playlistId = playlistRepository.getActive()?.id ?: return@launch
            playlistRepository.syncPlaylist(playlistId)
        }
    }

    fun syncEpgNow() {
        viewModelScope.launch {
            val playlistId = playlistRepository.getActive()?.id ?: return@launch
            playlistRepository.syncEpg(playlistId)
        }
    }

    fun disconnectTrakt() {
        viewModelScope.launch {
            traktRepository.disconnect()
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            appPreferences.setTheme(theme)
        }
    }

    fun toggleTunneledPlayback(enabled: Boolean) {
        viewModelScope.launch {
            val current = playerConfig.value
            appPreferences.updatePlayerConfig(current.copy(enableTunneledPlayback = enabled))
        }
    }

    fun updatePreferredAudioLanguage(lang: String) {
        preferredAudioLanguage = lang
    }

    fun updatePreferredSubtitleLanguage(lang: String) {
        preferredSubtitleLanguage = lang
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            val playlistId = playlistRepository.getActive()?.id ?: return@launch
            watchProgressRepository.clearAll(playlistId)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            appPreferences.updatePlayerConfig(PlayerConfig())
            appPreferences.setTheme("dark")
            appPreferences.setAutoSyncEnabled(true)
            preferredAudioLanguage = ""
            preferredSubtitleLanguage = ""
        }
    }
}
