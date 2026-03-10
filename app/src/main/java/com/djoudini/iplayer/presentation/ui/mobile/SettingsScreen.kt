package com.djoudini.iplayer.presentation.ui.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.PlayerConfig
import com.djoudini.iplayer.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val playerConfig by viewModel.playerConfig.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val traktEnabled by viewModel.traktEnabled.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection(title = stringResource(R.string.playlist)) {
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.refresh_playlist),
                    subtitle = stringResource(R.string.refresh_playlist_desc),
                    onClick = { viewModel.syncPlaylistNow() },
                )
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = stringResource(R.string.sync_epg),
                    subtitle = stringResource(R.string.sync_epg_desc),
                    onClick = { viewModel.syncEpgNow() },
                )
                SettingsToggleItem(
                    icon = if (autoSyncEnabled) Icons.Default.CloudSync else Icons.Default.SyncDisabled,
                    title = stringResource(R.string.auto_sync),
                    subtitle = stringResource(R.string.auto_sync_desc),
                    checked = autoSyncEnabled,
                    onCheckedChange = { viewModel.setAutoSync(it) },
                )
            }

            SettingsSection(title = stringResource(R.string.player)) {
                SettingsItem(
                    icon = Icons.Default.PhoneAndroid,
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
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.buffer_size),
                    subtitle = stringResource(R.string.buffer_size_format, playerConfig.minBufferMs / 1000, playerConfig.maxBufferMs / 1000),
                    onClick = {
                        when {
                            playerConfig.maxBufferMs <= 30_000 ->
                                viewModel.setBufferSizes(15_000, 60_000, 2_500, 5_000)
                            playerConfig.maxBufferMs <= 60_000 ->
                                viewModel.setBufferSizes(30_000, 120_000, 5_000, 10_000)
                            else ->
                                viewModel.setBufferSizes(5_000, 30_000, 1_500, 3_000)
                        }
                    },
                )
                SettingsToggleItem(
                    icon = Icons.Default.Memory,
                    title = stringResource(R.string.software_decoder),
                    subtitle = stringResource(R.string.software_decoder_desc),
                    checked = playerConfig.preferSoftwareDecoding,
                    onCheckedChange = { viewModel.toggleSoftwareDecoding(it) },
                )
                SettingsToggleItem(
                    icon = Icons.Default.Tv,
                    title = stringResource(R.string.tunneled_playback),
                    subtitle = stringResource(R.string.tunneled_playback_desc),
                    checked = playerConfig.enableTunneledPlayback,
                    onCheckedChange = { viewModel.toggleTunneledPlayback(it) },
                )
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.preferred_audio_language),
                    subtitle = viewModel.preferredAudioLanguage.ifBlank { stringResource(R.string.auto_system_default) },
                    onClick = {
                        val next = when (viewModel.preferredAudioLanguage) {
                            "" -> "de"
                            "de" -> "en"
                            "en" -> "tr"
                            "tr" -> "fr"
                            "fr" -> "es"
                            else -> ""
                        }
                        viewModel.updatePreferredAudioLanguage(next)
                    },
                )
                SettingsItem(
                    icon = Icons.Default.Subtitles,
                    title = stringResource(R.string.preferred_subtitle_language),
                    subtitle = viewModel.preferredSubtitleLanguage.ifBlank { stringResource(R.string.off) },
                    onClick = {
                        val next = when (viewModel.preferredSubtitleLanguage) {
                            "" -> "de"
                            "de" -> "en"
                            "en" -> "tr"
                            "tr" -> "fr"
                            "fr" -> ""
                            else -> ""
                        }
                        viewModel.updatePreferredSubtitleLanguage(next)
                    },
                )
            }

            SettingsSection(title = stringResource(R.string.trakt_tv)) {
                SettingsToggleItem(
                    icon = Icons.Default.Speed,
                    title = if (traktEnabled) stringResource(R.string.connected) else stringResource(R.string.connect_trakt),
                    subtitle = if (traktEnabled) stringResource(R.string.watch_progress_synced) else stringResource(R.string.sync_watched_content),
                    checked = traktEnabled,
                    onCheckedChange = {
                        if (traktEnabled) viewModel.disconnectTrakt()
                    },
                )
            }

            SettingsSection(title = stringResource(R.string.appearance)) {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.theme),
                    subtitle = theme.replaceFirstChar { it.uppercase() },
                    onClick = {
                        val next = when (theme) {
                            "system" -> "dark"
                            "dark" -> "light"
                            else -> "system"
                        }
                        viewModel.setTheme(next)
                    },
                )
            }

            SettingsSection(title = stringResource(R.string.data_management)) {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.clear_watch_history),
                    subtitle = stringResource(R.string.clear_watch_history_desc),
                    onClick = { viewModel.clearWatchHistory() },
                )
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.reset_all_settings),
                    subtitle = stringResource(R.string.reset_all_settings_desc),
                    onClick = { viewModel.resetSettings() },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
