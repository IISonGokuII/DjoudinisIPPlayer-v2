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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.PlayerConfig
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import com.djoudini.iplayer.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    appPreferences: AppPreferences,
    playlistRepository: PlaylistRepository,
    watchProgressRepository: WatchProgressRepository,
    viewModel: SettingsViewModel,
    onNavigateToVpnSetup: (() -> Unit)? = null,
    onNavigateToCloudRecordingSettings: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val playerConfig by appPreferences.playerConfig.collectAsStateWithLifecycle(initialValue = PlayerConfig())
    val autoSyncEnabled by appPreferences.autoSyncEnabled.collectAsStateWithLifecycle(initialValue = true)
    val theme by appPreferences.theme.collectAsStateWithLifecycle(initialValue = "dark")
    val defaultStartTab by appPreferences.defaultStartTab.collectAsStateWithLifecycle(initialValue = "live")
    val screenOrientation by appPreferences.screenOrientation.collectAsStateWithLifecycle(initialValue = "auto")
    val gestureControlsEnabled by appPreferences.gestureControlsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val reconnectMaxAttempts by appPreferences.reconnectMaxAttempts.collectAsStateWithLifecycle(initialValue = 3)
    val reconnectDelayMs by appPreferences.reconnectDelayMs.collectAsStateWithLifecycle(initialValue = 3_000)

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
                    onClick = {
                        scope.launch {
                            try {
                                val playlistId = playlistRepository.getActive()?.id ?: return@launch
                                playlistRepository.syncPlaylist(playlistId)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    },
                )
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = stringResource(R.string.sync_epg),
                    subtitle = stringResource(R.string.sync_epg_desc),
                    onClick = {
                        scope.launch {
                            try {
                                val playlistId = playlistRepository.getActive()?.id ?: return@launch
                                playlistRepository.syncEpg(playlistId)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    },
                )
                SettingsToggleItem(
                    icon = if (autoSyncEnabled) Icons.Default.CloudSync else Icons.Default.SyncDisabled,
                    title = stringResource(R.string.auto_sync),
                    subtitle = stringResource(R.string.auto_sync_desc),
                    checked = autoSyncEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            appPreferences.setAutoSyncEnabled(enabled)
                        }
                    },
                )
            }

            SettingsSection(title = stringResource(R.string.player)) {
                SettingsItem(
                    icon = Icons.Default.PhoneAndroid,
                    title = stringResource(R.string.user_agent),
                    subtitle = playerConfig.userAgent.take(40),
                    onClick = {
                        scope.launch {
                            val next = when (playerConfig.userAgent) {
                                PlayerConfig.DEFAULT_USER_AGENT -> PlayerConfig.SMART_TV_USER_AGENT
                                PlayerConfig.SMART_TV_USER_AGENT -> PlayerConfig.CHROME_USER_AGENT
                                else -> PlayerConfig.DEFAULT_USER_AGENT
                            }
                            appPreferences.setUserAgent(next)
                        }
                    },
                )
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.buffer_size),
                    subtitle = when {
                        playerConfig.maxBufferMs <= 30_000 -> "Minimal (5-30s)"
                        playerConfig.maxBufferMs <= 60_000 -> "Ausgewogen (15-60s)"
                        playerConfig.maxBufferMs <= 120_000 -> "Groß (30-120s)"
                        else -> "Sehr groß (60-240s)"
                    },
                    onClick = {
                        scope.launch {
                            val newConfig = when {
                                playerConfig.maxBufferMs <= 30_000 -> PlayerConfig(
                                    minBufferMs = 15_000, maxBufferMs = 60_000,
                                    bufferForPlaybackMs = 2_500, bufferForPlaybackAfterRebufferMs = 5_000
                                )
                                playerConfig.maxBufferMs <= 60_000 -> PlayerConfig(
                                    minBufferMs = 30_000, maxBufferMs = 120_000,
                                    bufferForPlaybackMs = 5_000, bufferForPlaybackAfterRebufferMs = 10_000
                                )
                                playerConfig.maxBufferMs <= 120_000 -> PlayerConfig(
                                    minBufferMs = 60_000, maxBufferMs = 240_000,
                                    bufferForPlaybackMs = 10_000, bufferForPlaybackAfterRebufferMs = 20_000
                                )
                                else -> PlayerConfig(
                                    minBufferMs = 5_000, maxBufferMs = 30_000,
                                    bufferForPlaybackMs = 1_500, bufferForPlaybackAfterRebufferMs = 3_000
                                )
                            }
                            appPreferences.updatePlayerConfig(newConfig)
                        }
                    },
                )
                SettingsToggleItem(
                    icon = Icons.Default.Memory,
                    title = stringResource(R.string.software_decoder),
                    subtitle = stringResource(R.string.software_decoder_desc),
                    checked = playerConfig.preferSoftwareDecoding,
                    onCheckedChange = { prefer ->
                        scope.launch {
                            val newConfig = playerConfig.copy(preferSoftwareDecoding = prefer)
                            appPreferences.updatePlayerConfig(newConfig)
                        }
                    },
                )
                SettingsToggleItem(
                    icon = Icons.Default.Tv,
                    title = stringResource(R.string.tunneled_playback),
                    subtitle = stringResource(R.string.tunneled_playback_desc),
                    checked = playerConfig.enableTunneledPlayback,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            val newConfig = playerConfig.copy(enableTunneledPlayback = enabled)
                            appPreferences.updatePlayerConfig(newConfig)
                        }
                    },
                )
                SettingsItem(
                    icon = Icons.Default.AspectRatio,
                    title = "Video-Format (Aspect Ratio)",
                    subtitle = "Im Player über Button einstellbar",
                    onClick = { },
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

            // === Playback / Reconnect ===
            SettingsSection(title = "Wiedergabe") {
                SettingsItem(
                    icon = Icons.Default.Wifi,
                    title = "Reconnect-Versuche",
                    subtitle = "$reconnectMaxAttempts Versuche bei Stream-Fehler",
                    onClick = {
                        scope.launch {
                            val next = when (reconnectMaxAttempts) {
                                1 -> 3; 3 -> 5; 5 -> 10; else -> 1
                            }
                            appPreferences.setReconnectSettings(next, reconnectDelayMs)
                        }
                    },
                )
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Reconnect-Verzögerung",
                    subtitle = "${reconnectDelayMs / 1_000}s zwischen Versuchen",
                    onClick = {
                        scope.launch {
                            val next = when (reconnectDelayMs) {
                                1_000 -> 3_000; 3_000 -> 5_000; 5_000 -> 10_000; else -> 1_000
                            }
                            appPreferences.setReconnectSettings(reconnectMaxAttempts, next)
                        }
                    },
                )
                SettingsToggleItem(
                    icon = Icons.Default.TouchApp,
                    title = "Gestensteuerung",
                    subtitle = "Wischen für Helligkeit / Lautstärke / Vor- und Zurückspulen",
                    checked = gestureControlsEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            appPreferences.setGestureControlsEnabled(enabled)
                        }
                    },
                )
                SettingsItem(
                    icon = Icons.Default.ScreenRotation,
                    title = "Bildschirmausrichtung",
                    subtitle = when (screenOrientation) {
                        "landscape" -> "Querformat (erzwingen)"
                        "portrait" -> "Hochformat (erzwingen)"
                        else -> "Automatisch (Sensor)"
                    },
                    onClick = {
                        scope.launch {
                            val next = when (screenOrientation) {
                                "auto" -> "landscape"; "landscape" -> "portrait"; else -> "auto"
                            }
                            appPreferences.setScreenOrientation(next)
                        }
                    },
                )
            }

            // === App Behaviour ===
            SettingsSection(title = "App-Verhalten") {
                SettingsItem(
                    icon = Icons.Default.Home,
                    title = "Standard-Starttab",
                    subtitle = when (defaultStartTab) {
                        "live" -> "Live TV"
                        "movies" -> "Filme"
                        "series" -> "Serien"
                        else -> "Live TV"
                    },
                    onClick = {
                        scope.launch {
                            val next = when (defaultStartTab) {
                                "live" -> "movies"; "movies" -> "series"; else -> "live"
                            }
                            appPreferences.setDefaultStartTab(next)
                        }
                    },
                )
            }

            // === VPN Settings ===
            VpnSettingsSection(viewModel = viewModel, onNavigateToVpnSetup = onNavigateToVpnSetup)

            SettingsSection(title = "Cloud-Aufnahmen") {
                SettingsItem(
                    icon = Icons.Default.CloudUpload,
                    title = "Cloud-Verbindung",
                    subtitle = "WebDAV, Google Drive oder OneDrive fuer Aufnahmen",
                    onClick = { onNavigateToCloudRecordingSettings?.invoke() },
                )
            }

            SettingsSection(title = stringResource(R.string.appearance)) {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.theme),
                    subtitle = theme.replaceFirstChar { it.uppercase() },
                    onClick = {
                        scope.launch {
                            val next = when (theme) {
                                "system" -> "dark"
                                "dark" -> "light"
                                else -> "system"
                            }
                            appPreferences.setTheme(next)
                        }
                    },
                )
            }

            SettingsSection(title = stringResource(R.string.data_management)) {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.clear_watch_history),
                    subtitle = stringResource(R.string.clear_watch_history_desc),
                    onClick = {
                        scope.launch {
                            try {
                                val playlistId = playlistRepository.getActive()?.id ?: return@launch
                                watchProgressRepository.clearAll(playlistId)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    },
                )
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.reset_all_settings),
                    subtitle = stringResource(R.string.reset_all_settings_desc),
                    onClick = {
                        scope.launch {
                            appPreferences.updatePlayerConfig(PlayerConfig())
                            appPreferences.setTheme("dark")
                            appPreferences.setAutoSyncEnabled(true)
                            appPreferences.setPreferredAudioLanguage("")
                            appPreferences.setPreferredSubtitleLanguage("")
                        }
                    },
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
