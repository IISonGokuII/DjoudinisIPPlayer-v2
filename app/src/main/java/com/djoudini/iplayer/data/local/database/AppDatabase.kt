package com.djoudini.iplayer.data.local.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.djoudini.iplayer.data.local.dao.CategoryDao
import com.djoudini.iplayer.data.local.dao.ChannelDao
import com.djoudini.iplayer.data.local.dao.EpgProgramDao
import com.djoudini.iplayer.data.local.dao.EpisodeDao
import com.djoudini.iplayer.data.local.dao.PlaylistDao
import com.djoudini.iplayer.data.local.dao.SeriesDao
import com.djoudini.iplayer.data.local.dao.VodDao
import com.djoudini.iplayer.data.local.dao.WatchProgressDao
import com.djoudini.iplayer.data.local.entity.CategoryEntity
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import com.djoudini.iplayer.data.local.entity.EpisodeEntity
import com.djoudini.iplayer.data.local.entity.PlaylistEntity
import com.djoudini.iplayer.data.local.entity.SeriesEntity
import com.djoudini.iplayer.data.local.entity.VodEntity
import com.djoudini.iplayer.data.local.entity.WatchProgressEntity

@Database(
    entities = [
        PlaylistEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        VodEntity::class,
        SeriesEntity::class,
        EpisodeEntity::class,
        EpgProgramEntity::class,
        WatchProgressEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao
    abstract fun vodDao(): VodDao
    abstract fun seriesDao(): SeriesDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun epgProgramDao(): EpgProgramDao
    abstract fun watchProgressDao(): WatchProgressDao
}
