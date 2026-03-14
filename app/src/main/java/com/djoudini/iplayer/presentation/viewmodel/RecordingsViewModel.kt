package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.RecordingDao
import com.djoudini.iplayer.data.local.entity.RecordingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    recordingDao: RecordingDao,
) : ViewModel() {

    val recordings: StateFlow<List<RecordingEntity>> = recordingDao.observeAll()
        .map { recordings -> recordings.sortedByDescending { it.startTimeMs } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
