package com.djoudini.iplayer.presentation.ui.tv

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.PlayerConfig
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.SettingsViewModel

/**
 * TV-optimized Settings screen.
 * Uses FocusableCard instead of clickable for D-Pad navigation.
 */
@Composable
fun TvSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val playerConfig by viewModel.playerConfig.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val traktEnabled by viewModel.traktEnabled.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Playlist Settings
            TvSettingsSection(title = stringResource(R.string.playlist)) {
                TvSettingsItem(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.refresh_playlist),
                    subtitle = stringResource(R.string.refresh_playlist_desc),
                    onClick = { viewModel.syncPlaylistNow() },
                )
                TvSettingsItem(
                    icon = Icons.Default.Sync,
                    title = stringResource(R.string.sync_epg),
                    subtitle = stringResource(R.string.sync_epg_desc),
                    onClick = { viewModel.syncEpgNow() },
                )
                TvSettingsToggleItem(
                    icon = if (autoSyncEnabled) Icons.Default.CloudSync else Icons.Default.SyncDisabled,
                    title = stringResource(R.string.auto_sync),
                    subtitle = stringResource(R.string.auto_sync_desc),
                    checked = autoSyncEnabled,
                    onCheckedChange = { viewModel.setAutoSync(it) },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Player Settings
            TvSettingsSection(title = stringResource(R.string.player)) {
                TvSettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.user_agent),
                    subtitle = playerConfig.userAgent.take(40),
                    onClick = {
                        val next = when (playerConfig.userAgent) {
                            PlayerConfig.DEFAULT_USER_AGENT -> PlayerConfig.SMART_TV_USER_AGENT
                            PlayerConfig.SMART_TV_USER_AGENT -> PlayerConfig.CHROME_USER_AGENT
                            else -> PlayerConfig.DEFAULT_USER_AGENT
                        }
                        viewModel.setUserAgent(next)
                    },
                )
                TvSettingsItem(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.buffer_size),
                    subtitle = when {
                        playerConfig.maxBufferMs <= 30_000 -> "Minimal (5-30s)"
                        playerConfig.maxBufferMs <= 60_000 -> "Ausgewogen (15-60s)"
                        playerConfig.maxBufferMs <= 120_000 -> "Groß (30-120s)"
                        else -> "Sehr groß (60-240s)"
                    },
                    onClick = {
                        when {
                            playerConfig.maxBufferMs <= 30_000 ->
                                viewModel.setBufferSizes(15_000, 60_000, 2_500, 5_000)
                            playerConfig.maxBufferMs <= 60_000 ->
                                viewModel.setBufferSizes(30_000, 120_000, 5_000, 10_000)
                            playerConfig.maxBufferMs <= 120_000 ->
                                viewModel.setBufferSizes(60_000, 240_000, 10_000, 20_000)
                            else ->
                                viewModel.setBufferSizes(5_000, 30_000, 1_500, 3_000)
                        }
                    },
                )
                TvSettingsToggleItem(
                    icon = Icons.Default.Memory,
                    title = stringResource(R.string.software_decoder),
                    subtitle = stringResource(R.string.software_decoder_desc),
                    checked = playerConfig.preferSoftwareDecoding,
                    onCheckedChange = { viewModel.toggleSoftwareDecoding(it) },
                )
                TvSettingsToggleItem(
                    icon = Icons.Default.Tv,
                    title = stringResource(R.string.tunneled_playback),
                    subtitle = stringResource(R.string.tunneled_playback_desc),
                    checked = playerConfig.enableTunneledPlayback,
                    onCheckedChange = { viewModel.toggleTunneledPlayback(it) },
                )
                TvSettingsItem(
                    icon = Icons.Default.AspectRatio,
                    title = "Video-Format",
                    subtitle = "Im Player einstellbar",
                    onClick = { },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Appearance Settings
            TvSettingsSection(title = stringResource(R.string.appearance)) {
                TvSettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.theme),
                    subtitle = theme.replaceFirstChar { it.uppercase() },
                    onClick = {
                        val next = when (theme) {
                            "system" -> "dark"
                            "dark" -> "light"
                            "light" -> "system"
                            else -> "dark"
                        }
                        viewModel.setTheme(next)
                    },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Data Management
            TvSettingsSection(title = stringResource(R.string.data_management)) {
                TvSettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.clear_watch_history),
                    subtitle = stringResource(R.string.clear_watch_history_desc),
                    onClick = { viewModel.clearWatchHistory() },
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun TvSettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun TvSettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        focusScale = 1.05f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TvSettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    FocusableCard(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        focusScale = 1.05f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (checked) "AN" else "AUS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
