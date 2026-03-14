package com.djoudini.iplayer.util

object EpgChannelIdNormalizer {

    fun normalize(id: String?): String? {
        if (id.isNullOrBlank()) return null

        return id
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .substringBefore('|')
            .replace("&amp;", "&")
            .replace("\\s+".toRegex(), " ")
            .trim()
            .lowercase()
            .takeIf { it.isNotBlank() }
    }
}
