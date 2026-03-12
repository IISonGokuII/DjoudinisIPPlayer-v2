package com.djoudini.iplayer.presentation.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
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
class SplashActivity : ComponentActivity() {

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

        // Nach 5-10 Sekunden zur MainActivity
        // Die Zeit kannst du hier anpassen (5000ms = 5s, 10000ms = 10s)
        lifecycleScope.launch {
            delay(SPLASH_DURATION_MS)
            navigateToMainActivity()
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
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
