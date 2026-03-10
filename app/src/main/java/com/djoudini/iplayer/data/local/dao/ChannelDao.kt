package com.djoudini.iplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Update
    suspend fun update(channel: ChannelEntity)

    @Query("SELECT * FROM channels WHERE category_id = :categoryId ORDER BY sort_order ASC, name ASC")
    fun observeByCategory(categoryId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlist_id = :playlistId ORDER BY name ASC")
    fun observeByPlaylist(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlist_id = :playlistId AND is_favorite = 1 ORDER BY name ASC")
    fun observeFavorites(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getById(id: Long): ChannelEntity?

    @Query("SELECT * FROM channels WHERE playlist_id = :playlistId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(playlistId: Long, query: String): Flow<List<ChannelEntity>>

    @Query("UPDATE channels SET is_favorite = :favorite WHERE id = :channelId")
    suspend fun setFavorite(channelId: Long, favorite: Boolean)

    @Query("UPDATE channels SET last_watched_at = :timestamp WHERE id = :channelId")
    suspend fun updateLastWatched(channelId: Long, timestamp: Long)

    @Query("SELECT * FROM channels WHERE playlist_id = :playlistId AND last_watched_at IS NOT NULL ORDER BY last_watched_at DESC LIMIT :limit")
    fun observeRecentlyWatched(playlistId: Long, limit: Int = 20): Flow<List<ChannelEntity>>

    @Query("DELETE FROM channels WHERE playlist_id = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)

    @Query("SELECT COUNT(*) FROM channels WHERE playlist_id = :playlistId")
    suspend fun countByPlaylist(playlistId: Long): Int

    @Query("SELECT COUNT(*) FROM channels WHERE category_id = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int
}
