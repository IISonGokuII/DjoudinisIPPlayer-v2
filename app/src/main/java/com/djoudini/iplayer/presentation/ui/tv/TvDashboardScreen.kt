package com.djoudini.iplayer.presentation.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.components.ProgressRing
import com.djoudini.iplayer.presentation.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TV-optimized Dashboard with large tiles and D-Pad focus management.
 * Every tile has visual focus feedback (scale + border glow).
 * 
 * Focus navigation:
 * - Search button is at the top and can only be reached by explicit UP navigation
 * - Main tiles are in a grid below
 */
@Composable
fun TvDashboardScreen(
    onNavigateLive: () -> Unit,
    onNavigateVod: () -> Unit,
    onNavigateSeries: () -> Unit,
    onNavigateEpg: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateFavorites: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val playlist by viewModel.activePlaylist.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
    ) {
        // Header with Search - Search is focusable and at the top
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist?.name ?: stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                playlist?.expirationDate?.let { expDate ->
                    val formatted = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        .format(Date(expDate))
                    Text(
                        text = stringResource(R.string.account_expires_format, formatted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Search button - explicitly at top, requires UP to reach from tiles
            FocusableCard(
                onClick = onNavigateSearch,
                focusScale = 1.1f,
                modifier = Modifier.size(64.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Sync progress indicator
            if (syncProgress.isActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(
                        progress = syncProgress.progress,
                        size = 32.dp,
                        strokeWidth = 3.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = syncProgress.phase,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Main tile grid - 3 columns, 2 rows
        // Row 1: Live TV, Movies, Series
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TvDashboardTile(
                title = stringResource(R.string.live_tv),
                icon = Icons.Default.LiveTv,
                onClick = onNavigateLive,
                modifier = Modifier.weight(1f),
            )
            TvDashboardTile(
                title = stringResource(R.string.movies),
                icon = Icons.Default.Movie,
                onClick = onNavigateVod,
                modifier = Modifier.weight(1f),
            )
            TvDashboardTile(
                title = stringResource(R.string.series),
                icon = Icons.Default.Tv,
                onClick = onNavigateSeries,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Row 2: Favorites, EPG Guide, Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TvDashboardTile(
                title = stringResource(R.string.favorites),
                icon = Icons.Default.Favorite,
                onClick = onNavigateFavorites,
                modifier = Modifier.weight(1f),
            )
            TvDashboardTile(
                title = stringResource(R.string.epg_guide),
                icon = Icons.Default.CalendarMonth,
                onClick = onNavigateEpg,
                modifier = Modifier.weight(1f),
            )
            TvDashboardTile(
                title = stringResource(R.string.settings),
                icon = Icons.Default.Settings,
                onClick = onNavigateSettings,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TvDashboardTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier.height(160.dp),
        focusScale = 1.08f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
