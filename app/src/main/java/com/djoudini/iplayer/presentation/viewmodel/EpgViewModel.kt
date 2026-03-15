package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.ChannelDao
import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import com.djoudini.iplayer.domain.repository.EpgRepository
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.runtime.Immutable
import javax.inject.Inject
import com.djoudini.iplayer.util.EpgChannelIdNormalizer

@Immutable
data class ChannelEpgData(
    val channelDbId: Long,
    val channelId: String,
    val channelName: String,
    val programs: List<EpgProgramEntity>,
)

@Immutable
data class EpgDiagnostics(
    val channelsWithTvgId: Int = 0,
    val channelsWithPrograms: Int = 0,
    val totalPrograms: Int = 0,
    val message: String = "EPG noch nicht geladen.",
)

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val channelDao: ChannelDao,
    private val epgRepository: EpgRepository,
) : ViewModel() {

    private val _epgData = MutableStateFlow<List<ChannelEpgData>>(emptyList())
    val epgData: StateFlow<List<ChannelEpgData>> = _epgData.asStateFlow()
    private val _diagnostics = MutableStateFlow(EpgDiagnostics())
    val diagnostics: StateFlow<EpgDiagnostics> = _diagnostics.asStateFlow()
    val syncProgress = playlistRepository.syncProgress
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, com.djoudini.iplayer.domain.model.SyncProgress.Idle)

    init {
        loadEpg()
    }

    fun reload() {
        loadEpg()
    }

    fun syncEpgNow() {
        viewModelScope.launch {
            val playlist = playlistRepository.getActive() ?: return@launch
            playlistRepository.syncEpg(playlist.id)
            loadEpg()
        }
    }

    private fun loadEpg() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val playlist = playlistRepository.getActive() ?: return@launch

            // Get channels that have tvg_id for EPG matching
            val channels = channelDao.observeByPlaylist(playlist.id).first()
                .filter { !it.tvgId.isNullOrBlank() }
                .take(100) // Limit for performance

            if (channels.isEmpty()) {
                _epgData.value = emptyList()
                _diagnostics.value = EpgDiagnostics(
                    channelsWithTvgId = 0,
                    channelsWithPrograms = 0,
                    totalPrograms = 0,
                    message = "Keine Sender mit tvg-id gefunden.",
                )
                return@launch
            }

            val now = System.currentTimeMillis()
            val endTime = now + 24 * 60 * 60 * 1000 // Next 24 hours

            // FIX: Batch query instead of N+1 queries
            // Old: 100+ individual queries (one per channel)
            // New: 1 query for all channels
            val channelIds = channels.mapNotNull { EpgChannelIdNormalizer.normalize(it.tvgId) }.distinct()
            val programsByChannel = epgRepository.getProgramsForChannels(
                channelIds = channelIds,
                fromTime = now - 2 * 60 * 60 * 1000, // 2 hours back
                toTime = endTime,
            )

            val epgList = channels.mapNotNull { channel ->
                val normalizedTvgId = EpgChannelIdNormalizer.normalize(channel.tvgId) ?: return@mapNotNull null
                val programs = programsByChannel[normalizedTvgId] ?: return@mapNotNull null
                if (programs.isEmpty()) return@mapNotNull null
                ChannelEpgData(
                    channelDbId = channel.id,
                    channelId = normalizedTvgId,
                    channelName = channel.name,
                    programs = programs,
                )
            }

            _epgData.value = epgList
            _diagnostics.value = EpgDiagnostics(
                channelsWithTvgId = channels.size,
                channelsWithPrograms = epgList.size,
                totalPrograms = epgList.sumOf { it.programs.size },
                message = "EPG geladen fuer ${epgList.size} von ${channels.size} Sendern.",
            )
            
            val duration = System.currentTimeMillis() - startTime
            Timber.d("[EPG] Loaded ${epgList.size} channels with EPG data in ${duration}ms (batch query)")
        }
    }
}
