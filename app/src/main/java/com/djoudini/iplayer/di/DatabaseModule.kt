package com.djoudini.iplayer.di

import android.content.Context
import androidx.room.Room
import com.djoudini.iplayer.data.local.dao.CategoryDao
import com.djoudini.iplayer.data.local.dao.ChannelDao
import com.djoudini.iplayer.data.local.dao.EpgProgramDao
import com.djoudini.iplayer.data.local.dao.EpisodeDao
import com.djoudini.iplayer.data.local.dao.PlaylistDao
import com.djoudini.iplayer.data.local.dao.SeriesDao
import com.djoudini.iplayer.data.local.dao.VodDao
import com.djoudini.iplayer.data.local.dao.RecordingDao
import com.djoudini.iplayer.data.local.dao.WatchProgressDao
import com.djoudini.iplayer.data.local.database.AppDatabase
import com.djoudini.iplayer.data.local.database.MIGRATION_2_3
import com.djoudini.iplayer.data.local.database.MIGRATION_3_4
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "djoudini_iplayer.db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideChannelDao(db: AppDatabase): ChannelDao = db.channelDao()
    @Provides fun provideVodDao(db: AppDatabase): VodDao = db.vodDao()
    @Provides fun provideSeriesDao(db: AppDatabase): SeriesDao = db.seriesDao()
    @Provides fun provideEpisodeDao(db: AppDatabase): EpisodeDao = db.episodeDao()
    @Provides fun provideEpgProgramDao(db: AppDatabase): EpgProgramDao = db.epgProgramDao()
    @Provides fun provideWatchProgressDao(db: AppDatabase): WatchProgressDao = db.watchProgressDao()
    @Provides fun provideRecordingDao(db: AppDatabase): RecordingDao = db.recordingDao()
}
