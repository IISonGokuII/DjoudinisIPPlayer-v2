package com.djoudini.iplayer.presentation.ui.mobile

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djoudini.iplayer.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.data.local.entity.VpnState
import com.djoudini.iplayer.data.local.entity.WatchProgressEntity
import com.djoudini.iplayer.domain.model.SyncProgress
import com.djoudini.iplayer.presentation.components.ProgressRing
import com.djoudini.iplayer.presentation.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    onNavigateLive: () -> Unit,
    onNavigateVod: () -> Unit,
    onNavigateSeries: () -> Unit,
    onNavigateEpg: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateMultiView: () -> Unit = {},
    onNavigateFavorites: () -> Unit = {},
    onContinueWatchingClick: (contentType: String, contentId: Long) -> Unit = { _, _ -> },
    onChannelClick: (Long) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val playlist by viewModel.activePlaylist.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val favoriteChannels by viewModel.favoriteChannels.collectAsStateWithLifecycle()
    val recentlyWatched by viewModel.recentlyWatched.collectAsStateWithLifecycle()

    // Current time for display
    var currentTime by remember { mutableStateOf("") }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        currentTime = timeFormat.format(Date())
        // Update time every minute
        while (true) {
            delay(60_000)
            currentTime = timeFormat.format(Date())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = playlist?.name ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        playlist?.expirationDate?.let { expDate ->
                            val formatted = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                                .format(Date(expDate))
                            Text(
                                text = stringResource(R.string.expires_format, formatted),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateSearch) {
                        Icon(Icons.Default.Search, stringResource(R.string.search))
                    }
                    IconButton(onClick = { viewModel.syncPlaylist() }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.sync))
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings))
                    }
                    // Current time display
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Sync progress banner
            if (syncProgress.isActive) {
                SyncProgressBanner(syncProgress)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // VPN Status Banner
            VpnStatusBanner(viewModel = viewModel, onNavigateToSettings = onNavigateSettings)
            Spacer(modifier = Modifier.height(16.dp))

            // Continue Watching section
            if (continueWatching.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.continue_watching),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(continueWatching, key = { it.id }) { progress ->
                        ContinueWatchingCard(
                            progress = progress,
                            onClick = { onContinueWatchingClick(progress.contentType, progress.contentId) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Favorite channels section
            if (favoriteChannels.isNotEmpty()) {
                SectionHeader(icon = Icons.Default.Favorite, title = stringResource(R.string.favorites))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(favoriteChannels, key = { it.id }) { channel ->
                        ChannelCard(channel = channel, onClick = { onChannelClick(channel.id) })
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Recently watched channels section
            if (recentlyWatched.isNotEmpty()) {
                SectionHeader(icon = Icons.Default.History, title = stringResource(R.string.recently_watched))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    items(recentlyWatched, key = { it.id }) { channel ->
                        ChannelCard(channel = channel, onClick = { onChannelClick(channel.id) })
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Main dashboard tiles (2-column grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardTile(
                    title = stringResource(R.string.live_tv),
                    icon = Icons.Default.LiveTv,
                    onClick = onNavigateLive,
                    syncProgress = if (syncProgress.isActive && syncProgress.phase.contains("channel", true)) syncProgress else null,
                    modifier = Modifier.weight(1f),
                )
                DashboardTile(
                    title = stringResource(R.string.movies),
                    icon = Icons.Default.Movie,
                    onClick = onNavigateVod,
                    syncProgress = if (syncProgress.isActive && syncProgress.phase.contains("movie", true)) syncProgress else null,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardTile(
                    title = stringResource(R.string.series),
                    icon = Icons.Default.Tv,
                    onClick = onNavigateSeries,
                    syncProgress = if (syncProgress.isActive && syncProgress.phase.contains("series", true)) syncProgress else null,
                    modifier = Modifier.weight(1f),
                )
                DashboardTile(
                    title = stringResource(R.string.favorites),
                    icon = Icons.Default.Favorite,
                    onClick = onNavigateFavorites,
                    modifier = Modifier.weight(1f),
                    showCount = favoriteChannels.size,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardTile(
                    title = stringResource(R.string.multi_view),
                    icon = Icons.Default.GridView,
                    onClick = onNavigateMultiView,
                    modifier = Modifier.weight(1f),
                )
                DashboardTile(
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
}

@Composable
private fun DashboardTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    syncProgress: SyncProgress? = null,
    showCount: Int? = null,
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                // Show count badge if provided
                showCount?.let { count ->
                    if (count > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$count items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Progress ring overlay during sync
            if (syncProgress != null) {
                ProgressRing(
                    progress = syncProgress.progress,
                    size = 36.dp,
                    strokeWidth = 3.dp,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

@Composable
private fun SyncProgressBanner(syncProgress: SyncProgress) {
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ContinueWatchingCard(
    progress: WatchProgressEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (progress.contentType) {
                        "vod" -> Icons.Default.Movie
                        "episode" -> Icons.Default.Tv
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = progress.contentType.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = progress.contentName.ifBlank { "ID: ${progress.contentId}" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress.progressPercent },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${(progress.progressPercent * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ChannelCard(
    channel: ChannelEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun VpnStatusBanner(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
) {
    val vpnEnabled by viewModel.vpnEnabled.collectAsStateWithLifecycle()
    val vpnConnectionInfo by viewModel.vpnConnectionInfo.collectAsStateWithLifecycle()
    val vpnConnected = vpnConnectionInfo.state is VpnState.Connected

    if (!vpnEnabled) return // Don't show if VPN is not enabled

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToSettings() },
        colors = CardDefaults.cardColors(
            containerColor = if (vpnConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (vpnConnected) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = if (vpnConnected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(24.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.vpn),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (vpnConnected) {
                            stringResource(R.string.vpn_connected)
                        } else {
                            stringResource(R.string.vpn_disconnected)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
