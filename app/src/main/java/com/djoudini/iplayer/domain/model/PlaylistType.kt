package com.djoudini.iplayer.domain.model

enum class PlaylistType(val value: String) {
    XTREAM("xtream"),
    M3U("m3u");

    companion object {
        fun fromValue(value: String): PlaylistType =
            entries.firstOrNull { it.value == value } ?: M3U
    }
}
