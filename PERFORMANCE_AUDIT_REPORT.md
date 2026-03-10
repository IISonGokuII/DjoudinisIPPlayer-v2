# DjoudinisIPPlayer-v2 Android IPTV App - Comprehensive Performance Audit Report

**Audit Date:** 2026-03-10  
**App Version:** 1.0.0  
**Total Files Analyzed:** 60+ Kotlin files

---

## EXECUTIVE SUMMARY

| Severity | Count | Categories |
|----------|-------|------------|
| 🔴 **Critical** | 3 | Memory leaks, Database issues, Race conditions |
| 🟠 **High** | 8 | Performance, Threading, Resource leaks |
| 🟡 **Medium** | 12 | Optimization opportunities, Code quality |
| 🟢 **Low** | 6 | Best practices, Minor improvements |

---

## 🔴 CRITICAL ISSUES

### 1. Memory Leak in SeriesDetailViewModel - Unmanaged Coroutine Flow Collection
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/viewmodel/SeriesDetailViewModel.kt`  
**Lines:** 121-128, 130-138

```kotlin
// PROBLEMATIC CODE:
fun loadEpisodes(season: Int) {
    viewModelScope.launch {
        episodeDao.observeBySeason(seriesId, season).collect { episodes ->  // <- Never cancelled!
            _episodes.value = episodes
            loadEpisodeProgress(episodes.map { it.id })
        }
    }
}

private fun loadEpisodeProgress(episodeIds: List<Long>) {
    viewModelScope.launch {
        val playlist = playlistRepository.getActive() ?: return@launch
        watchProgressDao.observeEpisodeProgress(playlist.id, episodeIds).collect { ... }  // <- Never cancelled!
    }
}
```

**Issue:** Flow collection started in `loadEpisodes()` and `loadEpisodeProgress()` is never cancelled. Each call starts a new collection that runs indefinitely, causing:
- Multiple concurrent collectors for the same data
- Memory leak as old collectors remain active
- Duplicate progress updates

**Fix:** Store job references and cancel previous collections:
```kotlin
private var episodesJob: Job? = null
private var progressJob: Job? = null

fun loadEpisodes(season: Int) {
    episodesJob?.cancel()
    episodesJob = viewModelScope.launch {
        episodeDao.observeBySeason(seriesId, season).collect { ... }
    }
}
```

---

### 2. Blocking Main Thread in PlaylistRepositoryImpl - Synchronous Category Insert
**File:** `app/src/main/java/com/djoudini/iplayer/data/repository/PlaylistRepositoryImpl.kt`  
**Lines:** 223-226

```kotlin
// PROBLEMATIC CODE:
for (entity in allCategoryEntities) {
    categoryDao.insert(entity)  // <- Sequential insert in loop, blocking coroutine
}
```

**Issue:** Categories are inserted one-by-one in a synchronous loop during the onboarding flow. With thousands of categories, this blocks the coroutine and causes UI jank.

**Fix:** Use batch insert:
```kotlin
categoryDao.insertAll(allCategoryEntities)  // Single batch transaction
```

---

### 3. Potential Race Condition in Sync Operations
**File:** `app/src/main/java/com/djoudini/iplayer/data/repository/PlaylistRepositoryImpl.kt`  
**Lines:** 112-141, 496-521, 524-551

```kotlin
// PROBLEMATIC PATTERN:
override suspend fun syncPlaylist(playlistId: Long) {
    cancelSync()  // <- Cancel may not complete before new sync starts
    coroutineScope {
        syncJob = launch { ... }  // <- New job launched immediately
    }
}
```

**Issue:** `cancelSync()` is called but there's no guarantee the previous job completes cancellation before the new sync starts. This can lead to:
- Multiple concurrent syncs modifying database simultaneously
- Corrupted sync progress state
- Database constraint violations

**Fix:** Use `syncJob?.join()` or atomic job management:
```kotlin
override suspend fun syncPlaylist(playlistId: Long) {
    syncJob?.cancelAndJoin()  // Wait for cancellation to complete
    syncJob = coroutineScope {
        launch { ... }
    }
}
```

---

## 🟠 HIGH SEVERITY ISSUES

### 4. N+1 Query Problem in EpgViewModel
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/viewmodel/EpgViewModel.kt`  
**Lines:** 50-63

```kotlin
// PROBLEMATIC CODE:
val epgList = channels.mapNotNull { channel ->
    val programs = epgRepository.getProgramsForRange(  // <- N queries for N channels!
        channelId = channel.tvgId ?: return@mapNotNull null,
        fromTime = now - 2 * 60 * 60 * 1000,
        toTime = endTime,
    )
    ...
}
```

