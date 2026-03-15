package com.djoudini.iplayer.presentation.ui.tv

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val googleDriveConnected by viewModel.googleDriveConnected.collectAsStateWithLifecycle()
    val oneDriveConnected by viewModel.oneDriveConnected.collectAsStateWithLifecycle()
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
                TvProviderStatusCard(
                    title = "WebDAV / Nextcloud",
                    body = "Direkte Cloud-Verbindung fuer eigene Server, Nextcloud und NAS.",
                    status = if (draft.webDavBaseUrl.isNotBlank()) "Konfiguriert" else "Noch nicht eingerichtet",
                    connected = draft.webDavBaseUrl.isNotBlank(),
                )
                TvCloudField("WebDAV-URL", draft.webDavBaseUrl) { draft = draft.copy(webDavBaseUrl = it) }
                TvCloudField("Benutzername", draft.webDavUsername) { draft = draft.copy(webDavUsername = it) }
                TvCloudField("Passwort", draft.webDavPassword) { draft = draft.copy(webDavPassword = it) }
                TvCloudField("Remote-Ordner", draft.webDavDirectory) { draft = draft.copy(webDavDirectory = it) }
            }
            CloudRecordingProvider.GOOGLE_DRIVE -> {
                TvProviderStatusCard(
                    title = "Google Drive",
                    body = "Echter Browser-Login mit Rueckkehr in die App. Danach steuerst du nur noch den Zielordner.",
                    status = if (googleDriveConnected) "Verbunden" else "Nicht verbunden",
                    connected = googleDriveConnected,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val url = viewModel.prepareGoogleDriveAuthorizationUrl()
                                if (url != null) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (googleDriveConnected) "Neu verbinden" else "Google Drive verbinden")
                    }
                    if (googleDriveConnected) {
                        TextButton(
                            onClick = {
                                viewModel.disconnectGoogleDrive()
                                draft = draft.copy(
                                    googleDriveAccessToken = "",
                                    googleDriveRefreshToken = "",
                                    googleDriveAccessTokenExpiryMs = 0L,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Trennen")
                        }
                    }
                }
                TvCloudField("Ordner-ID", draft.googleDriveFolderId) { draft = draft.copy(googleDriveFolderId = it) }
            }
            CloudRecordingProvider.ONEDRIVE -> {
                TvProviderStatusCard(
                    title = "OneDrive",
                    body = "TV-freundlicher Device-Code-Login. App zeigt den Code, Browser bestaetigt die Anmeldung.",
                    status = if (oneDriveConnected) "Verbunden" else "Nicht verbunden",
                    connected = oneDriveConnected,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.startOneDriveDeviceCode() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (oneDriveConnected) "Neu verbinden" else "OneDrive verbinden")
                    }
                    if (oneDriveConnected) {
                        TextButton(
                            onClick = {
                                viewModel.disconnectOneDrive()
                                draft = draft.copy(
                                    oneDriveAccessToken = "",
                                    oneDriveRefreshToken = "",
                                    oneDriveAccessTokenExpiryMs = 0L,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Trennen")
                        }
                    }
                }
                viewModel.oneDriveDeviceCodeSession?.let { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Code: ${session.userCode}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(session.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
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

@Composable
private fun TvProviderStatusCard(
    title: String,
    body: String,
    status: String,
    connected: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (connected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier
                    .background(
                        if (connected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    status,
                    color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
