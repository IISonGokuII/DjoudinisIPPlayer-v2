package com.djoudini.iplayer.domain.repository

import com.djoudini.iplayer.data.local.entity.WatchProgressEntity
import com.djoudini.iplayer.domain.model.WatchContentType
import kotlinx.coroutines.flow.Flow

interface WatchProgressRepository {
    suspend fun saveProgress(
        playlistId: Long,
        contentType: WatchContentType,
        contentId: Long,
        positionMs: Long,
        durationMs: Long,
        contentName: String = "",
        posterUrl: String = "",
    )

    suspend fun getProgress(
        playlistId: Long,
        contentType: WatchContentType,
        contentId: Long,
    ): WatchProgressEntity?

    fun observeProgress(
        playlistId: Long,
        contentType: WatchContentType,
        contentId: Long,
    ): Flow<WatchProgressEntity?>

    fun observeContinueWatching(playlistId: Long): Flow<List<WatchProgressEntity>>

    fun observeHistory(playlistId: Long): Flow<List<WatchProgressEntity>>

    suspend fun clearAll(playlistId: Long)
}
