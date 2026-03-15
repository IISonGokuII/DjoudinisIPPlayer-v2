package com.djoudini.iplayer.presentation.ui.tv

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.data.local.entity.CloudRecordingProvider
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun TvCloudRecordingSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val currentSettings by viewModel.cloudRecordingSettings.collectAsStateWithLifecycle()
    val authStatusMessage by viewModel.cloudAuthStatusMessage.collectAsStateWithLifecycle()
    val authStatusIsError by viewModel.cloudAuthStatusIsError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by remember(currentSettings) { mutableStateOf(currentSettings) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FocusableCard(
                onClick = onBack,
                modifier = Modifier.padding(end = 24.dp),
                focusScale = 1.05f,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    Text("Zurueck", modifier = Modifier.padding(start = 12.dp))
                }
            }
            Column {
                Text("Cloud-Aufnahmen", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "TV-optimierte Einstellungen fuer Upload und Cloud-Verbindung.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Auto-Upload", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Aufnahme bleibt lokal und wird danach in die Cloud hochgeladen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = draft.autoUploadEnabled,
                onCheckedChange = { draft = draft.copy(autoUploadEnabled = it) },
            )
        }

        Text("Provider", style = MaterialTheme.typography.titleMedium)
        CloudRecordingProvider.entries.forEach { provider ->
            FocusableCard(
                onClick = { draft = draft.copy(provider = provider) },
                modifier = Modifier.fillMaxWidth(),
                focusScale = 1.03f,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        when (provider) {
                            CloudRecordingProvider.NONE -> "Nur lokal"
                            CloudRecordingProvider.WEBDAV -> "WebDAV / Nextcloud"
                            CloudRecordingProvider.GOOGLE_DRIVE -> "Google Drive"
                            CloudRecordingProvider.ONEDRIVE -> "OneDrive"
                        } + if (draft.provider == provider) "  (aktiv)" else "",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        when (draft.provider) {
            CloudRecordingProvider.WEBDAV -> {
                TvCloudField("WebDAV-URL", draft.webDavBaseUrl) { draft = draft.copy(webDavBaseUrl = it) }
                TvCloudField("Benutzername", draft.webDavUsername) { draft = draft.copy(webDavUsername = it) }
                TvCloudField("Passwort", draft.webDavPassword) { draft = draft.copy(webDavPassword = it) }
                TvCloudField("Remote-Ordner", draft.webDavDirectory) { draft = draft.copy(webDavDirectory = it) }
            }
            CloudRecordingProvider.GOOGLE_DRIVE -> {
                Text(
                    "Google Drive nutzt den Browser-Login und braucht eine hinterlegte Client-ID.",
                    style = MaterialTheme.typography.bodyMedium,
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
                TvCloudField("Ordner-ID", draft.googleDriveFolderId) { draft = draft.copy(googleDriveFolderId = it) }
            }
            CloudRecordingProvider.ONEDRIVE -> {
                Text(
                    "OneDrive nutzt den Device-Code-Flow und braucht eine hinterlegte Client-ID.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { viewModel.startOneDriveDeviceCode() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("OneDrive verbinden")
                }
                viewModel.oneDriveDeviceCodeSession?.let { session ->
                    Text("Code: ${session.userCode}", style = MaterialTheme.typography.headlineSmall)
                    Text(session.message, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(session.verificationUri))) },
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
                TvCloudField("Ordnerpfad", draft.oneDriveFolderPath) { draft = draft.copy(oneDriveFolderPath = it) }
            }
            CloudRecordingProvider.NONE -> {
                Text(
                    "Kein Cloud-Provider aktiv. Aufnahmen bleiben lokal in der App.",
                    style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun TvCloudField(
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
