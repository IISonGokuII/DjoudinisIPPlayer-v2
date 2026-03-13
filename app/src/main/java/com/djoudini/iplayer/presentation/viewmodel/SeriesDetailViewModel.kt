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
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

    // FIX: seriesId korrekt aus SavedStateHandle auslesen mit verbessertem Error-Handling
    private val seriesId: Long = savedStateHandle[NavArgs.SERIES_ID] ?: run {
        val allKeys = savedStateHandle.keys()
        val errorMsg = "seriesId is null! Available keys: $allKeys"
        
        // Log to Timber
        Timber.e(errorMsg)
        allKeys.forEach { key ->
            Timber.e("  $key -> ${savedStateHandle.get<Any>(key)}")
        }
        
        // Log to Firebase Crashlytics for remote crash reporting
        FirebaseCrashlytics.getInstance().log(errorMsg)
        FirebaseCrashlytics.getInstance().setCustomKey("available_keys", allKeys.joinToString(", "))
        FirebaseCrashlytics.getInstance().recordException(IllegalStateException(errorMsg))
        
        throw IllegalStateException(errorMsg)
    }

    // FIX: Validate seriesId immediately
    init {
        if (seriesId <= 0) {
            val errorMsg = "Invalid seriesId: $seriesId. Must be > 0"
            Timber.e(errorMsg)
            
            // Log to Firebase Crashlytics
            FirebaseCrashlytics.getInstance().log(errorMsg)
            FirebaseCrashlytics.getInstance().setCustomKey("invalid_series_id", seriesId)
            FirebaseCrashlytics.getInstance().recordException(IllegalStateException(errorMsg))
            
            throw IllegalStateException(errorMsg)
        }
        Timber.d("[SeriesDetailViewModel] seriesId from SavedStateHandle: $seriesId")
        FirebaseCrashlytics.getInstance().log("Loading series with ID: $seriesId")
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
            Timber.d("[SeriesDetailViewModel] Loading series with ID: $seriesId")
            val s = seriesDao.getById(seriesId)
            Timber.d("[SeriesDetailViewModel] Series loaded: ${s?.name ?: "null"}")
            _series.value = s
            if (s != null) {
                Timber.d("[SeriesDetailViewModel] Fetching episodes for series: ${s.name}")
                fetchEpisodesFromApi(s)
            } else {
                Timber.e("[SeriesDetailViewModel] Series not found in database! ID: $seriesId")
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
