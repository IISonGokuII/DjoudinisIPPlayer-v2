package com.djoudini.iplayer.presentation.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.animation.doOnEnd
import androidx.lifecycle.lifecycleScope
import com.djoudini.iplayer.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TV-optimized Splash Screen with pulsing logo animation.
 * 
 * Shows a welcoming splash screen with:
 * - Pulsing app logo (heartbeat animation)
 * - "Willkommen bei DjoudiniTV" text
 * - Smooth fade-out transition to MainActivity
 * 
 * This prevents the black screen issue during app startup on Fire TV.
 */
class SplashActivity : ComponentActivity() {

    private lateinit var logoImageView: ImageView
    private lateinit var welcomeTextView: TextView
    private lateinit var subtitleTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set the splash screen layout
        setContentView(R.layout.activity_splash)

        // Initialize views
        logoImageView = findViewById(R.id.splash_logo)
        welcomeTextView = findViewById(R.id.splash_welcome_text)
        subtitleTextView = findViewById(R.id.splash_subtitle)

        // Start animations
        startPulsingAnimation()
        startTextAnimations()

        // Navigate to MainActivity after delay
        lifecycleScope.launch {
            delay(SPLASH_DURATION_MS)
            navigateToMainActivity()
        }
    }

    /**
     * Creates a pulsing/heartbeat animation for the logo.
     * The logo gently scales up and down to create a breathing effect.
     */
    private fun startPulsingAnimation() {
        // Scale animation (pulsing effect)
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.15f, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.15f, 1.0f)
        
        val pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(logoImageView, scaleX, scaleY).apply {
            duration = PULSE_DURATION_MS
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        pulseAnimator.start()

        // Add a subtle rotation animation for extra visual interest
        val rotateAnimator = ObjectAnimator.ofFloat(logoImageView, View.ROTATION, -3f, 3f).apply {
            duration = ROTATION_DURATION_MS
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        rotateAnimator.start()
    }

    /**
     * Animates the welcome text with a fade-in effect.
     */
    private fun startTextAnimations() {
        // Welcome text fade in
        welcomeTextView.alpha = 0f
        welcomeTextView.animate()
            .alpha(1f)
            .setDuration(FADE_IN_DURATION_MS)
            .setStartDelay(TEXT_DELAY_MS)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Subtitle fade in with more delay
        subtitleTextView.alpha = 0f
        subtitleTextView.animate()
            .alpha(1f)
            .setDuration(FADE_IN_DURATION_MS)
            .setStartDelay(TEXT_DELAY_MS + 300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    /**
     * Navigates to MainActivity with a smooth fade-out transition.
     */
    private fun navigateToMainActivity() {
        // Fade out all views
        val fadeOutViews = listOf(logoImageView, welcomeTextView, subtitleTextView)
        
        fadeOutViews.forEach { view ->
            view.animate()
                .alpha(0f)
                .setDuration(FADE_OUT_DURATION_MS)
                .setInterpolator(LinearInterpolator())
                .start()
        }

        // Start MainActivity after fade out begins
        lifecycleScope.launch {
            delay(FADE_OUT_DURATION_MS / 2)
            
            val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                // Prevent going back to splash
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            
            // Optional: Add a transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            
            finish()
        }
    }

    companion object {
        // Total splash duration: 2.5 seconds (enough to see the animation but not too long)
        private const val SPLASH_DURATION_MS = 2500L
        
        // Pulse animation duration: 1.2 seconds per pulse
        private const val PULSE_DURATION_MS = 1200L
        
        // Gentle rotation duration
        private const val ROTATION_DURATION_MS = 3000L
        
        // Fade in animation duration
        private const val FADE_IN_DURATION_MS = 600L
        
        // Fade out animation duration
        private const val FADE_OUT_DURATION_MS = 400L
        
        // Delay before text appears
        private const val TEXT_DELAY_MS = 200L
    }
}
