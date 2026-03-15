package com.djoudini.iplayer.presentation.ui.tv

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.VpnProviderInfo
import com.djoudini.iplayer.data.local.entity.VpnSetupState
import com.djoudini.iplayer.data.local.entity.VpnSetupStep
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.components.QrCodeCard
import com.djoudini.iplayer.presentation.viewmodel.VpnSetupViewModel

@Composable
fun TvVpnSetupWizardScreen(
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: VpnSetupViewModel = hiltViewModel(),
) {
    val setupState by viewModel.setupState.collectAsStateWithLifecycle()
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pastedConfig by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        importErrorMessage = null
        uri?.let {
            viewModel.importConfigFile(
                uri = it,
                onSuccess = { importErrorMessage = null },
                onError = { msg -> importErrorMessage = msg },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FocusableCard(
                onClick = {
                    if (viewModel.getCurrentStepNumber() > 1) viewModel.goToPreviousStep() else onNavigateBack()
                },
                focusScale = 1.1f,
                modifier = Modifier.size(64.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column {
                Text(
                    text = "VPN-Einrichtungsassistent",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Schritt ${viewModel.getCurrentStepNumber()} von ${viewModel.getTotalSteps()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            progress = { setupState.progress },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (setupState.currentStep) {
                is VpnSetupStep.ProviderSelection -> TvVpnProviderSelectionStep(
                    providers = viewModel.getAvailableProviders(),
                    popularProviders = viewModel.getPopularProviders(),
                    onProviderSelected = { viewModel.selectProvider(it) },
                )
                is VpnSetupStep.ConfigImport -> TvVpnConfigImportStep(
                    provider = setupState.selectedProvider,
                    errorMessage = importErrorMessage ?: setupState.errorMessage,
                    onOpenWebsite = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                    onOpenGuide = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                    onFileSelected = { filePickerLauncher.launch("*/*") },
                    onPasteConfig = { showPasteDialog = true },
                )
                is VpnSetupStep.ConnectionTest -> TvVpnConnectionTestStep(
                    onTestComplete = { viewModel.testConnection({}, {}) },
                    isTesting = setupState.isLoading,
                    testResult = setupState.connectionTestResult,
                    errorMessage = setupState.errorMessage,
                )
                is VpnSetupStep.Complete -> TvVpnSetupCompleteStep(
                    state = setupState,
                    onComplete = {
                        viewModel.completeSetup(
                            enableAutoConnect = true,
                            onComplete = onComplete,
                            onError = { },
                        )
                    },
                )
                else -> TvVpnProviderSelectionStep(
                    providers = viewModel.getAvailableProviders(),
                    popularProviders = viewModel.getPopularProviders(),
                    onProviderSelected = { viewModel.selectProvider(it) },
                )
            }

            if (setupState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "VPN wird geprueft...",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }
    }

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("WireGuard-Text einfuegen") },
            text = {
                OutlinedTextField(
                    value = pastedConfig,
                    onValueChange = { pastedConfig = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("WireGuard .conf Inhalt") },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importConfigText(
                            content = pastedConfig,
                            onSuccess = {
                                importErrorMessage = null
                                pastedConfig = ""
                                showPasteDialog = false
                            },
                            onError = { msg -> importErrorMessage = msg },
                        )
                    },
                    enabled = pastedConfig.isNotBlank(),
                ) {
                    Text("Importieren")
                }
            },
            dismissButton = {
                Button(onClick = { showPasteDialog = false }) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

@Composable
private fun TvVpnProviderSelectionStep(
    providers: List<VpnProviderInfo>,
    popularProviders: List<VpnProviderInfo>,
    onProviderSelected: (VpnProviderInfo) -> Unit,
) {
    val popularIds = remember(popularProviders) { popularProviders.map { it.id }.toSet() }
    val displayedProviders = remember(providers, popularIds) {
        providers.sortedWith(
            compareByDescending<VpnProviderInfo> { it.id in popularIds }
                .thenBy { it.displayName.lowercase() }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Text(
                text = "VPN-Anbieter auswaehlen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Die produktionsreife Option ist der Import einer echten WireGuard-Konfiguration.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (popularProviders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${popularProviders.size} beliebte Anbieter stehen zuerst.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        items(displayedProviders, key = { it.id }) { provider ->
            FocusableCard(
                onClick = { onProviderSelected(provider) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                focusScale = 1.05f,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        provider.description?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (provider.id == "manual") {
                                "Direkt in der App importieren"
                            } else {
                                "Eigene WireGuard-Datei importieren"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (provider.features.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = provider.features.take(3).joinToString(" • "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvVpnConfigImportStep(
    provider: VpnProviderInfo?,
    errorMessage: String?,
    onOpenWebsite: (String) -> Unit,
    onOpenGuide: (String) -> Unit,
    onFileSelected: () -> Unit,
    onPasteConfig: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "WireGuard-Konfiguration importieren",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = if (provider != null) {
                "Scanne den QR-Code mit dem Handy, melde dich beim Anbieter an und erzeuge dort deine echte WireGuard-Datei."
            } else {
                "Waehle eine echte WireGuard-.conf-Datei deines Anbieters."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        provider?.setupGuideUrl?.takeIf { it.isNotBlank() }?.let { guideUrl ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "${provider.displayName} mit dem Handy vorbereiten",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    QrCodeCard(
                        value = guideUrl,
                        label = "Mit dem Handy scannen, dort anmelden und die offizielle Anleitung oeffnen",
                        sizeDp = 220,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        provider.websiteUrl.takeIf { it.isNotBlank() }?.let { websiteUrl ->
                            Button(onClick = { onOpenWebsite(websiteUrl) }) {
                                Text("Website")
                            }
                        }
                        Button(onClick = { onOpenGuide(guideUrl) }) {
                            Text("Anleitung")
                        }
                    }
                    Text(
                        text = "Sobald du die WireGuard-Datei auf dem TV oder im Dateispeicher hast, importierst du sie hier in einem Schritt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        FocusableCard(
            onClick = onFileSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            focusScale = 1.05f,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Datei auswaehlen",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unterstuetzt: WireGuard (.conf)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        FocusableCard(onClick = onPasteConfig) {
            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("WireGuard-Text einfuegen")
            }
        }
    }
}

@Composable
private fun TvVpnConnectionTestStep(
    onTestComplete: () -> Unit,
    isTesting: Boolean,
    testResult: com.djoudini.iplayer.data.local.entity.VpnConnectionTestResult?,
    errorMessage: String?,
) {
    LaunchedEffect(Unit) {
        onTestComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Verbindung testen",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        if (isTesting) {
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally),
                strokeWidth = 8.dp,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "WireGuard-Tunnel wird aufgebaut und geprueft",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else if (testResult != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                TvTestResultCard(
                    icon = Icons.Default.Speed,
                    label = "Ping",
                    value = testResult.pingMs?.let { "${it}ms" } ?: "N/A",
                    modifier = Modifier.weight(1f),
                )
                TvTestResultCard(
                    icon = Icons.Default.Download,
                    label = "Download",
                    value = testResult.downloadSpeedMbps?.let { "$it Mbps" } ?: "N/A",
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                TvTestResultCard(
                    icon = Icons.Default.Upload,
                    label = "Upload",
                    value = testResult.uploadSpeedMbps?.let { "$it Mbps" } ?: "N/A",
                    modifier = Modifier.weight(1f),
                )
                TvTestResultCard(
                    icon = Icons.Default.Security,
                    label = "Oeffentliche IP",
                    value = testResult.newIpAddress ?: "N/A",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TvTestResultCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        onClick = { },
        modifier = modifier.height(160.dp),
        focusScale = 1.05f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TvVpnSetupCompleteStep(
    state: VpnSetupState,
    onComplete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(70.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(80.dp),
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "VPN-Konfiguration bereit",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Die Verbindung wurde mit deiner importierten WireGuard-Datei getestet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        state.selectedServerId?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    Text(
                        text = "Endpunkt",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text(
                text = "VPN jetzt aktivieren",
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
