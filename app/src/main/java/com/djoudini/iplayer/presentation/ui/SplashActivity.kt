package com.djoudini.iplayer.presentation.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.djoudini.iplayer.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash Screen mit SOFORTIGER Anzeige.
 *
 * Wichtig: Das Theme (@style/Theme.DjoudinisSplash) zeigt sofort ein statisches
 * Bild an, noch bevor diese Activity geladen ist. Dadurch gibt es keinen
 * schwarzen Bildschirm beim Start auf Fire TV.
 *
 * Diese Activity fügt dann die Animation hinzu (5-10 Sekunden).
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            navigateToMainActivity()
        } else {
            // Some permissions denied, still continue but log warning
            navigateToMainActivity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sofort das Layout laden (Theme war schon sichtbar!)
        setContentView(R.layout.activity_splash)

        // Views finden
        val logoImageView = findViewById<ImageView>(R.id.splash_logo)
        val welcomeTextView = findViewById<TextView>(R.id.splash_welcome_text)
        val subtitleTextView = findViewById<TextView>(R.id.splash_subtitle)

        // Sofort Animationen starten
        startPulsingAnimation(logoImageView)
        startTextAnimations(welcomeTextView, subtitleTextView)

        // Permissions anfragen wenn nötig
        requestRequiredPermissions()

        // Nach 5-10 Sekunden zur MainActivity
        // Die Zeit kannst du hier anpassen (5000ms = 5s, 10000ms = 10s)
        lifecycleScope.launch {
            delay(SPLASH_DURATION_MS)
            navigateToMainActivity()
        }
    }

    /**
     * Request required permissions based on Android version.
     */
    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Storage permission for Android 6.0 - 9.0 (API 23-28)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
        ) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // Notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    /**
     * Pulsierende Animation für das Logo.
     * Läuft 5-10 Sekunden.
     */
    private fun startPulsingAnimation(logoImageView: ImageView) {
        // Scale Animation (Herzschlag-Effekt)
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.2f, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.2f, 1.0f)
        
        ObjectAnimator.ofPropertyValuesHolder(logoImageView, scaleX, scaleY).apply {
            duration = PULSE_DURATION_MS
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Leichte Rotation für visuelles Interesse
        ObjectAnimator.ofFloat(logoImageView, "rotation", -2f, 2f).apply {
            duration = ROTATION_DURATION_MS
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * Text-Fade-In Animationen.
     */
    private fun startTextAnimations(welcomeText: TextView, subtitleText: TextView) {
        // Willkommen Text
        welcomeText.alpha = 0f
        welcomeText.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Untertitel
        subtitleText.alpha = 0f
        subtitleText.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    /**
     * Wechsel zur MainActivity.
     */
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        finish()
    }

    companion object {
        // Hier kannst du die Dauer einstellen:
        // 3000L = 3 Sekunden
        // 5000L = 5 Sekunden
        // 8000L = 8 Sekunden  
        // 10000L = 10 Sekunden
        private const val SPLASH_DURATION_MS = 3000L // 3 Sekunden
        
        // Ein Puls dauert 1.5 Sekunden
        private const val PULSE_DURATION_MS = 1500L
        
        // Rotation dauert 4 Sekunden
        private const val ROTATION_DURATION_MS = 4000L
    }
}
