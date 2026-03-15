package com.djoudini.iplayer.presentation.ui.tv

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.ContentListViewModel
import com.djoudini.iplayer.presentation.viewmodel.ChannelWithEpg
import com.djoudini.iplayer.presentation.viewmodel.ViewMode
import com.djoudini.iplayer.util.AutoFrameRateManager
import androidx.media3.common.Player
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * TV-optimized Live TV screen with IPTV Smarters Pro style layout.
 * 
 * Layout:
 * - Left: Category sidebar (~250dp)
 * - Middle: Channel list with EPG (~30% of remaining space)
 * - Right: Live preview (large, ~50-60% of screen)
 * 
 * Designed for Fire TV / Android TV with D-Pad navigation.
 */
@OptIn(UnstableApi::class)
@Composable
fun TvLiveCategoriesScreen(
    onChannelClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ContentListViewModel = hiltViewModel(),
) {
    val categories by viewModel.liveCategories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val channels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val inlineSearch by viewModel.inlineSearchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var selectedChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity
    val autoFrameRateManager = remember { AutoFrameRateManager() }
    val firstCategoryFocusRequester = remember { FocusRequester() }

    // Initialize player once
    LaunchedEffect(Unit) {
        exoPlayer = ExoPlayer.Builder(context).build()
        // Set initial D-pad focus to first category item, not the search TextField
        try { firstCategoryFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    // Update player when channel changes (preview only, NOT fullscreen)
    LaunchedEffect(selectedChannel) {
        selectedChannel?.let { channel ->
            exoPlayer?.apply {
                stop()
                clearMediaItems()
                setMediaItem(MediaItem.fromUri(channel.streamUrl))
                prepare()
                playWhenReady = true  // Play in preview
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { autoFrameRateManager.restoreOriginalFrameRate(it) }
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    DisposableEffect(exoPlayer, activity) {
        val player = exoPlayer
        val currentActivity = activity
        if (player == null || currentActivity == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState != Player.STATE_READY) return
                    val frameRate = player.videoFormat?.frameRate ?: return
                    if (frameRate > 0f) {
                        autoFrameRateManager.matchFrameRate(currentActivity, frameRate)
                    }
                }
            }
            player.addListener(listener)
            onDispose {
                player.removeListener(listener)
                autoFrameRateManager.restoreOriginalFrameRate(currentActivity)
            }
        }
    }

    // Auto-select first channel when category changes
    LaunchedEffect(selectedCategoryId, channels) {
        if (selectedChannel == null && channels.isNotEmpty()) {
            selectedChannel = channels.first().channel
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // === LEFT: Category Sidebar (250dp) ===
        Column(
            modifier = Modifier
                .width(250.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = stringResource(R.string.live_tv),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            // Search
            OutlinedTextField(
                value = inlineSearch,
                onValueChange = { viewModel.updateInlineSearch(it) },
                placeholder = {
                    Text(
                        "Search categories...",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .height(48.dp)
                    .focusProperties { canFocus = false },
                textStyle = MaterialTheme.typography.bodySmall,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            // Category list
            LazyColumn(
                modifier = Modifier
                    .weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // "All" category
                item {
                    CategoryItem(
                        name = "ALL",
                        count = channels.size,
                        isSelected = selectedCategoryId == 0L,
                        onClick = { viewModel.selectCategory(0L) },
                        focusRequester = firstCategoryFocusRequester,
                    )
                }

                items(categories, key = { it.id }) { category ->
                    CategoryItem(
                        name = category.name,
                        count = null,
                        isSelected = category.id == selectedCategoryId,
                        onClick = { viewModel.selectCategory(category.id) },
                    )
                }
            }
        }

        // === MIDDLE: Channel List (30% of remaining space) ===
        Column(
            modifier = Modifier
                .weight(0.46f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Channels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                // Sort button
                IconButton(onClick = { viewModel.cycleSortMode() }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = sortMode.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                // View mode button
                IconButton(onClick = { viewModel.cycleViewMode() }) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.LIST) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "View mode",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            // Channel list
            if (channels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No channels in this category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(channels, key = { it.channel.id }) { channelWithEpg ->
                        ChannelItem(
                            channel = channelWithEpg.channel,
                            currentProgram = channelWithEpg.currentProgram,
                            timeFormat = timeFormat,
                            isSelected = selectedChannel?.id == channelWithEpg.channel.id,
                            compact = viewMode != ViewMode.LIST,
                            onClick = {
                                if (selectedChannel?.id == channelWithEpg.channel.id) {
                                    // Already selected - open fullscreen
                                    onChannelClick(channelWithEpg.channel.id)
                                } else {
                                    // Different channel - change preview
                                    selectedChannel = channelWithEpg.channel
                                }
                            },
                            onToggleFavorite = {
                                viewModel.toggleFavorite(channelWithEpg.channel.id, channelWithEpg.channel.isFavorite)
                            },
                        )
                    }
                }
            }
        }

        // === RIGHT: Live Preview (60% of remaining space) ===
        Box(
            modifier = Modifier
                .weight(0.54f)
                .fillMaxHeight()
                .background(Color.Black),
        ) {
            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Placeholder when no channel selected
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Select a channel to preview",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Current channel info overlay (bottom)
            selectedChannel?.let { channel ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                        )
                        .padding(16.dp),
                ) {
                    Column {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // Current program
                        channels.find { it.channel.id == channel.id }?.currentProgram?.let { program ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = program.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        // Fullscreen hint
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White.copy(alpha = 0.7f),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Press OK for fullscreen",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    name: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier

    Row(
        modifier = focusModifier
            .then(Modifier
                .fillMaxWidth()
                .height(56.dp)
                .focusable(interactionSource = interactionSource)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter)
                    ) {
                        onClick()
                        true
                    } else false
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    }
                )
                .padding(horizontal = 16.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            count?.let {
                Text(
                    text = "$it channels",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChannelItem(
    channel: ChannelEntity,
    currentProgram: EpgProgramEntity?,
    timeFormat: SimpleDateFormat,
    isSelected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val itemHeight = if (compact) 64.dp else 88.dp
    val logoSize = if (compact) 32.dp else 44.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Channel logo or icon
        Box(
            modifier = Modifier
                .size(logoSize)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 22.dp else 32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Channel info
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = channel.name,
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = if (compact) 2 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!compact) {
                currentProgram?.let { program ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Favorite button
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (channel.isFavorite) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen",
                tint = if (channel.isFavorite) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }

        // Playing indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White,
                )
            }
        }
    }
}
