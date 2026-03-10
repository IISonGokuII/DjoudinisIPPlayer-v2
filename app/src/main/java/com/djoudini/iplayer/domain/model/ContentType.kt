package com.djoudini.iplayer.domain.model

enum class ContentType(val value: String) {
    LIVE("live"),
    VOD("vod"),
    SERIES("series");

    companion object {
        fun fromValue(value: String): ContentType =
            entries.firstOrNull { it.value == value } ?: LIVE
    }
}

enum class WatchContentType(val value: String) {
    CHANNEL("channel"),
    VOD("vod"),
    EPISODE("episode");

    companion object {
        fun fromValue(value: String): WatchContentType =
            entries.firstOrNull { it.value == value } ?: CHANNEL
    }
}
