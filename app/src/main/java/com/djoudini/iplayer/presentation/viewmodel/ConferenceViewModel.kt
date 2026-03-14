package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.ChannelDao
import com.djoudini.iplayer.data.local.dao.ConferenceMatchMappingDao
import com.djoudini.iplayer.data.local.dao.ConferenceProfileDao
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.data.local.entity.ConferenceMatchMappingEntity
import com.djoudini.iplayer.data.local.entity.ConferenceProfileEntity
import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import com.djoudini.iplayer.data.repository.ConferenceManager
import com.djoudini.iplayer.data.repository.ConferenceSelectableMatch
import com.djoudini.iplayer.domain.repository.EpgRepository
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.util.EpgChannelIdNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConferenceProfileSummary(
    val profile: ConferenceProfileEntity,
    val mappings: List<ConferenceMatchMappingEntity>,
)

data class ConferenceDraftSlot(
    val match: ConferenceSelectableMatch? = null,
    val channel: ChannelEntity? = null,
)

data class ConferenceChannelCandidate(
    val channel: ChannelEntity,
    val currentProgram: EpgProgramEntity?,
    val nextProgram: EpgProgramEntity?,
    val matchScore: Int,
)

@HiltViewModel
class ConferenceViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val channelDao: ChannelDao,
    private val conferenceProfileDao: ConferenceProfileDao,
    private val conferenceMatchMappingDao: ConferenceMatchMappingDao,
    private val conferenceManager: ConferenceManager,
    private val epgRepository: EpgRepository,
) : ViewModel() {

    private val _availableMatches = MutableStateFlow<List<ConferenceSelectableMatch>>(emptyList())
    val availableMatches: StateFlow<List<ConferenceSelectableMatch>> = _availableMatches.asStateFlow()

    private val _isLoadingMatches = MutableStateFlow(false)
    val isLoadingMatches: StateFlow<Boolean> = _isLoadingMatches.asStateFlow()

    private val _matchError = MutableStateFlow<String?>(null)
    val matchError: StateFlow<String?> = _matchError.asStateFlow()

    val sessionState = conferenceManager.sessionState

    val channels: StateFlow<List<ChannelEntity>> = playlistRepository.observeActive()
        .flatMapLatest { playlist ->
            playlist?.let { channelDao.observeByPlaylist(it.id) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profiles: StateFlow<List<ConferenceProfileSummary>> =
        conferenceProfileDao.observeAll()
            .flatMapLatest { profiles ->
                if (profiles.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        profiles.map { profile ->
                            conferenceMatchMappingDao.observeByConference(profile.id)
                        },
                    ) { mappingsArray ->
                        profiles.mapIndexed { index, profile ->
                            ConferenceProfileSummary(profile, mappingsArray[index])
                        }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshMatches()
    }

    fun refreshMatches() {
        viewModelScope.launch {
            _isLoadingMatches.value = true
            _matchError.value = null
            val matches = conferenceManager.fetchSelectableMatches()
            _availableMatches.value = matches
            _isLoadingMatches.value = false
            if (matches.isEmpty()) {
                _matchError.value = "Keine Spiele gefunden oder API-Antwort leer."
            }
        }
    }

    fun saveConference(
        name: String,
        cooldownEnabled: Boolean,
        cooldownSeconds: Int,
        holdSeconds: Int,
        slots: List<ConferenceDraftSlot>,
    ) {
        viewModelScope.launch {
            val validSlots = slots.filter { it.match != null && it.channel != null }
            if (validSlots.isEmpty()) return@launch

            val conferenceId = conferenceProfileDao.insert(
                ConferenceProfileEntity(
                    name = name,
                    cooldownEnabled = cooldownEnabled,
                    cooldownSeconds = cooldownSeconds,
                    holdSeconds = holdSeconds,
                ),
            )
            conferenceMatchMappingDao.insertAll(
                validSlots.mapIndexed { index, slot ->
                    ConferenceMatchMappingEntity(
                        conferenceId = conferenceId,
                        matchId = slot.match!!.matchId,
                        competitionName = slot.match.subtitle,
                        matchLabel = slot.match.title,
                        channelId = slot.channel!!.id,
                        channelName = slot.channel.name,
                        priority = index,
                    )
                },
            )
        }
    }

    fun deleteConference(conferenceId: Long) {
        viewModelScope.launch {
            if (sessionState.value.activeConferenceId == conferenceId) {
                conferenceManager.stopConference()
            }
            conferenceProfileDao.deleteById(conferenceId)
        }
    }

    fun startConference(conferenceId: Long, onReady: (Long?) -> Unit) {
        viewModelScope.launch {
            conferenceManager.startConference(conferenceId)
            val firstChannelId = conferenceMatchMappingDao.getByConference(conferenceId).firstOrNull()?.channelId
            onReady(firstChannelId)
        }
    }

    fun stopConference() {
        conferenceManager.stopConference()
    }

    suspend fun buildChannelCandidates(match: ConferenceSelectableMatch?): List<ConferenceChannelCandidate> {
        val channelsList = channels.value
        if (channelsList.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val normalizedIds = channelsList.mapNotNull { EpgChannelIdNormalizer.normalize(it.tvgId) }.distinct()
        val programsByChannel = if (normalizedIds.isEmpty()) {
            emptyMap()
        } else {
            epgRepository.getProgramsForChannels(
                channelIds = normalizedIds,
                fromTime = now - 2 * 60 * 60 * 1000,
                toTime = now + 6 * 60 * 60 * 1000,
            )
        }

        return channelsList.map { channel ->
            val programs = EpgChannelIdNormalizer.normalize(channel.tvgId)
                ?.let { programsByChannel[it] }
                .orEmpty()
            val currentProgram = programs.find { it.startTime <= now && it.stopTime > now }
            val nextProgram = programs.firstOrNull { it.startTime > now }
            ConferenceChannelCandidate(
                channel = channel,
                currentProgram = currentProgram,
                nextProgram = nextProgram,
                matchScore = matchConferenceScore(match, channel, currentProgram, nextProgram),
            )
        }.sortedWith(
            compareByDescending<ConferenceChannelCandidate> { it.matchScore }
                .thenBy { it.channel.name.lowercase() },
        )
    }

    private fun matchConferenceScore(
        match: ConferenceSelectableMatch?,
        channel: ChannelEntity,
        currentProgram: EpgProgramEntity?,
        nextProgram: EpgProgramEntity?,
    ): Int {
        if (match == null) return 0

        val tokens = match.title.lowercase()
            .replace("vs", " ")
            .replace("-", " ")
            .split(" ")
            .map { it.trim() }
            .filter { it.length >= 3 }

        val haystacks = listOfNotNull(
            channel.name,
            currentProgram?.title,
            nextProgram?.title,
            currentProgram?.description,
        ).joinToString(" ").lowercase()

        var score = 0
        tokens.forEach { token ->
            if (haystacks.contains(token)) {
                score += 2
            }
        }
        if (currentProgram != null && tokens.any { currentProgram.title.lowercase().contains(it) }) {
            score += 3
        }
        if (nextProgram != null && tokens.any { nextProgram.title.lowercase().contains(it) }) {
            score += 1
        }
        return score
    }
}
