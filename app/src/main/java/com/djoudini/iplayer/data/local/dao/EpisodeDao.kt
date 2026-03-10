package com.djoudini.iplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.djoudini.iplayer.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE series_id = :seriesId ORDER BY season_number ASC, episode_number ASC")
    fun observeBySeries(seriesId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE series_id = :seriesId AND season_number = :season ORDER BY episode_number ASC")
    fun observeBySeason(seriesId: Long, season: Int): Flow<List<EpisodeEntity>>

    @Query("SELECT DISTINCT season_number FROM episodes WHERE series_id = :seriesId ORDER BY season_number ASC")
    fun observeSeasons(seriesId: Long): Flow<List<Int>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: Long): EpisodeEntity?

    @Query("DELETE FROM episodes WHERE series_id = :seriesId")
    suspend fun deleteBySeries(seriesId: Long)

    @Query("DELETE FROM episodes WHERE playlist_id = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}
