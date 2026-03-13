package com.djoudini.iplayer.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.domain.repository.VpnRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * BroadcastReceiver for handling device boot completion.
 * Used to auto-connect VPN if enabled in settings.
 */
@AndroidEntryPoint
class VpnBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var vpnRepository: VpnRepository

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            Timber.d("Boot completed received, checking VPN auto-connect")
            handleBootCompleted()
        }
    }

    private fun handleBootCompleted() {
        scope.launch {
            try {
                // Check if VPN auto-connect on boot is enabled
                val autoConnectOnBoot = appPreferences.vpnAutoConnectOnBoot.first()
                val vpnEnabled = appPreferences.vpnEnabled.first()
                val serverId = appPreferences.vpnServerId.first()

                Timber.d(
                    "VPN Auto-connect check: enabled=$vpnEnabled, autoConnectOnBoot=$autoConnectOnBoot, server=$serverId"
                )

                if (vpnEnabled && autoConnectOnBoot && serverId.isNotEmpty()) {
                    // Wait a bit for system to stabilize
                    kotlinx.coroutines.delay(10_000) // 10 seconds delay

                    Timber.d("Auto-connecting to VPN server: $serverId")
                    
                    // Connect to VPN
                    vpnRepository.connect(serverId)
                        .onSuccess {
                            Timber.d("VPN auto-connect successful")
                        }
                        .onFailure { error ->
                            Timber.e(error, "VPN auto-connect failed")
                        }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during VPN auto-connect on boot")
            }
        }
    }
}
