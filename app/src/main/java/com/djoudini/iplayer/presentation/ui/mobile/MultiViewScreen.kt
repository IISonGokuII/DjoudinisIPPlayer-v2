package com.djoudini.iplayer.presentation.ui.mobile

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.CategoryEntity
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.presentation.viewmodel.ChannelWithEpg
import com.djoudini.iplayer.presentation.viewmodel.ContentListViewModel

private enum class GridLayout(val count: Int, val cols: Int, val rows: Int, val label: String) {
    SINGLE(1, 1, 1, "1"),
    DUAL(2, 2, 1, "1×2"),
    QUAD(4, 2, 2, "2×2"),
    SIX(6, 3, 2, "2×3"),
    NINE(9, 3, 3, "3×3"),
}

/**
 * Multi-View screen: displays configurable ExoPlayer instances in a grid.
 * One player is "active" (has audio), others are muted.
 * Tap empty slot to open channel picker dialog.
 */
@OptIn(UnstableApi::class)
@Composable
fun MultiViewScreen(
    onBack: () -> Unit,
    viewModel: ContentListViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var gridLayout by remember { mutableStateOf(GridLayout.QUAD) }
    val streams = remember { mutableStateListOf<MultiViewStream?>().apply { repeat(9) { add(null) } } }
    var activeIndex by remember { mutableIntStateOf(0) }
    var showPickerForSlot by remember { mutableStateOf<Int?>(null) }
    var showGridMenu by remember { mutableStateOf(false) }

    // Channel data for picker
    val categories by viewModel.liveCategories.collectAsStateWithLifecycle()
    val channels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.inlineSearchQuery.collectAsStateWithLifecycle()

    // Create up to 9 lightweight players
    val players = remember {
        (0 until 9).map {
            ExoPlayer.Builder(context).build().apply {
                volume = 0f
            }
        }
    }

    // Ensure active player has audio
    players.forEachIndexed { index, player ->
        player.volume = if (index == activeIndex) 1f else 0f
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            players.forEach { it.release() }
        }
    }

    // When grid shrinks, stop players outside the new grid
    fun onGridChange(newLayout: GridLayout) {
        // Stop and clear players that are outside the new grid
        for (i in newLayout.count until gridLayout.count) {
            players[i].stop()
            players[i].clearMediaItems()
            streams[i] = null
        }
        if (activeIndex >= newLayout.count) {
            activeIndex = 0
        }
        gridLayout = newLayout
    }

    // Channel picker dialog
    showPickerForSlot?.let { slotIndex ->
        ChannelPickerDialog(
            categories = categories,
            channels = channels,
            selectedCategoryId = selectedCategoryId,
            searchQuery = searchQuery,
            onSelectCategory = { viewModel.selectCategory(it) },
            onSearchChange = { viewModel.updateInlineSearch(it) },
            onChannelSelected = { channelWithEpg ->
                val channel = channelWithEpg.channel
                val stream = MultiViewStream(
                    name = channel.name,
                    streamUrl = channel.streamUrl,
                    channelId = channel.id,
                )
                streams[slotIndex] = stream

                // Set up player
                players[slotIndex].apply {
                    stop()
                    clearMediaItems()
                    setMediaItem(MediaItem.fromUri(channel.streamUrl))
                    prepare()
                    playWhenReady = true
                }

                showPickerForSlot = null
            },
            onDismiss = { showPickerForSlot = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.multi_view)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    // Grid layout selector
                    Box {
                        IconButton(onClick = { showGridMenu = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(gridLayout.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        DropdownMenu(
                            expanded = showGridMenu,
                            onDismissRequest = { showGridMenu = false },
                        ) {
                            GridLayout.entries.forEach { layout ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${layout.label} (${layout.count})",
                                            fontWeight = if (layout == gridLayout) FontWeight.Bold else FontWeight.Normal,
                                            color = if (layout == gridLayout) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = {
                                        onGridChange(layout)
                                        showGridMenu = false
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
        ) {
            // Dynamic grid
            val cols = gridLayout.cols
            val rows = gridLayout.rows
            for (row in 0 until rows) {
                if (row > 0) {
                    Spacer(modifier = Modifier.height(1.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    for (col in 0 until cols) {
                        val idx = row * cols + col
                        MultiViewCell(
                            player = players[idx],
                            isActive = activeIndex == idx,
                            stream = streams[idx],
                            onSelect = { activeIndex = idx },
                            onRemove = {
                                players[idx].stop()
                                players[idx].clearMediaItems()
                                streams[idx] = null
                            },
                            onAdd = { showPickerForSlot = idx },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun MultiViewCell(
    player: ExoPlayer,
    isActive: Boolean,
    stream: MultiViewStream?,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(2.dp))
            .border(
                width = if (isActive) 2.dp else 0.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(2.dp),
            )
            .clickable(onClick = if (stream != null) onSelect else onAdd),
    ) {
        if (stream != null && player.mediaItemCount > 0) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Stream name overlay
            Text(
                text = stream.name,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )

            // Audio indicator
            Icon(
                imageVector = if (isActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(16.dp),
            )

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp),
            ) {
                Icon(Icons.Default.Close, stringResource(R.string.remove), tint = Color.White, modifier = Modifier.size(16.dp))
            }
        } else {
            // Empty slot - tap to add
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_channel),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.add_channel),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelPickerDialog(
    categories: List<CategoryEntity>,
    channels: List<ChannelWithEpg>,
    selectedCategoryId: Long,
    searchQuery: String,
    onSelectCategory: (Long) -> Unit,
    onSearchChange: (String) -> Unit,
    onChannelSelected: (ChannelWithEpg) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_channel)) },
        text = {
            Column(modifier = Modifier.height(400.dp)) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(stringResource(R.string.search_channels)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category list (compact)
                if (categories.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.height(80.dp)) {
                        items(categories.take(20), key = { it.id }) { cat ->
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (cat.id == selectedCategoryId) FontWeight.Bold else FontWeight.Normal,
                                color = if (cat.id == selectedCategoryId) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCategory(cat.id) }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Channel list
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(channels, key = { it.channel.id }) { channelWithEpg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChannelSelected(channelWithEpg) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!channelWithEpg.channel.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = channelWithEpg.channel.logoUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                )
                            } else {
                                Icon(
                                    Icons.Default.LiveTv,
                                    null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = channelWithEpg.channel.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

data class MultiViewStream(
    val name: String,
    val streamUrl: String,
    val channelId: Long,
)
