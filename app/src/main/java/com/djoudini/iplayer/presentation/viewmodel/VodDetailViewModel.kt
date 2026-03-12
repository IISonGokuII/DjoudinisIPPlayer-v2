package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.VodDao
import com.djoudini.iplayer.data.local.entity.VodEntity
import com.djoudini.iplayer.data.remote.api.XtreamApi
import com.djoudini.iplayer.data.remote.api.TmdbApi
import com.djoudini.iplayer.domain.model.PlaylistType
import com.djoudini.iplayer.domain.model.WatchContentType
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import com.djoudini.iplayer.presentation.navigation.NavArgs
import com.djoudini.iplayer.presentation.ui.mobile.CastMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class VodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vodDao: VodDao,
    private val playlistRepository: PlaylistRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val xtreamApi: XtreamApi,
    private val tmdbApi: TmdbApi,
) : ViewModel() {

    private val vodId: Long = savedStateHandle.get<Long>(NavArgs.CONTENT_ID) ?: 0L

    private val _vod = MutableStateFlow<VodEntity?>(null)
    val vod: StateFlow<VodEntity?> = _vod.asStateFlow()

    private val _resumePositionMs = MutableStateFlow(0L)
    val resumePositionMs: StateFlow<Long> = _resumePositionMs.asStateFlow()

    init {
        viewModelScope.launch {
            // Load basic info from database
            _vod.value = vodDao.getById(vodId)
            
            // Load detailed info from API if Xtream playlist
            loadDetailedInfo()
            
            // Load watch progress
            val playlist = playlistRepository.getActive()
            if (playlist != null) {
                val progress = watchProgressRepository.getProgress(
                    playlist.id, WatchContentType.VOD, vodId
                )
                _resumePositionMs.value = progress?.positionMs ?: 0L
            }
        }
    }

    /**
     * Load detailed VOD info from Xtream API (plot, cast, director, etc.)
     */
    private suspend fun loadDetailedInfo() {
        try {
            val playlist = playlistRepository.getActive() ?: return
            if (PlaylistType.fromValue(playlist.type) != PlaylistType.XTREAM) return
            
            val vod = vodDao.getById(vodId) ?: return
            if (vod.remoteId.isBlank()) return
            
            val serverUrl = playlist.serverUrl ?: return
            val username = playlist.username ?: return
            val password = playlist.password ?: return
            val apiUrl = "$serverUrl/player_api.php"
            
            Timber.d("Loading VOD info for ${vod.remoteId}")
            val response = xtreamApi.getVodInfo(
                url = apiUrl,
                username = username,
                password = password,
                vodId = vod.remoteId,
            )
            
            // Update database with detailed info
            // response.info is XtreamStreamDto which has plot, cast, director, etc.
            val updatedVod = vod.copy(
                plot = response.info?.plot ?: vod.plot,
                cast = response.info?.cast ?: vod.cast,
                director = response.info?.director ?: vod.director,
                durationSeconds = vod.durationSeconds, // Not available in VOD info response
                rating = response.info?.rating?.toFloatOrNull() ?: vod.rating,
                logoUrl = response.info?.streamIcon ?: vod.logoUrl,
            )
            vodDao.update(updatedVod)
            _vod.value = updatedVod
            
            Timber.d("VOD info loaded successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load VOD info")
            // Continue with basic info - don't block UI
        }
    }

    /**
     * Load cast members from TMDB API.
     */
    suspend fun loadCast(tmdbId: Int): List<CastMember> {
        return tmdbApi.getCast(tmdbId)
    }
}
