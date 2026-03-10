package com.djoudini.iplayer.domain.repository

import com.djoudini.iplayer.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun observeByCategory(categoryId: Long): Flow<List<ChannelEntity>>
    fun observeFavorites(playlistId: Long): Flow<List<ChannelEntity>>
    fun observeRecentlyWatched(playlistId: Long, limit: Int = 20): Flow<List<ChannelEntity>>
    fun search(playlistId: Long, query: String): Flow<List<ChannelEntity>>
    suspend fun getById(id: Long): ChannelEntity?
    suspend fun setFavorite(channelId: Long, favorite: Boolean)
    suspend fun updateLastWatched(channelId: Long)
}