**Issue:** For each of the 100 channels, a separate database query is executed (N+1 pattern). This results in 101 total queries instead of 1-2.

**Impact:** Significant delay loading EPG grid, especially on slower devices.

**Fix:** Create a batch query DAO method:
```kotlin
@Query("SELECT * FROM epg_programs WHERE epg_channel_id IN (:channelIds) AND ...")
suspend fun getProgramsForChannels(channelIds: List<String>, ...): List<EpgProgramEntity>
```

---

### 5. Missing Database Index on Episodes Series Lookup
**File:** `app/src/main/java/com/djoudini/iplayer/data/local/entity/EpisodeEntity.kt`  
**Lines:** 28-33

```kotlin
indices = [
    Index(value = ["series_id"]),
    Index(value = ["playlist_id"]),
    Index(value = ["series_id", "season_number", "episode_number"]),
    Index(value = ["playlist_id", "remote_id"], unique = true),
]
```

**Issue:** The composite index `("series_id", "season_number", "episode_number")` exists, but queries in `EpisodeDao.getNextEpisode()` use:
```kotlin
WHERE series_id = :seriesId 
AND ((season_number = :currentSeason AND episode_number > :currentEpisode) 
     OR season_number > :currentSeason)
```

The `OR` condition prevents the index from being used effectively.

**Fix:** Add a covering index or restructure query:
```kotlin
// Add index
Index(value = ["series_id", "season_number"])
Index(value = ["series_id", "episode_number"])
```

---

### 6. Inefficient LIKE Query Without Index Support
**File:** `app/src/main/java/com/djoudini/iplayer/data/local/dao/ChannelDao.kt`  
**Lines:** 32-33

```kotlin
@Query("SELECT * FROM channels WHERE playlist_id = :playlistId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
fun search(playlistId: Long, query: String): Flow<List<ChannelEntity>>
```

**Issue:** Leading wildcard (`%query`) prevents SQLite from using the index. Full table scan occurs on every search.

**Impact:** Search performance degrades linearly with channel count.

**Fix:** Implement FTS (Full-Text Search) or add a normalized search column:
```kotlin
// Add to ChannelEntity:
@ColumnInfo(name = "name_normalized")
val nameNormalized: String,  // lowercase, stripped

// Index it:
Index(value = ["name_normalized"])
```

---

### 7. Heavy Calculation in Compose - Sorting on Main Thread
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/viewmodel/ContentListViewModel.kt`  
**Lines:** 141-171

```kotlin
// PROBLEMATIC CODE:
val filteredChannels: StateFlow<List<ChannelEntity>> =
    combine(channels, _inlineSearchQuery, _sortMode) { items, query, sort ->
        val filtered = if (query.length < 2) items
        else items.filter { it.name.contains(query, ignoreCase = true) }
        when (sort) {
            SortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }  // <- Main thread sorting!
            SortMode.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            SortMode.RECENTLY_ADDED -> filtered.sortedByDescending { it.id }
        }
    }.stateIn(...)
```

**Issue:** Sorting and filtering large lists (1000+ items) happens on the main thread in the `combine` operator.

**Fix:** Move to background thread:
```kotlin
.combine(...) { items, query, sort ->
    withContext(Dispatchers.Default) {
        // sorting/filtering here
    }
}
```

---

### 8. Missing Coil Image Size Constraints
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/components/ContentCard.kt`  
**Lines:** 65-72, 147-155

```kotlin
AsyncImage(
    model = logoUrl,  // <- No size constraints on URL
    contentDescription = name,
    contentScale = ContentScale.Crop,
    modifier = Modifier.size(56.dp),
)
```

**Issue:** Coil loads full-resolution images from URLs, then scales them down. High-resolution logos (e.g., 1024x1024) consume excessive memory.

**Fix:** Add Coil size hints:
```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(logoUrl)
        .size(128)  // Request smaller image
        .build(),
    ...
)
```

---

### 9. Potential ANR in XmltvParser with Large EPG Files
**File:** `app/src/main/java/com/djoudini/iplayer/data/parser/XmltvParser.kt`  
**Lines:** 44-149

**Issue:** While the parser uses XmlPullParser (streaming), the `onBatch` callback performs database inserts that can accumulate:
- No yield point between batches
- With very large EPG files (>100MB), this can block the dispatcher

