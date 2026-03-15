package com.djoudini.iplayer.presentation.ui.mobile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.data.local.entity.CloudRecordingProvider
import com.djoudini.iplayer.data.local.entity.CloudRecordingSettings
import com.djoudini.iplayer.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun CloudRecordingSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val currentSettings by viewModel.cloudRecordingSettings.collectAsStateWithLifecycle()
    val authStatusMessage by viewModel.cloudAuthStatusMessage.collectAsStateWithLifecycle()
    val authStatusIsError by viewModel.cloudAuthStatusIsError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by remember(currentSettings) { mutableStateOf(currentSettings) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud-Aufnahmen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Aufnahmen lokal speichern und danach automatisch in die Cloud hochladen.",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (authStatusMessage.isNotBlank()) {
                Button(
                    onClick = { viewModel.clearCloudAuthStatus() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (authStatusIsError) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (authStatusIsError) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text(authStatusMessage)
                }
            }

            ToggleRow(
                title = "Auto-Upload",
                checked = draft.autoUploadEnabled,
                onCheckedChange = { draft = draft.copy(autoUploadEnabled = it) },
            )

            ProviderSelector(
                provider = draft.provider,
                onProviderChange = { draft = draft.copy(provider = it) },
            )

            when (draft.provider) {
                CloudRecordingProvider.WEBDAV -> {
                    SettingsField("WebDAV-URL", draft.webDavBaseUrl) { draft = draft.copy(webDavBaseUrl = it) }
                    SettingsField("Benutzername", draft.webDavUsername) { draft = draft.copy(webDavUsername = it) }
                    SettingsField("Passwort", draft.webDavPassword) { draft = draft.copy(webDavPassword = it) }
                    SettingsField("Remote-Ordner", draft.webDavDirectory) { draft = draft.copy(webDavDirectory = it) }
                }
                CloudRecordingProvider.GOOGLE_DRIVE -> {
                    Text(
                        text = "Google Drive kann direkt verbunden werden, braucht aber eine hinterlegte Client-ID in der App-Konfiguration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                val url = viewModel.prepareGoogleDriveAuthorizationUrl()
                                if (url != null) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Google Drive verbinden")
                    }
                    SettingsField("Access-Token", draft.googleDriveAccessToken) { draft = draft.copy(googleDriveAccessToken = it) }
                    SettingsField("Ordner-ID", draft.googleDriveFolderId) { draft = draft.copy(googleDriveFolderId = it) }
                }
                CloudRecordingProvider.ONEDRIVE -> {
                    Text(
                        text = "OneDrive nutzt den Device-Code-Flow und braucht eine hinterlegte Client-ID in der App-Konfiguration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { viewModel.startOneDriveDeviceCode() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("OneDrive verbinden")
                    }
                    viewModel.oneDriveDeviceCodeSession?.let { session ->
                        Text("Code: ${session.userCode}", style = MaterialTheme.typography.titleMedium)
                        Text(session.message, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(session.verificationUri)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Anmeldeseite oeffnen")
                        }
                        Button(
                            onClick = { viewModel.completeOneDriveDeviceCodeLogin() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Login pruefen")
                        }
                    }
                    SettingsField("Access-Token", draft.oneDriveAccessToken) { draft = draft.copy(oneDriveAccessToken = it) }
                    SettingsField("Ordnerpfad", draft.oneDriveFolderPath) { draft = draft.copy(oneDriveFolderPath = it) }
                }
                CloudRecordingProvider.NONE -> {
                    Text(
                        text = "Kein Cloud-Provider aktiv. Aufnahmen bleiben lokal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.updateCloudRecordingSettings(draft)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Speichern")
            }
        }
    }
}

@Composable
private fun ProviderSelector(
    provider: CloudRecordingProvider,
    onProviderChange: (CloudRecordingProvider) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Cloud-Provider", style = MaterialTheme.typography.titleSmall)
        CloudRecordingProvider.entries.forEach { entry ->
            Button(
                onClick = { onProviderChange(entry) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when (entry) {
                        CloudRecordingProvider.NONE -> "Nur lokal"
                        CloudRecordingProvider.WEBDAV -> "WebDAV / Nextcloud"
                        CloudRecordingProvider.GOOGLE_DRIVE -> "Google Drive"
                        CloudRecordingProvider.ONEDRIVE -> "OneDrive"
                    } + if (entry == provider) "  (aktiv)" else "",
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}
