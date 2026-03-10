package com.djoudini.iplayer.util

import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds catch-up / timeshift URLs based on the channel's catch-up type.
 *
 * Supported catch-up types:
 * - "default": Xtream standard catch-up URL format
 * - "append": Appends timeshift parameters to the stream URL
 * - "shift": Uses a separate timeshift endpoint
 * - "flussonic": Flussonic CDN format with unix timestamps
 *
 * Usage: Pass a channel and the EPG program to get the catch-up stream URL.
 */
object CatchupUrlBuilder {

    private val utcFormat = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val xtreamDateFormat = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Build a catch-up URL for the given channel and program.
     *
     * @param channel The channel with catch-up configuration.
     * @param program The EPG program to play back.
     * @return Catch-up stream URL, or null if catch-up is not supported.
     */
    fun build(channel: ChannelEntity, program: EpgProgramEntity): String? {
        val catchupType = channel.catchupType ?: return null

        val startTime = program.startTime
        val endTime = program.stopTime
        val durationSeconds = ((endTime - startTime) / 1000).toInt()
        val startDate = Date(startTime)
        val endDate = Date(endTime)

        return when (catchupType.lowercase()) {
            "default" -> buildDefault(channel, startDate, durationSeconds)
            "append" -> buildAppend(channel, startDate, endDate, durationSeconds)
            "shift" -> buildShift(channel, startDate, durationSeconds)
            "flussonic", "fs" -> buildFlussonic(channel, startTime, endTime)
            else -> buildDefault(channel, startDate, durationSeconds)
        }
    }

    /**
     * Xtream Codes default catch-up:
     * {server}/{username}/{password}/timeshift/{duration}/{start}/{stream_id}.{ext}
     */
    private fun buildDefault(channel: ChannelEntity, start: Date, durationMinutes: Int): String {
        val baseUrl = channel.streamUrl
        val formattedStart = xtreamDateFormat.format(start)
        val duration = durationMinutes / 60 // Xtream uses hours

        // Extract components from the stream URL
        // Format: http://server/username/password/streamId.ext
        val parts = baseUrl.split("/")
        if (parts.size < 6) return baseUrl

        val server = parts.subList(0, 3).joinToString("/")
        val username = parts[3]
        val password = parts[4]
        val streamFile = parts.last()

        return "$server/timeshift/$username/$password/$duration/$formattedStart/$streamFile"
    }

    /**
     * Append type: adds catchup parameters to the source template.
     */
    private fun buildAppend(channel: ChannelEntity, start: Date, end: Date, durationSeconds: Int): String {
        val source = channel.catchupSource ?: return channel.streamUrl
        val startUnix = start.time / 1000
        val endUnix = end.time / 1000
        val formattedStart = utcFormat.format(start)

        return source
            .replace("\${start}", startUnix.toString())
            .replace("\${end}", endUnix.toString())
            .replace("\${duration}", durationSeconds.toString())
            .replace("\${timestamp}", formattedStart)
            .replace("{start}", startUnix.toString())
            .replace("{end}", endUnix.toString())
            .replace("{duration}", durationSeconds.toString())
            .replace("{timestamp}", formattedStart)
            .replace("{utc}", startUnix.toString())
            .replace("{lutc}", endUnix.toString())
    }

    /**
     * Shift type: simple timeshift parameter append.
     */
    private fun buildShift(channel: ChannelEntity, start: Date, durationSeconds: Int): String {
        val baseUrl = channel.streamUrl
        val startUnix = start.time / 1000
        val separator = if (baseUrl.contains("?")) "&" else "?"
        return "${baseUrl}${separator}utc=${startUnix}&lutc=${startUnix + durationSeconds}"
    }

    /**
     * Flussonic CDN format: uses unix timestamps directly in the URL path.
     * Format: {baseUrl}/timeshift_abs-{startUnix}.ts
     * or: {baseUrl}?start={startUnix}&end={endUnix}
     */
    private fun buildFlussonic(channel: ChannelEntity, startMs: Long, endMs: Long): String {
        val baseUrl = channel.streamUrl
        val startUnix = startMs / 1000
        val endUnix = endMs / 1000

        val source = channel.catchupSource
        if (!source.isNullOrBlank()) {
            return source
                .replace("{start}", startUnix.toString())
                .replace("{end}", endUnix.toString())
                .replace("\${start}", startUnix.toString())
                .replace("\${end}", endUnix.toString())
                .replace("{utc}", startUnix.toString())
                .replace("{lutc}", endUnix.toString())
        }

        // Default flussonic format
        val separator = if (baseUrl.contains("?")) "&" else "?"
        return "${baseUrl}${separator}start=${startUnix}&end=${endUnix}"
    }

    /**
     * Check if a channel supports catch-up playback.
     */
    fun isSupported(channel: ChannelEntity): Boolean {
        return !channel.catchupType.isNullOrBlank() && (channel.catchupDays ?: 0) > 0
    }

    /**
     * Check if a program is within the catch-up archive window.
     */
    fun isArchived(channel: ChannelEntity, program: EpgProgramEntity): Boolean {
        if (!isSupported(channel)) return false
        val archiveWindowMs = (channel.catchupDays ?: 0) * 24L * 60L * 60L * 1000L
        val archiveStart = System.currentTimeMillis() - archiveWindowMs
        return program.startTime >= archiveStart && program.stopTime <= System.currentTimeMillis()
    }
}
