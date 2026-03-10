package com.djoudini.iplayer.data.repository

import com.djoudini.iplayer.data.local.dao.EpgProgramDao
import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import com.djoudini.iplayer.domain.repository.EpgRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepositoryImpl @Inject constructor(
    private val epgProgramDao: EpgProgramDao,
) : EpgRepository {

    override fun observePrograms(channelId: String, fromTime: Long, toTime: Long): Flow<List<EpgProgramEntity>> =
        epgProgramDao.observeByChannel(channelId, fromTime, toTime)

    override suspend fun getCurrentProgram(channelId: String): EpgProgramEntity? =
        epgProgramDao.getCurrentProgram(channelId, System.currentTimeMillis())

    override suspend fun getNextProgram(channelId: String): EpgProgramEntity? =
        epgProgramDao.getNextProgram(channelId, System.currentTimeMillis())

    override suspend fun getProgramsForRange(channelId: String, fromTime: Long, toTime: Long): List<EpgProgramEntity> =
        epgProgramDao.getProgramsForRange(channelId, fromTime, toTime)

    override suspend fun getProgramsForChannels(
        channelIds: List<String>,
        fromTime: Long,
        toTime: Long
    ): Map<String, List<EpgProgramEntity>> {
        if (channelIds.isEmpty()) return emptyMap()
        val programs = epgProgramDao.getProgramsForChannels(channelIds, fromTime, toTime)
        return programs.groupBy { it.epgChannelId }
    }

    override suspend fun cleanupExpired() {
        val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        epgProgramDao.deleteExpired(oneDayAgo)
    }
}
