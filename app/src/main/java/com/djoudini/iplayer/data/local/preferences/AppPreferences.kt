package com.djoudini.iplayer.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.djoudini.iplayer.data.local.entity.CloudRecordingProvider
import com.djoudini.iplayer.data.local.entity.CloudRecordingSettings
import com.djoudini.iplayer.data.local.entity.PlayerConfig
import com.djoudini.iplayer.data.local.entity.VpnProtocol
import com.djoudini.iplayer.data.local.entity.VpnProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Centralized app preferences backed by DataStore.
 * Covers player config, sync intervals, UI preferences.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore get() = context.dataStore

    // --- Player Settings ---
    private object PlayerKeys {
        val USER_AGENT = stringPreferencesKey("player_user_agent")
        val MIN_BUFFER_MS = intPreferencesKey("player_min_buffer_ms")
        val MAX_BUFFER_MS = intPreferencesKey("player_max_buffer_ms")
        val BUFFER_PLAYBACK_MS = intPreferencesKey("player_buffer_playback_ms")
        val BUFFER_REBUFFER_MS = intPreferencesKey("player_buffer_rebuffer_ms")
        val PREFER_SOFTWARE_DECODING = booleanPreferencesKey("player_prefer_sw_decoding")
        val ENABLE_TUNNELED_PLAYBACK = booleanPreferencesKey("player_tunneled_playback")
        val PREFERRED_AUDIO_LANGUAGE = stringPreferencesKey("player_preferred_audio_language")
        val PREFERRED_SUBTITLE_LANGUAGE = stringPreferencesKey("player_preferred_subtitle_language")
    }

    val playerConfig: Flow<PlayerConfig> = dataStore.data.map { prefs ->
        PlayerConfig(
            userAgent = prefs[PlayerKeys.USER_AGENT] ?: PlayerConfig.DEFAULT_USER_AGENT,
            minBufferMs = prefs[PlayerKeys.MIN_BUFFER_MS] ?: 15_000,
            maxBufferMs = prefs[PlayerKeys.MAX_BUFFER_MS] ?: 60_000,
            bufferForPlaybackMs = prefs[PlayerKeys.BUFFER_PLAYBACK_MS] ?: 2_500,
            bufferForPlaybackAfterRebufferMs = prefs[PlayerKeys.BUFFER_REBUFFER_MS] ?: 5_000,
            preferSoftwareDecoding = prefs[PlayerKeys.PREFER_SOFTWARE_DECODING] ?: false,
            enableTunneledPlayback = prefs[PlayerKeys.ENABLE_TUNNELED_PLAYBACK] ?: true,
        )
    }

    suspend fun updatePlayerConfig(config: PlayerConfig) {
        dataStore.edit { prefs ->
            prefs[PlayerKeys.USER_AGENT] = config.userAgent
            prefs[PlayerKeys.MIN_BUFFER_MS] = config.minBufferMs
            prefs[PlayerKeys.MAX_BUFFER_MS] = config.maxBufferMs
            prefs[PlayerKeys.BUFFER_PLAYBACK_MS] = config.bufferForPlaybackMs
            prefs[PlayerKeys.BUFFER_REBUFFER_MS] = config.bufferForPlaybackAfterRebufferMs
            prefs[PlayerKeys.PREFER_SOFTWARE_DECODING] = config.preferSoftwareDecoding
            prefs[PlayerKeys.ENABLE_TUNNELED_PLAYBACK] = config.enableTunneledPlayback
        }
    }

    suspend fun setUserAgent(userAgent: String) {
        dataStore.edit { it[PlayerKeys.USER_AGENT] = userAgent }
    }

    val preferredAudioLanguage: Flow<String> = dataStore.data.map { prefs ->
        prefs[PlayerKeys.PREFERRED_AUDIO_LANGUAGE] ?: ""
    }

    val preferredSubtitleLanguage: Flow<String> = dataStore.data.map { prefs ->
        prefs[PlayerKeys.PREFERRED_SUBTITLE_LANGUAGE] ?: ""
    }

    suspend fun setPreferredAudioLanguage(language: String) {
        dataStore.edit { it[PlayerKeys.PREFERRED_AUDIO_LANGUAGE] = language }
    }

    suspend fun setPreferredSubtitleLanguage(language: String) {
        dataStore.edit { it[PlayerKeys.PREFERRED_SUBTITLE_LANGUAGE] = language }
    }

    // --- Sync Settings ---
    private object SyncKeys {
        val PLAYLIST_SYNC_INTERVAL_HOURS = longPreferencesKey("sync_playlist_interval_hours")
        val EPG_SYNC_INTERVAL_HOURS = longPreferencesKey("sync_epg_interval_hours")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("sync_auto_enabled")
        val AUTO_SYNC_EPG = booleanPreferencesKey("sync_auto_epg")
    }

    private object ConferenceKeys {
        val FOOTBALL_DATA_API_TOKEN = stringPreferencesKey("conference_football_data_api_token")
    }

    val playlistSyncIntervalHours: Flow<Long> = dataStore.data.map { prefs ->
        prefs[SyncKeys.PLAYLIST_SYNC_INTERVAL_HOURS] ?: 6L
    }

    val epgSyncIntervalHours: Flow<Long> = dataStore.data.map { prefs ->
        prefs[SyncKeys.EPG_SYNC_INTERVAL_HOURS] ?: 12L
    }

    val autoSyncEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SyncKeys.AUTO_SYNC_ENABLED] ?: true
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[SyncKeys.AUTO_SYNC_ENABLED] = enabled }
    }

    suspend fun setSyncIntervals(playlistHours: Long, epgHours: Long) {
        dataStore.edit { prefs ->
            prefs[SyncKeys.PLAYLIST_SYNC_INTERVAL_HOURS] = playlistHours
            prefs[SyncKeys.EPG_SYNC_INTERVAL_HOURS] = epgHours
        }
    }

    // --- UI Preferences ---
    private object UiKeys {
        val THEME = stringPreferencesKey("ui_theme") // "system", "dark", "light"
        val EPG_HOURS_VISIBLE = intPreferencesKey("ui_epg_hours_visible")
        val DEFAULT_START_TAB = stringPreferencesKey("ui_default_start_tab") // "live", "movies", "series"
        val SCREEN_ORIENTATION = stringPreferencesKey("ui_screen_orientation") // "auto", "landscape", "portrait"
        val GESTURE_CONTROLS = booleanPreferencesKey("ui_gesture_controls")
    }

    val theme: Flow<String> = dataStore.data.map { prefs ->
        prefs[UiKeys.THEME] ?: "dark"
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { it[UiKeys.THEME] = theme }
    }

    val epgHoursVisible: Flow<Int> = dataStore.data.map { prefs ->
        prefs[UiKeys.EPG_HOURS_VISIBLE] ?: 4
    }

    val defaultStartTab: Flow<String> = dataStore.data.map { prefs ->
        prefs[UiKeys.DEFAULT_START_TAB] ?: "live"
    }

    suspend fun setDefaultStartTab(tab: String) {
        dataStore.edit { it[UiKeys.DEFAULT_START_TAB] = tab }
    }

    val screenOrientation: Flow<String> = dataStore.data.map { prefs ->
        prefs[UiKeys.SCREEN_ORIENTATION] ?: "auto"
    }

    suspend fun setScreenOrientation(orientation: String) {
        dataStore.edit { it[UiKeys.SCREEN_ORIENTATION] = orientation }
    }

    val gestureControlsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[UiKeys.GESTURE_CONTROLS] ?: true
    }

    suspend fun setGestureControlsEnabled(enabled: Boolean) {
        dataStore.edit { it[UiKeys.GESTURE_CONTROLS] = enabled }
    }

    // --- Reconnect Settings ---
    private object ReconnectKeys {
        val MAX_ATTEMPTS = intPreferencesKey("reconnect_max_attempts")
        val DELAY_MS = intPreferencesKey("reconnect_delay_ms")
    }

    val reconnectMaxAttempts: Flow<Int> = dataStore.data.map { prefs ->
        prefs[ReconnectKeys.MAX_ATTEMPTS] ?: 3
    }

    val reconnectDelayMs: Flow<Int> = dataStore.data.map { prefs ->
        prefs[ReconnectKeys.DELAY_MS] ?: 3_000
    }

    suspend fun setReconnectSettings(maxAttempts: Int, delayMs: Int) {
        dataStore.edit { prefs ->
            prefs[ReconnectKeys.MAX_ATTEMPTS] = maxAttempts
            prefs[ReconnectKeys.DELAY_MS] = delayMs
        }
    }

    // --- Auto Sync EPG ---
    suspend fun autoSyncEpg(): Boolean {
        return dataStore.data.first()[SyncKeys.AUTO_SYNC_EPG] ?: true
    }

    suspend fun setAutoSyncEpg(enabled: Boolean) {
        dataStore.edit { it[SyncKeys.AUTO_SYNC_EPG] = enabled }
    }

    val conferenceFootballDataApiToken: Flow<String> = dataStore.data.map { prefs ->
        prefs[ConferenceKeys.FOOTBALL_DATA_API_TOKEN] ?: ""
    }

    suspend fun setConferenceFootballDataApiToken(token: String) {
        dataStore.edit { prefs ->
            prefs[ConferenceKeys.FOOTBALL_DATA_API_TOKEN] = token.trim()
        }
    }

    // --- VPN Settings ---
    private object VpnKeys {
        val VPN_ENABLED = booleanPreferencesKey("vpn_enabled")
        val VPN_AUTO_CONNECT = booleanPreferencesKey("vpn_auto_connect")
        val VPN_AUTO_CONNECT_ON_BOOT = booleanPreferencesKey("vpn_auto_connect_boot")
        val VPN_CONNECT_BEFORE_STREAMING = booleanPreferencesKey("vpn_connect_before_streaming")
        val VPN_SERVER_ID = stringPreferencesKey("vpn_server_id")
        val VPN_PROTOCOL = stringPreferencesKey("vpn_protocol")
        val VPN_KILL_SWITCH = booleanPreferencesKey("vpn_kill_switch")
        val VPN_DNS_LEAK_PROTECTION = booleanPreferencesKey("vpn_dns_leak_protection")
        val VPN_PROVIDER_TYPE = stringPreferencesKey("vpn_provider_type")
        val VPN_CUSTOM_CONFIG = stringPreferencesKey("vpn_custom_config")
        val VPN_RECONNECT_DELAY = intPreferencesKey("vpn_reconnect_delay")
    }

    val vpnEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[VpnKeys.VPN_ENABLED] ?: false
    }

    val vpnAutoConnect: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[VpnKeys.VPN_AUTO_CONNECT] ?: true
    }

    val vpnAutoConnectOnBoot: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[VpnKeys.VPN_AUTO_CONNECT_ON_BOOT] ?: false
    }

    val vpnConnectBeforeStreaming: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[VpnKeys.VPN_CONNECT_BEFORE_STREAMING] ?: false
    }

    val vpnServerId: Flow<String> = dataStore.data.map { prefs ->
        prefs[VpnKeys.VPN_SERVER_ID] ?: ""
    }

    val vpnProtocol: Flow<String> = dataStore.data.map { prefs ->
        prefs[VpnKeys.VPN_PROTOCOL] ?: VpnProtocol.WIREGUARD.name
    }

    val vpnKillSwitch: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[VpnKeys.VPN_KILL_SWITCH] ?: false
    }

    val vpnDnsLeakProtection: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[VpnKeys.VPN_DNS_LEAK_PROTECTION] ?: true
    }

    val vpnProviderType: Flow<String> = dataStore.data.map { prefs ->
        prefs[VpnKeys.VPN_PROVIDER_TYPE] ?: VpnProviderType.MANUAL_CONFIG.name
    }

    val vpnReconnectDelay: Flow<Int> = dataStore.data.map { prefs ->
        prefs[VpnKeys.VPN_RECONNECT_DELAY] ?: 5
    }

    suspend fun setVpnEnabled(enabled: Boolean) {
        dataStore.edit { it[VpnKeys.VPN_ENABLED] = enabled }
    }

    suspend fun setVpnAutoConnect(enabled: Boolean) {
        dataStore.edit { it[VpnKeys.VPN_AUTO_CONNECT] = enabled }
    }

    suspend fun setVpnAutoConnectOnBoot(enabled: Boolean) {
        dataStore.edit { it[VpnKeys.VPN_AUTO_CONNECT_ON_BOOT] = enabled }
    }

    suspend fun setVpnConnectBeforeStreaming(enabled: Boolean) {
        dataStore.edit { it[VpnKeys.VPN_CONNECT_BEFORE_STREAMING] = enabled }
    }

    suspend fun setVpnServerId(serverId: String) {
        dataStore.edit { it[VpnKeys.VPN_SERVER_ID] = serverId }
    }

    suspend fun setVpnProtocol(protocol: String) {
        dataStore.edit { it[VpnKeys.VPN_PROTOCOL] = protocol }
    }

    suspend fun setVpnKillSwitch(enabled: Boolean) {
        dataStore.edit { it[VpnKeys.VPN_KILL_SWITCH] = enabled }
    }

    suspend fun setVpnDnsLeakProtection(enabled: Boolean) {
        dataStore.edit { it[VpnKeys.VPN_DNS_LEAK_PROTECTION] = enabled }
    }

    suspend fun setVpnProviderType(type: String) {
        dataStore.edit { it[VpnKeys.VPN_PROVIDER_TYPE] = type }
    }

    suspend fun setVpnCustomConfig(config: String) {
        dataStore.edit { it[VpnKeys.VPN_CUSTOM_CONFIG] = config }
    }

    suspend fun setVpnReconnectDelay(seconds: Int) {
        dataStore.edit { it[VpnKeys.VPN_RECONNECT_DELAY] = seconds }
    }

    suspend fun getVpnCustomConfig(): String {
        return dataStore.data.first()[VpnKeys.VPN_CUSTOM_CONFIG] ?: ""
    }

    private object CloudRecordingKeys {
        val AUTO_UPLOAD = booleanPreferencesKey("cloud_recording_auto_upload")
        val PROVIDER = stringPreferencesKey("cloud_recording_provider")
        val WEBDAV_BASE_URL = stringPreferencesKey("cloud_recording_webdav_base_url")
        val WEBDAV_USERNAME = stringPreferencesKey("cloud_recording_webdav_username")
        val WEBDAV_PASSWORD = stringPreferencesKey("cloud_recording_webdav_password")
        val WEBDAV_DIRECTORY = stringPreferencesKey("cloud_recording_webdav_directory")
        val GOOGLE_ACCESS_TOKEN = stringPreferencesKey("cloud_recording_google_access_token")
        val GOOGLE_REFRESH_TOKEN = stringPreferencesKey("cloud_recording_google_refresh_token")
        val GOOGLE_ACCESS_TOKEN_EXPIRY = longPreferencesKey("cloud_recording_google_access_token_expiry")
        val GOOGLE_FOLDER_ID = stringPreferencesKey("cloud_recording_google_folder_id")
        val ONEDRIVE_ACCESS_TOKEN = stringPreferencesKey("cloud_recording_onedrive_access_token")
        val ONEDRIVE_REFRESH_TOKEN = stringPreferencesKey("cloud_recording_onedrive_refresh_token")
        val ONEDRIVE_ACCESS_TOKEN_EXPIRY = longPreferencesKey("cloud_recording_onedrive_access_token_expiry")
        val ONEDRIVE_FOLDER_PATH = stringPreferencesKey("cloud_recording_onedrive_folder_path")
        val PENDING_GOOGLE_STATE = stringPreferencesKey("cloud_recording_pending_google_state")
        val PENDING_GOOGLE_VERIFIER = stringPreferencesKey("cloud_recording_pending_google_verifier")
        val AUTH_STATUS_MESSAGE = stringPreferencesKey("cloud_recording_auth_status_message")
        val AUTH_STATUS_IS_ERROR = booleanPreferencesKey("cloud_recording_auth_status_is_error")
    }

    val cloudRecordingSettings: Flow<CloudRecordingSettings> = dataStore.data.map { prefs ->
        CloudRecordingSettings(
            autoUploadEnabled = prefs[CloudRecordingKeys.AUTO_UPLOAD] ?: false,
            provider = prefs[CloudRecordingKeys.PROVIDER]
                ?.let { runCatching { CloudRecordingProvider.valueOf(it) }.getOrNull() }
                ?: CloudRecordingProvider.NONE,
            webDavBaseUrl = prefs[CloudRecordingKeys.WEBDAV_BASE_URL] ?: "",
            webDavUsername = prefs[CloudRecordingKeys.WEBDAV_USERNAME] ?: "",
            webDavPassword = prefs[CloudRecordingKeys.WEBDAV_PASSWORD] ?: "",
            webDavDirectory = prefs[CloudRecordingKeys.WEBDAV_DIRECTORY] ?: "DjoudinisIPPlayer",
            googleDriveAccessToken = prefs[CloudRecordingKeys.GOOGLE_ACCESS_TOKEN] ?: "",
            googleDriveRefreshToken = prefs[CloudRecordingKeys.GOOGLE_REFRESH_TOKEN] ?: "",
            googleDriveAccessTokenExpiryMs = prefs[CloudRecordingKeys.GOOGLE_ACCESS_TOKEN_EXPIRY] ?: 0L,
            googleDriveFolderId = prefs[CloudRecordingKeys.GOOGLE_FOLDER_ID] ?: "",
            oneDriveAccessToken = prefs[CloudRecordingKeys.ONEDRIVE_ACCESS_TOKEN] ?: "",
            oneDriveRefreshToken = prefs[CloudRecordingKeys.ONEDRIVE_REFRESH_TOKEN] ?: "",
            oneDriveAccessTokenExpiryMs = prefs[CloudRecordingKeys.ONEDRIVE_ACCESS_TOKEN_EXPIRY] ?: 0L,
            oneDriveFolderPath = prefs[CloudRecordingKeys.ONEDRIVE_FOLDER_PATH] ?: "DjoudinisIPPlayer",
        )
    }

    val cloudAuthStatusMessage: Flow<String> = dataStore.data.map { prefs ->
        prefs[CloudRecordingKeys.AUTH_STATUS_MESSAGE] ?: ""
    }

    val cloudAuthStatusIsError: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CloudRecordingKeys.AUTH_STATUS_IS_ERROR] ?: false
    }

    suspend fun updateCloudRecordingSettings(settings: CloudRecordingSettings) {
        dataStore.edit { prefs ->
            prefs[CloudRecordingKeys.AUTO_UPLOAD] = settings.autoUploadEnabled
            prefs[CloudRecordingKeys.PROVIDER] = settings.provider.name
            prefs[CloudRecordingKeys.WEBDAV_BASE_URL] = settings.webDavBaseUrl
            prefs[CloudRecordingKeys.WEBDAV_USERNAME] = settings.webDavUsername
            prefs[CloudRecordingKeys.WEBDAV_PASSWORD] = settings.webDavPassword
            prefs[CloudRecordingKeys.WEBDAV_DIRECTORY] = settings.webDavDirectory
            prefs[CloudRecordingKeys.GOOGLE_ACCESS_TOKEN] = settings.googleDriveAccessToken
            prefs[CloudRecordingKeys.GOOGLE_REFRESH_TOKEN] = settings.googleDriveRefreshToken
            prefs[CloudRecordingKeys.GOOGLE_ACCESS_TOKEN_EXPIRY] = settings.googleDriveAccessTokenExpiryMs
            prefs[CloudRecordingKeys.GOOGLE_FOLDER_ID] = settings.googleDriveFolderId
            prefs[CloudRecordingKeys.ONEDRIVE_ACCESS_TOKEN] = settings.oneDriveAccessToken
            prefs[CloudRecordingKeys.ONEDRIVE_REFRESH_TOKEN] = settings.oneDriveRefreshToken
            prefs[CloudRecordingKeys.ONEDRIVE_ACCESS_TOKEN_EXPIRY] = settings.oneDriveAccessTokenExpiryMs
            prefs[CloudRecordingKeys.ONEDRIVE_FOLDER_PATH] = settings.oneDriveFolderPath
        }
    }

    suspend fun storePendingGoogleAuth(state: String, verifier: String) {
        dataStore.edit { prefs ->
            prefs[CloudRecordingKeys.PENDING_GOOGLE_STATE] = state
            prefs[CloudRecordingKeys.PENDING_GOOGLE_VERIFIER] = verifier
        }
    }

    suspend fun consumePendingGoogleAuth(): Pair<String, String> {
        val prefs = dataStore.data.first()
        val state = prefs[CloudRecordingKeys.PENDING_GOOGLE_STATE] ?: ""
        val verifier = prefs[CloudRecordingKeys.PENDING_GOOGLE_VERIFIER] ?: ""
        dataStore.edit { edited ->
            edited.remove(CloudRecordingKeys.PENDING_GOOGLE_STATE)
            edited.remove(CloudRecordingKeys.PENDING_GOOGLE_VERIFIER)
        }
        return state to verifier
    }

    suspend fun setCloudAuthStatus(message: String, isError: Boolean) {
        dataStore.edit { prefs ->
            prefs[CloudRecordingKeys.AUTH_STATUS_MESSAGE] = message
            prefs[CloudRecordingKeys.AUTH_STATUS_IS_ERROR] = isError
        }
    }

    suspend fun clearCloudAuthStatus() {
        dataStore.edit { prefs ->
            prefs.remove(CloudRecordingKeys.AUTH_STATUS_MESSAGE)
            prefs.remove(CloudRecordingKeys.AUTH_STATUS_IS_ERROR)
        }
    }

    suspend fun clearGoogleDriveLogin() {
        dataStore.edit { prefs ->
            prefs.remove(CloudRecordingKeys.GOOGLE_ACCESS_TOKEN)
            prefs.remove(CloudRecordingKeys.GOOGLE_REFRESH_TOKEN)
            prefs.remove(CloudRecordingKeys.GOOGLE_ACCESS_TOKEN_EXPIRY)
            prefs.remove(CloudRecordingKeys.PENDING_GOOGLE_STATE)
            prefs.remove(CloudRecordingKeys.PENDING_GOOGLE_VERIFIER)
        }
    }

    suspend fun clearOneDriveLogin() {
        dataStore.edit { prefs ->
            prefs.remove(CloudRecordingKeys.ONEDRIVE_ACCESS_TOKEN)
            prefs.remove(CloudRecordingKeys.ONEDRIVE_REFRESH_TOKEN)
            prefs.remove(CloudRecordingKeys.ONEDRIVE_ACCESS_TOKEN_EXPIRY)
        }
    }
}
