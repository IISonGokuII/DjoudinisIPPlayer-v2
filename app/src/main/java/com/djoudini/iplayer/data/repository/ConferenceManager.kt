package com.djoudini.iplayer.data.repository

import com.djoudini.iplayer.data.local.dao.ConferenceMatchMappingDao
import com.djoudini.iplayer.data.local.dao.ConferenceProfileDao
import com.djoudini.iplayer.data.local.entity.ConferenceMatchMappingEntity
import com.djoudini.iplayer.data.remote.api.FootballDataApi
import com.djoudini.iplayer.data.remote.dto.FootballDataMatchDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class ConferenceZapEvent(
    val channelId: Long,
    val message: String,
)

data class ConferenceSessionState(
    val activeConferenceId: Long? = null,
    val activeConferenceName: String? = null,
    val lastMessage: String? = null,
)

@Singleton
class ConferenceManager @Inject constructor(
    private val conferenceProfileDao: ConferenceProfileDao,
    private val conferenceMatchMappingDao: ConferenceMatchMappingDao,
    private val footballDataApi: FootballDataApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _sessionState = MutableStateFlow(ConferenceSessionState())
    val sessionState: StateFlow<ConferenceSessionState> = _sessionState.asStateFlow()

    private val _zapEvents = MutableSharedFlow<ConferenceZapEvent>(extraBufferCapacity = 8)
    val zapEvents: SharedFlow<ConferenceZapEvent> = _zapEvents.asSharedFlow()

    private var pollingJob: Job? = null
    private var lastZapAt: Long = 0L
    private val knownScores = mutableMapOf<Int, Pair<Int, Int>>()

    suspend fun startConference(conferenceId: Long) {
        val profile = conferenceProfileDao.getById(conferenceId) ?: return
        val mappings = conferenceMatchMappingDao.getByConference(conferenceId)
        if (mappings.isEmpty()) return

        stopConference()
        _sessionState.value = ConferenceSessionState(
            activeConferenceId = profile.id,
            activeConferenceName = profile.name,
            lastMessage = "Konferenz aktiv",
        )
        lastZapAt = 0L
        knownScores.clear()

        pollingJob = scope.launch {
            while (true) {
                pollConference(profile.id, profile.name, profile.cooldownEnabled, profile.cooldownSeconds, mappings)
                delay(15_000)
            }
        }
    }

    fun stopConference() {
        pollingJob?.cancel()
        pollingJob = null
        knownScores.clear()
        _sessionState.value = ConferenceSessionState()
    }

    private suspend fun pollConference(
        conferenceId: Long,
        conferenceName: String,
        cooldownEnabled: Boolean,
        cooldownSeconds: Int,
        mappings: List<ConferenceMatchMappingEntity>,
    ) {
        try {
            val liveMatches = footballDataApi.getMatches(status = "LIVE").matches.associateBy { it.id }
            mappings.forEach { mapping ->
                val match = liveMatches[mapping.matchId] ?: return@forEach
                val score = extractScore(match) ?: return@forEach
                val previousScore = knownScores[mapping.matchId]
                knownScores[mapping.matchId] = score

                if (previousScore != null && (score.first > previousScore.first || score.second > previousScore.second)) {
                    val now = System.currentTimeMillis()
                    val cooldownMs = cooldownSeconds * 1000L
                    if (cooldownEnabled && now - lastZapAt < cooldownMs) {
                        return@forEach
                    }

                    lastZapAt = now
                    val message = "Tor bei ${mapping.matchLabel} - Wechsel zu ${mapping.channelName}"
                    _sessionState.value = ConferenceSessionState(
                        activeConferenceId = conferenceId,
                        activeConferenceName = conferenceName,
                        lastMessage = message,
                    )
                    _zapEvents.emit(
                        ConferenceZapEvent(
                            channelId = mapping.channelId,
                            message = message,
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Conference polling failed")
            _sessionState.value = _sessionState.value.copy(lastMessage = e.localizedMessage ?: "Konferenzfehler")
        }
    }

    suspend fun fetchSelectableMatches(): List<ConferenceSelectableMatch> {
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
            footballDataApi.getMatches(dateFrom = today, dateTo = today).matches
                .filter { it.homeTeam?.name != null && it.awayTeam?.name != null }
                .sortedWith(compareBy<FootballDataMatchDto> { matchStatusPriority(it.status) }.thenBy { it.utcDate ?: "" })
                .map { match ->
                    ConferenceSelectableMatch(
                        matchId = match.id,
                        title = "${match.homeTeam?.name} vs ${match.awayTeam?.name}",
                        subtitle = buildString {
                            append(match.competition?.name ?: "Unbekannter Wettbewerb")
                            match.status?.takeIf { it.isNotBlank() }?.let {
                                append("  •  ")
                                append(it)
                            }
                        },
                    )
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch selectable matches")
            emptyList()
        }
    }

    private fun extractScore(match: FootballDataMatchDto): Pair<Int, Int>? {
        val score = match.score ?: return null
        val timeScore = score.fullTime ?: score.regularTime ?: score.halfTime ?: return null
        return Pair(timeScore.home ?: 0, timeScore.away ?: 0)
    }

    private fun matchStatusPriority(status: String?): Int {
        return when (status) {
            "LIVE", "IN_PLAY", "PAUSED" -> 0
            "TIMED", "SCHEDULED" -> 1
            else -> 2
        }
    }
}

data class ConferenceSelectableMatch(
    val matchId: Int,
    val title: String,
    val subtitle: String,
)
