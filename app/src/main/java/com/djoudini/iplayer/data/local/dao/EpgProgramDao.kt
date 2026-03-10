package com.djoudini.iplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgProgramDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<EpgProgramEntity>)

    /** Batch insert in chunks for OOM-safe EPG parsing */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(programs: List<EpgProgramEntity>)

    @Query("""
        SELECT * FROM epg_programs
        WHERE epg_channel_id = :channelId
        AND start_time >= :fromTime
        AND start_time < :toTime
        ORDER BY start_time ASC
    """)
    fun observeByChannel(channelId: String, fromTime: Long, toTime: Long): Flow<List<EpgProgramEntity>>

    @Query("""
        SELECT * FROM epg_programs
        WHERE epg_channel_id = :channelId
        AND start_time <= :now
        AND stop_time > :now
        LIMIT 1
    """)
    suspend fun getCurrentProgram(channelId: String, now: Long): EpgProgramEntity?

    @Query("""
        SELECT * FROM epg_programs
        WHERE epg_channel_id = :channelId
        AND start_time > :now
        ORDER BY start_time ASC
        LIMIT 1
    """)
    suspend fun getNextProgram(channelId: String, now: Long): EpgProgramEntity?

    @Query("""
        SELECT * FROM epg_programs
        WHERE epg_channel_id = :channelId
        AND start_time >= :fromTime
        AND start_time < :toTime
        ORDER BY start_time ASC
    """)
    suspend fun getProgramsForRange(channelId: String, fromTime: Long, toTime: Long): List<EpgProgramEntity>

    /** 
     * Batch query for EPG grid - loads programs for multiple channels in a single query.
     * Fixes N+1 problem: 1 query instead of 100+ queries for EPG grid.
     */
    @Query("""
        SELECT * FROM epg_programs
        WHERE epg_channel_id IN (:channelIds)
        AND start_time >= :fromTime
        AND start_time < :toTime
        ORDER BY epg_channel_id ASC, start_time ASC
    """)
    suspend fun getProgramsForChannels(
        channelIds: List<String>, 
        fromTime: Long, 
        toTime: Long
    ): List<EpgProgramEntity>

    @Query("DELETE FROM epg_programs WHERE playlist_id = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)

    /** Clean up expired programs older than the given timestamp */
    @Query("DELETE FROM epg_programs WHERE stop_time < :beforeTime")
    suspend fun deleteExpired(beforeTime: Long)

    @Query("SELECT COUNT(*) FROM epg_programs WHERE playlist_id = :playlistId")
    suspend fun countByPlaylist(playlistId: Long): Int
}
