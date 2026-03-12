package com.djoudini.iplayer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbCreditsResponse(
    val id: Int,
    val cast: List<CastCredit>,
    val crew: List<CrewCredit>,
)

@Serializable
data class CastCredit(
    val id: Int,
    val name: String,
    val character: String,
    @SerialName("profile_path")
    val profilePath: String? = null,
    val order: Int = 0,
)

@Serializable
data class CrewCredit(
    val id: Int,
    val name: String,
    val job: String,
    val department: String,
    @SerialName("profile_path")
    val profilePath: String? = null,
)
