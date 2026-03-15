package com.djoudini.iplayer.presentation.ui.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.data.repository.ConferenceSelectableMatch
import com.djoudini.iplayer.presentation.viewmodel.ConferenceDraftSlot
import com.djoudini.iplayer.presentation.viewmodel.ConferenceChannelCandidate
import com.djoudini.iplayer.presentation.viewmodel.ConferenceProfileSummary
import com.djoudini.iplayer.presentation.viewmodel.ConferenceViewModel

@Composable
fun ConferenceScreen(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konferenz") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshMatches() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Spiele aktualisieren")
                    }
                    IconButton(onClick = { viewModel.testApi() }) {
                        Icon(Icons.Default.SportsSoccer, contentDescription = "API testen")
                    }
                    IconButton(onClick = { showWizard = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Neue Konferenz")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Smart Konferenz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = sessionState.activeConferenceName?.let {
                            "Aktiv: $it${sessionState.lastMessage?.let { message -> "\n$message" } ?: ""}"
                        } ?: "Lege Spiele und Sender an. Das erste Spiel ist dein Hauptspiel, bei Toren kann die App automatisch umschalten.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showWizard = true }) {
                            Text("Konferenz anlegen")
                        }
                        OutlinedButton(onClick = { showApiTokenDialog = true }) {
                            Text(if (conferenceApiToken.isBlank()) "API-Token setzen" else "API-Token aendern")
                        }
                        if (sessionState.activeConferenceId != null) {
                            OutlinedButton(onClick = { viewModel.stopConference() }) {
                                Text("Stoppen")
                            }
                        }
                    }
                }
            }

            apiTestMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (apiTestIsError) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = if (apiTestIsError) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            if (isLoadingMatches) {
                Text("Lade Spiele von football-data.org ...")
            } else if (matchError != null) {
                Text(
                    text = matchError ?: "",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (profiles.isEmpty()) {
                Text("Noch keine Konferenzen gespeichert.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(profiles, key = { it.profile.id }) { profile ->
                        ConferenceProfileCard(
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
    }

    if (showWizard) {
        ConferenceWizardDialog(
            channels = channels,
            matches = availableMatches,
            loadChannelCandidates = { match -> viewModel.buildChannelCandidates(match) },
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
private fun ConferenceProfileCard(
    profile: ConferenceProfileSummary,
    isActive: Boolean,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = buildString {
                            append("${profile.mappings.size} Spiele")
                            append("  •  ")
                            append(
                                if (profile.profile.cooldownEnabled) {
                                    "Cooldown ${profile.profile.cooldownSeconds}s"
                                } else {
                                    "Kein Cooldown"
                                },
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (isActive) {
                    Text(
                        text = "LIVE",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            profile.mappings.forEach { mapping ->
                Text(
                    text = buildString {
                        if (mapping.priority == 0) {
                            append("Hauptspiel: ")
                        }
                        append("${mapping.matchLabel}  ->  ${mapping.channelName}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Starten")
                }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("Loeschen")
                }
            }
        }
    }
}

@Composable
private fun ConferenceWizardDialog(
    channels: List<ChannelEntity>,
    matches: List<ConferenceSelectableMatch>,
    loadChannelCandidates: suspend (ConferenceSelectableMatch?) -> List<ConferenceChannelCandidate>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        cooldownEnabled: Boolean,
        cooldownSeconds: Int,
        holdSeconds: Int,
        slots: List<ConferenceDraftSlot>,
    ) -> Unit,
) {
    var desiredSlots by remember { mutableIntStateOf(2) }
    var conferenceName by remember { mutableStateOf("Samstags-Konferenz") }
    var cooldownEnabled by remember { mutableStateOf(true) }
    var cooldownSeconds by remember { mutableIntStateOf(20) }
    var holdSeconds by remember { mutableIntStateOf(20) }
    var draftSlots by remember(desiredSlots) {
        mutableStateOf(List(desiredSlots) { ConferenceDraftSlot() })
    }
    var selectionMode by remember { mutableStateOf<SelectionMode?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Konferenz-Assistent",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Wie viele Spiele moechtest du koppeln?")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(1, 2, 3, 4).forEach { count ->
                            Button(
                                onClick = {
                                    desiredSlots = count
                                    draftSlots = List(count) { index ->
                                        draftSlots.getOrNull(index) ?: ConferenceDraftSlot()
                                    }
                                },
                            ) {
                                Text(count.toString())
                            }
                        }
                    }

                    OutlinedTextField(
                        value = conferenceName,
                        onValueChange = { conferenceName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Konferenzname") },
                        singleLine = true,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Cooldown aktivieren")
                        Switch(checked = cooldownEnabled, onCheckedChange = { cooldownEnabled = it })
                    }

                    if (cooldownEnabled) {
                        SelectionRow(
                            title = "Cooldown",
                            values = listOf(10, 20, 30, 45, 60),
                            selected = cooldownSeconds,
                            suffix = "s",
                            onSelected = { cooldownSeconds = it },
                        )
                    }

                    SelectionRow(
                        title = "Wie lange auf Tor-Sender bleiben",
                        values = listOf(10, 20, 30, 45, 60),
                        selected = holdSeconds,
                        suffix = "s",
                        onSelected = { holdSeconds = it },
                    )

                    draftSlots.forEachIndexed { index, slot ->
                        Card {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = if (index == 0) "Spiel ${index + 1} (Hauptspiel)" else "Spiel ${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                )
                                OutlinedButton(
                                    onClick = { selectionMode = SelectionMode.Match(index) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(slot.match?.title ?: "Spiel waehlen")
                                }
                                OutlinedButton(
                                    onClick = { selectionMode = SelectionMode.Channel(index) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(slot.channel?.name ?: "Sender waehlen")
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }
                    Button(
                        onClick = {
                            onSave(conferenceName, cooldownEnabled, cooldownSeconds, holdSeconds, draftSlots)
                        },
                        enabled = draftSlots.all { it.match != null && it.channel != null },
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }

    when (val currentSelection = selectionMode) {
        is SelectionMode.Match -> {
            SelectionDialog(
                title = "Spiel auswaehlen",
                items = matches.map { SelectionItem(it.title, it.subtitle) },
                onDismiss = { selectionMode = null },
                onSelect = { selectedIndex ->
                    draftSlots = draftSlots.toMutableList().also {
                        it[currentSelection.slotIndex] = it[currentSelection.slotIndex].copy(match = matches[selectedIndex])
                    }
                    selectionMode = null
                },
            )
        }
        is SelectionMode.Channel -> {
            val selectedMatch = draftSlots.getOrNull(currentSelection.slotIndex)?.match
            var channelCandidates by remember(selectedMatch, channels) {
                mutableStateOf(emptyList<ConferenceChannelCandidate>())
            }
            LaunchedEffect(selectedMatch, channels) {
                channelCandidates = loadChannelCandidates(selectedMatch)
            }
            SelectionDialog(
                title = "Sender auswaehlen",
                items = channelCandidates.map { candidate ->
                    SelectionItem(
                        title = candidate.channel.name,
                        subtitle = buildString {
                            if (candidate.matchScore > 0) {
                                append("EPG-Treffer  •  ")
                            }
                            append(
                                candidate.currentProgram?.let { "Jetzt: ${it.title}" }
                                    ?: "Jetzt: keine EPG-Daten",
                            )
                            candidate.nextProgram?.let { next ->
                                append("\nDanach: ${next.title}")
                            }
                        },
                    )
                },
                onDismiss = { selectionMode = null },
                onSelect = { selectedIndex ->
                    draftSlots = draftSlots.toMutableList().also {
                        it[currentSelection.slotIndex] = it[currentSelection.slotIndex].copy(channel = channelCandidates[selectedIndex].channel)
                    }
                    selectionMode = null
                },
            )
        }
        null -> Unit
    }
}

@Composable
private fun SelectionRow(
    title: String,
    values: List<Int>,
    selected: Int,
    suffix: String,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value ->
                OutlinedButton(onClick = { onSelected(value) }) {
                    Text("$value$suffix")
                }
            }
        }
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    items: List<SelectionItem>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (items.isEmpty()) {
                Text(
                    text = "Keine Eintraege verfuegbar. Bitte API pruefen oder die Spiele zuerst aktualisieren.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items.indices.toList()) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(index) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = false, onClick = { onSelect(index) })
                            Column {
                                Text(items[index].title, fontWeight = FontWeight.Medium)
                                Text(items[index].subtitle, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Schliessen")
            }
        },
    )
}

private data class SelectionItem(
    val title: String,
    val subtitle: String,
)

private sealed class SelectionMode {
    data class Match(val slotIndex: Int) : SelectionMode()
    data class Channel(val slotIndex: Int) : SelectionMode()
}

@Composable
private fun ApiTokenDialog(
    initialToken: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var token by remember(initialToken) { mutableStateOf(initialToken) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("football-data API-Token") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Falls der Build-Token auf dem Geraet fehlt, kannst du ihn hier direkt in der App hinterlegen.")
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("API-Token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(token) },
                enabled = token.isNotBlank(),
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}
