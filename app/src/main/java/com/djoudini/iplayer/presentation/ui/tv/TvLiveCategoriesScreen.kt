package com.djoudini.iplayer.presentation.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.ContentListViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * TV-optimized Live TV categories screen.
 * Shows channel grid with EPG preview for each channel.
 * Designed for D-Pad navigation on Fire TV / Android TV.
 */
@Composable
fun TvLiveCategoriesScreen(
    onChannelClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ContentListViewModel = hiltViewModel(),
) {
    val categories by viewModel.liveCategories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val channels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var showPreview by remember { mutableStateOf<ChannelEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FocusableCard(
                    onClick = onBack,
                    modifier = Modifier.size(64.dp),
                    focusScale = 1.1f,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Text(
                    text = stringResource(R.string.live_tv),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Category selection (horizontal scroll)
            if (categories.isNotEmpty()) {
                Text(
                    text = "Kategorie wählen:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    categories.forEach { category ->
                        val isSelected = category.id == selectedCategoryId
                        FocusableCard(
                            onClick = { viewModel.selectCategory(category.id) },
                            modifier = Modifier
                                .width(200.dp)
                                .height(64.dp),
                            focusScale = 1.05f,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Channel grid
            if (selectedCategoryId == 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.select_a_category),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (channels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Keine Kanäle in dieser Kategorie",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = "${channels.size} Kanäle",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(channels, key = { it.channel.id }) { channelWithEpg ->
                        TvChannelCard(
                            channel = channelWithEpg.channel,
                            currentProgram = channelWithEpg.currentProgram,
                            nextProgram = channelWithEpg.nextProgram,
                            timeFormat = timeFormat,
                            onClick = { onChannelClick(channelWithEpg.channel.id) },
                            onPreview = { showPreview = channelWithEpg.channel },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Preview overlay
        showPreview?.let { channel ->
            TvChannelPreviewOverlay(
                channel = channel,
                onFullscreen = { onChannelClick(channel.id) },
                onDismiss = { showPreview = null },
            )
        }
    }
}

@Composable
private fun TvChannelCard(
    channel: ChannelEntity,
    currentProgram: com.djoudini.iplayer.data.local.entity.EpgProgramEntity?,
    nextProgram: com.djoudini.iplayer.data.local.entity.EpgProgramEntity?,
    timeFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onPreview: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(280.dp)
            .height(180.dp),
        focusScale = 1.05f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Channel logo/name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current program
            if (currentProgram != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${timeFormat.format(currentProgram.startTime)} ${currentProgram.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Preview button
            FocusableCard(
                onClick = onPreview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                focusScale = 1.05f,
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vorschau",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvChannelPreviewOverlay(
    channel: ChannelEntity,
    onFullscreen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
    ) {
        // This would contain the actual player
        // For now, just show channel info
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FocusableCard(
                    onClick = onFullscreen,
                    modifier = Modifier
                        .width(200.dp)
                        .height(64.dp),
                    focusScale = 1.05f,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Vollbild",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                FocusableCard(
                    onClick = onDismiss,
                    modifier = Modifier
                        .width(200.dp)
                        .height(64.dp),
                    focusScale = 1.05f,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Schließen",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
