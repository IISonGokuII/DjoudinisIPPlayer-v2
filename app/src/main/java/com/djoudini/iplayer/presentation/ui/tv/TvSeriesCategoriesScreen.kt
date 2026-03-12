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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.SeriesEntity
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.ContentListViewModel

/**
 * TV-optimized Series categories screen with Outlook-style sidebar.
 * Left: Category sidebar
 * Right: Series grid
 * Designed for D-Pad navigation on Fire TV / Android TV.
 */
@Composable
fun TvSeriesCategoriesScreen(
    onSeriesClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ContentListViewModel = hiltViewModel(),
) {
    val categories by viewModel.seriesCategories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val seriesItems by viewModel.filteredSeriesItems.collectAsStateWithLifecycle()

    val sidebarFocusRequester = remember { FocusRequester() }

    // Automatische Auswahl der ersten Kategorie wenn keine ausgewählt ist
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCategoryId == 0L) {
            viewModel.selectCategory(categories.first().id)
        }
    }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty()) {
            sidebarFocusRequester.requestFocus()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        // Category Sidebar
        TvSeriesCategorySidebar(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onSelectCategory = { viewModel.selectCategory(it) },
            focusRequester = sidebarFocusRequester,
        )

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )

        // Series content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
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
                    text = stringResource(R.string.series),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Series grid
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
            } else if (seriesItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.no_series_in_category),
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
                        text = "${seriesItems.size} Serien",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    modifier = Modifier.weight(1f),
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    contentPadding = PaddingValues(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(seriesItems, key = { it.id }) { series ->
                        TvSeriesCard(
                            series = series,
                            onClick = { onSeriesClick(series.id) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun TvSeriesCategorySidebar(
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

        LazyColumn(
            modifier = Modifier
                .weight(1f),
        ) {
            items(categories, key = { it.id }) { category ->
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
private fun TvSeriesCard(
    series: SeriesEntity,
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(280.dp),
        focusScale = 1.05f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            // Series poster
            if (!series.coverUrl.isNullOrBlank() && 
                (series.coverUrl.startsWith("http://") || series.coverUrl.startsWith("https://"))) {
                AsyncImage(
                    model = series.coverUrl,
                    contentDescription = series.name,
                    contentScale = ContentScale.Crop,
                    placeholder = androidx.compose.ui.res.painterResource(R.drawable.placeholder_image),
                    error = androidx.compose.ui.res.painterResource(R.drawable.error_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Series title
            Text(
                text = series.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Genre if available
            series.genre?.let { genre ->
                Text(
                    text = genre,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
