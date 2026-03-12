package com.djoudini.iplayer.presentation.ui.tv

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
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
import com.djoudini.iplayer.presentation.viewmodel.SortMode
import com.djoudini.iplayer.presentation.viewmodel.ViewMode
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * TV-optimized Live TV categories screen with Outlook-style layout.
 * Left: Category sidebar
 * Right: Channel grid with SearchBar, SortMode, ViewMode
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
    val inlineSearch by viewModel.inlineSearchQuery.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var showPreview by remember { mutableStateOf<ChannelEntity?>(null) }

    // Focus requester for initial focus on sidebar
    val sidebarFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        sidebarFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Category Sidebar (200dp)
            TvCategorySidebar(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onSelectCategory = { viewModel.selectCategory(it) },
                focusRequester = sidebarFocusRequester,
            )

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            // Channel content area - FULL WIDTH, preview overlays on top
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
            // SearchBar with Sort and ViewMode
            TvSearchBar(
                query = inlineSearch,
                onQueryChange = { viewModel.updateInlineSearch(it) },
                placeholder = stringResource(R.string.search_channels),
                viewMode = viewMode,
                onToggleViewMode = { viewModel.cycleViewMode() },
                sortMode = sortMode,
                onToggleSortMode = { viewModel.cycleSortMode() },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Channel grid
            if (selectedCategoryId == 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Keine Kanäle in dieser Kategorie",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${channels.size} Kanäle",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                when (viewMode) {
                    ViewMode.LIST -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(channels, key = { it.channel.id }) { channelWithEpg ->
                                TvChannelListCard(
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
                    ViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
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
                    ViewMode.LARGE_GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            items(channels, key = { it.channel.id }) { channelWithEpg ->
                                TvChannelCard(
                                    channel = channelWithEpg.channel,
                                    currentProgram = channelWithEpg.currentProgram,
                                    nextProgram = channelWithEpg.nextProgram,
                                    timeFormat = timeFormat,
                                    onClick = { onChannelClick(channelWithEpg.channel.id) },
                                    onPreview = { showPreview = channelWithEpg.channel },
                                    large = true,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
        } // End Row

        // Preview Panel als Overlay rechts (nur wenn aktiv)
        // Bleibt sichtbar, Senderliste bleibt voll bedienbar
        if (showPreview != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd),
            ) {
                TvChannelPreviewSidePanel(
                    channel = showPreview!!,
                    onFullscreen = { onChannelClick(showPreview!!.id) },
                    onDismiss = { showPreview = null },
                )
            }
        }
    }
}

@Composable
private fun TvSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit,
    sortMode: SortMode,
    onToggleSortMode: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .focusProperties {
                down = androidx.compose.ui.focus.FocusRequester.Default
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    FocusableCard(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(40.dp),
                        focusScale = 1.0f,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Clear, stringResource(R.string.clear), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            textStyle = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Sort button
        FocusableCard(
            onClick = onToggleSortMode,
            modifier = Modifier
                .height(56.dp)
                .width(100.dp),
            focusScale = 1.05f,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.SortByAlpha, stringResource(R.string.sort), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(sortMode.label, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // ViewMode button
        FocusableCard(
            onClick = onToggleViewMode,
            modifier = Modifier
                .height(56.dp)
                .width(100.dp),
            focusScale = 1.05f,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = when (viewMode) {
                        ViewMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                        ViewMode.GRID -> Icons.Default.GridView
                        ViewMode.LARGE_GRID -> Icons.Default.ViewModule
                    },
                    contentDescription = stringResource(R.string.view_mode),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun TvCategorySidebar(
    categories: List<com.djoudini.iplayer.data.local.entity.CategoryEntity>,
    selectedCategoryId: Long,
    onSelectCategory: (Long) -> Unit,
    focusRequester: FocusRequester,
) {
    var expanded by remember { mutableStateOf(true) }
    val sidebarWidth = if (expanded) 200.dp else 64.dp

    Column(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .animateContentSize(),
    ) {
        // Toggle button
        FocusableCard(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .focusRequester(focusRequester),
            focusScale = 1.0f,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (expanded) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Folder,
                    contentDescription = if (expanded) "Einklappen" else "Ausklappen",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            categories.forEach { category ->
                val isSelected = category.id == selectedCategoryId
                FocusableCard(
                    onClick = { onSelectCategory(category.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    focusScale = 1.0f,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (expanded) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvChannelListCard(
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
            .fillMaxWidth()
            .height(100.dp),
        focusScale = 1.02f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Logo
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Channel info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // EPG info
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
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Preview button
            FocusableCard(
                onClick = onPreview,
                modifier = Modifier
                    .width(100.dp)
                    .height(48.dp),
                focusScale = 1.0f,
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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
private fun TvChannelCard(
    channel: ChannelEntity,
    currentProgram: com.djoudini.iplayer.data.local.entity.EpgProgramEntity?,
    nextProgram: com.djoudini.iplayer.data.local.entity.EpgProgramEntity?,
    timeFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onPreview: () -> Unit,
    large: Boolean = false,
) {
    val cardWidth = if (large) 350.dp else 280.dp
    val cardHeight = if (large) 220.dp else 180.dp
    val logoSize = if (large) 72.dp else 56.dp

    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight),
        focusScale = 1.03f,
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
                            .size(logoSize)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        modifier = Modifier.size(logoSize),
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
                        maxLines = 2,
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
                    .height(48.dp),
                focusScale = 1.0f,
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vorschau",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvChannelPreviewSidePanel(
    channel: ChannelEntity,
    onFullscreen: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Side-Panel als Overlay rechts (350dp breit, semi-transparent)
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(350.dp)
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header mit Schließen-Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            FocusableCard(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp),
                focusScale = 1.05f,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Channel Logo
        if (!channel.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Channel name
        Text(
            text = channel.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Placeholder für Live-Stream
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Live-Vorschau",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Drücke OK für Vollbild",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Vollbild-Button (groß und gut sichtbar)
        FocusableCard(
            onClick = onFullscreen,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
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
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Vollbild öffnen",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
