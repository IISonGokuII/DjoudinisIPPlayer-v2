package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.EpisodeDao
import com.djoudini.iplayer.data.local.dao.SeriesDao
import com.djoudini.iplayer.data.local.dao.WatchProgressDao
import com.djoudini.iplayer.data.local.entity.EpisodeEntity
import com.djoudini.iplayer.data.local.entity.SeriesEntity
import com.djoudini.iplayer.data.local.entity.WatchProgressEntity
import com.djoudini.iplayer.data.remote.api.XtreamApi
import com.djoudini.iplayer.domain.model.PlaylistType
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.presentation.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val seriesDao: SeriesDao,
    private val episodeDao: EpisodeDao,
    private val playlistRepository: PlaylistRepository,
    private val xtreamApi: XtreamApi,
    private val watchProgressDao: WatchProgressDao,
) : ViewModel() {

    private val seriesId: Long = try {
        savedStateHandle.get<Long>(NavArgs.SERIES_ID) ?: 0L
    } catch (e: Exception) {
        Timber.e(e, "Failed to get seriesId from SavedStateHandle")
        0L
    }
    
    // FIX: Track active collection jobs to prevent memory leaks
    private var episodesJob: Job? = null
    private var progressJob: Job? = null

    private val _series = MutableStateFlow<SeriesEntity?>(null)
    val series: StateFlow<SeriesEntity?> = _series.asStateFlow()

    val seasons: StateFlow<List<Int>> = episodeDao.observeSeasons(seriesId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _episodes = MutableStateFlow<List<EpisodeEntity>>(emptyList())
    val episodes: StateFlow<List<EpisodeEntity>> = _episodes.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _episodeProgress = MutableStateFlow<Map<Long, WatchProgressEntity>>(emptyMap())
    val episodeProgress: StateFlow<Map<Long, WatchProgressEntity>> = _episodeProgress.asStateFlow()

    init {
        viewModelScope.launch {
            val s = seriesDao.getById(seriesId)
            _series.value = s
            if (s != null) {
                fetchEpisodesFromApi(s)
            }
            _isLoading.value = false
        }
        loadEpisodes(1)
    }

    /**
     * Fetch episodes from Xtream API on-demand and cache them in the local DB.
     * Episodes are not fetched during the initial sync (only series metadata is).
     */
    private suspend fun fetchEpisodesFromApi(series: SeriesEntity) {
        try {
            val playlist = playlistRepository.getActive() ?: return
            if (PlaylistType.fromValue(playlist.type) != PlaylistType.XTREAM) return

            val serverUrl = playlist.serverUrl ?: return
            val username = playlist.username ?: return
            val password = playlist.password ?: return
            val apiUrl = "$serverUrl/player_api.php"

            val response = xtreamApi.getSeriesInfo(
                url = apiUrl,
                username = username,
                password = password,
                seriesId = series.remoteId,
            )

            val episodeEntities = mutableListOf<EpisodeEntity>()
            response.episodes?.forEach { (seasonKey, episodeList) ->
                val seasonNum = seasonKey.toIntOrNull() ?: 1
                for (dto in episodeList) {
                    val epId = dto.id ?: continue
                    val ext = dto.containerExtension ?: "mkv"
                    val streamUrl = "$serverUrl/series/$username/$password/$epId.$ext"
                    episodeEntities.add(
                        EpisodeEntity(
                            playlistId = playlist.id,
                            seriesId = seriesId,
                            remoteId = epId,
                            name = dto.title ?: "Episode ${dto.episodeNum ?: 0}",
                            streamUrl = streamUrl,
                            containerExtension = ext,
                            seasonNumber = dto.season ?: seasonNum,
                            episodeNumber = dto.episodeNum ?: 0,
                            coverUrl = dto.info?.movieImage,
                            plot = dto.info?.plot,
                            durationSeconds = dto.info?.durationSecs,
                            rating = dto.info?.rating?.toFloat(),
                        )
                    )
                }
            }

            if (episodeEntities.isNotEmpty()) {
                episodeDao.deleteBySeries(seriesId)
                episodeDao.insertAll(episodeEntities)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch episodes for series ${series.remoteId}")
        }
    }

    fun loadEpisodes(season: Int) {
        episodesJob?.cancel()
        episodesJob = viewModelScope.launch {
            episodeDao.observeBySeason(seriesId, season).collect { episodes ->
                _episodes.value = episodes
                loadEpisodeProgress(episodes.map { it.id })
            }
        }
    }

    private fun loadEpisodeProgress(episodeIds: List<Long>) {
        if (episodeIds.isEmpty()) return
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            val playlist = playlistRepository.getActive() ?: return@launch
            watchProgressDao.observeEpisodeProgress(playlist.id, episodeIds).collect { progressList ->
                _episodeProgress.value = progressList.associateBy { it.contentId }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel all coroutines to prevent memory leaks
        episodesJob?.cancel()
        episodesJob = null
        progressJob?.cancel()
        progressJob = null
        Timber.d("[SeriesDetail] ViewModel cleared, jobs cancelled")
    }
}
