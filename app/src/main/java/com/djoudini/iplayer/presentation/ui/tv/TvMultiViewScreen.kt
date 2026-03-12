package com.djoudini.iplayer.presentation.ui.tv

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
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
import com.djoudini.iplayer.presentation.viewmodel.DashboardViewModel

/**
 * TV-optimized MultiView screen with simplified channel selection.
 * Features:
 * - Quick select from favorites
 * - Quick select from recently watched
 * - Simple channel grid for selection
 * - Up to 4 channels simultaneously
 */
@Composable
fun TvMultiViewScreen(
    onChannelClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ContentListViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
) {
    val channels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val favoriteChannels by dashboardViewModel.favoriteChannels.collectAsStateWithLifecycle()
    val recentlyWatched by dashboardViewModel.recentlyWatched.collectAsStateWithLifecycle()

    var selectedChannels by remember { mutableStateOf<List<ChannelEntity>>(emptyList()) }
    var showChannelPicker by remember { mutableStateOf(false) }
    var pickerSlotIndex by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                            tint = Color.White,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.multi_view),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Channel grid (2x2 for 4 channels max)
            if (selectedChannels.isEmpty()) {
                // Empty state - show quick selection
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Wähle bis zu 4 Kanäle aus",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Favorites quick select
                    if (favoriteChannels.isNotEmpty()) {
                        QuickSelectSection(
                            title = "Favoriten",
                            icon = Icons.Default.Favorite,
                            channels = favoriteChannels.take(8),
                            onSelect = { channel ->
                                selectedChannels = listOf(channel)
                                pickerSlotIndex = 0
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Recently watched quick select
                    if (recentlyWatched.isNotEmpty()) {
                        QuickSelectSection(
                            title = "Zuletzt gesehen",
                            icon = Icons.Default.History,
                            channels = recentlyWatched.take(8),
                            onSelect = { channel ->
                                selectedChannels = listOf(channel)
                                pickerSlotIndex = 0
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // All channels button
                    Spacer(modifier = Modifier.weight(1f))
                    FocusableCard(
                        onClick = {
                            pickerSlotIndex = 0
                            showChannelPicker = true
                        },
                        modifier = Modifier
                            .width(250.dp)
                            .height(80.dp),
                        focusScale = 1.05f,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Alle Kanäle durchsuchen",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                // Channel grid
                val rows = if (selectedChannels.size <= 2) 1 else 2
                val columns = if (selectedChannels.size <= 2) selectedChannels.size else 2

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(selectedChannels, key = { it.id }) { channel ->
                        TvMultiViewCell(
                            channel = channel,
                            onClick = { onChannelClick(channel.id) },
                            onClose = { selectedChannels = selectedChannels.filter { it.id != channel.id } },
                            onReplace = { 
                                pickerSlotIndex = selectedChannels.indexOf(channel)
                                showChannelPicker = true 
                            },
                        )
                    }

                    // Add channel button if less than 4
                    if (selectedChannels.size < 4) {
                        item {
                            AddChannelCard(
                                onClick = {
                                    pickerSlotIndex = selectedChannels.size
                                    showChannelPicker = true
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Instructions
            Text(
                text = "Tipp: Drücke OK auf einem Kanal für Vollbild",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Channel picker overlay
        if (showChannelPicker) {
            TvChannelPickerOverlay(
                favoriteChannels = favoriteChannels,
                recentlyWatched = recentlyWatched,
                allChannels = channels.map { it.channel }.take(50),
                onChannelSelected = { channel ->
                    if (pickerSlotIndex < selectedChannels.size) {
                        selectedChannels = selectedChannels.toMutableList().apply {
                            set(pickerSlotIndex, channel)
                        }
                    } else {
                        selectedChannels = selectedChannels + channel
                    }
                    showChannelPicker = false
                },
                onDismiss = { showChannelPicker = false },
            )
        }
    }
}

@Composable
private fun QuickSelectSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    channels: List<ChannelEntity>,
    onSelect: (ChannelEntity) -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 12.dp),
        ) {
            items(channels, key = { it.id }) { channel ->
                QuickSelectCard(
                    channel = channel,
                    onClick = { onSelect(channel) },
                )
            }
        }
    }
}

@Composable
private fun QuickSelectCard(
    channel: ChannelEntity,
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(100.dp),
        focusScale = 1.05f,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Channel name overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp),
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AddChannelCard(
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .fillMaxWidth(),
        focusScale = 1.05f,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kanal hinzufügen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TvMultiViewCell(
    channel: ChannelEntity,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onReplace: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .fillMaxWidth()
    ) {
        FocusableCard(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            focusScale = 1.05f,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Channel logo/info
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Channel name overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(12.dp),
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Close/Replace buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Replace button
            FocusableCard(
                onClick = onReplace,
                modifier = Modifier.size(40.dp),
                focusScale = 1.0f,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ersetzen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            // Close button
            FocusableCard(
                onClick = onClose,
                modifier = Modifier.size(40.dp),
                focusScale = 1.0f,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // Fullscreen hint
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "OK für Vollbild",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun TvChannelPickerOverlay(
    favoriteChannels: List<ChannelEntity>,
    recentlyWatched: List<ChannelEntity>,
    allChannels: List<ChannelEntity>,
    onChannelSelected: (ChannelEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(48.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Kanal auswählen",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.weight(1f))
                FocusableCard(
                    onClick = onDismiss,
                    modifier = Modifier.size(56.dp),
                    focusScale = 1.1f,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Favorites section (if available)
            if (favoriteChannels.isNotEmpty()) {
                SectionRow(
                    title = "Favoriten",
                    icon = Icons.Default.Favorite,
                    channels = favoriteChannels.take(10),
                    onSelect = onChannelSelected,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Recently watched section (if available)
            if (recentlyWatched.isNotEmpty()) {
                SectionRow(
                    title = "Zuletzt gesehen",
                    icon = Icons.Default.History,
                    channels = recentlyWatched.take(10),
                    onSelect = onChannelSelected,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // All channels section
            Text(
                text = "Alle Kanäle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(allChannels, key = { it.id }) { channel ->
                    ChannelSelectCard(
                        channel = channel,
                        onClick = { onChannelSelected(channel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    channels: List<ChannelEntity>,
    onSelect: (ChannelEntity) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 12.dp),
    ) {
        items(channels, key = { it.id }) { channel ->
            QuickSelectCard(
                channel = channel,
                onClick = { onSelect(channel) },
            )
        }
    }
}

@Composable
private fun ChannelSelectCard(
    channel: ChannelEntity,
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(80.dp),
        focusScale = 1.05f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
