package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.RecordingDao
import com.djoudini.iplayer.data.local.entity.RecordingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RecordingFilter(val label: String) {
    ALL("Alle"),
    READY("Fertig"),
    RECORDING("Aufnahme laeuft"),
    FAILED("Fehlgeschlagen"),
    CLOUD("Cloud"),
}

enum class RecordingSort(val label: String) {
    NEWEST("Neueste"),
    OLDEST("Aelteste"),
    NAME("Name"),
}

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val recordingDao: RecordingDao,
) : ViewModel() {

    private val _filter = MutableStateFlow(RecordingFilter.ALL)
    val filter: StateFlow<RecordingFilter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(RecordingSort.NEWEST)
    val sort: StateFlow<RecordingSort> = _sort.asStateFlow()

    val recordings: StateFlow<List<RecordingEntity>> = combine(
        recordingDao.observeAll(),
        _filter,
        _sort,
    ) { recordings, filter, sort ->
        recordings
            .filterBy(filter)
            .sortBy(sort)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: RecordingFilter) {
        _filter.value = filter
    }

    fun setSort(sort: RecordingSort) {
        _sort.value = sort
    }

    fun deleteRecording(recordingId: Long) {
        viewModelScope.launch {
            recordingDao.deleteById(recordingId)
        }
    }

    private fun List<RecordingEntity>.filterBy(filter: RecordingFilter): List<RecordingEntity> {
        return when (filter) {
            RecordingFilter.ALL -> this
            RecordingFilter.READY -> filter { it.status == "completed" }
            RecordingFilter.RECORDING -> filter { it.status == "recording" }
            RecordingFilter.FAILED -> filter { it.status == "failed" || it.cloudStatus == "failed" }
            RecordingFilter.CLOUD -> filter { it.cloudStatus != "local" || it.cloudProvider.isNotBlank() }
        }
    }

    private fun List<RecordingEntity>.sortBy(sort: RecordingSort): List<RecordingEntity> {
        return when (sort) {
            RecordingSort.NEWEST -> sortedByDescending { it.startTimeMs }
            RecordingSort.OLDEST -> sortedBy { it.startTimeMs }
            RecordingSort.NAME -> sortedBy { it.channelName.lowercase() }
        }
    }
}
