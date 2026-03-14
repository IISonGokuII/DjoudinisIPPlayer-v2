package com.djoudini.iplayer.presentation.viewmodel

/**
 * Small pure helpers extracted from PlayerViewModel so they can be tested
 * without spinning up the full ViewModel dependency graph.
 */
object PlayerPlaybackLogic {

    fun buildStreamFallbacks(originalUrl: String): List<String> {
        val urls = mutableListOf(originalUrl)

        val liveIndex = originalUrl.indexOf("/live/")
        if (liveIndex != -1) {
            val lastSlash = originalUrl.lastIndexOf('/')
            if (lastSlash > liveIndex) {
                val dotIndex = originalUrl.lastIndexOf('.')
                if (dotIndex > lastSlash) {
                    val base = originalUrl.substring(0, dotIndex)
                    val extensions = listOf("ts", "m3u8", "mpegts")
                    for (ext in extensions) {
                        val alt = "$base.$ext"
                        if (alt != originalUrl && alt !in urls) {
                            urls.add(alt)
                        }
                    }
                }
            }
        } else {
            val lastSlash = originalUrl.lastIndexOf('/')
            val dotIndex = originalUrl.lastIndexOf('.')
            if (dotIndex > lastSlash) {
                val base = originalUrl.substring(0, dotIndex)
                val extensions = listOf(".ts", ".m3u8", ".mpegts")
                for (ext in extensions) {
                    val alt = "$base$ext"
                    if (alt != originalUrl && alt !in urls) {
                        urls.add(alt)
                    }
                }
            }
        }

        return urls
    }

    fun nextAspectRatio(current: AspectRatio): AspectRatio = when (current) {
        AspectRatio.FIT_16_9 -> AspectRatio.FIT_4_3
        AspectRatio.FIT_4_3 -> AspectRatio.ZOOM
        AspectRatio.ZOOM -> AspectRatio.STRETCH
        AspectRatio.STRETCH -> AspectRatio.ORIGINAL
        AspectRatio.ORIGINAL -> AspectRatio.FIT_16_9
    }

    fun clampVideoScale(scale: Float): Float = scale.coerceIn(0.5f, 3.0f)

    fun clampAudioDelay(delayMs: Int): Int = delayMs.coerceIn(-5000, 5000)
}
