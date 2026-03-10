package com.djoudini.iplayer.data.repository

import com.djoudini.iplayer.data.local.dao.WatchProgressDao
import com.djoudini.iplayer.data.local.entity.WatchProgressEntity
import com.djoudini.iplayer.domain.model.WatchContentType
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchProgressRepositoryImpl @Inject constructor(
    private val watchProgressDao: WatchProgressDao,
) : WatchProgressRepository {

    override suspend fun saveProgress(
        playlistId: Long,
        contentType: WatchContentType,
        contentId: Long,
        positionMs: Long,
        durationMs: Long,
        contentName: String,
        posterUrl: String,
    ) {
        val progressPercent = if (durationMs > 0) {
            (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        } else 0f

        val isCompleted = progressPercent >= 0.9f

        val existing = watchProgressDao.get(playlistId, contentType.value, contentId)

        val entity = WatchProgressEntity(
            id = existing?.id ?: 0,
            playlistId = playlistId,
            contentType = contentType.value,
            contentId = contentId,
            contentName = contentName.ifBlank { existing?.contentName ?: "" },
            posterUrl = posterUrl.ifBlank { existing?.posterUrl ?: "" },
            positionMs = positionMs,
            durationMs = durationMs,
            progressPercent = progressPercent,
            isCompleted = isCompleted,
            lastWatchedAt = System.currentTimeMillis(),
            traktSynced = false,
            traktId = existing?.traktId,
        )

        watchProgressDao.upsert(entity)
    }

    override suspend fun getProgress(
        playlistId: Long,
        contentType: WatchContentType,
        contentId: Long,
    ): WatchProgressEntity? =
        watchProgressDao.get(playlistId, contentType.value, contentId)

    override fun observeProgress(
        playlistId: Long,
        contentType: WatchContentType,
        contentId: Long,
    ): Flow<WatchProgressEntity?> =
        watchProgressDao.observe(playlistId, contentType.value, contentId)

    override fun observeContinueWatching(playlistId: Long): Flow<List<WatchProgressEntity>> =
        watchProgressDao.observeContinueWatching(playlistId)

    override fun observeHistory(playlistId: Long): Flow<List<WatchProgressEntity>> =
        watchProgressDao.observeHistory(playlistId)

    override suspend fun clearAll(playlistId: Long) {
        watchProgressDao.deleteByPlaylist(playlistId)
    }
}
