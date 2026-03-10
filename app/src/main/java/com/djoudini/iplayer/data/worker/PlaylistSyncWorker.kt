package com.djoudini.iplayer.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background worker for periodic playlist synchronization.
 * Syncs channels, VOD, and series data for the active playlist.
 *
 * Input data:
 * - PLAYLIST_ID (Long): specific playlist to sync, or -1 for active playlist
 */
@HiltWorker
class PlaylistSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val playlistRepository: PlaylistRepository,
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

            Timber.i("PlaylistSyncWorker: Starting sync for playlist $targetId")
            playlistRepository.syncPlaylist(targetId)
            Timber.i("PlaylistSyncWorker: Sync completed for playlist $targetId")

            Result.success(
                Data.Builder().putLong(KEY_PLAYLIST_ID, targetId).build()
            )
        } catch (e: Exception) {
            Timber.e(e, "PlaylistSyncWorker: Sync failed")
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure(
                    Data.Builder().putString(KEY_ERROR, e.localizedMessage ?: "Sync failed").build()
                )
            }
        }
    }

    companion object {
        const val KEY_PLAYLIST_ID = "playlist_id"
        const val KEY_ERROR = "error"
        const val WORK_NAME_PERIODIC = "playlist_sync_periodic"
        const val WORK_NAME_ONESHOT = "playlist_sync_oneshot"
        private const val MAX_RETRIES = 3
    }
}
