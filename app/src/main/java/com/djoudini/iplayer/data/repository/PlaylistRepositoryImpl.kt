package com.djoudini.iplayer.data.repository

import com.djoudini.iplayer.data.local.dao.CategoryDao
import com.djoudini.iplayer.data.local.dao.ChannelDao
import com.djoudini.iplayer.data.local.dao.EpgProgramDao
import com.djoudini.iplayer.data.local.dao.EpisodeDao
import com.djoudini.iplayer.data.local.dao.PlaylistDao
import com.djoudini.iplayer.data.local.dao.SeriesDao
import com.djoudini.iplayer.data.local.dao.VodDao
import com.djoudini.iplayer.data.local.entity.CategoryEntity
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.data.local.entity.EpisodeEntity
import com.djoudini.iplayer.data.local.entity.PlaylistEntity
import com.djoudini.iplayer.data.local.entity.SeriesEntity
import com.djoudini.iplayer.data.local.entity.VodEntity
import com.djoudini.iplayer.data.parser.M3uParser
import com.djoudini.iplayer.data.parser.XmltvParser
import com.djoudini.iplayer.data.remote.api.XtreamApi
import com.djoudini.iplayer.data.remote.dto.XtreamStreamDto
import com.djoudini.iplayer.domain.model.ContentType
import com.djoudini.iplayer.domain.model.PlaylistType
import com.djoudini.iplayer.domain.model.SyncProgress
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [PlaylistRepository].
 *
 * Core responsibilities:
 * - CRUD for playlists
 * - Full sync pipeline (Xtream API or M3U parsing → Room DB)
 * - EPG sync pipeline (XMLTV parsing → Room DB)
 * - Real-time progress reporting via [StateFlow<SyncProgress>]
 *
 * The progress StateFlow is the key "killer feature": it drives the Compose UI
 * progress rings/bars on dashboard tiles in real-time.
 */
