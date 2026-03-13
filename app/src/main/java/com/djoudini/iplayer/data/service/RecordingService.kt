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
class RecordingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null
    private var startTimeMs = 0L

    companion object {
        const val ACTION_START = "com.djoudini.iplayer.RECORDING_START"
        const val ACTION_STOP = "com.djoudini.iplayer.RECORDING_STOP"
        const val ACTION_RECORDING_COMPLETED = "com.djoudini.iplayer.RECORDING_COMPLETED"
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_CHANNEL_NAME = "channel_name"
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
                if (url != null) startRecording(url, name)
                START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopRecording()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private fun startRecording(streamUrl: String, channelName: String) {
        startTimeMs = System.currentTimeMillis()
        startForeground(NOTIFICATION_ID, buildNotification(channelName, 0L))

        recordingJob = serviceScope.launch {
            var outputStream: OutputStream? = null
            var connection: HttpURLConnection? = null
            var elapsedSeconds = 0L
            var writtenBytes = 0L

            // Timer coroutine
            val timerJob = launch {
                while (isActive) {
                    delay(1_000)
                    elapsedSeconds++
                    updateNotification(channelName, elapsedSeconds)
                }
            }

            try {
                outputStream = createOutputStream(channelName, startTimeMs)
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
                Timber.e(e, "[Recording] Error recording stream")
            } finally {
                timerJob.cancel()
                try { outputStream?.flush(); outputStream?.close() } catch (_: Exception) {}
                try { connection?.disconnect() } catch (_: Exception) {}

                // Broadcast completion
                sendBroadcast(Intent(ACTION_RECORDING_COMPLETED).apply {
                    putExtra("duration_ms", elapsedSeconds * 1_000L)
                    putExtra("file_size_bytes", writtenBytes)
                })
                Timber.d("[Recording] Completed: ${elapsedSeconds}s, ${writtenBytes / 1024}KB")
                stopSelf()
            }
        }
    }

    private fun stopRecording() {
        recordingJob?.cancel()
    }

    private fun createOutputStream(channelName: String, startTime: Long): OutputStream {
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
            contentResolver.openOutputStream(uri)
                ?: throw IOException("Could not open output stream")
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "DjoudinisIPPlayer"
            )
            dir.mkdirs()
            FileOutputStream(File(dir, fileName))
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
