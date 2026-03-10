package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.VodDao
import com.djoudini.iplayer.data.local.entity.VodEntity
import com.djoudini.iplayer.domain.model.WatchContentType
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import com.djoudini.iplayer.presentation.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vodDao: VodDao,
    private val playlistRepository: PlaylistRepository,
    private val watchProgressRepository: WatchProgressRepository,
) : ViewModel() {

    private val vodId: Long = savedStateHandle.get<Long>(NavArgs.CONTENT_ID) ?: 0L

    private val _vod = MutableStateFlow<VodEntity?>(null)
    val vod: StateFlow<VodEntity?> = _vod.asStateFlow()

    private val _resumePositionMs = MutableStateFlow(0L)
    val resumePositionMs: StateFlow<Long> = _resumePositionMs.asStateFlow()

    init {
        viewModelScope.launch {
            _vod.value = vodDao.getById(vodId)
            val playlist = playlistRepository.getActive()
            if (playlist != null) {
                val progress = watchProgressRepository.getProgress(
                    playlist.id, WatchContentType.VOD, vodId
                )
                _resumePositionMs.value = progress?.positionMs ?: 0L
            }
        }
    }
}
