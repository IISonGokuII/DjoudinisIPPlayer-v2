package com.djoudini.iplayer.di

import com.djoudini.iplayer.data.repository.ChannelRepositoryImpl
import com.djoudini.iplayer.data.repository.EpgRepositoryImpl
import com.djoudini.iplayer.data.repository.PlaylistRepositoryImpl
import com.djoudini.iplayer.data.repository.VpnRepositoryImpl
import com.djoudini.iplayer.data.repository.VpnSetupRepository
import com.djoudini.iplayer.data.repository.WatchProgressRepositoryImpl
import com.djoudini.iplayer.domain.repository.ChannelRepository
import com.djoudini.iplayer.domain.repository.EpgRepository
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.VpnRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindWatchProgressRepository(impl: WatchProgressRepositoryImpl): WatchProgressRepository

    @Binds
    @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository

    @Binds
    @Singleton
    abstract fun bindVpnRepository(impl: VpnRepositoryImpl): VpnRepository
}
