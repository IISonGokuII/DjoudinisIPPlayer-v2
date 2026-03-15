package com.djoudini.iplayer.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.djoudini.iplayer.data.cloud.CloudRecordingUploader
import com.djoudini.iplayer.data.cloud.CloudAuthRepository
import com.djoudini.iplayer.data.local.dao.RecordingDao
import com.djoudini.iplayer.data.local.entity.CloudRecordingProvider
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import timber.log.Timber

class CloudUploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val entryPoint = EntryPointAccessors.fromApplication(
        appContext,
        CloudUploadWorkerEntryPoint::class.java,
    )

    override suspend fun doWork(): Result {
        val recordingId = inputData.getLong(KEY_RECORDING_ID, -1L)
        if (recordingId <= 0L) {
            return Result.failure(Data.Builder().putString(KEY_ERROR, "recordingId fehlt").build())
        }

        val recordingDao = entryPoint.recordingDao()
        val settings = entryPoint.cloudAuthRepository().resolvedCloudSettings()

        if (!settings.autoUploadEnabled || settings.provider == CloudRecordingProvider.NONE) {
            return Result.success()
        }

        val recording = recordingDao.getById(recordingId)
            ?: return Result.failure(Data.Builder().putString(KEY_ERROR, "Aufnahme nicht gefunden").build())

        if (recording.status != "completed") {
            return Result.failure(Data.Builder().putString(KEY_ERROR, "Aufnahme noch nicht abgeschlossen").build())
        }

        return try {
            recordingDao.updateCloudState(recordingId, settings.provider.name, "uploading", null, null, null)
            val uploader = CloudRecordingUploader(applicationContext, entryPoint.okHttpClient())
            val fileName = recording.filePath.substringAfterLast('/').substringAfterLast(':')
                .ifBlank { "${recording.channelName}_${recording.startTimeMs}.ts" }
            val result = uploader.upload(recording.filePath, fileName, settings)
            recordingDao.updateCloudState(
                recordingId,
                settings.provider.name,
                "uploaded",
                result.remotePath,
                null,
                System.currentTimeMillis(),
            )
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Cloud upload failed for recording $recordingId")
            recordingDao.updateCloudState(
                recordingId,
                settings.provider.name,
                "failed",
                null,
                e.localizedMessage ?: "Upload fehlgeschlagen",
                null,
            )
            if (runAttemptCount < MAX_RETRIES) Result.retry()
            else Result.failure(Data.Builder().putString(KEY_ERROR, e.localizedMessage ?: "Upload fehlgeschlagen").build())
        }
    }

    companion object {
        const val KEY_RECORDING_ID = "recording_id"
        const val KEY_ERROR = "error"
        private const val WORK_PREFIX = "cloud_upload_"
        private const val MAX_RETRIES = 3

        fun enqueue(context: Context, recordingId: Long) {
            val request = OneTimeWorkRequestBuilder<CloudUploadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(Data.Builder().putLong(KEY_RECORDING_ID, recordingId).build())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "$WORK_PREFIX$recordingId",
                androidx.work.ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CloudUploadWorkerEntryPoint {
    fun recordingDao(): RecordingDao
    fun appPreferences(): AppPreferences
    fun okHttpClient(): OkHttpClient
    fun cloudAuthRepository(): CloudAuthRepository
}