**Fix:** Add explicit yield:
```kotlin
if (batch.size >= batchSize) {
    onBatch(batch.toList())
    batch.clear()
    onProgress(totalCount)
    yield()  // Allow other coroutines to run
}
```

---

### 10. WorkManager Not Configured for Expedited Work
**File:** `app/src/main/java/com/djoudini/iplayer/data/worker/SyncScheduler.kt`  
**Lines:** 35-52, 57-74

**Issue:** Periodic sync work uses default priority. On Android 12+ with Doze mode, sync may be significantly delayed.

**Fix:** Consider expedited work for manual syncs:
```kotlin
// For immediate one-shot syncs:
OneTimeWorkRequestBuilder<PlaylistSyncWorker>()
    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
    .setConstraints(networkConstraints)
    .build()
```

---

### 11. DataStore Anti-Pattern: Collect Without Cancellation
**File:** `app/src/main/java/com/djoudini/iplayer/data/local/preferences/AppPreferences.kt`  
**Lines:** 126-132

```kotlin
suspend fun getTraktRefreshToken(): String? {
    var token: String? = null
    dataStore.data.collect { prefs ->  // <- Suspends indefinitely!
        token = prefs[TraktKeys.REFRESH_TOKEN]
    }
    return token
}
```

**Issue:** `collect` is a terminal operator that never returns. The function never completes.

**Fix:** Use `first()` instead:
```kotlin
suspend fun getTraktRefreshToken(): String? {
    return dataStore.data.first()[TraktKeys.REFRESH_TOKEN]
}
```

---

## 🟡 MEDIUM SEVERITY ISSUES

### 12. No Transaction Boundaries in Batch Operations
**File:** `app/src/main/java/com/djoudini/iplayer/data/repository/PlaylistRepositoryImpl.kt`  
**Multiple locations**

**Issue:** Multiple related DAO operations aren't wrapped in transactions:
```kotlin
seriesDao.deleteByPlaylist(playlist.id)  // Line 347
episodeDao.deleteByPlaylist(playlist.id)  // Line 348
```

If the app crashes between these operations, the database is left in an inconsistent state.

**Fix:** Use `@Transaction` annotated methods in DAO:
```kotlin
@Transaction
suspend fun deleteSeriesAndEpisodes(playlistId: Long) {
    seriesDao.deleteByPlaylist(playlistId)
    episodeDao.deleteByPlaylist(playlistId)
}
```

---

### 13. Redundant Database Query in PlayerViewModel
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/viewmodel/PlayerViewModel.kt`  
**Lines:** 131, 369, 451

```kotlin
// loadChannel(), playPreviousChannel(), playNextChannel() all call:
val channel = channelDao.getById(contentId)
```

**Issue:** The channel is queried multiple times unnecessarily when the data is already available in the UI state.

---

### 14. Inefficient Recent Channels Loading
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/viewmodel/PlayerViewModel.kt`  
**Lines:** 143-145

```kotlin
val recentChannels = channelDao.getRecentlyWatchedIds(playlistId, limit = 5)
    .filter { it != contentId }
```

**Issue:** Loads IDs then filters one out in memory. Better to exclude in SQL:
```kotlin
@Query("SELECT id FROM channels WHERE playlist_id = :playlistId AND last_watched_at IS NOT NULL AND id != :excludeId ...")
```

---

### 15. Missing ProGuard/R8 Rules for Moshi Models
**File:** `app/proguard-rules.pro` (not present in analysis)

**Issue:** No ProGuard rules visible for Moshi's Kotlin reflection adapter. This can cause serialization failures in release builds.

**Fix:** Add to `proguard-rules.pro`:
```proguard
-keep class com.djoudini.iplayer.data.remote.dto.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
```

---

### 16. Potential OOM in M3uParser with AllItems List
**File:** `app/src/main/java/com/djoudini/iplayer/data/parser/M3uParser.kt`  
**Lines:** 57, 82

```kotlin
val allItems = mutableListOf<M3uItem>()  // <- Grows unbounded
// ...
allItems.add(item)  // <- Every item stored
```

**Issue:** Despite chunking for database insertion, `allItems` still accumulates every parsed item in memory. With 100k+ items, this causes OOM.

**Fix:** Remove `allItems` if not needed, or use a size-bounded list:
```kotlin
// If result not needed:
// Remove allItems entirely

// If needed for result:
if (allItems.size > 10000) {
    // Process or clear oldest items
}
```

---

### 17. Unused Index on CategoryEntity
**File:** `app/src/main/java/com/djoudini/iplayer/data/local/entity/CategoryEntity.kt`  
**Lines:** 23-27

