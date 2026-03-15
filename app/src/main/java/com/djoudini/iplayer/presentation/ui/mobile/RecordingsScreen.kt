package com.djoudini.iplayer.presentation.ui.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.RecordingEntity
import com.djoudini.iplayer.presentation.viewmodel.RecordingFilter
import com.djoudini.iplayer.presentation.viewmodel.RecordingSort
import com.djoudini.iplayer.presentation.viewmodel.RecordingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsScreen(
    onBack: () -> Unit,
    onRecordingClick: (Long) -> Unit = {},
    viewModel: RecordingsViewModel = hiltViewModel(),
) {
    val recordings = viewModel.recordings.collectAsStateWithLifecycle().value
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    var deleteCandidate by remember { mutableStateOf<RecordingEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recordings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (recordings.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.no_recordings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_recordings_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilterSortRow(
                    filter = filter,
                    sort = sort,
                    onFilterSelected = viewModel::setFilter,
                    onSortSelected = viewModel::setSort,
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(recordings, key = { it.id }) { recording ->
                        RecordingCard(
                            recording = recording,
                            onClick = {
                                onRecordingClick(recording.id)
                            },
                            onDelete = { deleteCandidate = recording },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    deleteCandidate?.let { recording ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Aufnahme loeschen") },
            text = { Text("Soll '${recording.channelName}' wirklich aus der Aufnahmen-Liste entfernt werden?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecording(recording.id)
                        deleteCandidate = null
                    },
                ) {
                    Text("Loeschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

@Composable
private fun FilterSortRow(
    filter: RecordingFilter,
    sort: RecordingSort,
    onFilterSelected: (RecordingFilter) -> Unit,
    onSortSelected: (RecordingSort) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecordingFilter.entries.forEach { entry ->
                OutlinedButton(onClick = { onFilterSelected(entry) }) {
                    Text(if (entry == filter) "• ${entry.label}" else entry.label)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecordingSort.entries.forEach { entry ->
                OutlinedButton(onClick = { onSortSelected(entry) }) {
                    Text(if (entry == sort) "• ${entry.label}" else entry.label)
                }
            }
        }
    }
}

@Composable
private fun RecordingCard(
    recording: RecordingEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = recording.status == "completed", onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.channelName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatRecordingDate(recording.startTimeMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (recording.status == "recording") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.Red,
                        )
                        Text(
                            text = stringResource(R.string.recording_in_progress),
                            color = Color.Red,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (recording.status == "completed") {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.open_recording),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Loeschen",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = formatDuration(recording.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatFileSize(recording.fileSizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = when (recording.status) {
                        "completed" -> stringResource(R.string.recording_completed)
                        "failed" -> stringResource(R.string.recording_failed)
                        else -> stringResource(R.string.recording_in_progress)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (recording.cloudStatus != "local" || !recording.cloudProvider.isBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
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
                        recording.cloudError?.takeIf { it.isNotBlank() }?.let {
                            append("\n")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (recording.cloudStatus == "failed") Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatRecordingDate(startTimeMs: Long): String {
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(startTimeMs))
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0 min"
    val totalMinutes = durationMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%dh %02dmin", hours, minutes)
    } else {
        String.format(Locale.ROOT, "%d min", minutes)
    }
}

private fun formatFileSize(fileSizeBytes: Long): String {
    if (fileSizeBytes <= 0L) return "0 MB"
    val sizeInMb = fileSizeBytes / (1024f * 1024f)
    return if (sizeInMb >= 1024f) {
        String.format(Locale.ROOT, "%.1f GB", sizeInMb / 1024f)
    } else {
        String.format(Locale.ROOT, "%.0f MB", sizeInMb)
    }
}
