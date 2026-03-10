package com.djoudini.iplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.djoudini.iplayer.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE playlist_id = :playlistId AND category_type = :type AND is_selected = 1 ORDER BY sort_order ASC, name ASC")
    fun observeByType(playlistId: Long, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE playlist_id = :playlistId AND category_type = :type ORDER BY sort_order ASC, name ASC")
    fun observeAllByType(playlistId: Long, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE playlist_id = :playlistId AND is_selected = 1 ORDER BY sort_order ASC")
    fun observeSelected(playlistId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE playlist_id = :playlistId ORDER BY category_type ASC, sort_order ASC, name ASC")
    suspend fun getAllByPlaylist(playlistId: Long): List<CategoryEntity>

    @Query("UPDATE categories SET is_selected = :selected WHERE id = :categoryId")
    suspend fun setSelected(categoryId: Long, selected: Boolean)

    @Query("DELETE FROM categories WHERE playlist_id = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)

    @Query("SELECT COUNT(*) FROM categories WHERE playlist_id = :playlistId AND category_type = :type")
    suspend fun countByType(playlistId: Long, type: String): Int
}
