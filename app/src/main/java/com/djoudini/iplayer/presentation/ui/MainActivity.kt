package com.djoudini.iplayer.presentation.ui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.djoudini.iplayer.DjoudinisApp
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.data.repository.TraktRepository
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.domain.repository.WatchProgressRepository
import com.djoudini.iplayer.presentation.navigation.AppNavGraph
import com.djoudini.iplayer.presentation.navigation.Route
import com.djoudini.iplayer.presentation.ui.theme.DjoudinisTheme

class MainActivity : ComponentActivity() {

    private val playlistRepository: PlaylistRepository by lazy {
        (application as DjoudinisApp).playlistRepository
    }

    private val appPreferences: AppPreferences by lazy {
        (application as DjoudinisApp).appPreferences
    }

    private val traktRepository: TraktRepository by lazy {
        (application as DjoudinisApp).traktRepository
    }

    private val watchProgressRepository: WatchProgressRepository by lazy {
        (application as DjoudinisApp).watchProgressRepository
    }

    private val isTvDevice: Boolean by lazy {
        // ONLY true for actual TV devices (Fire TV, Android TV)
        // Check for Leanback feature - this is the most reliable indicator
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
                        traktRepository = traktRepository,
                        watchProgressRepository = watchProgressRepository,
                        isTvDevice = isTvDevice,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppContent(
    playlistRepository: PlaylistRepository,
    appPreferences: AppPreferences,
    traktRepository: TraktRepository,
    watchProgressRepository: WatchProgressRepository,
    isTvDevice: Boolean,
) {
    val navController = rememberNavController()

    // Determine start destination: Dashboard if playlist exists, else Onboarding
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val active = playlistRepository.getActive()
        startDestination = if (active != null) {
            Route.Dashboard.route
        } else {
            Route.Onboarding.route
        }
    }

    startDestination?.let { start ->
        AppNavGraph(
            navController = navController,
            startDestination = start,
            isTvDevice = isTvDevice,
            playlistRepository = playlistRepository,
            appPreferences = appPreferences,
            traktRepository = traktRepository,
            watchProgressRepository = watchProgressRepository,
        )
    }
}
