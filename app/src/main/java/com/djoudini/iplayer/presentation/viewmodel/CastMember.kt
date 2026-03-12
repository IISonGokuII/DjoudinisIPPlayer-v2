package com.djoudini.iplayer.presentation.viewmodel

/**
 * Represents a cast member (actor) from TMDB.
 */
data class CastMember(
    val name: String,
    val character: String?,
    val profilePath: String?,
)
