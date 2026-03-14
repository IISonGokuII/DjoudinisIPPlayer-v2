package com.djoudini.iplayer.presentation.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import com.djoudini.iplayer.presentation.viewmodel.EpgViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val CHANNEL_NAME_WIDTH = 140.dp
private val ROW_HEIGHT = 64.dp
private val PIXELS_PER_MINUTE = 4.dp // 4dp per minute = 240dp per hour
private val TIMELINE_HEIGHT = 32.dp

@Composable
fun EpgGridScreen(
    onChannelClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: EpgViewModel = hiltViewModel(),
) {
    val epgData by viewModel.epgData.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val horizontalScrollState = rememberScrollState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val now = System.currentTimeMillis()
    // Grid starts 2 hours before now
    val gridStartTime = run {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now - 2 * 60 * 60 * 1000
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    // 24-hour window
    val gridEndTime = gridStartTime + 26 * 60 * 60 * 1000
    val totalGridMinutes = ((gridEndTime - gridStartTime) / 60_000).toInt()
    val totalGridWidth = (totalGridMinutes * PIXELS_PER_MINUTE.value).dp

    // Auto-scroll to current time on load
    val nowOffsetMinutes = ((now - gridStartTime) / 60_000).toInt()
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        val scrollPx = with(density) { (nowOffsetMinutes * PIXELS_PER_MINUTE.value).dp.toPx() }
        // Scroll to ~30min before "now" so it's visible
        horizontalScrollState.scrollTo((scrollPx - 200).toInt().coerceAtLeast(0))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.epg_guide)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncEpgNow() }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.sync_epg))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        if (epgData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    if (syncProgress.isActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = syncProgress.phase,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // Time header row
                Row {
                    // Empty corner for channel name column
                    Box(
                        modifier = Modifier
                            .width(CHANNEL_NAME_WIDTH)
                            .height(TIMELINE_HEIGHT)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.channel),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Scrollable time markers
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(TIMELINE_HEIGHT)
                            .horizontalScroll(horizontalScrollState),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(totalGridWidth)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            // Draw hourly markers
                            var markerTime = gridStartTime
                            while (markerTime < gridEndTime) {
                                val offsetMinutes = ((markerTime - gridStartTime) / 60_000).toInt()
                                val offsetDp = (offsetMinutes * PIXELS_PER_MINUTE.value).dp

                                Text(
                                    text = timeFormat.format(Date(markerTime)),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .offset(x = offsetDp + 4.dp)
                                        .padding(top = 8.dp),
                                )
                                markerTime += 60 * 60 * 1000 // 1 hour
                            }

                            // Current time indicator
                            val nowOffset = ((now - gridStartTime) / 60_000).toInt()
                            val nowDp = (nowOffset * PIXELS_PER_MINUTE.value).dp
                            Box(
                                modifier = Modifier
                                    .offset(x = nowDp)
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Channel rows
                LazyColumn {
                    items(epgData, key = { it.channelId }) { channelEpg ->
                        Row(modifier = Modifier.height(ROW_HEIGHT)) {
                            // Fixed channel name column
                            Box(
                                modifier = Modifier
                                    .width(CHANNEL_NAME_WIDTH)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { onChannelClick(channelEpg.channelDbId) }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text = channelEpg.channelName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            // Scrollable program blocks
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .horizontalScroll(horizontalScrollState),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(totalGridWidth)
                                        .fillMaxHeight(),
                                ) {
                                    channelEpg.programs.forEach { program ->
                                        if (program.stopTime <= gridStartTime || program.startTime >= gridEndTime) return@forEach

                                        val clampedStart = program.startTime.coerceAtLeast(gridStartTime)
                                        val clampedEnd = program.stopTime.coerceAtMost(gridEndTime)
                                        val startMin = ((clampedStart - gridStartTime) / 60_000).toInt()
                                        val durationMin = ((clampedEnd - clampedStart) / 60_000).toInt()

                                        val offsetDp = (startMin * PIXELS_PER_MINUTE.value).dp
                                        val widthDp = (durationMin * PIXELS_PER_MINUTE.value).dp.coerceAtLeast(24.dp)

                                        val isCurrent = program.startTime <= now && program.stopTime > now

                                        Box(
                                            modifier = Modifier
                                                .offset(x = offsetDp)
                                                .width(widthDp)
                                                .fillMaxHeight()
                                                .padding(1.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                                .border(
                                                    width = if (isCurrent) 1.dp else 0.5.dp,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(4.dp),
                                                )
                                                .clickable { onChannelClick(channelEpg.channelDbId) }
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${timeFormat.format(Date(program.startTime))} - ${timeFormat.format(Date(program.stopTime))}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                )
                                                Text(
                                                    text = program.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                                                    else MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }

                                    // Current time line
                                    val nowOffset = ((now - gridStartTime) / 60_000).toInt()
                                    val nowDp = (nowOffset * PIXELS_PER_MINUTE.value).dp
                                    Box(
                                        modifier = Modifier
                                            .offset(x = nowDp)
                                            .width(2.dp)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}
