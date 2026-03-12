package com.djoudini.iplayer.presentation.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.CategoryEntity
import com.djoudini.iplayer.data.local.entity.VodEntity
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.ContentListViewModel
import java.io.File

@Composable
fun VodCategoriesScreen(
    onVodClick: (Long) -> Unit = {},
    onBack: () -> Unit,
    viewModel: ContentListViewModel = hiltViewModel(),
) {
    val categories by viewModel.vodCategories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val vodItems by viewModel.filteredVodItems.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Debug: Log category count and selected category
    androidx.compose.runtime.LaunchedEffect(categories.size, selectedCategoryId) {
        val logMessage = buildString {
            appendLine("=== VOD CATEGORIES DEBUG ===")
            appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine("Categories loaded: ${categories.size}")
            appendLine("Selected category ID: $selectedCategoryId")
            categories.forEach { cat ->
                appendLine("  - Category: ${cat.name} (ID: ${cat.id}, Playlist: ${cat.playlistId})")
            }
        }
        timber.log.Timber.d(logMessage)
        
        // Save to file
        saveDebugLog(context, "vod_categories_debug.txt", logMessage)
    }

    // Debug: Log VOD items
    androidx.compose.runtime.LaunchedEffect(vodItems.size) {
        val logMessage = buildString {
            appendLine("=== VOD ITEMS DEBUG ===")
            appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine("VOD items loaded: ${vodItems.size}")
            if (vodItems.isNotEmpty()) {
                vodItems.forEach { vod ->
                    appendLine("  - VOD: ${vod.name} (ID: ${vod.id}, Category: ${vod.categoryId})")
                }
            } else {
                appendLine("NO VOD ITEMS FOUND - Database may be empty or category ID mismatch!")
            }
        }
        timber.log.Timber.d(logMessage)
        
        // Save to file
        saveDebugLog(context, "vod_items_debug.txt", logMessage)
    }

    // Automatische Auswahl der ersten Kategorie wenn keine ausgewählt ist
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCategoryId == 0L) {
            viewModel.selectCategory(categories.first().id)
        }
    }

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
            // Category Sidebar
            LazyColumn(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            ) {
                items(categories, key = { it.id }) { category ->
                    val isSelected = category.id == selectedCategoryId
                    Card(
                        onClick = { viewModel.selectCategory(category.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ),
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
                                modifier = Modifier.size(20.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
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

            VerticalDivider(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            // VOD Grid - FIX: Column entfernt und direkt LazyVerticalGrid verwendet
            if (selectedCategoryId == 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.select_a_category),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (vodItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.no_movies_in_category),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "TIP: Have you synced the playlist? Go to Dashboard → Sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                // FINALE LÖSUNG: LazyVerticalGrid ABER RICHTIG verwendet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            count = vodItems.size,
                            key = { index -> vodItems[index].id },
                        ) { index ->
                            VodCard(
                                vod = vodItems[index],
                                onClick = { onVodClick(vodItems[index].id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VodCard(
    vod: VodEntity,
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(150.dp)
            .height(240.dp),
        focusScale = 1.05f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            if (!vod.logoUrl.isNullOrBlank() && 
                (vod.logoUrl.startsWith("http://") || vod.logoUrl.startsWith("https://"))) {
                AsyncImage(
                    model = vod.logoUrl,
                    contentDescription = vod.name,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                    error = painterResource(id = android.R.drawable.ic_menu_report_image),
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
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = vod.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Save debug log to Downloads folder.
 */
private fun saveDebugLog(context: android.content.Context, fileName: String, content: String) {
    try {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val debugDir = File(downloadsDir, "DjoudinisIPPlayer_Debug")
        if (!debugDir.exists()) {
            debugDir.mkdirs()
        }
        
        val logFile = File(debugDir, fileName)
        logFile.writeText(content)
        
        timber.log.Timber.d("[DebugLog] Saved to: ${logFile.absolutePath}")
    } catch (e: Exception) {
        timber.log.Timber.e(e, "[DebugLog] Failed to save log")
    }
}
