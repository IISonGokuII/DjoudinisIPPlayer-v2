package com.djoudini.iplayer

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.data.worker.SyncScheduler
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import com.djoudini.iplayer.util.CrashHandler
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class with Hilt dependency injection.
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
    lateinit var watchProgressRepository: WatchProgressRepository

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Crash Handler (MUST be before WorkManager)
        CrashHandler.init(this)

        // Initialize Firebase Crashlytics for non-debug builds
        if (!BuildConfig.DEBUG) {
            try {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                Timber.d("[DjoudinisApp] Firebase Crashlytics enabled for release builds")
            } catch (e: Exception) {
                Timber.e(e, "[DjoudinisApp] Firebase Crashlytics init failed")
            }
        }

        // Initialize WorkManager
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
        )

        Timber.d("[DjoudinisApp] onCreate()")
    }

    /**
     * Optimized Coil ImageLoader for fast channel/logo loading.
     * Critical for Fire TV performance.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .crossfade(true)
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
