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

    var preferredAudioLanguage by mutableStateOf("")
        private set

    var preferredSubtitleLanguage by mutableStateOf("")
        private set

    val playerConfig: StateFlow<PlayerConfig> = appPreferences.playerConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerConfig())

    val autoSyncEnabled: StateFlow<Boolean> = appPreferences.autoSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val traktEnabled: StateFlow<Boolean> = appPreferences.traktEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val theme: StateFlow<String> = appPreferences.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "dark")

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
