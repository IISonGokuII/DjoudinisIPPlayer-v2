package com.djoudini.iplayer.presentation.ui.mobile

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.CategoryEntity
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.presentation.components.ContentCard
import com.djoudini.iplayer.presentation.components.PosterCard
import com.djoudini.iplayer.presentation.viewmodel.ContentListViewModel
import com.djoudini.iplayer.presentation.viewmodel.ChannelWithEpg
import com.djoudini.iplayer.presentation.viewmodel.SortMode
import com.djoudini.iplayer.presentation.viewmodel.ViewMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Shared collapsible category sidebar composable ---

@Composable
private fun CategorySidebar(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long,
    onSelectCategory: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    val sidebarWidth = if (expanded) 160.dp else 48.dp

    Column(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .animateContentSize(),
    ) {
        // Toggle button
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
        ) {
            Icon(
                imageVector = if (expanded) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(categories, key = { it.id }) { category ->
                val isSelected = category.id == selectedCategoryId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        )
                        .clickable {
                            onSelectCategory(category.id)
                        }
                        .padding(
                            horizontal = if (expanded) 10.dp else 12.dp,
                            vertical = 10.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (expanded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }
        }
    }
}

@Composable
private fun ViewModeIcon(viewMode: ViewMode) {
    Icon(
        imageVector = when (viewMode) {
            ViewMode.LIST -> Icons.Default.ViewList
            ViewMode.GRID -> Icons.Default.GridView
            ViewMode.LARGE_GRID -> Icons.Default.ViewModule
        },
        contentDescription = stringResource(R.string.view_mode),
    )
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit,
    sortMode: SortMode = SortMode.NAME_ASC,
    onToggleSortMode: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .focusProperties {
                // Verhindere, dass man von unten (z.B. der Liste) beim Navigieren nach rechts in die Suche springt.
                // Man muss explizit nach oben navigieren, um in die Suchleiste zu kommen.
                down = FocusRequester.Default
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, stringResource(R.string.clear), modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            textStyle = MaterialTheme.typography.bodySmall,
        )
        IconButton(onClick = onToggleSortMode) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SortByAlpha, stringResource(R.string.sort), modifier = Modifier.size(18.dp))
                Text(sortMode.label, style = MaterialTheme.typography.labelSmall)
            }
        }
        IconButton(onClick = onToggleViewMode) {
            ViewModeIcon(viewMode)
        }
    }
}

// --- Channel Preview Mini-Player (IPTV Smarters Pro Style) ---

private data class PreviewChannel(
    val id: Long,
    val name: String,
    val streamUrl: String,
)

