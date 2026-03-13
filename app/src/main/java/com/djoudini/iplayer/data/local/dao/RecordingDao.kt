package com.djoudini.iplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.djoudini.iplayer.data.local.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity): Long

    @Query("SELECT * FROM recordings ORDER BY startTimeMs DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("UPDATE recordings SET status = :status, durationMs = :durationMs, fileSizeBytes = :fileSizeBytes WHERE id = :id")
    suspend fun updateCompletion(id: Long, status: String, durationMs: Long, fileSizeBytes: Long)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM recordings WHERE status = 'recording'")
    suspend fun getActiveRecordings(): List<RecordingEntity>
}
