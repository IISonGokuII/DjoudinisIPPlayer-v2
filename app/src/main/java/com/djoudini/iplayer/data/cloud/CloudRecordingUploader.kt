package com.djoudini.iplayer.data.cloud

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.djoudini.iplayer.data.local.entity.CloudRecordingProvider
import com.djoudini.iplayer.data.local.entity.CloudRecordingSettings
import java.io.File
import java.io.InputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.closeQuietly
import org.json.JSONObject

data class CloudUploadResult(
    val remotePath: String,
)

class CloudRecordingUploader(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    fun upload(
        recordingPath: String,
        fileName: String,
        settings: CloudRecordingSettings,
    ): CloudUploadResult {
        return when (settings.provider) {
            CloudRecordingProvider.WEBDAV -> uploadWebDav(recordingPath, fileName, settings)
            CloudRecordingProvider.GOOGLE_DRIVE -> uploadGoogleDrive(recordingPath, fileName, settings)
            CloudRecordingProvider.ONEDRIVE -> uploadOneDrive(recordingPath, fileName, settings)
            CloudRecordingProvider.NONE -> error("No cloud provider configured")
        }
    }

    private fun uploadWebDav(
        recordingPath: String,
        fileName: String,
        settings: CloudRecordingSettings,
    ): CloudUploadResult {
        val baseUrl = settings.webDavBaseUrl.trim().trimEnd('/')
        require(baseUrl.isNotBlank()) { "WebDAV-URL fehlt" }

        val remoteDirectory = settings.webDavDirectory.trim().trim('/').takeIf { it.isNotBlank() }
        val remoteUrl = buildString {
            append(baseUrl)
            append("/")
            if (remoteDirectory != null) {
                append(remoteDirectory)
                append("/")
            }
            append(fileName)
        }

        val bytes = context.openRecordingBytes(recordingPath)
        val request = Request.Builder()
            .url(remoteUrl)
            .put(bytes.toRequestBody("video/mp2t".toMediaType()))
            .apply {
                if (settings.webDavUsername.isNotBlank()) {
                    header("Authorization", okhttp3.Credentials.basic(settings.webDavUsername, settings.webDavPassword))
                }
            }
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("WebDAV-Upload fehlgeschlagen: HTTP ${response.code}")
            }
        }
        return CloudUploadResult(remotePath = remoteUrl)
    }

    private fun uploadGoogleDrive(
        recordingPath: String,
        fileName: String,
        settings: CloudRecordingSettings,
    ): CloudUploadResult {
        require(settings.googleDriveAccessToken.isNotBlank()) { "Google-Drive-Access-Token fehlt" }

        val metadata = JSONObject().apply {
            put("name", fileName)
            if (settings.googleDriveFolderId.isNotBlank()) {
                put("parents", listOf(settings.googleDriveFolderId))
            }
        }

        val startRequest = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable")
            .header("Authorization", "Bearer ${settings.googleDriveAccessToken}")
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("X-Upload-Content-Type", "video/mp2t")
            .post(metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()

        val sessionUrl = okHttpClient.newCall(startRequest).execute().use { response ->
            if (!response.isSuccessful) {
                error("Google-Drive-Session fehlgeschlagen: HTTP ${response.code}")
            }
            response.header("Location") ?: error("Google Drive lieferte keine Upload-URL")
        }

        val uploadBody = context.openRecordingRequestBody(recordingPath)
        val uploadRequest = Request.Builder()
            .url(sessionUrl)
            .header("Authorization", "Bearer ${settings.googleDriveAccessToken}")
            .header("Content-Type", "video/mp2t")
            .put(uploadBody)
            .build()

        val fileId = okHttpClient.newCall(uploadRequest).execute().use { response ->
            if (!response.isSuccessful) {
                error("Google-Drive-Upload fehlgeschlagen: HTTP ${response.code}")
            }
            JSONObject(response.body?.string().orEmpty()).optString("id")
        }

        return CloudUploadResult(
            remotePath = if (fileId.isNotBlank()) "gdrive://$fileId" else "gdrive://$fileName",
        )
    }

    private fun uploadOneDrive(
        recordingPath: String,
        fileName: String,
        settings: CloudRecordingSettings,
    ): CloudUploadResult {
        require(settings.oneDriveAccessToken.isNotBlank()) { "OneDrive-Access-Token fehlt" }
        val remoteFolder = settings.oneDriveFolderPath.trim().trim('/').takeIf { it.isNotBlank() }
        val remotePath = buildString {
            if (remoteFolder != null) {
                append(remoteFolder)
                append("/")
            }
            append(fileName)
        }
        val encodedPath = remotePath.split("/").joinToString("/") { Uri.encode(it) }

        val sessionRequest = Request.Builder()
            .url("https://graph.microsoft.com/v1.0/me/drive/root:/$encodedPath:/createUploadSession")
            .header("Authorization", "Bearer ${settings.oneDriveAccessToken}")
            .header("Content-Type", "application/json")
            .post(
                """{"item":{"@microsoft.graph.conflictBehavior":"replace"}}"""
                    .toRequestBody("application/json".toMediaType()),
            )
            .build()

        val uploadUrl = okHttpClient.newCall(sessionRequest).execute().use { response ->
            if (!response.isSuccessful) {
                error("OneDrive-Session fehlgeschlagen: HTTP ${response.code}")
            }
            JSONObject(response.body?.string().orEmpty()).optString("uploadUrl")
        }
        require(uploadUrl.isNotBlank()) { "OneDrive lieferte keine Upload-URL" }

        val chunkSize = 320 * 1024 * 16
        val fileSize = context.recordingSize(recordingPath)
        context.openRecordingInputStream(recordingPath).use { input ->
            requireNotNull(input) { "Aufnahmedatei konnte nicht gelesen werden" }
            var offset = 0L
            val buffer = ByteArray(chunkSize)
            while (offset < fileSize) {
                val read = input.read(buffer, 0, minOf(chunkSize.toLong(), fileSize - offset).toInt())
                if (read <= 0) break
                val end = offset + read - 1
                val chunkRequest = Request.Builder()
                    .url(uploadUrl)
                    .header("Content-Length", read.toString())
                    .header("Content-Range", "bytes $offset-$end/$fileSize")
                    .put(buffer.copyOf(read).toRequestBody("application/octet-stream".toMediaType()))
                    .build()
                okHttpClient.newCall(chunkRequest).execute().use { response ->
                    if (!response.isSuccessful && response.code !in listOf(200, 201, 202)) {
                        error("OneDrive-Upload fehlgeschlagen: HTTP ${response.code}")
                    }
                }
                offset += read
            }
        }

        return CloudUploadResult(remotePath = "onedrive://$remotePath")
    }
}

private fun Context.openRecordingBytes(path: String): ByteArray {
    return openRecordingInputStream(path)?.use(InputStream::readBytes)
        ?: error("Aufnahmedatei konnte nicht gelesen werden")
}

private fun Context.openRecordingRequestBody(path: String): RequestBody {
    return if (path.startsWith("content://")) {
        openRecordingBytes(path).toRequestBody("video/mp2t".toMediaType())
    } else {
        File(path).asRequestBody("video/mp2t".toMediaType())
    }
}

private fun Context.recordingSize(path: String): Long {
    return if (path.startsWith("content://")) {
        contentResolver.openAssetFileDescriptor(Uri.parse(path), "r")?.use { it.length } ?: 0L
    } else {
        File(path).length()
    }
}

private fun Context.openRecordingInputStream(path: String): InputStream? {
    return if (path.startsWith("content://")) {
        contentResolver.openInputStream(Uri.parse(path))
    } else {
        File(path).inputStream()
    }
}
