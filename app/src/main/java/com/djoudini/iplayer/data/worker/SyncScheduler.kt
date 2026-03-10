package com.djoudini.iplayer.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules background sync operations via WorkManager.
 * Handles both periodic (auto) and manual (one-shot) sync triggers.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Schedule periodic playlist sync (default: every 6 hours).
     */
    fun schedulePeriodicPlaylistSync(intervalHours: Long = 6) {
        val request = PeriodicWorkRequestBuilder<PlaylistSyncWorker>(
            intervalHours, TimeUnit.HOURS,
            15, TimeUnit.MINUTES, // flex interval
        )
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(
                Data.Builder().putLong(PlaylistSyncWorker.KEY_PLAYLIST_ID, -1L).build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            PlaylistSyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Schedule periodic EPG sync (default: every 12 hours).
     */
    fun schedulePeriodicEpgSync(intervalHours: Long = 12) {
        val request = PeriodicWorkRequestBuilder<EpgSyncWorker>(
            intervalHours, TimeUnit.HOURS,
            30, TimeUnit.MINUTES,
        )
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(
                Data.Builder().putLong(EpgSyncWorker.KEY_PLAYLIST_ID, -1L).build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            EpgSyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Trigger a one-shot playlist sync now.
     */
    fun syncPlaylistNow(playlistId: Long = -1L) {
        val request = OneTimeWorkRequestBuilder<PlaylistSyncWorker>()
            .setConstraints(networkConstraints)
            .setInputData(
                Data.Builder().putLong(PlaylistSyncWorker.KEY_PLAYLIST_ID, playlistId).build()
            )
            .build()

        workManager.enqueueUniqueWork(
            PlaylistSyncWorker.WORK_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /**
     * Trigger a one-shot EPG sync now.
     */
    fun syncEpgNow(playlistId: Long = -1L) {
        val request = OneTimeWorkRequestBuilder<EpgSyncWorker>()
            .setConstraints(networkConstraints)
            .setInputData(
                Data.Builder().putLong(EpgSyncWorker.KEY_PLAYLIST_ID, playlistId).build()
            )
            .build()

        workManager.enqueueUniqueWork(
            EpgSyncWorker.WORK_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /**
     * Cancel all scheduled sync work.
     */
    fun cancelAll() {
        workManager.cancelUniqueWork(PlaylistSyncWorker.WORK_NAME_PERIODIC)
        workManager.cancelUniqueWork(EpgSyncWorker.WORK_NAME_PERIODIC)
        workManager.cancelUniqueWork(PlaylistSyncWorker.WORK_NAME_ONESHOT)
        workManager.cancelUniqueWork(EpgSyncWorker.WORK_NAME_ONESHOT)
    }
}
