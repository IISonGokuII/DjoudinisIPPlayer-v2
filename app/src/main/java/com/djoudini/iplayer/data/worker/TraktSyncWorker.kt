package com.djoudini.iplayer.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.djoudini.iplayer.data.repository.TraktRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background worker that syncs completed watches to Trakt.tv.
 * Runs periodically or after playback ends.
 */
@HiltWorker
class TraktSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val traktRepository: TraktRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.i("TraktSyncWorker: Starting Trakt sync")
            traktRepository.syncWatchedToTrakt()
            Timber.i("TraktSyncWorker: Trakt sync completed")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "TraktSyncWorker: Trakt sync failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure(
                Data.Builder().putString("error", e.localizedMessage).build()
            )
        }
    }

    companion object {
        const val WORK_NAME = "trakt_sync"
    }
}
