package com.djoudini.iplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.djoudini.iplayer.data.local.entity.VodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VodEntity>)

    @Update
    suspend fun update(item: VodEntity)

    @Query("SELECT * FROM vod_items WHERE category_id = :categoryId ORDER BY name ASC")
    fun observeByCategory(categoryId: Long): Flow<List<VodEntity>>

    @Query("SELECT * FROM vod_items WHERE playlist_id = :playlistId AND is_favorite = 1 ORDER BY name ASC")
    fun observeFavorites(playlistId: Long): Flow<List<VodEntity>>

    @Query("SELECT * FROM vod_items WHERE id = :id")
    suspend fun getById(id: Long): VodEntity?

    @Query("SELECT * FROM vod_items WHERE playlist_id = :playlistId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(playlistId: Long, query: String): Flow<List<VodEntity>>

    @Query("UPDATE vod_items SET is_favorite = :favorite WHERE id = :vodId")
    suspend fun setFavorite(vodId: Long, favorite: Boolean)

    @Query("DELETE FROM vod_items WHERE playlist_id = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)

    @Query("SELECT COUNT(*) FROM vod_items WHERE playlist_id = :playlistId")
    suspend fun countByPlaylist(playlistId: Long): Int

    @Query("SELECT * FROM vod_items WHERE playlist_id = :playlistId ORDER BY added_at DESC LIMIT :limit")
    fun observeRecentlyAdded(playlistId: Long, limit: Int = 50): Flow<List<VodEntity>>

    @Query("SELECT * FROM vod_items WHERE tmdb_id = :tmdbId AND playlist_id = :playlistId LIMIT 1")
    suspend fun getByTmdbId(playlistId: Long, tmdbId: Int): VodEntity?
}
