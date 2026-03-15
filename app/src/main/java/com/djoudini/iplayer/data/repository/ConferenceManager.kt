package com.djoudini.iplayer.data.repository

import com.djoudini.iplayer.BuildConfig
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

data class ConferenceApiTestResult(
    val success: Boolean,
    val message: String,
)

@Singleton
class ConferenceManager @Inject constructor(
    private val conferenceProfileDao: ConferenceProfileDao,
    private val conferenceMatchMappingDao: ConferenceMatchMappingDao,
    private val footballDataApi: FootballDataApi,
) {
    private val defaultCompetitionCodes = listOf(
        "BL1",  // Bundesliga
        "DED",  // Eredivisie
        "ELC",  // Championship
        "FL1",  // Ligue 1
        "PD",   // La Liga
        "PPL",  // Primeira Liga
        "SA",   // Serie A
        "PL",   // Premier League
        "CL",   // Champions League
        "EL",   // Europa League
        "EC",   // European Championship
        "WC",   // World Cup
    ).joinToString(",")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _sessionState = MutableStateFlow(ConferenceSessionState())
    val sessionState: StateFlow<ConferenceSessionState> = _sessionState.asStateFlow()

    private val _zapEvents = MutableSharedFlow<ConferenceZapEvent>(extraBufferCapacity = 8)
    val zapEvents: SharedFlow<ConferenceZapEvent> = _zapEvents.asSharedFlow()

    private var pollingJob: Job? = null
    private var returnJob: Job? = null
    private var lastZapAt: Long = 0L
    private var holdUntilAt: Long = 0L
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
        holdUntilAt = 0L
        knownScores.clear()

        pollingJob = scope.launch {
            while (true) {
                pollConference(
                    conferenceId = profile.id,
                    conferenceName = profile.name,
                    cooldownEnabled = profile.cooldownEnabled,
                    cooldownSeconds = profile.cooldownSeconds,
                    holdSeconds = profile.holdSeconds,
                    mainChannelId = mappings.minByOrNull { it.priority }?.channelId,
                    mappings = mappings,
                )
                delay(10_000)
            }
        }
    }

    fun stopConference() {
        pollingJob?.cancel()
        pollingJob = null
        returnJob?.cancel()
        returnJob = null
        knownScores.clear()
        holdUntilAt = 0L
        _sessionState.value = ConferenceSessionState()
    }

    private suspend fun pollConference(
        conferenceId: Long,
        conferenceName: String,
        cooldownEnabled: Boolean,
        cooldownSeconds: Int,
        holdSeconds: Int,
        mainChannelId: Long?,
        mappings: List<ConferenceMatchMappingEntity>,
    ) {
        try {
            val matchesById = fetchRelevantMatches().associateBy { it.id }
            mappings.forEach { mapping ->
                val match = matchesById[mapping.matchId] ?: return@forEach
                val score = extractScore(match) ?: return@forEach
                val previousScore = knownScores[mapping.matchId]
                knownScores[mapping.matchId] = score

                if (previousScore != null && (score.first > previousScore.first || score.second > previousScore.second)) {
                    val now = System.currentTimeMillis()
                    if (now < holdUntilAt) {
                        return@forEach
                    }

                    val cooldownMs = cooldownSeconds * 1000L
                    if (cooldownEnabled && now - lastZapAt < cooldownMs) {
                        return@forEach
                    }

                    lastZapAt = now
                    holdUntilAt = now + holdSeconds.coerceAtLeast(0) * 1000L
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
                    scheduleReturnToMain(
                        conferenceId = conferenceId,
                        conferenceName = conferenceName,
                        mainChannelId = mainChannelId,
                        currentChannelId = mapping.channelId,
                        holdSeconds = holdSeconds,
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Conference polling failed")
            _sessionState.value = _sessionState.value.copy(lastMessage = e.localizedMessage ?: "Konferenzfehler")
        }
    }

    private fun scheduleReturnToMain(
        conferenceId: Long,
        conferenceName: String,
        mainChannelId: Long?,
        currentChannelId: Long,
        holdSeconds: Int,
    ) {
        returnJob?.cancel()
        if (mainChannelId == null || mainChannelId == currentChannelId || holdSeconds <= 0) {
            return
        }
        returnJob = scope.launch {
            delay(holdSeconds * 1000L)
            _sessionState.value = ConferenceSessionState(
                activeConferenceId = conferenceId,
                activeConferenceName = conferenceName,
                lastMessage = "Zurueck zum Hauptspiel",
            )
            _zapEvents.emit(
                ConferenceZapEvent(
                    channelId = mainChannelId,
                    message = "Zurueck zum Hauptspiel",
                ),
            )
        }
    }

    suspend fun fetchSelectableMatches(): List<ConferenceSelectableMatch> {
        return fetchRelevantMatches()
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
    }

    suspend fun testApi(): ConferenceApiTestResult {
        return runCatching {
            val matches = fetchRelevantMatches()
            val liveCount = matches.count { it.status == "LIVE" || it.status == "IN_PLAY" || it.status == "PAUSED" }
            ConferenceApiTestResult(
                success = true,
                message = "API erreichbar. ${matches.size} Spiele geladen, davon $liveCount live/relevant.",
            )
        }.getOrElse { error ->
            ConferenceApiTestResult(
                success = false,
                message = error.message ?: "API-Test fehlgeschlagen.",
            )
        }
    }

    private suspend fun fetchRelevantMatches(): List<FootballDataMatchDto> {
        require(BuildConfig.FOOTBALL_DATA_API_TOKEN.isNotBlank()) {
            "football-data.org ist noch nicht konfiguriert. Bitte zuerst einen API-Token in local.properties hinterlegen."
        }
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val now = System.currentTimeMillis()
        val dateFrom = formatter.format(Date(now - 24L * 60L * 60L * 1000L))
        val dateTo = formatter.format(Date(now + 24L * 60L * 60L * 1000L))
        return footballDataApi.getMatches(
            dateFrom = dateFrom,
            dateTo = dateTo,
            competitions = defaultCompetitionCodes,
        ).matches
    }

    private fun extractScore(match: FootballDataMatchDto): Pair<Int, Int>? {
        val score = match.score ?: return null
        val timeScore = score.regularTime ?: score.fullTime ?: score.halfTime ?: return null
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
