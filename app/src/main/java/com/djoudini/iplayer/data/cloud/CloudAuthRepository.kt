package com.djoudini.iplayer.data.cloud

import android.util.Base64
import android.content.Context
import android.net.Uri
import com.djoudini.iplayer.BuildConfig
import com.djoudini.iplayer.data.local.entity.CloudRecordingProvider
import com.djoudini.iplayer.data.local.entity.CloudRecordingSettings
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class OneDriveDeviceCodeSession(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int,
    val message: String,
)

@Singleton
class CloudAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val appPreferences: AppPreferences,
) {

    suspend fun buildGoogleAuthorizationUrl(): String {
        require(BuildConfig.GOOGLE_DRIVE_CLIENT_ID.isNotBlank()) { "Google Drive Client ID fehlt" }
        val state = randomUrlSafe(32)
        val verifier = randomUrlSafe(64)
        val challenge = verifier.sha256UrlSafe()
        appPreferences.storePendingGoogleAuth(state, verifier)

        return Uri.Builder()
            .scheme("https")
            .authority("accounts.google.com")
            .path("o/oauth2/v2/auth")
            .appendQueryParameter("client_id", BuildConfig.GOOGLE_DRIVE_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.GOOGLE_DRIVE_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "https://www.googleapis.com/auth/drive.file")
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }

    suspend fun completeGoogleAuthorization(callbackUri: Uri) {
        val code = callbackUri.getQueryParameter("code") ?: error("Google-Code fehlt")
        val returnedState = callbackUri.getQueryParameter("state") ?: ""
        val (expectedState, verifier) = appPreferences.consumePendingGoogleAuth()
        require(expectedState.isNotBlank() && expectedState == returnedState) { "Google-State ungueltig" }

        val body = FormBody.Builder()
            .add("client_id", BuildConfig.GOOGLE_DRIVE_CLIENT_ID)
            .add("redirect_uri", BuildConfig.GOOGLE_DRIVE_REDIRECT_URI)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("code_verifier", verifier)
            .build()

        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(body)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Google Token-Exchange fehlgeschlagen: HTTP ${response.code}")
            val json = JSONObject(response.body?.string().orEmpty())
            val settings = appPreferences.cloudRecordingSettings.first()
            appPreferences.updateCloudRecordingSettings(
                settings.copy(
                    provider = CloudRecordingProvider.GOOGLE_DRIVE,
                    googleDriveAccessToken = json.optString("access_token"),
                    googleDriveRefreshToken = json.optString("refresh_token", settings.googleDriveRefreshToken),
                    googleDriveAccessTokenExpiryMs = System.currentTimeMillis() + json.optLong("expires_in") * 1000L,
                ),
            )
        }
    }

    suspend fun startOneDriveDeviceCode(): OneDriveDeviceCodeSession {
        require(BuildConfig.ONEDRIVE_CLIENT_ID.isNotBlank()) { "OneDrive Client ID fehlt" }
        val body = FormBody.Builder()
            .add("client_id", BuildConfig.ONEDRIVE_CLIENT_ID)
            .add("scope", "offline_access Files.ReadWrite")
            .build()
        val request = Request.Builder()
            .url("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode")
            .post(body)
            .build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("OneDrive Device-Code fehlgeschlagen: HTTP ${response.code}")
            val json = JSONObject(response.body?.string().orEmpty())
            OneDriveDeviceCodeSession(
                deviceCode = json.getString("device_code"),
                userCode = json.getString("user_code"),
                verificationUri = json.optString("verification_uri", json.optString("verification_uri_complete")),
                expiresInSeconds = json.getInt("expires_in"),
                intervalSeconds = json.optInt("interval", 5),
                message = json.optString("message"),
            )
        }
    }

    suspend fun pollOneDriveDeviceCode(session: OneDriveDeviceCodeSession) {
        val deadline = System.currentTimeMillis() + session.expiresInSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            val body = FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .add("client_id", BuildConfig.ONEDRIVE_CLIENT_ID)
                .add("device_code", session.deviceCode)
                .build()
            val request = Request.Builder()
                .url("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
                .post(body)
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(raw)
                    val settings = appPreferences.cloudRecordingSettings.first()
                    appPreferences.updateCloudRecordingSettings(
                        settings.copy(
                            provider = CloudRecordingProvider.ONEDRIVE,
                            oneDriveAccessToken = json.optString("access_token"),
                            oneDriveRefreshToken = json.optString("refresh_token", settings.oneDriveRefreshToken),
                            oneDriveAccessTokenExpiryMs = System.currentTimeMillis() + json.optLong("expires_in") * 1000L,
                        ),
                    )
                    return
                }
                val error = runCatching { JSONObject(raw).optString("error") }.getOrDefault("")
                if (error == "authorization_pending") {
                    delay(session.intervalSeconds * 1000L)
                    return@use
                }
                if (error == "slow_down") {
                    delay((session.intervalSeconds + 5) * 1000L)
                    return@use
                }
                error("OneDrive Login fehlgeschlagen: $error")
            }
        }
        error("OneDrive Device-Code abgelaufen")
    }

    suspend fun resolvedCloudSettings(): CloudRecordingSettings {
        var settings = appPreferences.cloudRecordingSettings.first()
        when (settings.provider) {
            CloudRecordingProvider.GOOGLE_DRIVE -> {
                if (settings.googleDriveRefreshToken.isNotBlank() && settings.googleDriveAccessTokenExpiryMs <= System.currentTimeMillis() + 60_000) {
                    settings = refreshGoogleToken(settings)
                }
            }
            CloudRecordingProvider.ONEDRIVE -> {
                if (settings.oneDriveRefreshToken.isNotBlank() && settings.oneDriveAccessTokenExpiryMs <= System.currentTimeMillis() + 60_000) {
                    settings = refreshOneDriveToken(settings)
                }
            }
            else -> Unit
        }
        return settings
    }

    private suspend fun refreshGoogleToken(settings: CloudRecordingSettings): CloudRecordingSettings {
        val body = FormBody.Builder()
            .add("client_id", BuildConfig.GOOGLE_DRIVE_CLIENT_ID)
            .add("grant_type", "refresh_token")
            .add("refresh_token", settings.googleDriveRefreshToken)
            .build()
        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(body)
            .build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Google Token-Refresh fehlgeschlagen: HTTP ${response.code}")
            val json = JSONObject(response.body?.string().orEmpty())
            val updated = settings.copy(
                googleDriveAccessToken = json.optString("access_token"),
                googleDriveAccessTokenExpiryMs = System.currentTimeMillis() + json.optLong("expires_in") * 1000L,
            )
            appPreferences.updateCloudRecordingSettings(updated)
            updated
        }
    }

    private suspend fun refreshOneDriveToken(settings: CloudRecordingSettings): CloudRecordingSettings {
        val body = FormBody.Builder()
            .add("client_id", BuildConfig.ONEDRIVE_CLIENT_ID)
            .add("grant_type", "refresh_token")
            .add("refresh_token", settings.oneDriveRefreshToken)
            .add("scope", "offline_access Files.ReadWrite")
            .build()
        val request = Request.Builder()
            .url("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
            .post(body)
            .build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("OneDrive Token-Refresh fehlgeschlagen: HTTP ${response.code}")
            val json = JSONObject(response.body?.string().orEmpty())
            val updated = settings.copy(
                oneDriveAccessToken = json.optString("access_token"),
                oneDriveRefreshToken = json.optString("refresh_token", settings.oneDriveRefreshToken),
                oneDriveAccessTokenExpiryMs = System.currentTimeMillis() + json.optLong("expires_in") * 1000L,
            )
            appPreferences.updateCloudRecordingSettings(updated)
            updated
        }
    }
}

private fun randomUrlSafe(length: Int): String {
    val bytes = ByteArray(length)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

private fun String.sha256UrlSafe(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
