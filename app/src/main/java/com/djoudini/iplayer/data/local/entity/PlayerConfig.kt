package com.djoudini.iplayer.data.local.entity

/**
 * Configuration for the ExoPlayer instance.
 * Passed to the PlayerFactory to customize buffer, decoding, and network behavior.
 */
data class PlayerConfig(
    /** User-Agent string for HTTP requests. Defaults to VLC spoofing. */
    val userAgent: String = DEFAULT_USER_AGENT,

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
) {
    companion object {
        const val DEFAULT_USER_AGENT = "VLC/3.0.20 LibVLC/3.0.20"
        const val SMART_TV_USER_AGENT = "Mozilla/5.0 (SMART-TV; Linux; Tizen 6.5) AppleWebKit/537.36 (KHTML, like Gecko) Version/6.5 TV Safari/537.36"
        const val CHROME_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}
