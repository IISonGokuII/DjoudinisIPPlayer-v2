package com.djoudini.iplayer

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.data.repository.TraktRepository
import com.djoudini.iplayer.data.worker.SyncScheduler
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Application class with optimized Coil image loading for TV/Fire TV.
 *
 * Key optimizations:
 * - Large memory cache for fast logo loading
 * - Persistent disk cache for offline logo viewing
 * - Aggressive caching policies
 */
@HiltAndroidApp
class DjoudinisApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var playlistRepository: PlaylistRepository

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var traktRepository: TraktRepository

    @Inject
    lateinit var watchProgressRepository: WatchProgressRepository

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize WorkManager
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
        )

        // Note: Background sync scheduling is handled by SettingsViewModel
        // when user enables auto-sync
    }

    /**
     * Optimized Coil ImageLoader for fast channel/logo loading.
     * Critical for Fire TV performance.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Memory cache: 25% of available memory for logos
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            // Disk cache: 100MB for persistent logo storage
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB
                    .build()
            }
            // Aggressive caching for logos
            .respectCacheHeaders(false)
            // Allow larger images for TV screens
            .allowHardware(true)
            // Crossfade animation for smooth loading
            .crossfade(true)
            // Logger in debug mode
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
