package com.djoudini.iplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.djoudini.iplayer.data.local.entity.ConferenceProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConferenceProfileDao {
    @Query("SELECT * FROM conference_profiles ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ConferenceProfileEntity>>

    @Query("SELECT * FROM conference_profiles WHERE id = :id")
    suspend fun getById(id: Long): ConferenceProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ConferenceProfileEntity): Long

    @Query("DELETE FROM conference_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}
