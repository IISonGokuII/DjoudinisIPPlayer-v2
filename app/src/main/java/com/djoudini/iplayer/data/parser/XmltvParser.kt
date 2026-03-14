package com.djoudini.iplayer.data.parser

import com.djoudini.iplayer.data.local.entity.EpgProgramEntity
import com.djoudini.iplayer.util.EpgChannelIdNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.InputStream
import java.io.PushbackInputStream
import java.text.SimpleDateFormat
import java.util.zip.GZIPInputStream
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory-safe XMLTV EPG parser using XmlPullParser (streaming).
 *
 * Processes large EPG files without loading them fully into memory.
 * Emits batches of parsed programs via callback for chunked DB insertion.
 */
@Singleton
class XmltvParser @Inject constructor() {

    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val dateFormatNoTz = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Parse XMLTV EPG data from an input stream.
     *
     * @param inputStream Raw XMLTV data stream.
     * @param playlistId Playlist to associate programs with.
     * @param batchSize Number of programs to accumulate before flushing.
     * @param onBatch Called with each batch for DB insertion.
     * @param onProgress Called with (programCount) periodically.
     * @return Total number of programs parsed.
     */
    suspend fun parse(
        inputStream: InputStream,
        playlistId: Long,
        batchSize: Int = 500,
        onBatch: suspend (List<EpgProgramEntity>) -> Unit = {},
        onProgress: suspend (count: Int) -> Unit = {},
    ): Int = withContext(Dispatchers.IO) {
        var totalCount = 0
        val batch = mutableListOf<EpgProgramEntity>()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        val normalizedInput = decompressIfNeeded(inputStream)
        parser.setInput(normalizedInput, "UTF-8")

        var eventType = parser.eventType
        var inProgramme = false
        var channelId = ""
        var startTime = 0L
        var stopTime = 0L
        var title = ""
        var description: String? = null
        var category: String? = null
        var iconUrl: String? = null
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            ensureActive()

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            inProgramme = true
                            channelId = EpgChannelIdNormalizer.normalize(
                                parser.getAttributeValue(null, "channel"),
                            ) ?: ""
                            startTime = parseXmltvDate(parser.getAttributeValue(null, "start"))
                            stopTime = parseXmltvDate(parser.getAttributeValue(null, "stop"))
                            title = ""
                            description = null
                            category = null
                            iconUrl = null
                        }
                        "icon" -> {
                            if (inProgramme) {
                                iconUrl = parser.getAttributeValue(null, "src")
                            }
                        }
                        else -> {
                            if (inProgramme) {
                                currentTag = parser.name
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inProgramme) {
                        val text = parser.text?.trim() ?: ""
                        when (currentTag) {
                            "title" -> title = text
                            "desc" -> description = text.takeIf { it.isNotEmpty() }
                            "category" -> category = text.takeIf { it.isNotEmpty() }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme" && inProgramme) {
                        inProgramme = false
                        currentTag = ""

                        if (channelId.isNotBlank() && title.isNotBlank() && startTime > 0 && stopTime > 0) {
                            val program = EpgProgramEntity(
                                playlistId = playlistId,
                                epgChannelId = channelId,
                                title = title,
                                description = description,
                                startTime = startTime,
                                stopTime = stopTime,
                                category = category,
                                iconUrl = iconUrl,
                            )
                            batch.add(program)
                            totalCount++

                            if (batch.size >= batchSize) {
                                onBatch(batch.toList())
                                batch.clear()
                                onProgress(totalCount)
                            }
                        }
                    } else {
                        currentTag = ""
                    }
                }
            }

            eventType = parser.next()
        }

        // Flush remaining batch
        if (batch.isNotEmpty()) {
            onBatch(batch.toList())
            onProgress(totalCount)
        }

        totalCount
    }

    private fun parseXmltvDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        return try {
            if (dateStr.contains(" ")) {
                dateFormat.parse(dateStr)?.time ?: 0L
            } else {
                dateFormatNoTz.parse(dateStr)?.time ?: 0L
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse XMLTV date: $dateStr")
            0L
        }
    }

    private fun decompressIfNeeded(inputStream: InputStream): InputStream {
        val pushbackStream = PushbackInputStream(inputStream, 2)
        val signature = ByteArray(2)
        val bytesRead = pushbackStream.read(signature)
        if (bytesRead > 0) {
            pushbackStream.unread(signature, 0, bytesRead)
        }

        val isGzip = bytesRead == 2 &&
            signature[0] == 0x1f.toByte() &&
            signature[1] == 0x8b.toByte()

        return if (isGzip) GZIPInputStream(pushbackStream) else pushbackStream
    }
}
