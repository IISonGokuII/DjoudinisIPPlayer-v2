package com.djoudini.iplayer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbCreditsDto(
    val id: Int,
    val cast: List<CastDto>,
    val crew: List<CrewDto>,
)

@Serializable
data class CastDto(
    val id: Int,
    val name: String,
    val character: String,
    @SerialName("profile_path")
    val profilePath: String? = null,
    val order: Int = 0,
)

@Serializable
data class CrewDto(
    val id: Int,
    val name: String,
    val job: String,
    val department: String,
    @SerialName("profile_path")
    val profilePath: String? = null,
)
