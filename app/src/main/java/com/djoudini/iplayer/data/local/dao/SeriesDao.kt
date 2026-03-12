package com.djoudini.iplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.djoudini.iplayer.data.local.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SeriesEntity>)

    @Update
    suspend fun update(item: SeriesEntity)

    @Query("SELECT * FROM series WHERE category_id = :categoryId ORDER BY name ASC")
    fun observeByCategory(categoryId: Long): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getById(id: Long): SeriesEntity?

    @Query("SELECT * FROM series WHERE playlist_id = :playlistId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(playlistId: Long, query: String): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE playlist_id = :playlistId AND is_favorite = 1 ORDER BY name ASC")
    fun observeFavorites(playlistId: Long): Flow<List<SeriesEntity>>

    @Query("UPDATE series SET is_favorite = :favorite WHERE id = :seriesId")
    suspend fun setFavorite(seriesId: Long, favorite: Boolean)

    @Query("DELETE FROM series WHERE playlist_id = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)

    @Query("SELECT COUNT(*) FROM series WHERE playlist_id = :playlistId")
    suspend fun countByPlaylist(playlistId: Long): Int
}
