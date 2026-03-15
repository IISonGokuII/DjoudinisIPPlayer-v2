package com.djoudini.iplayer.presentation.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.ui.mobile.ApiTokenDialog
import com.djoudini.iplayer.presentation.ui.mobile.ConferenceWizardDialog
import com.djoudini.iplayer.presentation.viewmodel.ConferenceProfileSummary
import com.djoudini.iplayer.presentation.viewmodel.ConferenceViewModel

@Composable
fun TvConferenceScreen(
    onBack: () -> Unit,
    onOpenChannel: (Long) -> Unit,
    viewModel: ConferenceViewModel = hiltViewModel(),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val availableMatches by viewModel.availableMatches.collectAsStateWithLifecycle()
    val isLoadingMatches by viewModel.isLoadingMatches.collectAsStateWithLifecycle()
    val matchError by viewModel.matchError.collectAsStateWithLifecycle()
    val apiTestMessage by viewModel.apiTestMessage.collectAsStateWithLifecycle()
    val apiTestIsError by viewModel.apiTestIsError.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val conferenceApiToken by viewModel.conferenceApiToken.collectAsStateWithLifecycle()

    var showWizard by remember { mutableStateOf(false) }
    var showApiTokenDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Konferenz",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = sessionState.activeConferenceName?.let {
                "Aktiv: $it${sessionState.lastMessage?.let { message -> "\n$message" } ?: ""}"
            } ?: "TV-Konferenz mit Hauptspiel, Auto-Zapping und fernbedienungstauglichen Aktionen.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FocusableCard(onClick = onBack) {
                Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text("Zurueck")
                }
            }
            FocusableCard(onClick = { viewModel.refreshMatches() }) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("Neu laden")
                }
            }
            FocusableCard(onClick = { viewModel.testApi() }) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Icon(Icons.Default.SportsSoccer, contentDescription = null)
                    Text("API testen")
                }
            }
            FocusableCard(onClick = { showWizard = true }) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = null)
                    Text("Anlegen")
                }
            }
            FocusableCard(onClick = { showApiTokenDialog = true }) {
                Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text(if (conferenceApiToken.isBlank()) "API-Token setzen" else "API-Token aendern")
                }
            }
            if (sessionState.activeConferenceId != null) {
                FocusableCard(onClick = { viewModel.stopConference() }) {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Text("Stoppen")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (apiTestIsError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isLoadingMatches) "Lade Spiele von football-data.org ..."
                    else matchError ?: "${availableMatches.size} Spiele fuer die Konferenz verfuegbar.",
                    color = if (matchError != null) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                )
                apiTestMessage?.let {
                    Text(
                        text = it,
                        color = if (apiTestIsError) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        if (profiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Noch keine Konferenzen gespeichert.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(profiles, key = { it.profile.id }) { profile ->
                    TvConferenceProfileCard(
                        profile = profile,
                        isActive = sessionState.activeConferenceId == profile.profile.id,
                        onStart = {
                            viewModel.startConference(profile.profile.id) { channelId ->
                                channelId?.let(onOpenChannel)
                            }
                        },
                        onDelete = { viewModel.deleteConference(profile.profile.id) },
                    )
                }
            }
        }
    }

    if (showWizard) {
        ConferenceWizardDialog(
            channels = channels,
            matches = availableMatches,
            loadChannelCandidates = { match -> viewModel.buildChannelCandidates(match) },
            onRefreshMatches = { viewModel.refreshMatches() },
            onDismiss = { showWizard = false },
            onSave = { name, cooldownEnabled, cooldownSeconds, holdSeconds, slots ->
                viewModel.saveConference(name, cooldownEnabled, cooldownSeconds, holdSeconds, slots)
                showWizard = false
            },
        )
    }

    if (showApiTokenDialog) {
        ApiTokenDialog(
            initialToken = conferenceApiToken,
            onDismiss = { showApiTokenDialog = false },
            onSave = { token ->
                viewModel.saveApiToken(token)
                showApiTokenDialog = false
            },
        )
    }
}

@Composable
private fun TvConferenceProfileCard(
    profile: ConferenceProfileSummary,
    isActive: Boolean,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${profile.mappings.size} Spiele • ${if (profile.profile.cooldownEnabled) "Cooldown ${profile.profile.cooldownSeconds}s" else "Kein Cooldown"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isActive) {
                    Text("LIVE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            profile.mappings.forEach { mapping ->
                Text(
                    text = buildString {
                        if (mapping.priority == 0) append("Hauptspiel: ")
                        append("${mapping.matchLabel} -> ${mapping.channelName}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusableCard(onClick = onStart) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("Starten")
                    }
                }
                FocusableCard(onClick = onDelete) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Delete, contentDescription = null)
                        Text("Loeschen")
                    }
                }
            }
        }
    }
}
