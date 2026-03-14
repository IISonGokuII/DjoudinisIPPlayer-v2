package com.djoudini.iplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.djoudini.iplayer.data.local.entity.ConferenceMatchMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConferenceMatchMappingDao {
    @Query("SELECT * FROM conference_match_mappings WHERE conference_id = :conferenceId ORDER BY priority ASC, id ASC")
    fun observeByConference(conferenceId: Long): Flow<List<ConferenceMatchMappingEntity>>

    @Query("SELECT * FROM conference_match_mappings WHERE conference_id = :conferenceId ORDER BY priority ASC, id ASC")
    suspend fun getByConference(conferenceId: Long): List<ConferenceMatchMappingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<ConferenceMatchMappingEntity>)

    @Query("DELETE FROM conference_match_mappings WHERE conference_id = :conferenceId")
    suspend fun deleteByConference(conferenceId: Long)
}
