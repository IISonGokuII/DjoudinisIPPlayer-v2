package com.djoudini.iplayer.presentation.ui.mobile

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.data.local.entity.CloudRecordingProvider
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
    val googleDriveConnected by viewModel.googleDriveConnected.collectAsStateWithLifecycle()
    val oneDriveConnected by viewModel.oneDriveConnected.collectAsStateWithLifecycle()
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
                    ProviderInfoCard(
                        title = "WebDAV / Nextcloud",
                        body = "Direkte Serververbindung fuer Nextcloud, NAS oder andere WebDAV-Ziele.",
                        status = if (draft.webDavBaseUrl.isNotBlank()) "Manuelle Verbindung konfiguriert" else "Noch nicht eingerichtet",
                        connected = draft.webDavBaseUrl.isNotBlank(),
                    )
                    SettingsField("WebDAV-URL", draft.webDavBaseUrl) { draft = draft.copy(webDavBaseUrl = it) }
                    SettingsField("Benutzername", draft.webDavUsername) { draft = draft.copy(webDavUsername = it) }
                    SettingsField("Passwort", draft.webDavPassword) { draft = draft.copy(webDavPassword = it) }
                    SettingsField("Remote-Ordner", draft.webDavDirectory) { draft = draft.copy(webDavDirectory = it) }
                }
                CloudRecordingProvider.GOOGLE_DRIVE -> {
                    ProviderInfoCard(
                        title = "Google Drive",
                        body = "Echter Browser-Login mit Rueckkehr in die App. Nach erfolgreicher Anmeldung bleibt nur noch die Zielordner-ID konfigurierbar.",
                        status = if (googleDriveConnected) "Verbunden" else "Nicht verbunden",
                        connected = googleDriveConnected,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
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
                    SettingsField("Ordner-ID", draft.googleDriveFolderId) { draft = draft.copy(googleDriveFolderId = it) }
                }
                CloudRecordingProvider.ONEDRIVE -> {
                    ProviderInfoCard(
                        title = "OneDrive",
                        body = "Echter TV-freundlicher Device-Code-Login. App erzeugt einen Code, du bestaetigst die Anmeldung im Browser.",
                        status = if (oneDriveConnected) "Verbunden" else "Nicht verbunden",
                        connected = oneDriveConnected,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
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
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("Code: ${session.userCode}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(session.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
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

@Composable
private fun ProviderInfoCard(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier
                    .background(
                        if (connected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    status,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
