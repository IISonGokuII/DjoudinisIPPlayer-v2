package com.djoudini.iplayer.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.djoudini.iplayer.domain.repository.EpgRepository
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background worker for periodic EPG data synchronization.
 * Downloads and parses XMLTV data, then cleans up expired programs.
 */
@HiltWorker
class EpgSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val playlistRepository: PlaylistRepository,
    private val epgRepository: EpgRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val playlistId = inputData.getLong(KEY_PLAYLIST_ID, -1L)

        return try {
            val targetId = if (playlistId == -1L) {
                playlistRepository.getActive()?.id
                    ?: return Result.failure(
                        Data.Builder().putString(KEY_ERROR, "No active playlist").build()
                    )
            } else {
                playlistId
            }

            Timber.i("EpgSyncWorker: Starting EPG sync for playlist $targetId")

            // Sync EPG data
            playlistRepository.syncEpg(targetId)

            // Cleanup expired entries
            epgRepository.cleanupExpired()

            Timber.i("EpgSyncWorker: EPG sync completed for playlist $targetId")

            Result.success(
                Data.Builder().putLong(KEY_PLAYLIST_ID, targetId).build()
            )
        } catch (e: Exception) {
            Timber.e(e, "EpgSyncWorker: EPG sync failed")
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure(
                    Data.Builder().putString(KEY_ERROR, e.localizedMessage ?: "EPG sync failed").build()
                )
            }
        }
    }

    companion object {
        const val KEY_PLAYLIST_ID = "playlist_id"
        const val KEY_ERROR = "error"
        const val WORK_NAME_PERIODIC = "epg_sync_periodic"
        const val WORK_NAME_ONESHOT = "epg_sync_oneshot"
        private const val MAX_RETRIES = 3
    }
}
