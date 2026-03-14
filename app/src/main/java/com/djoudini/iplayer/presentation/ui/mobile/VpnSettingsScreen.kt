package com.djoudini.iplayer.presentation.ui.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.VpnState
import com.djoudini.iplayer.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * VPN Settings Section for Mobile
 */
@Composable
fun VpnSettingsSection(
    viewModel: SettingsViewModel,
    onNavigateToVpnSetup: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    
    val vpnEnabled by viewModel.vpnEnabled.collectAsStateWithLifecycle()
    val vpnAutoConnect by viewModel.vpnAutoConnect.collectAsStateWithLifecycle()
    val vpnConnectionState by viewModel.vpnConnectionState.collectAsStateWithLifecycle()
    
    var vpnKillSwitch by remember { mutableStateOf(viewModel.vpnKillSwitch) }
    var vpnDnsLeakProtection by remember { mutableStateOf(viewModel.vpnDnsLeakProtection) }
    var vpnAutoConnectOnBoot by remember { mutableStateOf(viewModel.vpnAutoConnectOnBoot) }
    var vpnConnectBeforeStreaming by remember { mutableStateOf(viewModel.vpnConnectBeforeStreaming) }
    
    val currentServer = remember(viewModel.vpnServerId) {
        viewModel.getVpnServerById(viewModel.vpnServerId)
    }
    
    val vpnStatusText = when (vpnConnectionState.state) {
        is VpnState.Connected -> stringResource(R.string.vpn_connected)
        is VpnState.Connecting -> stringResource(R.string.vpn_connecting)
        is VpnState.Disconnecting -> stringResource(R.string.vpn_disconnecting)
        is VpnState.Error -> stringResource(R.string.vpn_error)
        else -> stringResource(R.string.vpn_disconnected)
    }
    
    val vpnServerSubtitle = currentServer?.displayName
        ?: vpnConnectionState.server?.displayName 
        ?: "WireGuard-Konfiguration importieren"

    VpnSettingsSectionContainer(title = stringResource(R.string.vpn)) {
        // VPN Enable/Disable Toggle
        VpnSettingsToggleItem(
            icon = Icons.Default.Security,
            title = stringResource(R.string.vpn_enable),
            subtitle = vpnStatusText,
            checked = vpnEnabled,
            onCheckedChange = { enabled ->
                viewModel.setVpnEnabled(enabled)
            },
        )
        
        // Auto-Connect Toggle
        VpnSettingsToggleItem(
            icon = Icons.Default.CloudSync,
            title = stringResource(R.string.vpn_auto_connect),
            subtitle = stringResource(R.string.vpn_auto_connect_desc),
            checked = vpnAutoConnect,
            onCheckedChange = { enabled ->
                viewModel.setVpnAutoConnect(enabled)
            },
        )
        
        // Auto-Connect on Boot Toggle
        VpnSettingsToggleItem(
            icon = Icons.Default.Timer,
            title = stringResource(R.string.vpn_auto_connect_boot),
            subtitle = stringResource(R.string.vpn_auto_connect_boot_desc),
            checked = vpnAutoConnectOnBoot,
            onCheckedChange = { enabled ->
                viewModel.setVpnAutoConnectOnBoot(enabled)
            },
        )
        
        // Connect Before Streaming Toggle
        VpnSettingsToggleItem(
            icon = Icons.Default.PlayArrow,
            title = stringResource(R.string.vpn_connect_before_streaming),
            subtitle = stringResource(R.string.vpn_connect_before_streaming_desc),
            checked = vpnConnectBeforeStreaming,
            onCheckedChange = { enabled ->
                viewModel.setVpnConnectBeforeStreaming(enabled)
            },
        )
        
        // Imported WireGuard endpoint
        VpnSettingsItem(
            icon = Icons.Default.LocationOn,
            title = stringResource(R.string.vpn_server),
            subtitle = vpnServerSubtitle,
            onClick = {
                onNavigateToVpnSetup?.invoke()
            },
        )
        
        // WireGuard protocol is derived from the imported config
        VpnSettingsItem(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.vpn_protocol),
            subtitle = stringResource(R.string.vpn_wireguard),
            onClick = {
                onNavigateToVpnSetup?.invoke()
            },
        )
        
        // Kill Switch Toggle
        VpnSettingsToggleItem(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.vpn_kill_switch),
            subtitle = stringResource(R.string.vpn_kill_switch_desc),
            checked = vpnKillSwitch,
            onCheckedChange = { enabled ->
                viewModel.setVpnKillSwitch(enabled)
            },
        )
        
        // DNS Leak Protection Toggle
        VpnSettingsToggleItem(
            icon = Icons.Default.Dns,
            title = stringResource(R.string.vpn_dns_leak_protection),
            subtitle = stringResource(R.string.vpn_dns_leak_protection_desc),
            checked = vpnDnsLeakProtection,
            onCheckedChange = { enabled ->
                viewModel.setVpnDnsLeakProtection(enabled)
            },
        )
        
        // Connection Info (only when connected)
        if (vpnConnectionState.state is VpnState.Connected) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.vpn_connection_info),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            
            // Connection Duration
            VpnSettingsItem(
                icon = Icons.Default.Timer,
                title = stringResource(R.string.vpn_duration),
                subtitle = vpnConnectionState.formattedDuration ?: "00:00:00",
                onClick = {},
            )
            
            // Ping
            VpnSettingsItem(
                icon = Icons.Default.Speed,
                title = stringResource(R.string.vpn_ping),
                subtitle = vpnConnectionState.currentPing?.let { "${it}ms" } ?: "Noch nicht gemessen",
                onClick = {
                    viewModel.pingVpnServer(viewModel.vpnServerId)
                },
            )
            
            // Local IP
            VpnSettingsItem(
                icon = Icons.Default.Dns,
                title = stringResource(R.string.vpn_local_ip),
                subtitle = vpnConnectionState.localIp ?: "N/A",
                onClick = {},
            )
            
            // Remote IP
            VpnSettingsItem(
                icon = Icons.Default.Security,
                title = stringResource(R.string.vpn_remote_ip),
                subtitle = vpnConnectionState.remoteIp ?: "N/A",
                onClick = {},
            )
            
            // Test Speed Button
            VpnSettingsItem(
                icon = Icons.Default.Speed,
                title = stringResource(R.string.vpn_test_speed),
                subtitle = "Geschwindigkeit zum Server testen",
                onClick = {
                    viewModel.testVpnSpeed(viewModel.vpnServerId)
                },
            )
            
            // Reconnect Button
            VpnSettingsItem(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.vpn_reconnect),
                subtitle = "Neu verbinden mit aktuellem Server",
                onClick = {
                    viewModel.reconnectVpn()
                },
            )
            
            // Disconnect Button (only when connected)
            VpnSettingsItem(
                icon = Icons.Default.Security,
                title = "VPN Trennen",
                subtitle = "Aktuelle Verbindung beenden",
                onClick = {
                    viewModel.disconnectVpn()
                },
            )
        }

        // VPN Setup Wizard button (always visible)
        if (onNavigateToVpnSetup != null) {
            Spacer(modifier = Modifier.height(8.dp))
            VpnSettingsItem(
                icon = Icons.Default.Build,
                title = stringResource(R.string.vpn_setup_wizard),
                subtitle = "Anbieter konfigurieren oder eigene Config importieren",
                onClick = onNavigateToVpnSetup,
            )
        }
    }
}

@Composable
private fun VpnSettingsSectionContainer(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            content()
        }
    }
}

@Composable
private fun VpnSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VpnSettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
