package com.djoudini.iplayer.presentation.ui.tv

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
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
import com.djoudini.iplayer.data.local.entity.VodEntity
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.ContentListViewModel
import timber.log.Timber

@Composable
fun TvVodCategoriesScreen(
    onVodClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ContentListViewModel = hiltViewModel(),
) {
    val categories by viewModel.vodCategories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val vodItems by viewModel.filteredVodItems.collectAsStateWithLifecycle()

    val sidebarFocusRequester = remember { FocusRequester() }

    LaunchedEffect(categories.size) {
        Timber.d("[TvVod] Categories loaded: ${categories.size}")
    }

    LaunchedEffect(vodItems.size) {
        Timber.d("[TvVod] VOD items loaded: ${vodItems.size}")
    }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCategoryId == 0L) {
            viewModel.selectCategory(categories.first().id)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        // Sidebar: Kategorien
        TvVodCategorySidebar(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onSelectCategory = { viewModel.selectCategory(it) },
            focusRequester = sidebarFocusRequester,
        )

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )

        // Content: VOD Grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            // Header
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
                    text = stringResource(R.string.movies),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                selectedCategoryId == 0L -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.select_a_category),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                vodItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_movies_in_category),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> {
                    // VOD Grid - 4 Spalten für TV
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(vodItems.size, key = { index -> vodItems[index].id }) { index ->
                            TvVodCard(
                                vod = vodItems[index],
                                onClick = { onVodClick(vodItems[index].id) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun TvVodCategorySidebar(
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
            modifier = Modifier.weight(1f),
        ) {
            items(categories.size, key = { index -> categories[index].id }) { index ->
                val category = categories[index]
                val isSelected = category.id == selectedCategoryId
                FocusableCard(
                    onClick = {
                        Timber.d("[TvVod] Category clicked: ${category.name} (ID=${category.id})")
                        onSelectCategory(category.id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    focusScale = 1.05f,
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
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (expanded) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvVodCard(
    vod: VodEntity,
    onClick: () -> Unit,
) {
    Timber.d("[TvVodCard] Creating card for: ${vod.name} (ID=${vod.id})")
    FocusableCard(
        onClick = {
            Timber.d("[TvVodCard] Card clicked: ${vod.name} (ID=${vod.id})")
            onClick()
        },
        modifier = Modifier
            .width(200.dp)
            .height(300.dp),
        focusScale = 1.05f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            // Movie poster
            if (!vod.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = vod.logoUrl,
                    contentDescription = vod.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Movie title
            Text(
                text = vod.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Release year
            vod.year?.let { year ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
