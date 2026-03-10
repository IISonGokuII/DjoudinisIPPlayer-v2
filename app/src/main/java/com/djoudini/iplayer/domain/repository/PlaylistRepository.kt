package com.djoudini.iplayer.domain.repository

import com.djoudini.iplayer.data.local.entity.PlaylistEntity
import com.djoudini.iplayer.domain.model.SyncProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for playlist management and synchronization.
 * Defines the contract between Domain and Data layers.
 */
interface PlaylistRepository {

    /** Observable list of all playlists */
    fun observeAll(): Flow<List<PlaylistEntity>>

    /** Observable currently active playlist */
    fun observeActive(): Flow<PlaylistEntity?>

    /** Get active playlist synchronously */
    suspend fun getActive(): PlaylistEntity?

    /** Add a new Xtream Codes playlist */
    suspend fun addXtreamPlaylist(name: String, serverUrl: String, username: String, password: String): Long

    /** Add a new M3U playlist by URL */
    suspend fun addM3uPlaylist(name: String, m3uUrl: String): Long

    /** Delete a playlist and all associated data (cascading) */
    suspend fun deletePlaylist(playlistId: Long)

    /** Set the active playlist (deactivates all others) */
    suspend fun setActive(playlistId: Long)

    /** Real-time sync progress as StateFlow (0.0f to 1.0f) */
    val syncProgress: StateFlow<SyncProgress>

    /** Sync all data for a playlist (channels, VOD, series, categories) */
    suspend fun syncPlaylist(playlistId: Long)

    /** Fast: Only fetch and save categories (no streams). Used during onboarding. */
    suspend fun syncCategoriesOnly(playlistId: Long)

    /** Sync only streams for user-selected categories. Much faster than full sync. */
    suspend fun syncSelectedStreams(playlistId: Long)

    /** Sync EPG data for a playlist */
    suspend fun syncEpg(playlistId: Long)

    /** Cancel any ongoing sync */
    fun cancelSync()
}