```kotlin
indices = [
    Index(value = ["playlist_id"]),  // <- Redundant with composite index below
    Index(value = ["playlist_id", "category_type"]),
    Index(value = ["playlist_id", "remote_id"], unique = true),
]
```

**Issue:** The single-column `playlist_id` index is redundant when composite indices starting with `playlist_id` exist.

---

### 18. No Debounce on Search Query
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/viewmodel/ContentListViewModel.kt`  
**Lines:** 175-210

```kotlin
val searchResults: StateFlow<List<SearchResult>> =
    _searchQuery.flatMapLatest { query ->
        // Query executed immediately on every keystroke
    }
```

**Issue:** Database search runs on every character typed, causing unnecessary load.

**Fix:** Add debounce:
```kotlin
_searchQuery
    .debounce(300)
    .flatMapLatest { query -> ... }
```

---

### 19. SettingsViewModel Uses Compose State Instead of StateFlow
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/viewmodel/SettingsViewModel.kt`  
**Lines:** 30-34

```kotlin
var preferredAudioLanguage by mutableStateOf("")
    private set
```

**Issue:** Mixing Compose state with StateFlow in ViewModels breaks the unidirectional data flow pattern and can cause lifecycle issues.

**Fix:** Use StateFlow consistently:
```kotlin
private val _preferredAudioLanguage = MutableStateFlow("")
val preferredAudioLanguage: StateFlow<String> = _preferredAudioLanguage.asStateFlow()
```

---

### 20. Duplicate Network Connection Pool Configuration
**File:** `app/src/main/java/com/djoudini/iplayer/di/NetworkModule.kt`  
**Lines:** 46

```kotlin
.connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
```

**Issue:** Default connection pool (5 idle, 5min keepalive) is sufficient for this app. Custom configuration increases memory usage without benefit.

---

### 21. PlayerScreen Recomposition on Every Position Update
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/ui/mobile/PlayerScreen.kt`  
**Lines:** 199-209

```kotlin
LaunchedEffect(exoPlayer) {
    while (true) {
        viewModel.updatePlaybackState(...)  // <- Triggers recomposition every second
        delay(1000)
    }
}
```

**Issue:** Updating playback position every second causes the entire PlayerScreen to recompose.

**Fix:** Use derived state or limit UI updates:
```kotlin
// In ViewModel:
private val _playbackPosition = MutableStateFlow(0L)
val playbackPosition: StateFlow<Long> = _playbackPosition
    .sample(5000)  // Only emit every 5 seconds for UI
    .stateIn(...)
```

---

### 22. Missing Error Handling in Image Loading
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/components/ContentCard.kt`  
**Lines:** 65-79

**Issue:** AsyncImage doesn't handle errors (404, timeout). Failed logo loads show blank space without fallback.

**Fix:** Add error placeholder:
```kotlin
AsyncImage(
    model = logoUrl,
    error = painterResource(R.drawable.ic_channel_placeholder),
    ...
)
```

---

### 23. Trakt API Key Hardcoded as Empty String
**File:** `app/src/main/java/com/djoudini/iplayer/di/NetworkModule.kt`  
**Lines:** 81

```kotlin
.header("trakt-api-key", "") // Set via BuildConfig in production
```

**Issue:** Empty API key will cause all Trakt operations to fail. Should fail fast with clear error.

---

## 🟢 LOW SEVERITY ISSUES

