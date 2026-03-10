package com.djoudini.iplayer.data.local.entity

/**
 * Buffer presets for different network conditions.
 */
enum class BufferPreset(val label: String, val minMs: Int, val maxMs: Int, val playbackMs: Int, val rebufferMs: Int) {
    FAST("Schnell (wenig Buffer)", 5_000, 20_000, 1_000, 2_000),
    BALANCED("Ausgewogen", 15_000, 60_000, 2_500, 5_000),
    AGGRESSIVE("Viel Buffer (stabil)", 30_000, 120_000, 5_000, 10_000),
    CUSTOM("Benutzerdefiniert", 15_000, 60_000, 2_500, 5_000),
}

/**
 * Configuration for the ExoPlayer instance.
 * Passed to the PlayerFactory to customize buffer, decoding, and network behavior.
 */
data class PlayerConfig(
    /** User-Agent string for HTTP requests. Defaults to VLC spoofing. */
    val userAgent: String = DEFAULT_USER_AGENT,

    /** Buffer preset for quick configuration */
    val bufferPreset: BufferPreset = BufferPreset.BALANCED,

    /** Minimum buffer duration before playback starts (ms) */
    val minBufferMs: Int = 15_000,

    /** Maximum buffer to keep in memory (ms) */
    val maxBufferMs: Int = 60_000,

    /** Buffer required after re-buffering event (ms) */
    val bufferForPlaybackMs: Int = 2_500,

    /** Buffer required after re-buffering to resume (ms) */
    val bufferForPlaybackAfterRebufferMs: Int = 5_000,

    /** Prefer software decoding over hardware. Useful for problematic streams. */
    val preferSoftwareDecoding: Boolean = false,

    /** Enable tunneled video playback for lower latency on supported devices */
    val enableTunneledPlayback: Boolean = true,

    /** Enable async buffer queueing for smoother playback */
    val enableAsyncQueueing: Boolean = true,

    /** Connection timeout in seconds */
    val connectTimeoutMs: Int = 30_000,

    /** Read timeout in seconds (higher for slow streams) */
    val readTimeoutMs: Int = 120_000,

    /** Maximum retry attempts for failed streams */
    val maxRetryAttempts: Int = 3,
) {
    companion object {
        const val DEFAULT_USER_AGENT = "VLC/3.0.20 LibVLC/3.0.20"
        const val SMART_TV_USER_AGENT = "Mozilla/5.0 (SMART-TV; Linux; Tizen 6.5) AppleWebKit/537.36 (KHTML, like Gecko) Version/6.5 TV Safari/537.36"
        const val CHROME_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        /**
         * Create a PlayerConfig from a preset.
         */
        fun fromPreset(preset: BufferPreset): PlayerConfig {
            return PlayerConfig(
                bufferPreset = preset,
                minBufferMs = preset.minMs,
                maxBufferMs = preset.maxMs,
                bufferForPlaybackMs = preset.playbackMs,
                bufferForPlaybackAfterRebufferMs = preset.rebufferMs,
            )
        }
    }
}
