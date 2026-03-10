package com.djoudini.iplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.djoudini.iplayer.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deleteById(playlistId: Long)

    @Query("SELECT * FROM playlists ORDER BY created_at DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observeById(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE is_active = 1 LIMIT 1")
    suspend fun getActive(): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE is_active = 1 LIMIT 1")
    fun observeActive(): Flow<PlaylistEntity?>

    @Query("UPDATE playlists SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE playlists SET is_active = 1 WHERE id = :playlistId")
    suspend fun activate(playlistId: Long)

    @Query("UPDATE playlists SET last_synced_at = :timestamp WHERE id = :playlistId")
    suspend fun updateLastSynced(playlistId: Long, timestamp: Long)

    @Query("UPDATE playlists SET epg_last_synced_at = :timestamp WHERE id = :playlistId")
    suspend fun updateEpgLastSynced(playlistId: Long, timestamp: Long)

    @Query("UPDATE playlists SET expiration_date = :expDate, max_connections = :maxConn, status = :status WHERE id = :playlistId")
    suspend fun updateAccountInfo(playlistId: Long, expDate: Long?, maxConn: Int?, status: String)

    @Query("UPDATE playlists SET epg_url = :epgUrl WHERE id = :playlistId")
    suspend fun updateEpgUrl(playlistId: Long, epgUrl: String)

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun count(): Int
}
