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
import com.djoudini.iplayer.data.remote.dto.XtreamCategoryDto
import com.djoudini.iplayer.data.remote.dto.XtreamStreamDto
import com.djoudini.iplayer.domain.model.ContentType
import com.djoudini.iplayer.domain.model.PlaylistType
import com.djoudini.iplayer.domain.model.SyncProgress
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.util.EpgChannelIdNormalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
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

        runSyncJob(
            initialPhase = "Connecting...",
            onErrorMessage = "Sync failed",
            logLabel = "Sync failed for playlist $playlistId",
        ) {
            when (PlaylistType.fromValue(playlist.type)) {
                PlaylistType.XTREAM -> syncXtream(playlist)
                PlaylistType.M3U -> syncM3u(playlist)
            }

            playlistDao.updateLastSynced(playlistId, System.currentTimeMillis())
            _syncProgress.value = SyncProgress.completed()
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

        // Always refresh the Xtream XMLTV endpoint so EPG follows current credentials.
        val epgUrl = "$serverUrl/xmltv.php?username=$username&password=$password"
        if (playlist.epgUrl != epgUrl) {
            playlistDao.updateEpgUrl(playlist.id, epgUrl)
        }

        _syncProgress.value = SyncProgress.active("Loading categories...", 0.3f)
        
        // Helper function for retrying API calls
        suspend fun <T> retryApiCall(block: suspend () -> T): T? {
            var currentDelay = 1000L
            repeat(3) { attempt ->
                try {
                    return block()
                } catch (e: Exception) {
                    Timber.w(e, "API call failed on attempt ${attempt + 1}")
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(currentDelay)
                        currentDelay *= 2 // Exponential backoff
                    } else {
                        Timber.e(e, "API call failed after 3 attempts")
                    }
                }
            }
            return null
        }

        val liveCategories = retryApiCall { xtreamApi.getLiveCategories(apiUrl, username, password) } ?: emptyList()
        val vodCategories = retryApiCall { xtreamApi.getVodCategories(apiUrl, username, password) } ?: emptyList()
        val seriesCategories = retryApiCall { xtreamApi.getSeriesCategories(apiUrl, username, password) } ?: emptyList()

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

        // Batch insert for better performance - uses single transaction
        if (allCategoryEntities.isNotEmpty()) {
            categoryDao.insertAll(allCategoryEntities)
        }

        _syncProgress.value = SyncProgress.completed()
    }

    /**
     * Phase 2: Sync only streams for user-selected categories.
     * OPTIMIERUNG: Parallele API-Aufrufe für alle Kategorien statt sequentiell.
     */
    private suspend fun syncXtreamSelectedStreams(playlist: PlaylistEntity) = coroutineScope {
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
        Timber.d("[Sync] Loaded ${allCategories.size} categories from DB for playlist ${playlist.id}")

        for (cat in allCategories) {
            categoryIdMap[categoryKey(cat.categoryType, cat.remoteId)] = cat.id
            if (cat.isSelected) {
                selectedRemoteIds[cat.categoryType]?.add(cat.remoteId)
            }
        }

        Timber.d("[Sync] Selected categories: Live=${selectedRemoteIds[ContentType.LIVE.value]?.size}, Vod=${selectedRemoteIds[ContentType.VOD.value]?.size}, Series=${selectedRemoteIds[ContentType.SERIES.value]?.size}")

        val selectedLive = selectedRemoteIds[ContentType.LIVE.value] ?: emptySet()
        val selectedVod = selectedRemoteIds[ContentType.VOD.value] ?: emptySet()
        val selectedSeries = selectedRemoteIds[ContentType.SERIES.value] ?: emptySet()

        // Validate that at least one category is selected
        if (selectedLive.isEmpty() && selectedVod.isEmpty() && selectedSeries.isEmpty()) {
            Timber.w("[Sync] No categories selected for playlist ${playlist.id}, skipping stream sync")
            throw IllegalStateException("No categories selected. Please select at least one category to sync.")
        }

        // OPTIMIERUNG: Parallele Fetching-Jobs für alle Content-Typen
        val liveJob = if (selectedLive.isNotEmpty()) async {
            fetchLiveChannels(playlist, selectedLive, categoryIdMap, serverUrl, username, password, ContentType.LIVE.value)
        } else null

        val vodJob = if (selectedVod.isNotEmpty()) async {
            fetchVodItems(playlist, selectedVod, categoryIdMap, serverUrl, username, password, ContentType.VOD.value)
        } else null

        val seriesJob = if (selectedSeries.isNotEmpty()) async {
            fetchSeriesItems(playlist, selectedSeries, categoryIdMap, apiUrl, username, password, ContentType.SERIES.value)
        } else null

        // Warte auf alle Jobs und sammle Ergebnisse
        _syncProgress.value = SyncProgress.active("Loading content...", 0.5f)
        
        val liveChannels = liveJob?.await() ?: emptyList()
        val vodItems = vodJob?.await() ?: emptyList()
        val seriesItems = seriesJob?.await() ?: emptyList()

        // Batch-Insert aller Daten in Transaktion
        _syncProgress.value = SyncProgress.active("Saving to database...", 0.9f)
        
        if (liveChannels.isNotEmpty()) {
            channelDao.insertAll(liveChannels)
            Timber.d("[Sync] Inserted ${liveChannels.size} channels into DB")
        } else {
            Timber.d("[Sync] No live channels to insert")
        }

        if (vodItems.isNotEmpty()) {
            vodDao.insertAll(vodItems)
            Timber.d("[Sync] Inserted ${vodItems.size} VOD items into DB")
        } else {
            Timber.d("[Sync] No VOD items to insert")
        }

        if (seriesItems.isNotEmpty()) {
            seriesDao.insertAll(seriesItems)
            Timber.d("[Sync] Inserted ${seriesItems.size} series into DB")
        } else {
            Timber.d("[Sync] No series items to insert")
        }

        _syncProgress.value = SyncProgress.active("Finalizing...", 0.95f)
    }

    /**
     * OPTIMIERUNG: Paralleles Fetching für Live-Kanäle mit koroutinen-basiertem Batch-Processing
     */
    private suspend fun fetchLiveChannels(
        playlist: PlaylistEntity,
        selectedRemoteIds: Set<String>,
        categoryIdMap: Map<String, Long>,
        serverUrl: String,
        username: String,
        password: String,
        contentType: String
    ): List<ChannelEntity> = coroutineScope {
        val channelEntities = mutableListOf<ChannelEntity>()
        val apiUrl = "$serverUrl/player_api.php"
        
        fun categoryKey(type: String, remoteId: String) = "$type:$remoteId"
        
        // Parallele API-Aufrufe für alle Kategorien
        val deferredResults = selectedRemoteIds.map { remoteId ->
            async {
                try {
                    val streams = xtreamApi.getLiveStreams(apiUrl, username, password, categoryId = remoteId)
                    val catKey = categoryKey(contentType, remoteId)
                    val catId = categoryIdMap[catKey]
                    if (catId == null) {
                        Timber.w("[Sync] Live category not found for remoteId=$remoteId")
                        emptyList()
                    } else {
                        streams.mapNotNull { dto ->
                            dto.toChannelEntity(playlist, catId, serverUrl, username, password)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch live category $remoteId")
                    emptyList<ChannelEntity>()
                }
            }
        }
        
        // Sammle alle Ergebnisse
        deferredResults.forEach { deferred ->
            channelEntities.addAll(deferred.await())
        }
        
        channelEntities
    }

    /**
     * OPTIMIERUNG: Paralleles Fetching für VOD-Items
     */
    private suspend fun fetchVodItems(
        playlist: PlaylistEntity,
        selectedRemoteIds: Set<String>,
        categoryIdMap: Map<String, Long>,
        serverUrl: String,
        username: String,
        password: String,
        contentType: String
    ): List<VodEntity> = coroutineScope {
        val vodEntities = mutableListOf<VodEntity>()
        val apiUrl = "$serverUrl/player_api.php"
        
        fun categoryKey(type: String, remoteId: String) = "$type:$remoteId"
        
        // Parallele API-Aufrufe für alle Kategorien
        val deferredResults = selectedRemoteIds.map { remoteId ->
            async {
                try {
                    val streams = xtreamApi.getVodStreams(apiUrl, username, password, categoryId = remoteId)
                    val catKey = categoryKey(contentType, remoteId)
                    val catId = categoryIdMap[catKey]
                    if (catId == null) {
                        Timber.w("[Sync] VOD category not found for remoteId=$remoteId")
                        emptyList()
                    } else {
                        streams.mapNotNull { dto ->
                            dto.toVodEntity(playlist, catId, serverUrl, username, password)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch VOD category $remoteId")
                    emptyList<VodEntity>()
                }
            }
        }
        
        // Sammle alle Ergebnisse
        deferredResults.forEach { deferred ->
            vodEntities.addAll(deferred.await())
        }
        
        vodEntities
    }

    /**
     * OPTIMIERUNG: Paralleles Fetching für Series-Items
     */
    private suspend fun fetchSeriesItems(
        playlist: PlaylistEntity,
        selectedRemoteIds: Set<String>,
        categoryIdMap: Map<String, Long>,
        apiUrl: String,
        username: String,
        password: String,
        contentType: String
    ): List<SeriesEntity> = coroutineScope {
        val seriesEntities = mutableListOf<SeriesEntity>()
        
        fun categoryKey(type: String, remoteId: String) = "$type:$remoteId"
        
        // Parallele API-Aufrufe für alle Kategorien
        val deferredResults = selectedRemoteIds.map { remoteId ->
            async {
                try {
                    val streams = xtreamApi.getSeries(apiUrl, username, password, categoryId = remoteId)
                    val catKey = categoryKey(contentType, remoteId)
                    val catId = categoryIdMap[catKey]
                    if (catId == null) {
                        Timber.w("[Sync] Series category not found for remoteId=$remoteId")
                        emptyList()
                    } else {
                        streams.mapNotNull { dto ->
                            dto.toSeriesEntity(playlist, catId)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch series category $remoteId")
                    emptyList<SeriesEntity>()
                }
            }
        }
        
        // Sammle alle Ergebnisse
        deferredResults.forEach { deferred ->
            seriesEntities.addAll(deferred.await())
        }
        
        seriesEntities
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

        result.epgUrl?.let { detectedEpgUrl ->
            if (playlist.epgUrl != detectedEpgUrl) {
                playlistDao.updateEpgUrl(playlist.id, detectedEpgUrl)
            }
        }

        _syncProgress.value = SyncProgress.active("Finalizing ($totalItems items)...", 0.95f)
    }

    // --- EPG Sync ---

    override suspend fun syncEpg(playlistId: Long) {
        val playlist = playlistDao.getById(playlistId)
            ?: throw IllegalArgumentException("Playlist $playlistId not found")

        val epgUrl = playlist.epgUrl
        
        // Log for debugging
        Timber.d("[EPG Sync] Starting EPG sync for playlist $playlistId")
        Timber.d("[EPG Sync] EPG URL: $epgUrl")

        if (epgUrl.isNullOrBlank()) {
            Timber.e("[EPG Sync] No EPG URL configured for playlist $playlistId")
            _syncProgress.value = SyncProgress.failed("Keine EPG-URL konfiguriert. Bitte Xtream-Login erneut durchführen.")
            return
        }

        try {
            _syncProgress.value = SyncProgress.active("Downloading EPG...", 0.05f)
            Timber.d("[EPG Sync] Downloading EPG from: $epgUrl")
            
            val responseBody = xtreamApi.fetchRawUrl(epgUrl)
            Timber.d("[EPG Sync] Download complete, size: ${responseBody.byteStream().available()} bytes")

            _syncProgress.value = SyncProgress.active("Parsing EPG...", 0.1f)
            epgProgramDao.deleteByPlaylist(playlistId)

            var totalPrograms = 0
            xmltvParser.parse(
                inputStream = responseBody.byteStream(),
                playlistId = playlistId,
                batchSize = 500,
                onBatch = { batch ->
                    epgProgramDao.insertBatch(batch)
                    Timber.d("[EPG Sync] Inserted batch of ${batch.size} programs")
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

            val insertedPrograms = epgProgramDao.countByPlaylist(playlistId)
            if (insertedPrograms == 0) {
                _syncProgress.value = SyncProgress.failed(
                    "EPG geladen, aber keine passenden Programmdaten gefunden. Bitte tvg-id/EPG-Zuordnung pruefen.",
                )
                Timber.w("[EPG Sync] No programs inserted for playlist $playlistId")
                return
            }

            playlistDao.updateEpgLastSynced(playlistId, System.currentTimeMillis())
            _syncProgress.value = SyncProgress.completed()
            Timber.i("[EPG Sync] Complete: $insertedPrograms programs for playlist $playlistId")
        } catch (e: CancellationException) {
            Timber.w("[EPG Sync] Cancelled by user")
            _syncProgress.value = SyncProgress.Idle
            throw e
        } catch (e: Exception) {
            Timber.e(e, "[EPG Sync] Failed for playlist $playlistId")
            _syncProgress.value = SyncProgress.failed("EPG Sync fehlgeschlagen: ${e.localizedMessage ?: "Unbekannter Fehler"}")
            // Don't re-throw - allow partial EPG sync
        }
    }

    override suspend fun syncCategoriesOnly(playlistId: Long) {
        val playlist = playlistDao.getById(playlistId)
            ?: throw IllegalArgumentException("Playlist $playlistId not found")

        runSyncJob(
            initialPhase = "Connecting...",
            onErrorMessage = "Category sync failed",
            logLabel = "Category sync failed for playlist $playlistId",
            rethrowOnError = true,
        ) {
            when (PlaylistType.fromValue(playlist.type)) {
                PlaylistType.XTREAM -> syncXtreamCategoriesOnly(playlist)
                PlaylistType.M3U -> syncM3u(playlist)
            }
        }
    }

    override suspend fun syncSelectedStreams(playlistId: Long) {
        val playlist = playlistDao.getById(playlistId)
            ?: throw IllegalArgumentException("Playlist $playlistId not found")

        runSyncJob(
            initialPhase = "Preparing sync...",
            onErrorMessage = "Stream sync failed",
            logLabel = "Stream sync failed for playlist $playlistId",
        ) {
            Timber.d("[Sync] Starting stream sync for playlist $playlistId (type=${playlist.type})")
            when (PlaylistType.fromValue(playlist.type)) {
                PlaylistType.XTREAM -> syncXtreamSelectedStreams(playlist)
                PlaylistType.M3U -> {
                    Timber.d("[Sync] M3U playlist - streams already synced during categories")
                }
            }
            playlistDao.updateLastSynced(playlistId, System.currentTimeMillis())
            _syncProgress.value = SyncProgress.completed()
            Timber.d("[Sync] Stream sync completed successfully for playlist $playlistId")
        }
    }

    override fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
        _syncProgress.value = SyncProgress.Idle
    }

    private suspend fun runSyncJob(
        initialPhase: String,
        onErrorMessage: String,
        logLabel: String,
        rethrowOnError: Boolean = false,
        block: suspend () -> Unit,
    ) {
        syncJob?.cancelAndJoin()
        syncJob = null

        coroutineScope {
            syncJob = launch {
                try {
                    _syncProgress.value = SyncProgress.indeterminate(initialPhase)
                    block()
                } catch (e: CancellationException) {
                    _syncProgress.value = SyncProgress.Idle
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, logLabel)
                    _syncProgress.value = SyncProgress.failed(
                        e.localizedMessage ?: onErrorMessage,
                    )
                    if (rethrowOnError) throw e
                }
            }
        }
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
            tvgId = EpgChannelIdNormalizer.normalize(epgChannelId),
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