@OptIn(androidx.media3.common.util.UnstableApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun ChannelPreviewPlayer(
    channel: PreviewChannel,
    onFullscreen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    // FIXED: Create player once, update media item when channel changes
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Update media item when channel changes
    LaunchedEffect(channel.id) {
        exoPlayer.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    // IPTV SMARTERS PRO STYLE: Full-width preview at top (60% of screen)
    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        // Preview video - FULL WIDTH at top (60% of screen)
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxWidth()
                .background(Color.Black)
                .clickable { onFullscreen() },
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        // Keep screen on during preview
                        keepScreenOn = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Dismiss button in top-right corner (semi-transparent)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)),
            ) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.close),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Channel name overlay at bottom of preview
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
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
                    Text(
                        text = "Tippen für Vollbild",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }

        // Channel list below preview - scrollable
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(16.dp),
        ) {
            Text(
                text = "Kanal wechseln",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Wähle einen anderen Kanal aus der Liste",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ======================== Live TV ========================

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun LiveCategoriesScreen(
    onCategoryClick: (Long) -> Unit,
    onChannelClick: (Long) -> Unit = {},
    onBack: () -> Unit,
    viewModel: ContentListViewModel = hiltViewModel(),
) {
    val categories by viewModel.liveCategories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val channels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val inlineSearch by viewModel.inlineSearchQuery.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()

    // Preview state
    var previewChannel by remember { mutableStateOf<PreviewChannel?>(null) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.live_tv)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Main content area
            Row(modifier = Modifier.weight(1f)) {
                CategorySidebar(categories, selectedCategoryId) { viewModel.selectCategory(it) }
                VerticalDivider()

                val focusRequester = remember { FocusRequester() }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                ) {
                    SearchBar(inlineSearch, viewModel::updateInlineSearch, stringResource(R.string.search_channels), viewMode,
                        onToggleViewMode = { viewModel.cycleViewMode() },
                        sortMode = sortMode,
                        onToggleSortMode = { viewModel.cycleSortMode() },
                    )

                    if (selectedCategoryId == 0L) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.select_a_category), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        when (viewMode) {
                            ViewMode.LIST -> {
                                LazyColumn(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    items(channels, key = { it.channel.id }) { channelWithEpg ->
                                        val epgNow = channelWithEpg.currentProgram?.let { program ->
                                            "${timeFormat.format(Date(program.startTime))} ${program.title}"
                                        }
                                        val epgNext = channelWithEpg.nextProgram?.let { program ->
                                            "${stringResource(R.string.next_short)}: ${timeFormat.format(Date(program.startTime))} ${program.title}"
                                        }
                                        val epgText = if (epgNow != null && epgNext != null) {
                                            "$epgNow • $epgNext"
                                        } else epgNow ?: epgNext

                                        ContentCard(
                                            name = channelWithEpg.channel.name,
                                            logoUrl = channelWithEpg.channel.logoUrl,
                                            onClick = {
                                                if (previewChannel?.id == channelWithEpg.channel.id) {
                                                    onChannelClick(channelWithEpg.channel.id)
                                                } else {
                                                    previewChannel = PreviewChannel(
                                                        id = channelWithEpg.channel.id,
                                                        name = channelWithEpg.channel.name,
                                                        streamUrl = channelWithEpg.channel.streamUrl,
                                                    )
                                                }
                                            },
                                            isFavorite = channelWithEpg.channel.isFavorite,
                                            onFavoriteClick = { viewModel.toggleFavorite(channelWithEpg.channel.id, channelWithEpg.channel.isFavorite) },
                                            epgNow = epgText,
                                        )
                                    }
                                }
                            }
                            ViewMode.GRID, ViewMode.LARGE_GRID -> {
                                val columns = if (viewMode == ViewMode.GRID) 3 else 2
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(columns),
                                    contentPadding = PaddingValues(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(channels, key = { it.channel.id }) { channelWithEpg ->
                                        PosterCard(
                                            name = channelWithEpg.channel.name,
                                            posterUrl = channelWithEpg.channel.logoUrl,
                                            onClick = {
                                                if (previewChannel?.id == channelWithEpg.channel.id) {
                                                    onChannelClick(channelWithEpg.channel.id)
                                                } else {
                                                    previewChannel = PreviewChannel(
                                                        id = channelWithEpg.channel.id,
                                                        name = channelWithEpg.channel.name,
                                                        streamUrl = channelWithEpg.channel.streamUrl,
                                                    )
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Mini preview player on the right side
            AnimatedVisibility(
                visible = previewChannel != null,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
            ) {
                previewChannel?.let { channel ->
                    ChannelPreviewPlayer(
                        channel = channel,
                        onFullscreen = { onChannelClick(channel.id) },
                        onDismiss = { previewChannel = null },
                    )
                }
            }
        }
    }
}

// ======================== Movies (VOD) ========================

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun VodCategoriesScreen(
    onCategoryClick: (Long) -> Unit,
    onVodClick: (Long) -> Unit = {},
    onBack: () -> Unit,
    viewModel: ContentListViewModel = hiltViewModel(),
) {
    val categories by viewModel.vodCategories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val vodItems by viewModel.filteredVodItems.collectAsStateWithLifecycle()
    val inlineSearch by viewModel.inlineSearchQuery.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.movies)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            CategorySidebar(categories, selectedCategoryId) { viewModel.selectCategory(it) }
            VerticalDivider()

            val focusRequester = remember { FocusRequester() }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            ) {
                SearchBar(inlineSearch, viewModel::updateInlineSearch, stringResource(R.string.search_movies), viewMode,
                    onToggleViewMode = { viewModel.cycleViewMode() },
                    sortMode = sortMode,
                    onToggleSortMode = { viewModel.cycleSortMode() },
                )

                if (selectedCategoryId == 0L) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.select_a_category), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    when (viewMode) {
                        ViewMode.LIST -> {
                            LazyColumn(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(vodItems, key = { it.id }) { vod ->
                                    ContentCard(
                                        name = vod.name,
                                        logoUrl = vod.logoUrl,
                                        subtitle = vod.year?.toString(),
                                        onClick = { onVodClick(vod.id) },
                                    )
                                }
                            }
                        }
                        ViewMode.GRID, ViewMode.LARGE_GRID -> {
                            val columns = if (viewMode == ViewMode.GRID) 3 else 2
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(vodItems, key = { it.id }) { vod ->
                                    PosterCard(
                                        name = vod.name,
                                        posterUrl = vod.logoUrl,
                                        subtitle = vod.year?.toString(),
                                        onClick = { onVodClick(vod.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== Series ========================

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun SeriesCategoriesScreen(
    onCategoryClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit = {},
    onBack: () -> Unit,
    viewModel: ContentListViewModel = hiltViewModel(),
) {
    val categories by viewModel.seriesCategories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val seriesItems by viewModel.filteredSeriesItems.collectAsStateWithLifecycle()
    val inlineSearch by viewModel.inlineSearchQuery.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.series)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            CategorySidebar(categories, selectedCategoryId) { viewModel.selectCategory(it) }
            VerticalDivider()

            val focusRequester = remember { FocusRequester() }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            ) {
                SearchBar(inlineSearch, viewModel::updateInlineSearch, stringResource(R.string.search_series), viewMode,
                    onToggleViewMode = { viewModel.cycleViewMode() },
                    sortMode = sortMode,
                    onToggleSortMode = { viewModel.cycleSortMode() },
                )

                if (selectedCategoryId == 0L) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.select_a_category), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    when (viewMode) {
                        ViewMode.LIST -> {
                            LazyColumn(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(seriesItems, key = { it.id }) { series ->
                                    ContentCard(
                                        name = series.name,
                                        logoUrl = series.coverUrl,
                                        subtitle = series.genre,
                                        onClick = { onSeriesClick(series.id) },
                                    )
                                }
                            }
                        }
                        ViewMode.GRID, ViewMode.LARGE_GRID -> {
                            val columns = if (viewMode == ViewMode.GRID) 3 else 2
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(seriesItems, key = { it.id }) { series ->
                                    PosterCard(
                                        name = series.name,
                                        posterUrl = series.coverUrl,
                                        subtitle = series.genre,
                                        onClick = { onSeriesClick(series.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
