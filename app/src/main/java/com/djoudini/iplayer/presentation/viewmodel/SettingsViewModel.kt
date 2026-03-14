package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.djoudini.iplayer.data.local.entity.PlayerConfig
import com.djoudini.iplayer.data.local.entity.VpnServer
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.VpnRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val playlistRepository: PlaylistRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val vpnRepository: VpnRepository,
) : ViewModel() {

    var preferredAudioLanguage by mutableStateOf("")
        private set

    var preferredSubtitleLanguage by mutableStateOf("")
        private set

    var _vpnServerId by mutableStateOf("")
        private set

    var _vpnProtocol by mutableStateOf("WIREGUARD")
        private set

    var _vpnKillSwitch by mutableStateOf(false)
        private set

    var _vpnDnsLeakProtection by mutableStateOf(true)
        private set

    var _vpnAutoConnectOnBoot by mutableStateOf(false)
        private set

    var _vpnConnectBeforeStreaming by mutableStateOf(false)
        private set

    var _vpnReconnectDelay by mutableStateOf(5)
        private set

    var availableVpnServers by mutableStateOf<List<VpnServer>>(emptyList())
        private set

    // VPN property accessors
    val vpnServerId: String get() = _vpnServerId
    val vpnProtocol: String get() = _vpnProtocol
    val vpnKillSwitch: Boolean get() = _vpnKillSwitch
    val vpnDnsLeakProtection: Boolean get() = _vpnDnsLeakProtection
    val vpnAutoConnectOnBoot: Boolean get() = _vpnAutoConnectOnBoot
    val vpnConnectBeforeStreaming: Boolean get() = _vpnConnectBeforeStreaming
    val vpnReconnectDelay: Int get() = _vpnReconnectDelay

    fun audioLanguage(): String = preferredAudioLanguage
    fun subtitleLanguage(): String = preferredSubtitleLanguage

    val playerConfig: StateFlow<PlayerConfig> = appPreferences.playerConfig
        .stateIn(viewModelScope, SharingStarted.Lazily, PlayerConfig())

    val autoSyncEnabled: StateFlow<Boolean> = appPreferences.autoSyncEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val theme: StateFlow<String> = appPreferences.theme
        .stateIn(viewModelScope, SharingStarted.Lazily, "dark")

    val vpnEnabled: StateFlow<Boolean> = appPreferences.vpnEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val vpnAutoConnect: StateFlow<Boolean> = appPreferences.vpnAutoConnect
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val vpnConnectionState = vpnRepository.connectionInfo
        .stateIn(viewModelScope, SharingStarted.Lazily, com.djoudini.iplayer.data.local.entity.VpnConnectionInfo())

    init {
        loadVpnServers()
        loadVpnSettings()
    }

    private fun loadVpnServers() {
        viewModelScope.launch {
            vpnRepository.getAllServers().let { servers ->
                availableVpnServers = servers
            }
        }
    }

    private fun loadVpnSettings() {
        viewModelScope.launch {
            appPreferences.vpnServerId.collect {
                _vpnServerId = it
                loadVpnServers()
            }
        }
        viewModelScope.launch {
            appPreferences.vpnProtocol.collect { _vpnProtocol = it }
        }
        viewModelScope.launch {
            appPreferences.vpnKillSwitch.collect { _vpnKillSwitch = it }
        }
        viewModelScope.launch {
            appPreferences.vpnDnsLeakProtection.collect { _vpnDnsLeakProtection = it }
        }
        viewModelScope.launch {
            appPreferences.vpnAutoConnectOnBoot.collect { _vpnAutoConnectOnBoot = it }
        }
        viewModelScope.launch {
            appPreferences.vpnConnectBeforeStreaming.collect { _vpnConnectBeforeStreaming = it }
        }
        viewModelScope.launch {
            appPreferences.vpnReconnectDelay.collect { _vpnReconnectDelay = it }
        }
    }

    fun setUserAgent(userAgent: String) {
        viewModelScope.launch {
            appPreferences.setUserAgent(userAgent)
        }
    }

    fun setBufferSizes(minMs: Int, maxMs: Int, playbackMs: Int, rebufferMs: Int) {
        viewModelScope.launch {
            val current = playerConfig.value
            appPreferences.updatePlayerConfig(
                current.copy(
                    minBufferMs = minMs,
                    maxBufferMs = maxMs,
                    bufferForPlaybackMs = playbackMs,
                    bufferForPlaybackAfterRebufferMs = rebufferMs,
                )
            )
        }
    }

    fun toggleSoftwareDecoding(prefer: Boolean) {
        viewModelScope.launch {
            val current = playerConfig.value
            appPreferences.updatePlayerConfig(current.copy(preferSoftwareDecoding = prefer))
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setAutoSyncEnabled(enabled)
        }
    }

    fun syncPlaylistNow() {
        viewModelScope.launch {
            try {
                val playlistId = playlistRepository.getActive()?.id ?: return@launch
                playlistRepository.syncPlaylist(playlistId)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun syncEpgNow() {
        viewModelScope.launch {
            try {
                val playlistId = playlistRepository.getActive()?.id ?: return@launch
                playlistRepository.syncEpg(playlistId)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            appPreferences.setTheme(theme)
        }
    }

    fun toggleTunneledPlayback(enabled: Boolean) {
        viewModelScope.launch {
            val current = playerConfig.value
            appPreferences.updatePlayerConfig(current.copy(enableTunneledPlayback = enabled))
        }
    }

    fun updatePreferredAudioLanguage(lang: String) {
        preferredAudioLanguage = lang
    }

    fun updatePreferredSubtitleLanguage(lang: String) {
        preferredSubtitleLanguage = lang
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            try {
                val playlistId = playlistRepository.getActive()?.id ?: return@launch
                watchProgressRepository.clearAll(playlistId)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            appPreferences.updatePlayerConfig(PlayerConfig())
            appPreferences.setTheme("dark")
            appPreferences.setAutoSyncEnabled(true)
            preferredAudioLanguage = ""
            preferredSubtitleLanguage = ""
        }
    }

    // ==================== VPN Methods ====================

    fun setVpnEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setVpnEnabled(enabled)
            if (enabled) {
                if (vpnServerId.isNotBlank()) {
                    vpnRepository.connect(vpnServerId)
                }
            } else {
                vpnRepository.disconnect()
            }
        }
    }

    fun setVpnAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setVpnAutoConnect(enabled)
        }
    }

    fun setVpnAutoConnectOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setVpnAutoConnectOnBoot(enabled)
        }
    }

    fun setVpnConnectBeforeStreaming(enabled: Boolean) {
        viewModelScope.launch {
            _vpnConnectBeforeStreaming = enabled
            appPreferences.setVpnConnectBeforeStreaming(enabled)
        }
    }

    fun setVpnServerId(serverId: String) {
        viewModelScope.launch {
            _vpnServerId = serverId
            appPreferences.setVpnServerId(serverId)
            // If already connected, reconnect to new server
            if (vpnRepository.isConnected()) {
                vpnRepository.connect(serverId)
            }
        }
    }

    fun setVpnProtocol(protocol: String) {
        viewModelScope.launch {
            _vpnProtocol = protocol
            appPreferences.setVpnProtocol(protocol)
        }
    }

    fun setVpnKillSwitch(enabled: Boolean) {
        viewModelScope.launch {
            _vpnKillSwitch = enabled
            appPreferences.setVpnKillSwitch(enabled)
        }
    }

    fun setVpnDnsLeakProtection(enabled: Boolean) {
        viewModelScope.launch {
            _vpnDnsLeakProtection = enabled
            appPreferences.setVpnDnsLeakProtection(enabled)
        }
    }

    fun setVpnReconnectDelay(seconds: Int) {
        viewModelScope.launch {
            _vpnReconnectDelay = seconds
            appPreferences.setVpnReconnectDelay(seconds)
        }
    }

    fun connectVpn(serverId: String = vpnServerId) {
        viewModelScope.launch {
            vpnRepository.connect(serverId)
        }
    }

    fun disconnectVpn() {
        viewModelScope.launch {
            vpnRepository.disconnect()
        }
    }

    fun reconnectVpn() {
        viewModelScope.launch {
            vpnRepository.reconnect()
        }
    }

    fun testVpnSpeed(serverId: String) {
        viewModelScope.launch {
            vpnRepository.testServerSpeed(serverId)
        }
    }

    fun pingVpnServer(serverId: String) {
        viewModelScope.launch {
            vpnRepository.pingServer(serverId)
        }
    }

    fun getVpnServerById(serverId: String): VpnServer? {
        return availableVpnServers.find { it.id == serverId }
    }

    fun checkAndAutoConnect() {
        viewModelScope.launch {
            val enabled = vpnEnabled.value
            val autoConnect = vpnAutoConnect.value
            if (enabled && autoConnect && vpnServerId.isNotBlank()) {
                vpnRepository.connect(vpnServerId)
            }
        }
    }
}
