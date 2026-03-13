package com.djoudini.iplayer.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.djoudini.iplayer.R
import com.djoudini.iplayer.presentation.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Foreground notification service shown while a WireGuard tunnel is active.
 * The actual VPN tunnel is managed by GoBackend (com.wireguard.android.backend.GoBackend$VpnService).
 * This service only handles the persistent "VPN aktiv" status notification.
 */
@AndroidEntryPoint
class VpnService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): VpnService = this@VpnService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                val tunnelName = intent.getStringExtra(EXTRA_TUNNEL_NAME) ?: "VPN"
                startForeground(NOTIFICATION_ID, createNotification(tunnelName))
                Timber.d("VPN notification shown: $tunnelName")
            }
            ACTION_HIDE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Timber.d("VPN notification hidden")
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                VPN_CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN connection status"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(tunnelName: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, VPN_CHANNEL_ID)
            .setContentTitle("VPN aktiv")
            .setContentText("Verbunden: $tunnelName")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_SHOW = "com.djoudini.iplayer.vpn.SHOW_NOTIFICATION"
        const val ACTION_HIDE = "com.djoudini.iplayer.vpn.HIDE_NOTIFICATION"
        const val EXTRA_TUNNEL_NAME = "tunnelName"
        const val NOTIFICATION_ID = 1001
        const val VPN_CHANNEL_ID = "vpn_service_channel"
    }
}
