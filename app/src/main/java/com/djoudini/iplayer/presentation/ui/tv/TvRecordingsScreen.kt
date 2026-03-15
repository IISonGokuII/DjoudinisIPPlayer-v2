package com.djoudini.iplayer.presentation.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.RecordingEntity
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.RecordingFilter
import com.djoudini.iplayer.presentation.viewmodel.RecordingSort
import com.djoudini.iplayer.presentation.viewmodel.RecordingsViewModel

@Composable
fun TvRecordingsScreen(
    onBack: () -> Unit,
    onRecordingClick: (Long) -> Unit = {},
    viewModel: RecordingsViewModel = hiltViewModel(),
) {
    val recordings = viewModel.recordings.collectAsStateWithLifecycle().value
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    var deleteCandidate by remember { mutableStateOf<RecordingEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.recordings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.recordings_hint_tv),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RecordingFilter.entries.forEach { entry ->
                FocusableCard(
                    onClick = { viewModel.setFilter(entry) },
                ) {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Text(if (entry == filter) "• ${entry.label}" else entry.label)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RecordingSort.entries.forEach { entry ->
                FocusableCard(
                    onClick = { viewModel.setSort(entry) },
                ) {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Text(if (entry == sort) "• ${entry.label}" else entry.label)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.no_recordings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_recordings_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                FocusableCard(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.back),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            items(recordings, key = { it.id }) { recording ->
                TvRecordingCard(
                    recording = recording,
                    onOpen = { onRecordingClick(recording.id) },
                    onDelete = { deleteCandidate = recording },
                )
            }
        }
    }

    deleteCandidate?.let { recording ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Aufnahme loeschen") },
            text = { Text("'${recording.channelName}' aus der Liste entfernen?") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.deleteRecording(recording.id)
                        deleteCandidate = null
                    },
                ) {
                    Text("Loeschen")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteCandidate = null }) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

@Composable
private fun TvRecordingCard(
    recording: RecordingEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FocusableCard(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            focusScale = 1.03f,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = recording.channelName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = buildString {
                        append(stringResource(R.string.recording_started_at))
                        append(": ")
                        append(
                            java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(recording.startTimeMs)),
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (recording.status) {
                        "completed" -> stringResource(R.string.recording_ready_to_open)
                        "failed" -> stringResource(R.string.recording_failed)
                        else -> stringResource(R.string.recording_in_progress)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (recording.cloudStatus != "local" || recording.cloudProvider.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = buildString {
                            append("Cloud: ")
                            append(
                                when (recording.cloudStatus) {
                                    "queued" -> "Warteschlange"
                                    "uploading" -> "Wird hochgeladen"
                                    "uploaded" -> "Hochgeladen"
                                    "failed" -> "Fehlgeschlagen"
                                    else -> "Lokal"
                                },
                            )
                            if (recording.cloudProvider.isNotBlank()) {
                                append("  |  ")
                                append(recording.cloudProvider)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (recording.cloudStatus == "failed") MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (recording.status == "completed") {
                FocusableCard(onClick = onOpen) {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Text("Oeffnen")
                    }
                }
            }
            FocusableCard(onClick = onDelete) {
                Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Text("Loeschen")
                }
            }
        }
    }
}
