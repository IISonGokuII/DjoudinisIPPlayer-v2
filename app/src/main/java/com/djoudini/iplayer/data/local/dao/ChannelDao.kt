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

    /**
     * Get all channels in a category ordered for channel zapping.
     * Returns channels sorted by their order in the playlist.
     */
    @Query("SELECT * FROM channels WHERE category_id = :categoryId ORDER BY sort_order ASC, name ASC")
    suspend fun getByCategoryOrdered(categoryId: Long): List<ChannelEntity>

    /**
     * Get the previous channel in the category for zapping.
     * Returns the channel with the highest sort_order that is less than the current channel's sort_order.
     */
    @Query("""
        SELECT * FROM channels 
        WHERE category_id = :categoryId 
        AND (sort_order < :currentSortOrder OR (sort_order = :currentSortOrder AND name < (SELECT name FROM channels WHERE id = :currentChannelId)))
        ORDER BY sort_order DESC, name DESC 
        LIMIT 1
    """)
    suspend fun getPreviousChannelInCategory(categoryId: Long, currentChannelId: Long, currentSortOrder: Int): ChannelEntity?

    /**
     * Get the next channel in the category for zapping.
     * Returns the channel with the lowest sort_order that is greater than the current channel's sort_order.
     */
    @Query("""
        SELECT * FROM channels 
        WHERE category_id = :categoryId 
        AND (sort_order > :currentSortOrder OR (sort_order = :currentSortOrder AND name > (SELECT name FROM channels WHERE id = :currentChannelId)))
        ORDER BY sort_order ASC, name ASC 
        LIMIT 1
    """)
    suspend fun getNextChannelInCategory(categoryId: Long, currentChannelId: Long, currentSortOrder: Int): ChannelEntity?
}
