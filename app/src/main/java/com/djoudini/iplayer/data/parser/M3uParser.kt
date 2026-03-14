package com.djoudini.iplayer.data.parser

import com.djoudini.iplayer.domain.model.ContentType
import com.djoudini.iplayer.util.EpgChannelIdNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory-safe M3U/M3U8 playlist parser.
 *
 * Parses line-by-line using BufferedReader to avoid loading entire playlists into memory.
 * Supports #EXTINF attributes: tvg-id, tvg-name, tvg-logo, group-title, catchup, etc.
 * Emits parsed items in chunks via a callback to enable progress tracking.
 */
@Singleton
class M3uParser @Inject constructor() {

    data class M3uItem(
        val name: String,
        val streamUrl: String,
        val groupTitle: String,
        val tvgId: String?,
        val tvgName: String?,
        val tvgLogo: String?,
        val catchupType: String?,
        val catchupSource: String?,
        val catchupDays: Int?,
        val userAgent: String?,
        val contentType: ContentType,
    )

    data class M3uResult(
        val items: List<M3uItem>,
        val groups: Set<String>,
        val epgUrl: String?,
    )

    /**
     * Parse an M3U stream in a memory-safe manner.
     *
     * @param inputStream Raw input stream of the M3U content.
     * @param onProgress Called with (processedLines, currentItem) periodically.
     * @param chunkSize Number of items to accumulate before flushing to [onChunk].
     * @param onChunk Called with each chunk of parsed items for batch DB insertion.
     */
    suspend fun parse(
        inputStream: InputStream,
        onProgress: suspend (lineCount: Int) -> Unit = {},
        chunkSize: Int = 500,
        onChunk: suspend (List<M3uItem>) -> Unit = {},
    ): M3uResult = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<M3uItem>()
        val groups = mutableSetOf<String>()
        val chunk = mutableListOf<M3uItem>()

        var lineCount = 0
        var currentExtInf: String? = null
        var epgUrl: String? = null

        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                ensureActive()
                lineCount++

                val trimmed = line.trim()

                when {
                    trimmed.startsWith("#EXTM3U") -> {
                        epgUrl = extractFirstNonBlankAttribute(
                            trimmed,
                            "x-tvg-url",
                            "url-tvg",
                            "tvg-url",
                        ) ?: epgUrl
                    }
                    trimmed.startsWith("#EXTINF:") -> {
                        currentExtInf = trimmed
                    }
                    trimmed.isNotEmpty() && !trimmed.startsWith("#") && currentExtInf != null -> {
                        val item = parseExtInfLine(currentExtInf!!, trimmed)
                        if (item != null) {
                            allItems.add(item)
                            chunk.add(item)
                            groups.add(item.groupTitle)

                            if (chunk.size >= chunkSize) {
                                onChunk(chunk.toList())
                                chunk.clear()
                            }
                        }
                        currentExtInf = null
                    }
                    else -> {
                        // Comment or empty line
                        currentExtInf = null
                    }
                }

                if (lineCount % 1000 == 0) {
                    onProgress(lineCount)
                }

                line = reader.readLine()
            }
        }

        // Flush remaining chunk
        if (chunk.isNotEmpty()) {
            onChunk(chunk.toList())
        }
        onProgress(lineCount)

        M3uResult(items = allItems, groups = groups, epgUrl = epgUrl)
    }

    private fun parseExtInfLine(extInf: String, url: String): M3uItem? {
        return try {
            // Extract attributes from #EXTINF:-1 tvg-id="..." tvg-name="..." ...  ,Channel Name
            val tvgId = EpgChannelIdNormalizer.normalize(extractAttribute(extInf, "tvg-id"))
            val tvgName = extractAttribute(extInf, "tvg-name")
            val tvgLogo = extractAttribute(extInf, "tvg-logo")
            val groupTitle = extractAttribute(extInf, "group-title") ?: "Uncategorized"
            val catchup = extractAttribute(extInf, "catchup")
            val catchupSource = extractAttribute(extInf, "catchup-source")
            val catchupDays = extractAttribute(extInf, "catchup-days")?.toIntOrNull()
            val userAgent = extractAttribute(extInf, "user-agent")

            // Channel name is after the last comma
            val commaIndex = extInf.lastIndexOf(',')
            val name = if (commaIndex >= 0 && commaIndex < extInf.length - 1) {
                extInf.substring(commaIndex + 1).trim()
            } else {
                tvgName ?: url.substringAfterLast('/').substringBeforeLast('.')
            }

            // Determine content type by URL pattern
            val contentType = inferContentType(url, groupTitle)

            M3uItem(
                name = name,
                streamUrl = url,
                groupTitle = groupTitle,
                tvgId = tvgId,
                tvgName = tvgName,
                tvgLogo = tvgLogo,
                catchupType = catchup,
                catchupSource = catchupSource,
                catchupDays = catchupDays,
                userAgent = userAgent,
                contentType = contentType,
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse M3U line: $extInf")
            null
        }
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = """$attribute="([^"]*)"""".toRegex()
        return pattern.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun extractFirstNonBlankAttribute(line: String, vararg attributes: String): String? {
        return attributes.firstNotNullOfOrNull { attribute ->
            extractAttribute(line, attribute)
        }?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun inferContentType(url: String, groupTitle: String): ContentType {
        // OPTIMIERUNG: Early Returns und weniger String-Operationen
        val lowerUrl = url.lowercase()
        
        // URL-basierte Erkennung (schnellste Methode)
        when {
            lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".avi") -> return ContentType.VOD
            lowerUrl.contains("/movie/") -> return ContentType.VOD
            lowerUrl.contains("/series/") -> return ContentType.SERIES
        }
        
        // Group-basierte Erkennung (nur wenn URL nicht eindeutig)
        val lowerGroup = groupTitle.lowercase()
        return when {
            lowerGroup.contains("series") || lowerGroup.contains("serie") -> ContentType.SERIES
            lowerGroup.contains("vod") || lowerGroup.contains("movie") || lowerGroup.contains("film") -> ContentType.VOD
            else -> ContentType.LIVE
        }
    }
}
