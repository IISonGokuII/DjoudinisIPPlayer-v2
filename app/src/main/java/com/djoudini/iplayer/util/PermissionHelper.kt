package com.djoudini.iplayer.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Permission helper object for requesting runtime permissions.
 * Handles storage permissions for crash reports (Android 6.0 - 9.0).
 */
object PermissionHelper {

    private const val PERMISSION_REQUEST_STORAGE = 1001

    /**
     * Check if storage permission is granted.
     * Only needed for Android 6.0 - 9.0 (API 23-28).
     * Android 10+ (API 29+) uses scoped storage, no permission needed for app-specific folders.
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        // Android 10+ doesn't need permission for app-specific folders
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Timber.d("[PermissionHelper] Android 10+, no storage permission needed")
            return true
        }

        // Android 6.0 - 9.0: Check WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            Timber.d("[PermissionHelper] Storage permission granted: $hasPermission")
            return hasPermission
        }

        // Android 5.x and below: Permission granted at install time
        Timber.d("[PermissionHelper] Android 5.x, permission granted at install")
        return true
    }

    /**
     * Request storage permission from activity.
     * Only needed for Android 6.0 - 9.0 (API 23-28).
     */
    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Timber.d("[PermissionHelper] Android 10+, no permission request needed")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Timber.d("[PermissionHelper] Requesting storage permission...")

            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_STORAGE
            )
        }
    }

    /**
     * Check if notification permission is needed (Android 13+).
     * Required for foreground service notifications.
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        // Android 13+ requires POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            Timber.d("[PermissionHelper] Notification permission granted: $hasPermission")
            return hasPermission
        }

        // Android 12 and below: No notification permission needed
        Timber.d("[PermissionHelper] Android 12 or below, no notification permission needed")
        return true
    }

    /**
     * Request notification permission (Android 13+).
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Timber.d("[PermissionHelper] Android 12 or below, no permission request needed")
            return
        }

        Timber.d("[PermissionHelper] Requesting notification permission...")

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            PERMISSION_REQUEST_STORAGE + 1
        )
    }

    /**
     * Check if all required permissions are granted.
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        val hasStorage = isStoragePermissionGranted(context)
        val hasNotification = isNotificationPermissionGranted(context)

        Timber.d("[PermissionHelper] All permissions: storage=$hasStorage, notification=$hasNotification")
        return hasStorage && hasNotification
    }

    /**
     * Handle permission request result.
     * Call this from Activity.onRequestPermissionsResult()
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        return when (requestCode) {
            PERMISSION_REQUEST_STORAGE -> {
                val granted = grantResults.isNotEmpty() &&
                        grantResults.all { it == PackageManager.PERMISSION_GRANTED }

                if (granted) {
                    Timber.d("[PermissionHelper] Storage permission granted")
                } else {
                    Timber.w("[PermissionHelper] Storage permission denied")
                }

                granted
            }

            PERMISSION_REQUEST_STORAGE + 1 -> {
                val granted = grantResults.isNotEmpty() &&
                        grantResults.all { it == PackageManager.PERMISSION_GRANTED }

                if (granted) {
                    Timber.d("[PermissionHelper] Notification permission granted")
                } else {
                    Timber.w("[PermissionHelper] Notification permission denied")
                }

                granted
            }

            else -> {
                Timber.d("[PermissionHelper] Unknown permission request code: $requestCode")
                false
            }
        }
    }

    /**
     * Check if permission should be shown with rationale.
     */
    fun shouldShowPermissionRationale(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        return false
    }
}
