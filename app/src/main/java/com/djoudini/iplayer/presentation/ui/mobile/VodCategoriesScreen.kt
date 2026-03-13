package com.djoudini.iplayer.presentation.ui.mobile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

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

    LaunchedEffect(categories.size, selectedCategoryId) {
        val logMessage = buildString {
            appendLine("=== VOD CATEGORIES DEBUG ===")
            appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("Categories loaded: ${categories.size}")
            appendLine("Selected category ID: $selectedCategoryId")
            categories.forEach { cat ->
                appendLine("  - Category: ${cat.name} (ID: ${cat.id}, Playlist: ${cat.playlistId})")
            }
        }
        Timber.d(logMessage)
        saveDebugLog(context, "vod_categories_debug.txt", logMessage)
    }

    LaunchedEffect(vodItems.size) {
        val logMessage = buildString {
            appendLine("=== VOD ITEMS DEBUG ===")
            appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("VOD items loaded: ${vodItems.size}")
            if (vodItems.isNotEmpty()) {
                vodItems.take(20).forEach { vod ->
                    appendLine("  - VOD: ${vod.name} (ID: ${vod.id}, Category: ${vod.categoryId})")
                }
                if (vodItems.size > 20) {
                    appendLine("  ... and ${vodItems.size - 20} more")
                }
            } else {
                appendLine("NO VOD ITEMS FOUND!")
            }
        }
        Timber.d(logMessage)
        saveDebugLog(context, "vod_items_debug.txt", logMessage)
    }

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
            // Sidebar - Kategorien
            LazyColumn(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            ) {
                items(categories, key = { it.id }) { category ->
                    val isSelected = category.id == selectedCategoryId
                    Card(
                        onClick = {
                            Timber.d("[VodCategories] Category clicked: ${category.name} (ID: ${category.id})")
                            viewModel.selectCategory(category.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 8.dp else 2.dp,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
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

            VerticalDivider(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            // Content - VOD Grid mit Column (NICHT LazyVerticalGrid!)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                when {
                    selectedCategoryId == 0L -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.select_a_category),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    vodItems.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
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
                                    text = "TIP: Go to Dashboard and sync the playlist",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    else -> {
                        // Einfaches Grid mit Column + Row
                        val columns = 3 // 3 Spalten für Mobile
                        val rows = (vodItems.size + columns - 1) / columns
                        
                        for (rowIndex in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                for (colIndex in 0 until columns) {
                                    val index = rowIndex * columns + colIndex
                                    if (index < vodItems.size) {
                                        VodCard(
                                            vod = vodItems[index],
                                            onClick = { onVodClick(vodItems[index].id) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            if (rowIndex < rows - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
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
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier.width(120.dp).height(200.dp),
        focusScale = 1.05f,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
        ) {
            if (!vod.logoUrl.isNullOrBlank() &&
                (vod.logoUrl.startsWith("http://") || vod.logoUrl.startsWith("https://"))) {
                AsyncImage(
                    model = vod.logoUrl,
                    contentDescription = vod.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp)
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

            Spacer(modifier = Modifier.height(4.dp))

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
        Timber.d("[DebugLog] Saved to: ${logFile.absolutePath}")
    } catch (e: Exception) {
        Timber.e(e, "[DebugLog] Failed to save log")
    }
}
