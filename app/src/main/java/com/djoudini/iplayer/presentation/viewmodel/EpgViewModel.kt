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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelEpgData(
    val channelDbId: Long,
    val channelId: String,
    val channelName: String,
    val programs: List<EpgProgramEntity>,
)

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val channelDao: ChannelDao,
    private val epgRepository: EpgRepository,
) : ViewModel() {

    private val _epgData = MutableStateFlow<List<ChannelEpgData>>(emptyList())
    val epgData: StateFlow<List<ChannelEpgData>> = _epgData.asStateFlow()

    init {
        loadEpg()
    }

    private fun loadEpg() {
        viewModelScope.launch {
            val playlist = playlistRepository.getActive() ?: return@launch

            // Get channels that have tvg_id for EPG matching
            val channels = channelDao.observeByPlaylist(playlist.id).first()
                .filter { !it.tvgId.isNullOrBlank() }
                .take(100) // Limit for performance

            val now = System.currentTimeMillis()
            val endTime = now + 24 * 60 * 60 * 1000 // Next 24 hours

            val epgList = channels.mapNotNull { channel ->
                val programs = epgRepository.getProgramsForRange(
                    channelId = channel.tvgId ?: return@mapNotNull null,
                    fromTime = now - 2 * 60 * 60 * 1000, // 2 hours back
                    toTime = endTime,
                )
                if (programs.isEmpty()) return@mapNotNull null
                ChannelEpgData(
                    channelDbId = channel.id,
                    channelId = channel.tvgId ?: "",
                    channelName = channel.name,
                    programs = programs,
                )
            }

            _epgData.value = epgList
        }
    }
}
