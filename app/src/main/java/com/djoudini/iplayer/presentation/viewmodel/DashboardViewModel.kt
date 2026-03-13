package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.ChannelDao
import com.djoudini.iplayer.data.local.dao.EpisodeDao
import com.djoudini.iplayer.data.local.dao.SeriesDao
import com.djoudini.iplayer.data.local.dao.VodDao
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.data.local.entity.PlaylistEntity
import com.djoudini.iplayer.data.local.entity.SeriesEntity
import com.djoudini.iplayer.data.local.entity.VodEntity
import com.djoudini.iplayer.data.local.entity.WatchProgressEntity
import com.djoudini.iplayer.domain.model.SyncProgress
import com.djoudini.iplayer.data.local.entity.VpnConnectionInfo
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.VpnRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val channelDao: ChannelDao,
    private val vodDao: VodDao,
    private val seriesDao: SeriesDao,
    private val episodeDao: EpisodeDao,
    private val appPreferences: AppPreferences,
    private val vpnRepository: VpnRepository,
) : ViewModel() {

    // OPTIMIERUNG: SharingStarted.Lazily für persistenten Cache
    val activePlaylist: StateFlow<PlaylistEntity?> = playlistRepository
        .observeActive()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val continueWatching: StateFlow<List<WatchProgressEntity>> =
        playlistRepository.observeActive().flatMapLatest { playlist ->
            playlist?.let { watchProgressRepository.observeContinueWatching(it.id) }
                ?: flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteChannels: StateFlow<List<ChannelEntity>> =
        playlistRepository.observeActive().flatMapLatest { playlist ->
            playlist?.let { channelDao.observeFavorites(it.id) }
                ?: flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentlyWatched: StateFlow<List<ChannelEntity>> =
        playlistRepository.observeActive().flatMapLatest { playlist ->
            playlist?.let { channelDao.observeRecentlyWatched(it.id) }
                ?: flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteVod: StateFlow<List<VodEntity>> =
        playlistRepository.observeActive().flatMapLatest { playlist ->
            playlist?.let { vodDao.observeFavorites(it.id) }
                ?: flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteSeries: StateFlow<List<SeriesEntity>> =
        playlistRepository.observeActive().flatMapLatest { playlist ->
            playlist?.let { seriesDao.observeFavorites(it.id) }
                ?: flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val syncProgress: StateFlow<SyncProgress> = playlistRepository.syncProgress

    val vpnEnabled: StateFlow<Boolean> = appPreferences.vpnEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val vpnConnectionInfo: StateFlow<VpnConnectionInfo> = vpnRepository.connectionInfo
        .stateIn(viewModelScope, SharingStarted.Lazily, VpnConnectionInfo())

    fun syncPlaylist() {
        val playlistId = activePlaylist.value?.id ?: return
        viewModelScope.launch {
            playlistRepository.syncPlaylist(playlistId)
        }
    }

    fun syncEpg() {
        val playlistId = activePlaylist.value?.id ?: return
        viewModelScope.launch {
            playlistRepository.syncEpg(playlistId)
        }
    }

    fun cancelSync() {
        playlistRepository.cancelSync()
    }

    /**
     * Get content name for display in Continue Watching
     */
    suspend fun getContentName(progress: WatchProgressEntity): String {
        return when (progress.contentType) {
            "channel" -> channelDao.getById(progress.contentId)?.name
            "vod" -> vodDao.getById(progress.contentId)?.name
            "episode" -> episodeDao.getById(progress.contentId)?.name
            "series" -> seriesDao.getById(progress.contentId)?.name
            else -> null
        } ?: progress.contentName.ifBlank { "ID: ${progress.contentId}" }
    }
}
