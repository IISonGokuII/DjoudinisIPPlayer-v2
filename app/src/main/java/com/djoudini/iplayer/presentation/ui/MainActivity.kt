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
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.presentation.navigation.AppNavGraph
import com.djoudini.iplayer.presentation.navigation.Route
import com.djoudini.iplayer.presentation.ui.theme.DjoudinisTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playlistRepository: PlaylistRepository

    @Inject
    lateinit var appPreferences: AppPreferences

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
                    AppContent(playlistRepository = playlistRepository)
                }
            }
        }
    }
}

@Composable
private fun AppContent(playlistRepository: PlaylistRepository) {
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
        )
    }
}
