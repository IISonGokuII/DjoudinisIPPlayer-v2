package com.djoudini.iplayer.presentation.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.djoudini.iplayer.data.cloud.CloudAuthRepository
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class CloudAuthCallbackActivity : ComponentActivity() {

    @Inject lateinit var cloudAuthRepository: CloudAuthRepository
    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callbackUri = intent?.data
        if (callbackUri == null) {
            finishToMain()
            return
        }

        lifecycleScope.launch {
            try {
                cloudAuthRepository.completeGoogleAuthorization(callbackUri)
                appPreferences.setCloudAuthStatus("Google Drive erfolgreich verbunden.", false)
            } catch (e: Exception) {
                Timber.e(e, "Google cloud auth callback failed")
                appPreferences.setCloudAuthStatus(e.message ?: "Google-Drive-Anmeldung fehlgeschlagen.", true)
            } finally {
                finishToMain()
            }
        }
    }

    private fun finishToMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
        finish()
    }
}