@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val categoryDao: CategoryDao,
    private val channelDao: ChannelDao,
    private val vodDao: VodDao,
    private val seriesDao: SeriesDao,
    private val episodeDao: EpisodeDao,
    private val epgProgramDao: EpgProgramDao,
    private val xtreamApi: XtreamApi,
    private val m3uParser: M3uParser,
    private val xmltvParser: XmltvParser,
) : PlaylistRepository {

    private val _syncProgress = MutableStateFlow(SyncProgress.Idle)
    override val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    private var syncJob: Job? = null

    // --- CRUD ---

    override fun observeAll(): Flow<List<PlaylistEntity>> = playlistDao.observeAll()

    override fun observeActive(): Flow<PlaylistEntity?> = playlistDao.observeActive()

    override suspend fun getActive(): PlaylistEntity? = playlistDao.getActive()

    override suspend fun addXtreamPlaylist(
        name: String,
        serverUrl: String,
        username: String,
        password: String,
    ): Long {
        val normalizedUrl = serverUrl.trimEnd('/')
        val playlist = PlaylistEntity(
            name = name,
            type = PlaylistType.XTREAM.value,
            serverUrl = normalizedUrl,
            username = username,
            password = password,
        )
        return playlistDao.insert(playlist)
    }

    override suspend fun addM3uPlaylist(name: String, m3uUrl: String): Long {
        val playlist = PlaylistEntity(
            name = name,
            type = PlaylistType.M3U.value,
            m3uUrl = m3uUrl,
        )
        return playlistDao.insert(playlist)
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deleteById(playlistId)
    }

    override suspend fun setActive(playlistId: Long) {
        playlistDao.deactivateAll()
        playlistDao.activate(playlistId)
    }

    // --- Sync Pipeline ---

    override suspend fun syncPlaylist(playlistId: Long) {
        val playlist = playlistDao.getById(playlistId)
            ?: throw IllegalArgumentException("Playlist $playlistId not found")

        cancelSync()

        coroutineScope {
            syncJob = launch {
                try {
                    _syncProgress.value = SyncProgress.indeterminate("Connecting...")

                    when (PlaylistType.fromValue(playlist.type)) {
                        PlaylistType.XTREAM -> syncXtream(playlist)
                        PlaylistType.M3U -> syncM3u(playlist)
                    }

                    playlistDao.updateLastSynced(playlistId, System.currentTimeMillis())
                    _syncProgress.value = SyncProgress.completed()
                } catch (e: CancellationException) {
                    _syncProgress.value = SyncProgress.Idle
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Sync failed for playlist $playlistId")
                    _syncProgress.value = SyncProgress.failed(
                        e.localizedMessage ?: "Sync failed"
                    )
                }
            }
        }
    }

    /**
     * Phase 1: Only authenticate and fetch categories (no streams).
     * Very fast – typically completes in 1-2 seconds.
     */
    private suspend fun syncXtreamCategoriesOnly(playlist: PlaylistEntity) {
        val serverUrl = playlist.serverUrl ?: throw IllegalStateException("Missing server URL")
        val username = playlist.username ?: throw IllegalStateException("Missing username")
        val password = playlist.password ?: throw IllegalStateException("Missing password")
        val apiUrl = "$serverUrl/player_api.php"

        _syncProgress.value = SyncProgress.active("Authenticating...", 0.1f)
        val authResponse = xtreamApi.authenticate(apiUrl, username, password)
        val userInfo = authResponse.userInfo

        if (userInfo?.auth != 1) {
            throw IllegalStateException("Authentication failed: ${userInfo?.message ?: "Invalid credentials"}")
        }

        playlistDao.updateAccountInfo(
            playlistId = playlist.id,
            expDate = userInfo.expDate?.toLongOrNull()?.let { it * 1000 },
            maxConn = userInfo.maxConnections?.toIntOrNull(),
            status = userInfo.status ?: "active"
        )

        // Auto-set EPG URL for Xtream playlists
        if (playlist.epgUrl == null) {
            val epgUrl = "$serverUrl/xmltv.php?username=$username&password=$password"
            playlistDao.updateEpgUrl(playlist.id, epgUrl)
        }

        _syncProgress.value = SyncProgress.active("Loading categories...", 0.3f)
        val liveCategories = xtreamApi.getLiveCategories(apiUrl, username, password)
        val vodCategories = xtreamApi.getVodCategories(apiUrl, username, password)
        val seriesCategories = xtreamApi.getSeriesCategories(apiUrl, username, password)

        // Preserve existing selection state during re-sync
        val existingCategories = categoryDao.getAllByPlaylist(playlist.id)
        val existingSelectionMap = existingCategories.associate {
            "${it.categoryType}:${it.remoteId}" to it.isSelected
        }

        categoryDao.deleteByPlaylist(playlist.id)

        val allCategoryEntities = buildList {
            liveCategories.forEach { dto ->
                val remoteId = dto.categoryId ?: return@forEach
                val wasSelected = existingSelectionMap["${ContentType.LIVE.value}:$remoteId"]
                add(CategoryEntity(
                    playlistId = playlist.id,
                    remoteId = remoteId,
                    name = dto.categoryName ?: "Unknown",
                    categoryType = ContentType.LIVE.value,
                    isSelected = wasSelected ?: true,
                ))
            }
            vodCategories.forEach { dto ->
                val remoteId = dto.categoryId ?: return@forEach
                val wasSelected = existingSelectionMap["${ContentType.VOD.value}:$remoteId"]
                add(CategoryEntity(
                    playlistId = playlist.id,
                    remoteId = remoteId,
                    name = dto.categoryName ?: "Unknown",
                    categoryType = ContentType.VOD.value,
                    isSelected = wasSelected ?: true,
                ))
            }
            seriesCategories.forEach { dto ->
                val remoteId = dto.categoryId ?: return@forEach
                val wasSelected = existingSelectionMap["${ContentType.SERIES.value}:$remoteId"]
                add(CategoryEntity(
                    playlistId = playlist.id,
                    remoteId = remoteId,
                    name = dto.categoryName ?: "Unknown",
                    categoryType = ContentType.SERIES.value,
                    isSelected = wasSelected ?: true,
                ))
            }
        }

        // Insert one by one to track auto-generated IDs
        for (entity in allCategoryEntities) {
            categoryDao.insert(entity)
        }

        _syncProgress.value = SyncProgress.completed()
    }

    /**
     * Phase 2: Sync only streams for user-selected categories.
     * Much faster than full sync because unselected categories are skipped entirely.
     */
    private suspend fun syncXtreamSelectedStreams(playlist: PlaylistEntity) {
        val serverUrl = playlist.serverUrl ?: throw IllegalStateException("Missing server URL")
        val username = playlist.username ?: throw IllegalStateException("Missing username")
        val password = playlist.password ?: throw IllegalStateException("Missing password")
        val apiUrl = "$serverUrl/player_api.php"

        fun categoryKey(type: String, remoteId: String) = "$type:$remoteId"

        // Build category ID map from already-saved categories
        val categoryIdMap = mutableMapOf<String, Long>()
        val selectedRemoteIds = mutableMapOf<String, MutableSet<String>>()

        for (type in listOf(ContentType.LIVE.value, ContentType.VOD.value, ContentType.SERIES.value)) {
            selectedRemoteIds[type] = mutableSetOf()
        }

        val allCategories = categoryDao.getAllByPlaylist(playlist.id)
        for (cat in allCategories) {
            categoryIdMap[categoryKey(cat.categoryType, cat.remoteId)] = cat.id
            if (cat.isSelected) {
                selectedRemoteIds[cat.categoryType]?.add(cat.remoteId)
            }
        }

        val selectedLive = selectedRemoteIds[ContentType.LIVE.value] ?: emptySet()
        val selectedVod = selectedRemoteIds[ContentType.VOD.value] ?: emptySet()
        val selectedSeries = selectedRemoteIds[ContentType.SERIES.value] ?: emptySet()

        val totalSteps = (if (selectedLive.isNotEmpty()) 1 else 0) +
                (if (selectedVod.isNotEmpty()) 1 else 0) +
                (if (selectedSeries.isNotEmpty()) 1 else 0)
        var currentStep = 0

        fun stepProgress(intraProgress: Float): Float {
            if (totalSteps == 0) return 0.95f
            return ((currentStep + intraProgress) / totalSteps).coerceAtMost(0.95f)
        }

        // Sync live channels (only selected categories)
        if (selectedLive.isNotEmpty()) {
            _syncProgress.value = SyncProgress.active("Loading live channels...", stepProgress(0f))
            channelDao.deleteByPlaylist(playlist.id)

            // Fetch per selected category for faster response
            val channelEntities = mutableListOf<ChannelEntity>()
            selectedLive.forEachIndexed { index, remoteId ->
                val streams = xtreamApi.getLiveStreams(apiUrl, username, password, categoryId = remoteId)
                val catId = categoryIdMap[categoryKey(ContentType.LIVE.value, remoteId)] ?: return@forEachIndexed
                streams.mapNotNullTo(channelEntities) { dto ->
                    dto.toChannelEntity(playlist, catId, serverUrl, username, password)
                }
                _syncProgress.value = SyncProgress(
                    phase = "Live channels... (${channelEntities.size})",
                    progress = stepProgress((index + 1).toFloat() / selectedLive.size),
                    processedItems = channelEntities.size,
                    isActive = true,
                )
            }
            channelDao.insertAll(channelEntities)
            currentStep++
        } else {
            channelDao.deleteByPlaylist(playlist.id)
        }

        // Sync VOD (only selected categories)
        if (selectedVod.isNotEmpty()) {
            _syncProgress.value = SyncProgress.active("Loading movies...", stepProgress(0f))
            vodDao.deleteByPlaylist(playlist.id)

            val vodEntities = mutableListOf<VodEntity>()
            selectedVod.forEachIndexed { index, remoteId ->
                val streams = xtreamApi.getVodStreams(apiUrl, username, password, categoryId = remoteId)
                val catId = categoryIdMap[categoryKey(ContentType.VOD.value, remoteId)] ?: return@forEachIndexed
                streams.mapNotNullTo(vodEntities) { dto ->
                    dto.toVodEntity(playlist, catId, serverUrl, username, password)
                }
                _syncProgress.value = SyncProgress(
                    phase = "Movies... (${vodEntities.size})",
                    progress = stepProgress((index + 1).toFloat() / selectedVod.size),
                    processedItems = vodEntities.size,
                    isActive = true,
                )
            }
            vodDao.insertAll(vodEntities)
            currentStep++
        } else {
            vodDao.deleteByPlaylist(playlist.id)
        }

        // Sync series (only selected categories)
        if (selectedSeries.isNotEmpty()) {
            _syncProgress.value = SyncProgress.active("Loading series...", stepProgress(0f))
            seriesDao.deleteByPlaylist(playlist.id)
            episodeDao.deleteByPlaylist(playlist.id)

            val seriesEntities = mutableListOf<SeriesEntity>()
            selectedSeries.forEachIndexed { index, remoteId ->
                val streams = xtreamApi.getSeries(apiUrl, username, password, categoryId = remoteId)
                val catId = categoryIdMap[categoryKey(ContentType.SERIES.value, remoteId)] ?: return@forEachIndexed
                streams.mapNotNullTo(seriesEntities) { dto ->
                    dto.toSeriesEntity(playlist, catId)
                }
                _syncProgress.value = SyncProgress(
                    phase = "Series... (${seriesEntities.size})",
                    progress = stepProgress((index + 1).toFloat() / selectedSeries.size),
                    processedItems = seriesEntities.size,
                    isActive = true,
                )
            }
            seriesDao.insertAll(seriesEntities)
            currentStep++
        } else {
            seriesDao.deleteByPlaylist(playlist.id)
            episodeDao.deleteByPlaylist(playlist.id)
        }

        _syncProgress.value = SyncProgress.active("Finalizing...", 0.95f)
    }

    private suspend fun syncXtream(playlist: PlaylistEntity) {
        syncXtreamCategoriesOnly(playlist)
        _syncProgress.value = SyncProgress.active("Loading streams...", 0.2f)
        syncXtreamSelectedStreams(playlist)
    }

    private suspend fun syncM3u(playlist: PlaylistEntity) {
        val m3uUrl = playlist.m3uUrl ?: throw IllegalStateException("Missing M3U URL")

        _syncProgress.value = SyncProgress.active("Downloading playlist...", 0.1f)

        val responseBody = xtreamApi.fetchRawUrl(m3uUrl)

        // Clear existing data
        channelDao.deleteByPlaylist(playlist.id)
        vodDao.deleteByPlaylist(playlist.id)
        seriesDao.deleteByPlaylist(playlist.id)
        categoryDao.deleteByPlaylist(playlist.id)

        _syncProgress.value = SyncProgress.active("Parsing playlist...", 0.2f)

        val groupToCategoryId = mutableMapOf<String, Long>()
        var totalItems = 0

        val result = m3uParser.parse(
            inputStream = responseBody.byteStream(),
            onProgress = { lineCount ->
                val progressValue = (0.2f + 0.6f * (lineCount.toFloat() / maxOf(lineCount, 1))).coerceAtMost(0.8f)
                _syncProgress.value = SyncProgress(
                    phase = "Parsing... ($totalItems items)",
                    progress = progressValue,
                    processedItems = totalItems,
                    isActive = true,
                )
            },
            chunkSize = 500,
            onChunk = { items ->
                totalItems += items.size

                // Ensure categories exist
                for (item in items) {
                    if (item.groupTitle !in groupToCategoryId) {
                        val categoryId = categoryDao.insert(
                            CategoryEntity(
                                playlistId = playlist.id,
                                remoteId = item.groupTitle.hashCode().toString(),
                                name = item.groupTitle,
                                categoryType = item.contentType.value,
                            )
                        )
                        groupToCategoryId[item.groupTitle] = categoryId
                    }
                }

                // Batch insert by content type
                val liveItems = items.filter { it.contentType == ContentType.LIVE }
                val vodItems = items.filter { it.contentType == ContentType.VOD }

                if (liveItems.isNotEmpty()) {
                    channelDao.insertAll(liveItems.map { item ->
                        ChannelEntity(
                            playlistId = playlist.id,
                            categoryId = groupToCategoryId[item.groupTitle] ?: 0,
                            remoteId = item.streamUrl.hashCode().toString(),
                            name = item.name,
                            streamUrl = item.streamUrl,
                            logoUrl = item.tvgLogo,
                            tvgId = item.tvgId,
                            tvgName = item.tvgName,
                            catchupType = item.catchupType,
                            catchupSource = item.catchupSource,
                            catchupDays = item.catchupDays,
                            userAgent = item.userAgent,
                        )
                    })
                }

                if (vodItems.isNotEmpty()) {
                    vodDao.insertAll(vodItems.map { item ->
                        VodEntity(
                            playlistId = playlist.id,
                            categoryId = groupToCategoryId[item.groupTitle] ?: 0,
                            remoteId = item.streamUrl.hashCode().toString(),
                            name = item.name,
                            streamUrl = item.streamUrl,
                            logoUrl = item.tvgLogo,
                        )
                    })
                }
            }
        )

        _syncProgress.value = SyncProgress.active("Finalizing ($totalItems items)...", 0.95f)
    }

    // --- EPG Sync ---

    override suspend fun syncEpg(playlistId: Long) {
        val playlist = playlistDao.getById(playlistId)
            ?: throw IllegalArgumentException("Playlist $playlistId not found")

        val epgUrl = playlist.epgUrl ?: return

        try {
            _syncProgress.value = SyncProgress.active("Downloading EPG...", 0.05f)
            val responseBody = xtreamApi.fetchRawUrl(epgUrl)

            _syncProgress.value = SyncProgress.active("Parsing EPG...", 0.1f)
            epgProgramDao.deleteByPlaylist(playlistId)

            var totalPrograms = 0
            xmltvParser.parse(
                inputStream = responseBody.byteStream(),
                playlistId = playlistId,
                batchSize = 500,
                onBatch = { batch ->
                    epgProgramDao.insertBatch(batch)
                },
                onProgress = { count ->
                    totalPrograms = count
                    val progress = 0.1f + 0.85f * (count.toFloat() / maxOf(count, 1))
                    _syncProgress.value = SyncProgress(
                        phase = "EPG: $count programs...",
                        progress = progress.coerceAtMost(0.95f),
                        processedItems = count,
                        isActive = true,
                    )
                }
            )

            playlistDao.updateEpgLastSynced(playlistId, System.currentTimeMillis())
            _syncProgress.value = SyncProgress.completed()
            Timber.i("EPG sync complete: $totalPrograms programs")
        } catch (e: CancellationException) {
            _syncProgress.value = SyncProgress.Idle
            throw e
        } catch (e: Exception) {
            Timber.e(e, "EPG sync failed")
            _syncProgress.value = SyncProgress.failed(e.localizedMessage ?: "EPG sync failed")
        }
    }

    override suspend fun syncCategoriesOnly(playlistId: Long) {
        val playlist = playlistDao.getById(playlistId)
            ?: throw IllegalArgumentException("Playlist $playlistId not found")

        cancelSync()

        coroutineScope {
            syncJob = launch {
                try {
                    _syncProgress.value = SyncProgress.indeterminate("Connecting...")
                    when (PlaylistType.fromValue(playlist.type)) {
                        PlaylistType.XTREAM -> syncXtreamCategoriesOnly(playlist)
                        PlaylistType.M3U -> syncM3u(playlist) // M3U needs full parse
                    }
                } catch (e: CancellationException) {
                    _syncProgress.value = SyncProgress.Idle
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Category sync failed for playlist $playlistId")
                    _syncProgress.value = SyncProgress.failed(
                        e.localizedMessage ?: "Category sync failed"
                    )
                    throw e
                }
            }
        }
    }

    override suspend fun syncSelectedStreams(playlistId: Long) {
        val playlist = playlistDao.getById(playlistId)
            ?: throw IllegalArgumentException("Playlist $playlistId not found")

        cancelSync()

        coroutineScope {
            syncJob = launch {
                try {
                    _syncProgress.value = SyncProgress.indeterminate("Preparing sync...")
                    when (PlaylistType.fromValue(playlist.type)) {
                        PlaylistType.XTREAM -> syncXtreamSelectedStreams(playlist)
                        PlaylistType.M3U -> { /* Already synced during categoriesOnly */ }
                    }
                    playlistDao.updateLastSynced(playlistId, System.currentTimeMillis())
                    _syncProgress.value = SyncProgress.completed()
                } catch (e: CancellationException) {
                    _syncProgress.value = SyncProgress.Idle
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Stream sync failed for playlist $playlistId")
                    _syncProgress.value = SyncProgress.failed(
                        e.localizedMessage ?: "Stream sync failed"
                    )
                }
            }
        }
    }

    override fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
        _syncProgress.value = SyncProgress.Idle
    }

    // --- Extension functions for DTO → Entity mapping ---

    private fun XtreamStreamDto.toChannelEntity(
        playlist: PlaylistEntity,
        categoryId: Long,
        serverUrl: String,
        username: String,
        password: String,
    ): ChannelEntity? {
        val id = streamId ?: return null
        val ext = containerExtension ?: "ts"
        val url = "$serverUrl/live/$username/$password/$id.$ext"
        return ChannelEntity(
            playlistId = playlist.id,
            categoryId = categoryId,
            remoteId = id.toString(),
            name = name ?: "Unknown",
            streamUrl = url,
            logoUrl = streamIcon,
            tvgId = epgChannelId,
            containerExtension = ext,
            catchupType = if (tvArchive == 1) "default" else null,
            catchupDays = tvArchiveDuration,
        )
    }

    private fun XtreamStreamDto.toVodEntity(
        playlist: PlaylistEntity,
        categoryId: Long,
        serverUrl: String,
        username: String,
        password: String,
    ): VodEntity? {
        val id = streamId ?: return null
        val ext = containerExtension ?: "mp4"
        val url = "$serverUrl/movie/$username/$password/$id.$ext"
        return VodEntity(
            playlistId = playlist.id,
            categoryId = categoryId,
            remoteId = id.toString(),
            name = name ?: "Unknown",
            streamUrl = url,
            containerExtension = ext,
            logoUrl = streamIcon,
            plot = plot,
            cast = cast,
            director = director,
            genre = genre,
            releaseDate = releaseDate,
            rating = rating?.toFloatOrNull(),
            tmdbId = tmdb?.toIntOrNull(),
        )
    }

    private fun XtreamStreamDto.toSeriesEntity(
        playlist: PlaylistEntity,
        categoryId: Long,
    ): SeriesEntity? {
        val id = seriesId ?: return null
        return SeriesEntity(
            playlistId = playlist.id,
            categoryId = categoryId,
            remoteId = id.toString(),
            name = name ?: "Unknown",
            coverUrl = cover ?: streamIcon,
            plot = plot,
            cast = cast,
            genre = genre,
            releaseDate = releaseDate,
            rating = rating?.toFloatOrNull(),
            lastModified = lastModified?.toLongOrNull(),
        )
    }
}
