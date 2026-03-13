package com.djoudini.iplayer.presentation.ui

import android.app.Activity
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.data.service.VpnPermissionManager
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import com.djoudini.iplayer.presentation.navigation.AppNavGraph
import com.djoudini.iplayer.presentation.navigation.Route
import com.djoudini.iplayer.presentation.ui.theme.DjoudinisTheme
import com.djoudini.iplayer.presentation.viewmodel.SettingsViewModel
import com.djoudini.iplayer.util.CrashHandler
import com.djoudini.iplayer.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playlistRepository: PlaylistRepository

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var watchProgressRepository: WatchProgressRepository

    @Inject
    lateinit var vpnPermissionManager: VpnPermissionManager

    private val isTvDevice: Boolean by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePref by appPreferences.theme.collectAsStateWithLifecycle(initialValue = "dark")
            val isDark = when (themePref) {
                "dark" -> true
                "light" -> false
                else -> isTvDevice || isSystemInDarkTheme()
            }

            DjoudinisTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppContent(
                        playlistRepository = playlistRepository,
                        appPreferences = appPreferences,
                        watchProgressRepository = watchProgressRepository,
                        isTvDevice = isTvDevice,
                        vpnPermissionManager = vpnPermissionManager,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CrashHandler.setCurrentActivity(this)
        Timber.d("[MainActivity] onResume()")
    }

    override fun onPause() {
        super.onPause()
        CrashHandler.setCurrentActivity(null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val handled = PermissionHelper.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )
        Timber.d("[MainActivity] Permission result handled: $handled")
    }
}

@Composable
private fun AppContent(
    playlistRepository: PlaylistRepository,
    appPreferences: AppPreferences,
    watchProgressRepository: WatchProgressRepository,
    isTvDevice: Boolean,
    vpnPermissionManager: VpnPermissionManager,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val active = playlistRepository.getActive()
        startDestination = if (active != null) {
            Route.Dashboard.route
        } else {
            Route.Onboarding.route
        }
    }

    // VPN permission launcher — shows Android's system dialog
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Timber.d("VPN permission granted")
            vpnPermissionManager.onPermissionGranted()
        } else {
            Timber.w("VPN permission denied")
            vpnPermissionManager.onPermissionDenied()
        }
    }

    // Collect VPN permission requests from the repository
    LaunchedEffect(vpnPermissionManager) {
        vpnPermissionManager.permissionRequest.collect {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                // System dialog needed
                vpnPermissionLauncher.launch(intent)
            } else {
                // Already granted — notify immediately
                vpnPermissionManager.onPermissionGranted()
            }
        }
    }

    val settingsViewModel: SettingsViewModel = hiltViewModel()

    startDestination?.let { start ->
        AppNavGraph(
            navController = navController,
            startDestination = start,
            isTvDevice = isTvDevice,
            appPreferences = appPreferences,
            playlistRepository = playlistRepository,
            watchProgressRepository = watchProgressRepository,
            settingsViewModel = settingsViewModel,
            onChannelClick = { channelId ->
                navController.navigate(Route.Player.create("channel", channelId))
            },
        )
    }
}
