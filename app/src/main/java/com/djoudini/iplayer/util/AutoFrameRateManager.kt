package com.djoudini.iplayer.util

import android.app.Activity
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auto Frame Rate (AFR) Manager.
 *
 * Automatically adjusts the TV display refresh rate to match the content's frame rate.
 * Supports common rates: 23.976, 24, 25, 29.97, 30, 50, 59.94, 60 fps.
 *
 * Uses:
 * - Android 6.0+: Display.Mode API for preferred display mode
 * - Android 12+: Surface.setFrameRate() for seamless switching
 */
@Singleton
class AutoFrameRateManager @Inject constructor() {

    private var originalMode: Display.Mode? = null

    /**
     * Set the display to match the given content frame rate.
     *
     * @param activity The current activity (needed for window access).
     * @param contentFrameRate The frame rate of the video content.
     * @param surface The playback surface (for Android 12+ seamless switching).
     */
    fun matchFrameRate(activity: Activity, contentFrameRate: Float, surface: Surface? = null) {
        if (contentFrameRate <= 0f) return

        Timber.d("AFR: Requested frame rate match for ${contentFrameRate}fps")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && surface != null) {
            // Android 12+: Use Surface.setFrameRate for seamless switching
            matchFrameRateViaSurface(surface, contentFrameRate)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+: Use Display.Mode API
            matchFrameRateViaDisplayMode(activity, contentFrameRate)
        }
    }

    /**
     * Android 12+: Seamless frame rate switching via Surface API.
     */
    private fun matchFrameRateViaSurface(surface: Surface, frameRate: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                surface.setFrameRate(
                    frameRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    Surface.CHANGE_FRAME_RATE_ALWAYS,
                )
                Timber.i("AFR: Set surface frame rate to ${frameRate}fps (seamless)")
            } catch (e: Exception) {
                Timber.w(e, "AFR: Failed to set surface frame rate")
            }
        }
    }

    /**
     * Android 6+: Match by selecting the best display mode.
     */
    private fun matchFrameRateViaDisplayMode(activity: Activity, targetFps: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        try {
            val display = activity.windowManager.defaultDisplay
            val supportedModes = display.supportedModes
            val currentMode = display.mode

            // Save original mode for restoration
            if (originalMode == null) {
                originalMode = currentMode
            }

            // Find the best matching mode:
            // 1. Same resolution as current
            // 2. Closest refresh rate to target (or exact multiple)
            val bestMode = supportedModes
                .filter { mode ->
                    mode.physicalWidth == currentMode.physicalWidth &&
                    mode.physicalHeight == currentMode.physicalHeight
                }
                .minByOrNull { mode ->
                    val modeRate = mode.refreshRate
                    // Prefer exact match or integer multiple
                    val diff = if (isMultiple(modeRate, targetFps)) {
                        0f
                    } else {
                        kotlin.math.abs(modeRate - targetFps)
                    }
                    diff
                }

            if (bestMode != null && bestMode.modeId != currentMode.modeId) {
                val params = activity.window.attributes
                params.preferredDisplayModeId = bestMode.modeId
                activity.window.attributes = params
                Timber.i("AFR: Switched display mode to ${bestMode.refreshRate}Hz (target: ${targetFps}fps)")
            } else {
                Timber.d("AFR: Current mode ${currentMode.refreshRate}Hz is already optimal for ${targetFps}fps")
            }
        } catch (e: Exception) {
            Timber.w(e, "AFR: Failed to switch display mode")
        }
    }

    /**
     * Restore the original display refresh rate.
     */
    fun restoreOriginalFrameRate(activity: Activity, surface: Surface? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && surface != null) {
            try {
                surface.setFrameRate(
                    0f,
                    Surface.FRAME_RATE_COMPATIBILITY_DEFAULT,
                    Surface.CHANGE_FRAME_RATE_ALWAYS,
                )
                Timber.i("AFR: Reset surface frame rate")
            } catch (e: Exception) {
                Timber.w(e, "AFR: Failed to reset surface frame rate")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            originalMode?.let { mode ->
                try {
                    val params = activity.window.attributes
                    params.preferredDisplayModeId = mode.modeId
                    activity.window.attributes = params
                    Timber.i("AFR: Restored display mode to ${mode.refreshRate}Hz")
                } catch (e: Exception) {
                    Timber.w(e, "AFR: Failed to restore display mode")
                }
                originalMode = null
            }
        }
    }

    /**
     * Check if a display rate is an integer multiple of the content frame rate.
     * E.g., 60Hz is a valid multiple for 24fps (60/24 = 2.5 -> no), 30fps (60/30 = 2 -> yes).
     */
    private fun isMultiple(displayRate: Float, contentRate: Float): Boolean {
        if (contentRate <= 0f) return false
        val ratio = displayRate / contentRate
        return kotlin.math.abs(ratio - kotlin.math.round(ratio)) < 0.02f
    }

    companion object {
        /** Common video frame rates */
        const val FPS_23_976 = 23.976f
        const val FPS_24 = 24f
        const val FPS_25 = 25f
        const val FPS_29_97 = 29.97f
        const val FPS_30 = 30f
        const val FPS_50 = 50f
        const val FPS_59_94 = 59.94f
        const val FPS_60 = 60f
    }
}
