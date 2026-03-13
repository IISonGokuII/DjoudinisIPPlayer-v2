package com.djoudini.iplayer.presentation.ui.mobile

import android.app.Activity
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Dns
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
import androidx.compose.material3.OutlinedTextField
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
import com.djoudini.iplayer.presentation.viewmodel.VpnSetupViewModel

/**
 * VPN Setup Wizard - Main Screen
 * A modern, step-by-step wizard for VPN configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnSetupWizardScreen(
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
                onSuccess = { /* Navigate to next step automatically */ },
                onError = { /* Show error */ }
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
                                viewModel.getTotalSteps()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.getCurrentStepNumber() > 1) {
                            viewModel.goToPreviousStep()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.back)
                        )
                    }
                },
            )
        },
        bottomBar = {
            // Bottom action bar
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
                        when (setupState.currentStep) {
                            is VpnSetupStep.Complete -> {
                                viewModel.completeSetup(
                                    enableAutoConnect = true,
                                    onComplete = onComplete,
                                    onError = { }
                                )
                            }
                            else -> {
                                // Next step handled by individual screens
                            }
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
                        }
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
            // Progress indicator at top
            LinearProgressIndicator(
                progress = setupState.progress,
                modifier = Modifier.fillMaxWidth(),
            )
            
            // Current step content
            when (val currentStep = setupState.currentStep) {
                is VpnSetupStep.ProviderSelection -> VpnProviderSelectionStep(
                    providers = viewModel.getPopularProviders(),
                    allProviders = viewModel.getAvailableProviders(),
                    onProviderSelected = { viewModel.selectProvider(it) },
                )
                is VpnSetupStep.Login -> VpnLoginStep(
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
                            onError = { /* Show error */ }
                        )
                    },
                    onBack = { viewModel.goToPreviousStep() },
                )
                is VpnSetupStep.ConfigImport -> VpnConfigImportStep(
                    provider = setupState.selectedProvider,
                    onFileSelected = { filePickerLauncher.launch("*/*") },
                    onContinue = { viewModel.setCredentials() },
                )
                is VpnSetupStep.ServerSelection -> VpnServerSelectionStep(
                    provider = setupState.selectedProvider,
                    onServerSelected = { viewModel.selectServer(it) },
                )
                is VpnSetupStep.ConnectionTest -> VpnConnectionTestStep(
                    onTestComplete = { viewModel.testConnection({}, {}) },
                    isTesting = setupState.isLoading,
                    testResult = setupState.connectionTestResult,
                )
                is VpnSetupStep.Complete -> VpnSetupCompleteStep(
                    state = setupState,
                )
                else -> VpnProviderSelectionStep(
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
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
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
    allProviders: List<VpnProviderInfo>,
    onProviderSelected: (VpnProviderInfo) -> Unit,
) {
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
        }
        
        // Popular providers
        item {
            Text(
                text = stringResource(R.string.vpn_popular_providers),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        
        items(providers) { provider ->
            ProviderCard(
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
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        
        items(allProviders.filter { !it.isPopular }) { provider ->
            ProviderCard(
                provider = provider,
                onClick = { onProviderSelected(provider) },
            )
        }
    }
}

@Composable
private fun ProviderCard(
    provider: VpnProviderInfo,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (provider.id == "builtin_free") {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Provider icon
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
            
            // Provider info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (provider.description != null) {
                    Text(
                        text = provider.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                // Features chips
                if (provider.features.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        provider.features.take(3).forEach { feature ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
            
            // Premium badge
            if (provider.isPremium) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Premium",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondary,
                    )
                }
            } else if (provider.id == "builtin_free") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Kostenlos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun VpnLoginStep(
    provider: VpnProviderInfo?,
    authType: VpnAuthType,
    onCredentialsSubmitted: (String?, String?, String?) -> Unit,
    onBack: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.vpn_login_credentials),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        
        Text(
            text = stringResource(R.string.vpn_login_desc),
            style = MaterialTheme.typography.bodyMedium,
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
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {
                Text("Unsupported auth type")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Link to provider website
        if (provider?.websiteUrl != null) {
            Text(
                text = stringResource(R.string.vpn_dont_have_account),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.vpn_get_account),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { /* Open website */ },
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onCredentialsSubmitted(username, password, accountNumber) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.vpn_next))
        }
    }
}

@Composable
private fun VpnConfigImportStep(
    provider: VpnProviderInfo?,
    onFileSelected: () -> Unit,
    onContinue: () -> Unit,
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
            text = stringResource(R.string.vpn_import_config_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // File selection card
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Supported formats
        Text(
            text = "Unterstützte Formate: WireGuard (.conf), OpenVPN (.ovpn)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.vpn_next))
        }
    }
}

@Composable
private fun VpnServerSelectionStep(
    provider: VpnProviderInfo?,
    onServerSelected: (String) -> Unit,
) {
    val servers = remember {
        // Use built-in servers for demo
        KnownVpnProviders.BUILTIN_FREE.let { _ ->
            listOf(
                "de-frankfurt" to "Frankfurt, Deutschland",
                "nl-amsterdam" to "Amsterdam, Niederlande",
                "fr-paris" to "Paris, Frankreich",
                "us-newyork" to "New York, USA",
            )
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        
        items(servers) { (serverId, location) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onServerSelected(serverId) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = location,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${(20..80).random()}ms • ${(50..100).random()}% Last",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun VpnConnectionTestStep(
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
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.vpn_test_running),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else if (testResult != null) {
            // Test results
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TestResultCard(
                    icon = Icons.Default.Speed,
                    label = stringResource(R.string.vpn_test_ping),
                    value = "${testResult.pingMs}ms",
                    modifier = Modifier.weight(1f),
                )
                TestResultCard(
                    icon = Icons.Default.Download,
                    label = stringResource(R.string.vpn_test_download),
                    value = "${testResult.downloadSpeedMbps} Mbps",
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
                    value = "${testResult.uploadSpeedMbps} Mbps",
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
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
    state: com.djoudini.iplayer.data.local.entity.VpnSetupState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Success icon
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
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Provider summary
        if (state.selectedProvider != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "VPN-Anbieter",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.selectedProvider?.displayName ?: "N/A",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Server summary
        if (state.selectedServerId != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Server",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.selectedServerId ?: "N/A",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