### 24. Modifier Ordering Not Optimized
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/components/FocusableCard.kt`  
**Lines:** 67-85

**Issue:** `.focusable()` is applied after `.shadow()`, causing unnecessary shadow recomputations on focus changes.

**Fix:** Reorder: `focusable()` before visual modifiers.

---

### 25. String Concatenation in SQL Queries
**File:** `app/src/main/java/com/djoudini/iplayer/data/local/dao/CategoryDao.kt`  
**Lines:** 23-27

**Issue:** String concatenation (`||`) in SQL is slightly less efficient than using SQLite's built-in functions.

---

### 26. Magic Numbers in Buffer Configuration
**File:** `app/src/main/java/com/djoudini/iplayer/data/local/entity/PlayerConfig.kt`  
**Lines:** 11-21

```kotlin
val minBufferMs: Int = 15_000
val maxBufferMs: Int = 60_000
```

**Issue:** Magic numbers without documentation. Should reference IPTV industry standards.

---

### 27. Date Format Object Created on Every Call
**File:** `app/src/main/java/com/djoudini/iplayer/presentation/ui/mobile/DashboardScreen.kt`  
**Lines:** 94-95, 376, 380, 384

```kotlin
val formatted = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    .format(Date(expDate))  // <- New SimpleDateFormat every call
```

**Issue:** `SimpleDateFormat` is expensive to create. Should use `remember` or static instances.

---

### 28. Unnecessary Unique Work Names for One-Shot Work
**File:** `app/src/main/java/com/djoudini/iplayer/data/worker/SyncScheduler.kt`  
**Lines:** 79-91

**Issue:** Using `enqueueUniqueWork` with `REPLACE` for manual sync means only one manual sync can run at a time. This is correct behavior but should be documented.

---

### 29. Missing @Stable/@Immutable Annotations
**File:** `app/src/main/java/com/djoudini/iplayer/domain/model/SyncProgress.kt`  
**Lines:** 7-36

**Issue:** Data classes used in Compose should be annotated with `@Stable` or `@Immutable` to help the compiler optimize recompositions.

---

## RECOMMENDATIONS SUMMARY

### Immediate Action Required (Critical + High)
1. Fix SeriesDetailViewModel coroutine leaks (Issue #1)
2. Fix DataStore collect hang (Issue #11)
3. Add batch insert for categories (Issue #2)
4. Fix sync race condition (Issue #3)
5. Implement FTS for search (Issue #6)

### Short-term Improvements (Medium)
1. Add debounce to search queries
2. Move sorting to background thread
3. Optimize Coil image requests
4. Add transaction boundaries
5. Fix N+1 queries in EPG

### Long-term Optimizations (Low)
1. Implement Compose stability annotations
2. Add caching for date formatting
3. Optimize modifier ordering
4. Review all database indices

---

## FILES REQUIRING MODIFICATION

| File | Issues | Priority |
|------|--------|----------|
| `SeriesDetailViewModel.kt` | #1, #13 | Critical |
| `PlaylistRepositoryImpl.kt` | #2, #3, #12 | Critical |
| `AppPreferences.kt` | #11 | Critical |
| `EpgViewModel.kt` | #4 | High |
| `ContentListViewModel.kt` | #7, #18 | High |
| `EpisodeEntity.kt` | #5 | High |
| `ChannelDao.kt` | #6 | High |
| `ContentCard.kt` | #8, #22 | High |
| `XmltvParser.kt` | #9 | High |
| `SyncScheduler.kt` | #10 | High |
| `M3uParser.kt` | #16 | Medium |
| `PlayerViewModel.kt` | #14, #15 | Medium |
| `SettingsViewModel.kt` | #19 | Medium |
| `PlayerScreen.kt` | #21 | Medium |
| `NetworkModule.kt` | #20, #23 | Medium |
| `DashboardScreen.kt` | #27 | Low |

---

## APPENDIX: Quick Fixes

### Fix #1: SeriesDetailViewModel
```kotlin
class SeriesDetailViewModel @Inject constructor(...) : ViewModel() {
    private var episodesJob: Job? = null
    private var progressJob: Job? = null

    fun loadEpisodes(season: Int) {
        episodesJob?.cancel()
        episodesJob = viewModelScope.launch {
            episodeDao.observeBySeason(seriesId, season).collect { episodes ->
                _episodes.value = episodes
                loadEpisodeProgress(episodes.map { it.id })
            }
        }
    }

    private fun loadEpisodeProgress(episodeIds: List<Long>) {
        progressJob?.cancel()
        if (episodeIds.isEmpty()) return
        progressJob = viewModelScope.launch {
            val playlist = playlistRepository.getActive() ?: return@launch
            watchProgressDao.observeEpisodeProgress(playlist.id, episodeIds).collect { ... }
        }
    }

    override fun onCleared() {
        super.onCleared()
        episodesJob?.cancel()
        progressJob?.cancel()
    }
}
```

### Fix #11: AppPreferences
```kotlin
suspend fun getTraktRefreshToken(): String? {
    return dataStore.data.first()[TraktKeys.REFRESH_TOKEN]
}
```

### Fix #6: Add FTS for Search
```kotlin
// Create FTS entity
@Entity(tableName = "channels_fts")
@Fts4(contentEntity = ChannelEntity::class)
data class ChannelEntityFts(
    val name: String
)

// Query
@Query("SELECT channels.* FROM channels JOIN channels_fts ON channels.id = channels_fts.rowid WHERE channels_fts MATCH :query")
fun searchFts(query: String): Flow<List<ChannelEntity>>
```

---

*End of Report*
