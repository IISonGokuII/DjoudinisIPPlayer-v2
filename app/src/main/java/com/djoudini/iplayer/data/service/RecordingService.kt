package com.djoudini.iplayer.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.dao.RecordingDao
import com.djoudini.iplayer.data.local.entity.RecordingEntity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.IOException
import android.os.Environment

/**
 * Foreground service that records a live IPTV stream to a file in Downloads.
 *
 * Start:  sendIntent(ACTION_START) with EXTRA_STREAM_URL + EXTRA_CHANNEL_NAME
 * Stop:   sendIntent(ACTION_STOP)
 * Result: Broadcasts ACTION_RECORDING_COMPLETED when done
 */
@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var recordingDao: RecordingDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null
    private var startTimeMs = 0L
    private var activeRecordingId: Long? = null

    companion object {
        const val ACTION_START = "com.djoudini.iplayer.RECORDING_START"
        const val ACTION_STOP = "com.djoudini.iplayer.RECORDING_STOP"
        const val ACTION_RECORDING_COMPLETED = "com.djoudini.iplayer.RECORDING_COMPLETED"
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_CHANNEL_NAME = "channel_name"
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "recording_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_STREAM_URL)
                val name = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "Channel"
                val channelId = intent.getLongExtra(EXTRA_CHANNEL_ID, 0L)
                if (url != null) startRecording(url, name, channelId)
                START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopRecording()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private fun startRecording(streamUrl: String, channelName: String, channelId: Long) {
        if (recordingJob?.isActive == true) return
        startTimeMs = System.currentTimeMillis()
        startForeground(NOTIFICATION_ID, buildNotification(channelName, 0L))

        recordingJob = serviceScope.launch {
            var recordingId: Long? = null
            var outputStream: OutputStream? = null
            var connection: HttpURLConnection? = null
            var elapsedSeconds = 0L
            var writtenBytes = 0L
            var outputLocation: String? = null
            var finalStatus = "completed"

            // Timer coroutine
            val timerJob = launch {
                while (isActive) {
                    delay(1_000)
                    elapsedSeconds++
                    updateNotification(channelName, elapsedSeconds)
                }
            }

            try {
                val outputTarget = createOutputTarget(channelName, startTimeMs)
                outputLocation = outputTarget.location
                outputStream = outputTarget.outputStream
                recordingId = recordingDao.insert(
                    RecordingEntity(
                        channelName = channelName,
                        channelId = channelId,
                        filePath = outputTarget.location,
                        startTimeMs = startTimeMs,
                        status = "recording",
                    ),
                )
                activeRecordingId = recordingId
                connection = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 120_000
                    setRequestProperty("User-Agent", "VLC/3.0.20 LibVLC/3.0.20")
                    connect()
                }

                val inputStream = connection.inputStream
                val buffer = ByteArray(8_192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1 && isActive) {
                    outputStream.write(buffer, 0, bytesRead)
                    writtenBytes += bytesRead
                }
            } catch (e: Exception) {
                finalStatus = "failed"
                Timber.e(e, "[Recording] Error recording stream")
            } finally {
                timerJob.cancel()
                try { outputStream?.flush(); outputStream?.close() } catch (_: Exception) {}
                try { connection?.disconnect() } catch (_: Exception) {}
                val durationMs = elapsedSeconds * 1_000L
                recordingId?.let { id ->
                    recordingDao.updateCompletion(id, finalStatus, durationMs, writtenBytes)
                }
                activeRecordingId = null

                // Broadcast completion
                sendBroadcast(Intent(ACTION_RECORDING_COMPLETED).apply {
                    `package` = packageName
                    putExtra("duration_ms", durationMs)
                    putExtra("file_size_bytes", writtenBytes)
                    putExtra("status", finalStatus)
                    putExtra("file_path", outputLocation)
                })
                Timber.d("[Recording] Finished: status=$finalStatus, ${elapsedSeconds}s, ${writtenBytes / 1024}KB")
                stopSelf()
            }
        }
    }

    private fun stopRecording() {
        recordingJob?.cancel()
    }

    private data class OutputTarget(
        val location: String,
        val outputStream: OutputStream,
    )

    private fun createOutputTarget(channelName: String, startTime: Long): OutputTarget {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(startTime))
        val safeName = channelName.take(20).replace(Regex("[^a-zA-Z0-9]"), "_")
        val fileName = "REC_${safeName}_$ts.ts"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "video/mp2t")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/DjoudinisIPPlayer")
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("MediaStore insert failed")
            val outputStream = contentResolver.openOutputStream(uri)
                ?: throw IOException("Could not open output stream")
            OutputTarget(
                location = uri.toString(),
                outputStream = outputStream,
            )
        } else {
            val dir = File(
                getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir,
                "recordings",
            )
            dir.mkdirs()
            val file = File(dir, fileName)
            OutputTarget(
                location = file.absolutePath,
                outputStream = FileOutputStream(file),
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LiveTV Recording",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows active LiveTV recording status"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(channelName: String, elapsedSeconds: Long): android.app.Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, RecordingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val elapsed = formatDuration(elapsedSeconds)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("● REC  $channelName")
            .setContentText("Aufnahme läuft: $elapsed")
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, "Stopp", stopIntent)
            .build()
    }

    private fun updateNotification(channelName: String, elapsedSeconds: Long) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(channelName, elapsedSeconds))
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    override fun onDestroy() {
        recordingJob?.cancel()
        super.onDestroy()
    }
}
