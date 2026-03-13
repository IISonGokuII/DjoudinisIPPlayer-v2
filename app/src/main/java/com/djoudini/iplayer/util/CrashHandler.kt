package com.djoudini.iplayer.util

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.widget.Toast
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global Crash Handler that saves crash reports to Downloads folder
 * and shows error dialog to user.
 * Also reports to Firebase Crashlytics for online monitoring.
 */
object CrashHandler : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var context: Context? = null
    private var currentActivity: Activity? = null

    /**
     * Initialize the crash handler. Call from Application.onCreate()
     */
    fun init(context: Context) {
        this.context = context.applicationContext
        
        // Initialize Firebase Crashlytics
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            FirebaseCrashlytics.getInstance().setCustomKey("app_version", getAppVersion(context))
            Timber.d("[CrashHandler] Firebase Crashlytics initialized")
        } catch (e: Exception) {
            Timber.e(e, "[CrashHandler] Firebase Crashlytics init failed (expected in debug builds without google-services.json)")
        }
        
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        Timber.d("[CrashHandler] Initialized")
    }

    /**
     * Set current activity for showing dialogs.
     * Call from Activity.onResume() and clear in onPause()
     */
    fun setCurrentActivity(activity: Activity?) {
        currentActivity = activity
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Timber.e(throwable, "[CrashHandler] Uncaught exception in thread: ${thread.name}")

        // Report to Firebase Crashlytics
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable)
            FirebaseCrashlytics.getInstance().setCustomKey("thread_name", thread.name)
            FirebaseCrashlytics.getInstance().log("Crash in thread: ${thread.name}")
            Timber.d("[CrashHandler] Reported to Firebase Crashlytics")
        } catch (e: Exception) {
            Timber.e(e, "[CrashHandler] Failed to report to Firebase")
        }

        // Save crash report to file
        val reportFile = saveCrashReport(throwable, thread)

        // Show error dialog if activity is available
        currentActivity?.let { activity ->
            if (!activity.isFinishing && !activity.isDestroyed) {
                try {
                    showErrorDialog(activity, throwable, reportFile)
                } catch (e: Exception) {
                    Timber.e(e, "[CrashHandler] Failed to show error dialog")
                }
            }
        }

        // Wait a bit for dialog to show, then pass to default handler
        try {
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            Timber.e(e, "[CrashHandler] Interrupted while waiting")
        }

        // Pass to original handler (usually shows system crash dialog)
        defaultHandler?.uncaughtException(thread, throwable)
    }

    /**
     * Save crash report to Downloads folder
     */
    private fun saveCrashReport(throwable: Throwable, thread: Thread): File? {
        val context = context ?: return null

        return try {
            // Get Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

            // Create app-specific crash reports folder
            val crashDir = File(downloadsDir, "DjoudinisIPPlayer_CrashReports")
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }

            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.GERMANY)
                .format(Date())
            val fileName = "crash_$timestamp.txt"
            val crashFile = File(crashDir, fileName)

            // Build crash report
            val report = buildCrashReport(throwable, thread, context)

            // Write to file
            FileWriter(crashFile).use { writer ->
                writer.write(report)
            }

            Timber.d("[CrashHandler] Crash report saved to: ${crashFile.absolutePath}")

            // Also save to app's internal storage (no permission needed)
            saveToInternalStorage(throwable, thread, context)

            crashFile
        } catch (e: Exception) {
            Timber.e(e, "[CrashHandler] Failed to save crash report")
            null
        }
    }

    /**
     * Save crash report to internal storage (no permission needed)
     */
    private fun saveToInternalStorage(throwable: Throwable, thread: Thread, context: Context): File? {
        return try {
            val crashDir = File(context.filesDir, "crash_reports")
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.GERMANY)
                .format(Date())
            val fileName = "crash_$timestamp.txt"
            val crashFile = File(crashDir, fileName)

            val report = buildCrashReport(throwable, thread, context)

            FileWriter(crashFile).use { writer ->
                writer.write(report)
            }

            Timber.d("[CrashHandler] Internal crash report saved to: ${crashFile.absolutePath}")
            crashFile
        } catch (e: Exception) {
            Timber.e(e, "[CrashHandler] Failed to save internal crash report")
            null
        }
    }

    /**
     * Build comprehensive crash report
     */
    private fun buildCrashReport(throwable: Throwable, thread: Thread, context: Context): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY)
            .format(Date())

        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        throwable.printStackTrace(printWriter)
        val stackTrace = writer.toString()

        // Get app version info
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }

        val report = StringBuilder()
        report.append("═══════════════════════════════════════════════════════════\n")
        report.append("           DJOUDINI'S IP PLAYER - CRASH REPORT\n")
        report.append("═══════════════════════════════════════════════════════════\n\n")

        report.append("📅 TIMESTAMP: $timestamp\n\n")

        report.append("📱 DEVICE INFO:\n")
        report.append("   Manufacturer: ${Build.MANUFACTURER}\n")
        report.append("   Model: ${Build.MODEL}\n")
        report.append("   Product: ${Build.PRODUCT}\n")
        report.append("   Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        report.append("   SDK: ${Build.VERSION.SDK}\n")
        report.append("   Display: ${Build.DISPLAY}\n\n")

        report.append("📦 APP INFO:\n")
        report.append("   Package: ${context.packageName}\n")
        report.append("   Version Name: ${packageInfo?.versionName ?: "unknown"}\n")
        report.append("   Version Code: ${packageInfo?.versionCode ?: "unknown"}\n")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            report.append("   Long Version Code: ${packageInfo?.longVersionCode ?: "unknown"}\n")
        }
        report.append("\n")

        report.append("🧵 THREAD INFO:\n")
        report.append("   Thread Name: ${thread.name}\n")
        report.append("   Thread ID: ${thread.id}\n\n")

        report.append("❌ EXCEPTION INFO:\n")
        report.append("   Type: ${throwable.javaClass.name}\n")
        report.append("   Message: ${throwable.message ?: "null"}\n")
        report.append("   Cause: ${throwable.cause?.javaClass?.name ?: "null"}\n\n")

        report.append("📚 STACK TRACE:\n")
        report.append("───────────────────────────────────────────────────────────\n")
        report.append(stackTrace)
        report.append("───────────────────────────────────────────────────────────\n\n")

        // Add suppressed exceptions if any
        if (throwable.suppressed.isNotEmpty()) {
            report.append("🗑️ SUPPRESSED EXCEPTIONS:\n")
            throwable.suppressed.forEachIndexed { index, suppressed ->
                report.append("   [$index] ${suppressed.javaClass.name}: ${suppressed.message}\n")
            }
            report.append("\n")
        }

        // Add cause chain
        var cause = throwable.cause
        var causeLevel = 0
        while (cause != null && causeLevel < 5) {
            report.append("🔗 CAUSED BY (Level ${++causeLevel}):\n")
            report.append("   Type: ${cause.javaClass.name}\n")
            report.append("   Message: ${cause.message}\n")
            val causeWriter = StringWriter()
            cause.printStackTrace(PrintWriter(causeWriter))
            report.append("   Stack:\n")
            causeWriter.toString().lines().forEach { line ->
                report.append("      $line\n")
            }
            cause = cause.cause
            report.append("\n")
        }

        report.append("═══════════════════════════════════════════════════════════\n")
        report.append("                      END OF REPORT\n")
        report.append("═══════════════════════════════════════════════════════════\n")

        return report.toString()
    }

    /**
     * Show error dialog with crash details
     */
    private fun showErrorDialog(activity: Activity, throwable: Throwable, reportFile: File?) {
        activity.runOnUiThread {
            try {
                val message = buildString {
                    append("Die App ist leider abgestürzt.\n\n")
                    append("Fehler: ${throwable.javaClass.simpleName}\n")
                    append("${throwable.message ?: "Unbekannter Fehler"}\n\n")

                    reportFile?.let {
                        append("📁 Crash-Report gespeichert:\n")
                        append("${it.absolutePath}\n\n")
                    }

                    append("Bitte sende diesen Report an den Entwickler.")
                }

                AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("😵 App-Absturz")
                    .setMessage(message)
                    .setPositiveButton("Kopieren") { _, _ ->
                        // Copy report to clipboard
                        copyToClipboard(activity, throwable, reportFile)
                        Toast.makeText(
                            activity,
                            "Fehler in Zwischenablage kopiert",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .setNegativeButton("Schließen") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            } catch (e: Exception) {
                Timber.e(e, "[CrashHandler] Failed to show error dialog")
            }
        }
    }

    /**
     * Copy crash report to clipboard
     */
    private fun copyToClipboard(context: Context, throwable: Throwable, reportFile: File?) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val reportText = buildString {
                append("CRASH REPORT - Djoudini's IP Player\n")
                append("═══════════════════════════════════\n\n")
                append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                append("\n")
                append("Error: ${throwable.javaClass.name}\n")
                append("Message: ${throwable.message}\n")
                append("\n")
                append("Stack Trace:\n")
                append("─────────────\n")

                val writer = StringWriter()
                throwable.printStackTrace(PrintWriter(writer))
                append(writer.toString())

                reportFile?.let {
                    append("\n")
                    append("─────────────\n")
                    append("Full report saved to: ${it.absolutePath}")
                }
            }

            val clip = ClipData.newPlainText("Crash Report", reportText)
            clipboard.setPrimaryClip(clip)
        } catch (e: Exception) {
            Timber.e(e, "[CrashHandler] Failed to copy to clipboard")
        }
    }

    /**
     * Get app version string for crash reports
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
