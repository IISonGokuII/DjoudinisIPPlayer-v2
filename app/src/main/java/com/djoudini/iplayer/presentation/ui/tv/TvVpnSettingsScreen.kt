package com.djoudini.iplayer.presentation.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.VpnState
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.SettingsViewModel

/**
 * VPN Settings Section for TV
 */
@Composable
fun TvVpnSettingsSection(
    viewModel: SettingsViewModel,
    onOpenSetupWizard: () -> Unit = {},
) {
    val vpnEnabled by viewModel.vpnEnabled.collectAsStateWithLifecycle()
    val vpnAutoConnect by viewModel.vpnAutoConnect.collectAsStateWithLifecycle()
    val vpnConnectionState by viewModel.vpnConnectionState.collectAsStateWithLifecycle()
    
    var vpnKillSwitch by remember { mutableStateOf(false) }
    var vpnDnsLeakProtection by remember { mutableStateOf(false) }
    var vpnAutoConnectOnBoot by remember { mutableStateOf(false) }
    var vpnConnectBeforeStreaming by remember { mutableStateOf(false) }
    
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
        ?: stringResource(R.string.vpn_select_server)

    Column {
        // Header
        Text(
            text = stringResource(R.string.vpn),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // VPN Enable/Disable Toggle
        TvVpnToggleItem(
            icon = Icons.Default.Security,
            title = stringResource(R.string.vpn_enable),
            subtitle = vpnStatusText,
            checked = vpnEnabled,
            onCheckedChange = { enabled ->
                viewModel.setVpnEnabled(enabled)
            },
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Auto-Connect Toggle
        TvVpnToggleItem(
            icon = Icons.Default.CloudSync,
            title = stringResource(R.string.vpn_auto_connect),
            subtitle = stringResource(R.string.vpn_auto_connect_desc),
            checked = vpnAutoConnect,
            onCheckedChange = { enabled ->
                viewModel.setVpnAutoConnect(enabled)
            },
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Auto-Connect on Boot Toggle
        TvVpnToggleItem(
            icon = Icons.Default.Timer,
            title = stringResource(R.string.vpn_auto_connect_boot),
            subtitle = stringResource(R.string.vpn_auto_connect_boot_desc),
            checked = vpnAutoConnectOnBoot,
            onCheckedChange = { enabled ->
                viewModel.setVpnAutoConnectOnBoot(enabled)
            },
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Connect Before Streaming Toggle
        TvVpnToggleItem(
            icon = Icons.Default.PlayArrow,
            title = stringResource(R.string.vpn_connect_before_streaming),
            subtitle = stringResource(R.string.vpn_connect_before_streaming_desc),
            checked = vpnConnectBeforeStreaming,
            onCheckedChange = { enabled ->
                viewModel.setVpnConnectBeforeStreaming(enabled)
            },
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // VPN Server Selection
        TvVpnItem(
            icon = Icons.Default.LocationOn,
            title = stringResource(R.string.vpn_server),
            subtitle = vpnServerSubtitle,
            onClick = {
                // TODO: Open server selection dialog
            },
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // VPN Protocol Selection
        TvVpnItem(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.vpn_protocol),
            subtitle = when (viewModel.vpnProtocol) {
                "WIREGUARD" -> stringResource(R.string.vpn_wireguard)
                "OPENVPN_UDP" -> stringResource(R.string.vpn_openvpn_udp)
                "OPENVPN_TCP" -> stringResource(R.string.vpn_openvpn_tcp)
                "IKEV2" -> stringResource(R.string.vpn_ikev2)
                else -> stringResource(R.string.vpn_custom)
            },
            onClick = {
                // TODO: Open protocol selection dialog
            },
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Kill Switch Toggle
        TvVpnToggleItem(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.vpn_kill_switch),
            subtitle = stringResource(R.string.vpn_kill_switch_desc),
            checked = vpnKillSwitch,
            onCheckedChange = { enabled ->
                viewModel.setVpnKillSwitch(enabled)
            },
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // DNS Leak Protection Toggle
        TvVpnToggleItem(
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
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.vpn_connection_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Connection Duration
            TvVpnItem(
                icon = Icons.Default.Timer,
                title = stringResource(R.string.vpn_duration),
                subtitle = vpnConnectionState.formattedDuration ?: "00:00:00",
                onClick = {},
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Ping
            TvVpnItem(
                icon = Icons.Default.Speed,
                title = stringResource(R.string.vpn_ping),
                subtitle = "${vpnConnectionState.currentPing ?: currentServer?.ping ?: 0}ms",
                onClick = {
                    viewModel.pingVpnServer(viewModel.vpnServerId)
                },
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Local IP
            TvVpnItem(
                icon = Icons.Default.Dns,
                title = stringResource(R.string.vpn_local_ip),
                subtitle = vpnConnectionState.localIp ?: "N/A",
                onClick = {},
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Remote IP
            TvVpnItem(
                icon = Icons.Default.Security,
                title = stringResource(R.string.vpn_remote_ip),
                subtitle = vpnConnectionState.remoteIp ?: "N/A",
                onClick = {},
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Test Speed Button
            TvVpnItem(
                icon = Icons.Default.Speed,
                title = stringResource(R.string.vpn_test_speed),
                subtitle = "Geschwindigkeit zum Server testen",
                onClick = {
                    viewModel.testVpnSpeed(viewModel.vpnServerId)
                },
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Reconnect Button
            TvVpnItem(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.vpn_reconnect),
                subtitle = "Neu verbinden mit aktuellem Server",
                onClick = {
                    viewModel.reconnectVpn()
                },
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Disconnect Button
            TvVpnItem(
                icon = Icons.Default.Security,
                title = "VPN Trennen",
                subtitle = "Aktuelle Verbindung beenden",
                onClick = {
                    viewModel.disconnectVpn()
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Setup Wizard Button
        TvVpnItem(
            icon = Icons.Default.Refresh,
            title = "VPN Einrichtungsassistent",
            subtitle = "Neuen VPN-Anbieter einrichten",
            onClick = onOpenSetupWizard,
        )
    }
}

@Composable
private fun TvVpnItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        focusScale = 1.05f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TvVpnToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    FocusableCard(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        focusScale = 1.05f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}
