package com.djoudini.iplayer.presentation.ui.tv

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.data.local.entity.WatchProgressEntity
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.components.ProgressRing
import com.djoudini.iplayer.presentation.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TV-optimized Dashboard with large tiles and D-Pad focus management.
 * 
 * Features:
 * - Continue Watching section
 * - Main tile grid (3x2)
 * - MultiView support
 * - Search, Sync, Settings
 *
 * Focus navigation:
 * - Search button is at the top
 * - Main tiles are in a grid below
 * - Continue Watching at bottom
 */
@Composable
fun TvDashboardScreen(
    onNavigateLive: () -> Unit,
    onNavigateVod: () -> Unit,
    onNavigateSeries: () -> Unit,
    onNavigateEpg: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateMultiView: () -> Unit,
    onNavigateFavorites: () -> Unit,
    onContinueWatchingClick: (contentType: String, contentId: Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val playlist by viewModel.activePlaylist.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val favoriteChannels by viewModel.favoriteChannels.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header with Search and Sync button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist?.name ?: stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                playlist?.expirationDate?.let { expDate ->
                    val formatted = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        .format(Date(expDate))
                    Text(
                        text = stringResource(R.string.account_expires_format, formatted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Search button
            FocusableCard(
                onClick = onNavigateSearch,
                focusScale = 1.1f,
                modifier = Modifier.size(64.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Sync button
            FocusableCard(
                onClick = { viewModel.syncPlaylist() },
                focusScale = 1.1f,
                modifier = Modifier.size(64.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.sync),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Settings button
            FocusableCard(
                onClick = onNavigateSettings,
                focusScale = 1.1f,
                modifier = Modifier.size(64.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Sync progress indicator
            if (syncProgress.isActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(
                        progress = syncProgress.progress,
                        size = 32.dp,
                        strokeWidth = 3.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = syncProgress.phase,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sync progress banner
        if (syncProgress.isActive) {
            SyncProgressBanner(syncProgress)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Continue Watching section - SCROLLABLE to prevent overflow
        if (continueWatching.isNotEmpty()) {
            Text(
                text = stringResource(R.string.continue_watching),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(end = 16.dp),
            ) {
                items(continueWatching, key = { it.id }) { progress ->
                    ContinueWatchingCard(
                        progress = progress,
                        onClick = { onContinueWatchingClick(progress.contentType, progress.contentId) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Main tile grid - 3 columns, 2 rows
        // Row 1: Live TV, Movies, Series
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TvDashboardTile(
                title = stringResource(R.string.live_tv),
                icon = Icons.Default.LiveTv,
                onClick = onNavigateLive,
                modifier = Modifier.weight(1f),
            )
            TvDashboardTile(
                title = stringResource(R.string.movies),
                icon = Icons.Default.Movie,
                onClick = onNavigateVod,
                modifier = Modifier.weight(1f),
            )
            TvDashboardTile(
                title = stringResource(R.string.series),
                icon = Icons.Default.Tv,
                onClick = onNavigateSeries,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Row 2: Favorites, MultiView, EPG Guide
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TvDashboardTile(
                title = stringResource(R.string.favorites),
                icon = Icons.Default.Favorite,
                onClick = onNavigateFavorites,
                modifier = Modifier.weight(1f),
            )
            TvDashboardTile(
                title = stringResource(R.string.multi_view),
                icon = Icons.Default.GridView,
                onClick = onNavigateMultiView,
                modifier = Modifier.weight(1f),
            )
            TvDashboardTile(
                title = stringResource(R.string.epg_guide),
                icon = Icons.Default.CalendarMonth,
                onClick = onNavigateEpg,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Account info card
        playlist?.let { p ->
            AccountInfoCard(
                status = p.status,
                maxConnections = p.maxConnections,
                expirationDate = p.expirationDate,
                lastSynced = p.lastSyncedAt,
            )
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    progress: WatchProgressEntity,
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier.width(200.dp),
        focusScale = 1.05f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Icon based on content type
            androidx.compose.material3.Icon(
                imageVector = when (progress.contentType) {
                    "vod" -> Icons.Default.Movie
                    "episode" -> Icons.Default.Tv
                    "channel" -> Icons.Default.LiveTv
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Content name
            Text(
                text = progress.contentName.ifBlank { 
                    when (progress.contentType) {
                        "channel" -> "Live TV Sender"
                        "vod" -> "Film"
                        "episode" -> "Serie Episode"
                        else -> "ID: ${progress.contentId}"
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Progress percentage
            Text(
                text = "${(progress.progressPercent * 100).toInt()}% angesehen",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TvDashboardTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier.height(160.dp),
        focusScale = 1.08f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SyncProgressBanner(syncProgress: com.djoudini.iplayer.domain.model.SyncProgress) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(
                progress = syncProgress.progress,
                size = 40.dp,
                strokeWidth = 4.dp,
                progressColor = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = syncProgress.phase,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (syncProgress.processedItems > 0) {
                    Text(
                        text = stringResource(R.string.items_processed, syncProgress.processedItems),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                text = "${(syncProgress.progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AccountInfoCard(
    status: String,
    maxConnections: Int?,
    expirationDate: Long?,
    lastSynced: Long,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.account_info),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

            InfoRow(stringResource(R.string.status), status.replaceFirstChar { it.uppercase() })
            maxConnections?.let { InfoRow(stringResource(R.string.max_connections), it.toString()) }
            expirationDate?.let {
                InfoRow(stringResource(R.string.expiration), dateFormat.format(Date(it)))
            }
            if (lastSynced > 0) {
                InfoRow(stringResource(R.string.last_synced), dateFormat.format(Date(lastSynced)))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}
