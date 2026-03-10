package com.djoudini.iplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.djoudini.iplayer.data.local.entity.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: WatchProgressEntity)

    @Query("""
        SELECT * FROM watch_progress
        WHERE playlist_id = :playlistId
        AND content_type = :contentType
        AND content_id = :contentId
    """)
    suspend fun get(playlistId: Long, contentType: String, contentId: Long): WatchProgressEntity?

    @Query("""
        SELECT * FROM watch_progress
        WHERE playlist_id = :playlistId
        AND content_type = :contentType
        AND content_id = :contentId
    """)
    fun observe(playlistId: Long, contentType: String, contentId: Long): Flow<WatchProgressEntity?>

    @Query("""
        SELECT * FROM watch_progress
        WHERE playlist_id = :playlistId
        AND is_completed = 0
        ORDER BY last_watched_at DESC
        LIMIT :limit
    """)
    fun observeContinueWatching(playlistId: Long, limit: Int = 20): Flow<List<WatchProgressEntity>>

    @Query("""
        SELECT * FROM watch_progress
        WHERE playlist_id = :playlistId
        ORDER BY last_watched_at DESC
        LIMIT :limit
    """)
    fun observeHistory(playlistId: Long, limit: Int = 100): Flow<List<WatchProgressEntity>>

    @Query("""
        SELECT * FROM watch_progress
        WHERE playlist_id = :playlistId
        AND content_type = 'episode'
        AND content_id IN (:episodeIds)
    """)
    fun observeEpisodeProgress(playlistId: Long, episodeIds: List<Long>): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE trakt_synced = 0")
    suspend fun getUnsyncedForTrakt(): List<WatchProgressEntity>

    @Query("UPDATE watch_progress SET trakt_synced = 1 WHERE id = :progressId")
    suspend fun markTraktSynced(progressId: Long)

    @Query("DELETE FROM watch_progress WHERE playlist_id = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}
