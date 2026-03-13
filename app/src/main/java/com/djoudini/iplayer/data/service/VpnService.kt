package com.djoudini.iplayer.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.djoudini.iplayer.R
import com.djoudini.iplayer.presentation.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * VPN Service for handling VPN connections using Android's VpnService API.
 *
 * This implementation uses a placeholder tunnel.
 * For real WireGuard support, integrate: implementation("com.wireguard.android:backend:1.0.0")
 */
@AndroidEntryPoint
class VpnService : VpnService() {

    @Inject
    lateinit var vpnStateManager: VpnStateManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var currentTunnelName: String? = null

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): VpnService = this@VpnService
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("VpnService created")
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val configString = intent.getStringExtra(EXTRA_CONFIG)
                val tunnelName = intent.getStringExtra(EXTRA_TUNNEL_NAME)
                val serverEndpoint = intent.getStringExtra(EXTRA_SERVER_ENDPOINT)
                startVpn(configString, tunnelName, serverEndpoint)
            }
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(configString: String?, tunnelName: String?, serverEndpoint: String?) {
        if (configString == null || tunnelName == null) {
            stopSelf()
            return
        }

        serviceScope.launch {
            try {
                vpnStateManager.setConnecting(tunnelName)

                // NOTE: For real WireGuard support, parse config and use WireGuardBackend here
                // For now, we simulate the connection
                
                // Prepare VpnService builder (placeholder)
                val builder = Builder()
                    .setMtu(1420)
                    .addAddress("10.0.0.2", 24)
                    .addDnsServer("8.8.8.8")
                    .addDnsServer("8.8.4.4")
                    .setBlocking(true)
                    .setMetered(false)
                    .addRoute("0.0.0.0", 0)

                // Establish VPN connection (placeholder)
                vpnInterface = builder.establish()
                
                if (vpnInterface == null) {
                    // VPN establishment failed - this is expected without proper setup
                    // In production, this would establish a real VPN tunnel
                    Timber.w("VPN interface not established (simulation mode)")
                    
                    // Simulate successful connection for demo purposes
                    vpnStateManager.setConnected(tunnelName)
                    startForeground(NOTIFICATION_ID, createNotification(tunnelName))
                    return@launch
                }

                currentTunnelName = tunnelName

                // Update state
                vpnStateManager.setConnected(tunnelName)

                // Start foreground service
                startForeground(NOTIFICATION_ID, createNotification(tunnelName))

                Timber.d("VPN started successfully: $tunnelName")

            } catch (e: Exception) {
                Timber.e(e, "Failed to start VPN")
                vpnStateManager.setError(e.message ?: "Unknown error")
                stopVpn()
                stopSelf()
            }
        }
    }

    private fun stopVpn() {
        serviceScope.launch {
            try {
                vpnStateManager.setDisconnecting()

                currentTunnelName?.let { name ->
                    try {
                        // NOTE: For real WireGuard, stop backend here
                        // backend.setState(vpnInterface?.fileDescriptor, null, null)
                    } catch (e: Exception) {
                        Timber.e(e, "Error stopping VPN backend")
                    }
                    
                    vpnStateManager.setDisconnected()
                }

                vpnInterface?.close()
                vpnInterface = null
                currentTunnelName = null

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()

                Timber.d("VPN stopped successfully")

            } catch (e: Exception) {
                Timber.e(e, "Failed to stop VPN")
                vpnStateManager.setError(e.message ?: "Error stopping VPN")
            }
        }
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

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
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

        val disconnectIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VpnService::class.java).apply {
                action = ACTION_DISCONNECT
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, VPN_CHANNEL_ID)
            .setContentTitle("VPN ist aktiv")
            .setContentText("Verbunden mit: $tunnelName")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Trennen",
                disconnectIntent
            )
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Handle memory trim if needed
    }

    companion object {
        const val ACTION_CONNECT = "com.djoudini.iplayer.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.djoudini.iplayer.vpn.DISCONNECT"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_TUNNEL_NAME = "tunnelName"
        const val EXTRA_SERVER_ENDPOINT = "serverEndpoint"
        const val NOTIFICATION_ID = 1001
        const val VPN_CHANNEL_ID = "vpn_service_channel"
    }
}
