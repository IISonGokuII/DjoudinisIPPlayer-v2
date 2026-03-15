package com.djoudini.iplayer.data.local.entity

data class CloudRecordingSettings(
    val autoUploadEnabled: Boolean = false,
    val provider: CloudRecordingProvider = CloudRecordingProvider.NONE,
    val webDavBaseUrl: String = "",
    val webDavUsername: String = "",
    val webDavPassword: String = "",
    val webDavDirectory: String = "DjoudinisIPPlayer",
    val googleDriveAccessToken: String = "",
    val googleDriveRefreshToken: String = "",
    val googleDriveAccessTokenExpiryMs: Long = 0L,
    val googleDriveFolderId: String = "",
    val oneDriveAccessToken: String = "",
    val oneDriveRefreshToken: String = "",
    val oneDriveAccessTokenExpiryMs: Long = 0L,
    val oneDriveFolderPath: String = "DjoudinisIPPlayer",
)
