package com.djoudini.iplayer.presentation.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.EpgViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TV-optimized EPG Grid screen.
 * Shows channels with current/next programs in a time grid.
 * Designed for D-Pad navigation on Fire TV / Android TV.
 */
@Composable
fun TvEpgGridScreen(
    onChannelClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: EpgViewModel = hiltViewModel(),
) {
    val epgData by viewModel.epgData.collectAsStateWithLifecycle()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val now = System.currentTimeMillis()
    val gridStartTime = run {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = now - 2 * 60 * 60 * 1000
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val gridEndTime = gridStartTime + 24 * 60 * 60 * 1000
    val totalGridMinutes = ((gridEndTime - gridStartTime) / 60_000).toInt()
    val pixelsPerMinute = 2.dp
    val totalGridWidth = (totalGridMinutes * pixelsPerMinute.value.toInt()).coerceAtLeast(2000).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        if (epgData.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.no_epg_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sync_epg_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Column {
                // Header with back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(32.dp),
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.epg_guide),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Time header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Channel name column header
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Kanal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Scrollable time markers
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        Row(
                            modifier = Modifier.width(totalGridWidth),
                        ) {
                            var markerTime = gridStartTime
                            while (markerTime < gridEndTime) {
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(
                                        text = timeFormat.format(Date(markerTime)),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                    )
                                }
                                markerTime += 60 * 60 * 1000
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Channel rows with programs
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(epgData, key = { it.channelDbId }) { channelEpg ->
                        TvEpgRow(
                            channelEpg = channelEpg,
                            gridStartTime = gridStartTime,
                            gridEndTime = gridEndTime,
                            pixelsPerMinute = pixelsPerMinute,
                            timeFormat = timeFormat,
                            now = now,
                            onClick = { onChannelClick(channelEpg.channelDbId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvEpgRow(
    channelEpg: com.djoudini.iplayer.presentation.viewmodel.ChannelEpgData,
    gridStartTime: Long,
    gridEndTime: Long,
    pixelsPerMinute: androidx.compose.ui.unit.Dp,
    timeFormat: SimpleDateFormat,
    now: Long,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Channel name column (fixed)
        FocusableCard(
            onClick = onClick,
            modifier = Modifier
                .width(200.dp)
                .height(80.dp),
            focusScale = 1.05f,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = channelEpg.channelName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Scrollable programs
        Box(
            modifier = Modifier
                .weight(1f)
                .height(80.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.width(
                    ((gridEndTime - gridStartTime) / 60_000 * pixelsPerMinute.value).dp
                ),
            ) {
                channelEpg.programs.forEach { program ->
                    if (program.stopTime <= gridStartTime || program.startTime >= gridEndTime) return@forEach

                    val clampedStart = program.startTime.coerceAtLeast(gridStartTime)
                    val clampedEnd = program.stopTime.coerceAtMost(gridEndTime)
                    val startMin = ((clampedStart - gridStartTime) / 60_000).toInt()
                    val durationMin = ((clampedEnd - clampedStart) / 60_000).toInt()

                    val width = (durationMin * pixelsPerMinute.value).dp.coerceAtLeast(80.dp)

                    val isCurrent = program.startTime <= now && program.stopTime > now

                    TvEpgProgramBlock(
                        program = program,
                        width = width,
                        isCurrent = isCurrent,
                        timeFormat = timeFormat,
                        onClick = onClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvEpgProgramBlock(
    program: EpgProgramEntity,
    width: androidx.compose.ui.unit.Dp,
    isCurrent: Boolean,
    timeFormat: SimpleDateFormat,
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .height(80.dp)
            .padding(horizontal = 2.dp),
        focusScale = 1.05f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
                .padding(8.dp),
        ) {
            // Time
            Text(
                text = "${timeFormat.format(program.startTime)} - ${timeFormat.format(program.stopTime)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Program title
            Text(
                text = program.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (isCurrent) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "JETZT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
