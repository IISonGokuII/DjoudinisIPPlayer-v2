package com.djoudini.iplayer.data.repository

import com.djoudini.iplayer.data.local.dao.ChannelDao
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao,
) : ChannelRepository {

    override fun observeByCategory(categoryId: Long): Flow<List<ChannelEntity>> =
        channelDao.observeByCategory(categoryId)

    override fun observeFavorites(playlistId: Long): Flow<List<ChannelEntity>> =
        channelDao.observeFavorites(playlistId)

    override fun observeRecentlyWatched(playlistId: Long, limit: Int): Flow<List<ChannelEntity>> =
        channelDao.observeRecentlyWatched(playlistId, limit)

    override fun search(playlistId: Long, query: String): Flow<List<ChannelEntity>> =
        channelDao.search(playlistId, query)

    override suspend fun getById(id: Long): ChannelEntity? =
        channelDao.getById(id)

    override suspend fun setFavorite(channelId: Long, favorite: Boolean) =
        channelDao.setFavorite(channelId, favorite)

    override suspend fun updateLastWatched(channelId: Long) =
        channelDao.updateLastWatched(channelId, System.currentTimeMillis())
}
