package com.djoudini.iplayer.domain.repository

import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import kotlinx.coroutines.flow.Flow

interface EpgRepository {
    fun observePrograms(channelId: String, fromTime: Long, toTime: Long): Flow<List<EpgProgramEntity>>
    suspend fun getCurrentProgram(channelId: String): EpgProgramEntity?
    suspend fun getNextProgram(channelId: String): EpgProgramEntity?
    suspend fun getProgramsForRange(channelId: String, fromTime: Long, toTime: Long): List<EpgProgramEntity>
    suspend fun cleanupExpired()
}
