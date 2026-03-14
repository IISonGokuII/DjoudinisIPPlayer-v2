package com.djoudini.iplayer.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FootballDataMatchesResponse(
    @Json(name = "matches") val matches: List<FootballDataMatchDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class FootballDataMatchDto(
    @Json(name = "id") val id: Int,
    @Json(name = "status") val status: String?,
    @Json(name = "utcDate") val utcDate: String?,
    @Json(name = "competition") val competition: FootballDataCompetitionDto?,
    @Json(name = "homeTeam") val homeTeam: FootballDataTeamDto?,
    @Json(name = "awayTeam") val awayTeam: FootballDataTeamDto?,
    @Json(name = "score") val score: FootballDataScoreDto?,
)

@JsonClass(generateAdapter = true)
data class FootballDataCompetitionDto(
    @Json(name = "name") val name: String?,
)

@JsonClass(generateAdapter = true)
data class FootballDataTeamDto(
    @Json(name = "name") val name: String?,
)

@JsonClass(generateAdapter = true)
data class FootballDataScoreDto(
    @Json(name = "fullTime") val fullTime: FootballDataTimeScoreDto?,
    @Json(name = "halfTime") val halfTime: FootballDataTimeScoreDto?,
    @Json(name = "regularTime") val regularTime: FootballDataTimeScoreDto?,
)

@JsonClass(generateAdapter = true)
data class FootballDataTimeScoreDto(
    @Json(name = "home") val home: Int?,
    @Json(name = "away") val away: Int?,
)
