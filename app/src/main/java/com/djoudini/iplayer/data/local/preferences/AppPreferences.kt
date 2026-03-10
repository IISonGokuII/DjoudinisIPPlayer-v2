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
import com.djoudini.iplayer.data.local.entity.PlayerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Centralized app preferences backed by DataStore.
 * Covers player config, sync intervals, Trakt.tv tokens, UI preferences.
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

    // --- Sync Settings ---
    private object SyncKeys {
        val PLAYLIST_SYNC_INTERVAL_HOURS = longPreferencesKey("sync_playlist_interval_hours")
        val EPG_SYNC_INTERVAL_HOURS = longPreferencesKey("sync_epg_interval_hours")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("sync_auto_enabled")
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

    // --- Trakt.tv ---
    private object TraktKeys {
        val ACCESS_TOKEN = stringPreferencesKey("trakt_access_token")
        val REFRESH_TOKEN = stringPreferencesKey("trakt_refresh_token")
        val EXPIRES_AT = longPreferencesKey("trakt_expires_at")
        val ENABLED = booleanPreferencesKey("trakt_enabled")
    }

    val traktAccessToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[TraktKeys.ACCESS_TOKEN]
    }

    val traktEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[TraktKeys.ENABLED] ?: false
    }

    suspend fun saveTraktTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        dataStore.edit { prefs ->
            prefs[TraktKeys.ACCESS_TOKEN] = accessToken
            prefs[TraktKeys.REFRESH_TOKEN] = refreshToken
            prefs[TraktKeys.EXPIRES_AT] = expiresAt
            prefs[TraktKeys.ENABLED] = true
        }
    }

    suspend fun getTraktRefreshToken(): String? {
        var token: String? = null
        dataStore.data.collect { prefs ->
            token = prefs[TraktKeys.REFRESH_TOKEN]
        }
        return token
    }

    suspend fun clearTraktTokens() {
        dataStore.edit { prefs ->
            prefs.remove(TraktKeys.ACCESS_TOKEN)
            prefs.remove(TraktKeys.REFRESH_TOKEN)
            prefs.remove(TraktKeys.EXPIRES_AT)
            prefs[TraktKeys.ENABLED] = false
        }
    }

    // --- UI Preferences ---
    private object UiKeys {
        val THEME = stringPreferencesKey("ui_theme") // "system", "dark", "light"
        val EPG_HOURS_VISIBLE = intPreferencesKey("ui_epg_hours_visible")
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
}
