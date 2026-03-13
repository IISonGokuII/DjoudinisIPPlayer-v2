package com.djoudini.iplayer.presentation.ui.tv

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.KnownVpnProviders
import com.djoudini.iplayer.data.local.entity.VpnAuthType
import com.djoudini.iplayer.data.local.entity.VpnProviderInfo
import com.djoudini.iplayer.data.local.entity.VpnSetupStep
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.VpnSetupViewModel

/**
 * TV-optimized VPN Setup Wizard.
 * Uses FocusableCard for D-Pad navigation.
 */
@Composable
fun TvVpnSetupWizardScreen(
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: VpnSetupViewModel = hiltViewModel(),
) {
    val setupState by viewModel.setupState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val filePath = it.path ?: return@let
            viewModel.importConfigFile(
                filePath = filePath,
                onSuccess = { },
                onError = { }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FocusableCard(
                onClick = {
                    if (viewModel.getCurrentStepNumber() > 1) {
                        viewModel.goToPreviousStep()
                    } else {
                        onNavigateBack()
                    }
                },
                focusScale = 1.1f,
                modifier = Modifier.size(64.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column {
                Text(
                    text = stringResource(R.string.vpn_setup_wizard),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        R.string.vpn_step_indicator,
                        viewModel.getCurrentStepNumber(),
                        viewModel.getTotalSteps()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = setupState.progress,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Current step content
        Box(modifier = Modifier.weight(1f)) {
            when (val currentStep = setupState.currentStep) {
                is VpnSetupStep.ProviderSelection -> TvVpnProviderSelectionStep(
                    providers = viewModel.getPopularProviders(),
                    allProviders = viewModel.getAvailableProviders(),
                    onProviderSelected = { viewModel.selectProvider(it) },
                )
                is VpnSetupStep.Login -> TvVpnLoginStep(
                    provider = setupState.selectedProvider,
                    authType = setupState.authMethod,
                    onCredentialsSubmitted = { username, password, accountNumber ->
                        viewModel.authenticateWithProvider(
                            onSuccess = {
                                viewModel.setCredentials(
                                    username = username,
                                    password = password,
                                    accountNumber = accountNumber,
                                )
                            },
                            onError = { }
                        )
                    },
                )
                is VpnSetupStep.ConfigImport -> TvVpnConfigImportStep(
                    provider = setupState.selectedProvider,
                    onFileSelected = { filePickerLauncher.launch("*/*") },
                    onContinue = { viewModel.setCredentials() },
                )
                is VpnSetupStep.ServerSelection -> TvVpnServerSelectionStep(
                    provider = setupState.selectedProvider,
                    onServerSelected = { viewModel.selectServer(it) },
                )
                is VpnSetupStep.ConnectionTest -> TvVpnConnectionTestStep(
                    onTestComplete = { viewModel.testConnection({}, {}) },
                    isTesting = setupState.isLoading,
                    testResult = setupState.connectionTestResult,
                )
                is VpnSetupStep.Complete -> TvVpnSetupCompleteStep(
                    state = setupState,
                    onComplete = {
                        viewModel.completeSetup(
                            enableAutoConnect = true,
                            onComplete = onComplete,
                            onError = { }
                        )
                    },
                )
                else -> TvVpnProviderSelectionStep(
                    providers = viewModel.getPopularProviders(),
                    allProviders = viewModel.getAvailableProviders(),
                    onProviderSelected = { viewModel.selectProvider(it) },
                )
            }

            // Loading overlay
            if (setupState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.vpn_loading),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvVpnProviderSelectionStep(
    providers: List<VpnProviderInfo>,
    allProviders: List<VpnProviderInfo>,
    onProviderSelected: (VpnProviderInfo) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Popular providers
        item {
            Text(
                text = stringResource(R.string.vpn_popular_providers),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(providers) { provider ->
            TvVpnProviderCard(
                provider = provider,
                onClick = { onProviderSelected(provider) },
            )
        }

        // All providers
        item {
            Text(
                text = stringResource(R.string.vpn_all_providers),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(allProviders.filter { !it.isPopular }) { provider ->
            TvVpnProviderCard(
                provider = provider,
                onClick = { onProviderSelected(provider) },
            )
        }
    }
}

@Composable
private fun TvVpnProviderCard(
    provider: VpnProviderInfo,
    onClick: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    FocusableCard(
        onClick = onClick,
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
            // Provider icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (provider.id == "builtin_free") {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    ),
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

            // Provider info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (provider.description != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = provider.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }

                // Features
                if (provider.features.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        provider.features.take(3).forEach { feature ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }

            // Badge
            if (provider.isPremium) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Premium",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else if (provider.id == "builtin_free") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Kostenlos",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvVpnLoginStep(
    provider: VpnProviderInfo?,
    authType: VpnAuthType,
    onCredentialsSubmitted: (String?, String?, String?) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.vpn_login_credentials),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = stringResource(R.string.vpn_login_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (authType) {
            VpnAuthType.USERNAME_PASSWORD -> {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.vpn_username)) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.vpn_password)) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            VpnAuthType.ACCOUNT_NUMBER -> {
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text(stringResource(R.string.vpn_account_number)) },
                    placeholder = { Text(stringResource(R.string.vpn_account_number_desc)) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
            else -> {
                Text("Unsupported auth type")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onCredentialsSubmitted(username, password, accountNumber) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.vpn_next))
        }
    }
}

@Composable
private fun TvVpnConfigImportStep(
    provider: VpnProviderInfo?,
    onFileSelected: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.vpn_import_config_file),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = stringResource(R.string.vpn_import_config_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // File selection card
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
                    text = stringResource(R.string.vpn_select_file),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "WireGuard (.conf) oder OpenVPN (.ovpn)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.vpn_next))
        }
    }
}

@Composable
private fun TvVpnServerSelectionStep(
    provider: VpnProviderInfo?,
    onServerSelected: (String) -> Unit,
) {
    val servers = remember {
        listOf(
            "de-frankfurt" to "Frankfurt, Deutschland",
            "nl-amsterdam" to "Amsterdam, Niederlande",
            "fr-paris" to "Paris, Frankreich",
            "us-newyork" to "New York, USA",
            "gb-london" to "London, UK",
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.vpn_select_server),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.vpn_select_server_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        items(servers) { (serverId, location) ->
            FocusableCard(
                onClick = { onServerSelected(serverId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                focusScale = 1.05f,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = location,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(20..80).random()}ms • ${(50..100).random()}% Last",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TvVpnConnectionTestStep(
    onTestComplete: () -> Unit,
    isTesting: Boolean,
    testResult: com.djoudini.iplayer.data.local.entity.VpnConnectionTestResult?,
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
            text = stringResource(R.string.vpn_test_connection),
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
                text = stringResource(R.string.vpn_test_running),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else if (testResult != null) {
            // Test results grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                TvTestResultCard(
                    icon = Icons.Default.Speed,
                    label = stringResource(R.string.vpn_test_ping),
                    value = "${testResult.pingMs}ms",
                    modifier = Modifier.weight(1f),
                )
                TvTestResultCard(
                    icon = Icons.Default.Download,
                    label = stringResource(R.string.vpn_test_download),
                    value = "${testResult.downloadSpeedMbps} Mbps",
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                TvTestResultCard(
                    icon = Icons.Default.Upload,
                    label = stringResource(R.string.vpn_test_upload),
                    value = "${testResult.uploadSpeedMbps} Mbps",
                    modifier = Modifier.weight(1f),
                )
                TvTestResultCard(
                    icon = Icons.Default.Security,
                    label = stringResource(R.string.vpn_test_ip),
                    value = testResult.newIpAddress ?: "N/A",
                    modifier = Modifier.weight(1f),
                )
            }
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
    state: com.djoudini.iplayer.data.local.entity.VpnSetupState,
    onComplete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Success icon
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
            text = stringResource(R.string.vpn_setup_complete),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.vpn_setup_complete_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Summary cards
        if (state.selectedProvider != null) {
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
                        text = "VPN-Anbieter",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.selectedProvider?.displayName ?: "N/A",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.selectedServerId != null) {
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
                        text = "Server",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.selectedServerId ?: "N/A",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Complete button
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text(
                text = stringResource(R.string.vpn_connect_now),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
