package com.djoudini.iplayer.presentation.ui.mobile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.VpnProviderInfo
import com.djoudini.iplayer.data.local.entity.VpnSetupState
import com.djoudini.iplayer.data.local.entity.VpnSetupStep
import com.djoudini.iplayer.presentation.components.QrCodeCard
import com.djoudini.iplayer.presentation.viewmodel.VpnSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnSetupWizardScreen(
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: VpnSetupViewModel = hiltViewModel(),
) {
    val setupState by viewModel.setupState.collectAsStateWithLifecycle()
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.vpn_setup_wizard))
                        Text(
                            text = stringResource(
                                R.string.vpn_step_indicator,
                                viewModel.getCurrentStepNumber(),
                                viewModel.getTotalSteps(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.getCurrentStepNumber() > 1) viewModel.goToPreviousStep() else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (viewModel.getCurrentStepNumber() > 1) {
                    Button(
                        onClick = { viewModel.goToPreviousStep() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.vpn_back))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Button(
                    onClick = {
                        if (setupState.currentStep is VpnSetupStep.Complete) {
                            viewModel.completeSetup(
                                enableAutoConnect = true,
                                onComplete = onComplete,
                                onError = { },
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !setupState.isLoading,
                ) {
                    Text(
                        if (setupState.currentStep is VpnSetupStep.Complete) {
                            stringResource(R.string.vpn_connect_now)
                        } else {
                            stringResource(R.string.vpn_next)
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LinearProgressIndicator(
                progress = { setupState.progress },
                modifier = Modifier.fillMaxWidth(),
            )

            when (setupState.currentStep) {
                is VpnSetupStep.ProviderSelection -> VpnProviderSelectionStep(
                    providers = viewModel.getAvailableProviders(),
                    popularProviders = viewModel.getPopularProviders(),
                    onProviderSelected = { viewModel.selectProvider(it) },
                )
                is VpnSetupStep.ConfigImport -> VpnConfigImportStep(
                    provider = setupState.selectedProvider,
                    errorMessage = importErrorMessage ?: setupState.errorMessage,
                    onOpenWebsite = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                    onOpenGuide = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                    onFileSelected = { filePickerLauncher.launch("*/*") },
                )
                is VpnSetupStep.ConnectionTest -> VpnConnectionTestStep(
                    onTestComplete = { viewModel.testConnection({}, {}) },
                    isTesting = setupState.isLoading,
                    testResult = setupState.connectionTestResult,
                    errorMessage = setupState.errorMessage,
                )
                is VpnSetupStep.Complete -> VpnSetupCompleteStep(state = setupState)
                else -> VpnProviderSelectionStep(
                    providers = viewModel.getAvailableProviders(),
                    popularProviders = viewModel.getPopularProviders(),
                    onProviderSelected = { viewModel.selectProvider(it) },
                )
            }

            if (setupState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.vpn_loading))
                    }
                }
            }
        }
    }
}

@Composable
private fun VpnProviderSelectionStep(
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.vpn_select_provider),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.vpn_setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (popularProviders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${popularProviders.size} beliebte Anbieter stehen oben in der Liste.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        items(displayedProviders, key = { it.id }) { provider ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProviderSelected(provider) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        provider.description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (provider.id == "manual") {
                                "Direkt in der App importieren"
                            } else {
                                "Fuehrt dich zum Import deiner eigenen WireGuard-Datei"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (provider.features.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = provider.features.take(3).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
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
private fun VpnConfigImportStep(
    provider: VpnProviderInfo?,
    errorMessage: String?,
    onOpenWebsite: (String) -> Unit,
    onOpenGuide: (String) -> Unit,
    onFileSelected: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.vpn_import_config_file),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = provider?.let {
                "Scanne den QR-Code mit deinem Handy, melde dich beim Anbieter an und erstelle dort deine WireGuard-Datei. Danach importierst du sie hier auf dem TV."
            } ?: stringResource(R.string.vpn_import_config_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        provider?.setupGuideUrl?.takeIf { it.isNotBlank() }?.let { guideUrl ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${provider.displayName} auf dem Handy vorbereiten",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    QrCodeCard(
                        value = guideUrl,
                        label = "Mit dem Handy scannen und die offizielle Anleitung oeffnen",
                        sizeDp = 180,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        provider.websiteUrl.takeIf { it.isNotBlank() }?.let { websiteUrl ->
                            Button(onClick = { onOpenWebsite(websiteUrl) }) {
                                Text("Website")
                            }
                        }
                        Button(onClick = { onOpenGuide(guideUrl) }) {
                            Text("Anleitung oeffnen")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (provider.isPremium) {
                            "Hinweis: Fuer viele Anbieter brauchst du ein aktives Konto, bevor du eine WireGuard-Datei erzeugen kannst."
                        } else {
                            "Hinweis: Manche Anbieter haben kostenlose Tarife, aber WireGuard-Dateien sind je nach Plan eingeschraenkt."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onFileSelected),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.vpn_select_file),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = "Unterstuetztes Format: WireGuard (.conf)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun VpnConnectionTestStep(
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.vpn_test_connection),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        if (isTesting) {
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.vpn_test_running),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else if (testResult != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TestResultCard(
                    icon = Icons.Default.Speed,
                    label = stringResource(R.string.vpn_test_ping),
                    value = testResult.pingMs?.let { "${it}ms" } ?: "N/A",
                    modifier = Modifier.weight(1f),
                )
                TestResultCard(
                    icon = Icons.Default.Download,
                    label = stringResource(R.string.vpn_test_download),
                    value = testResult.downloadSpeedMbps?.let { "$it Mbps" } ?: "N/A",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TestResultCard(
                    icon = Icons.Default.Upload,
                    label = stringResource(R.string.vpn_test_upload),
                    value = testResult.uploadSpeedMbps?.let { "$it Mbps" } ?: "N/A",
                    modifier = Modifier.weight(1f),
                )
                TestResultCard(
                    icon = Icons.Default.Security,
                    label = stringResource(R.string.vpn_test_ip),
                    value = testResult.newIpAddress ?: "N/A",
                    modifier = Modifier.weight(1f),
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
    }
}

@Composable
private fun TestResultCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VpnSetupCompleteStep(
    state: VpnSetupState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(64.dp),
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.vpn_setup_complete),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.vpn_setup_complete_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        state.selectedServerId?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.vpn_server),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
