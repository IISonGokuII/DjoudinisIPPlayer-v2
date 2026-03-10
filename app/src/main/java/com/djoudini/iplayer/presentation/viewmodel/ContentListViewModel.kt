package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.CategoryDao
import com.djoudini.iplayer.data.local.dao.SeriesDao
import com.djoudini.iplayer.data.local.dao.VodDao
import com.djoudini.iplayer.data.local.entity.CategoryEntity
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.data.local.entity.SeriesEntity
import com.djoudini.iplayer.data.local.entity.VodEntity
import com.djoudini.iplayer.domain.repository.ChannelRepository
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import com.djoudini.iplayer.presentation.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.Immutable

@Immutable
data class SearchResult(
    val id: Long,
    val name: String,
    val logoUrl: String?,
    val contentType: String,
)

enum class ViewMode { LIST, GRID, LARGE_GRID }

enum class SortMode(val label: String) {
    NAME_ASC("A-Z"),
    NAME_DESC("Z-A"),
    RECENTLY_ADDED("Newest"),
}

@HiltViewModel
class ContentListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val channelRepository: ChannelRepository,
    private val categoryDao: CategoryDao,
    private val vodDao: VodDao,
    private val seriesDao: SeriesDao,
) : ViewModel() {

    private val categoryId: Long = savedStateHandle.get<Long>(NavArgs.CATEGORY_ID) ?: 0L

    // --- View Mode ---
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.NAME_ASC)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    fun cycleSortMode() {
        _sortMode.value = when (_sortMode.value) {
            SortMode.NAME_ASC -> SortMode.NAME_DESC
            SortMode.NAME_DESC -> SortMode.RECENTLY_ADDED
            SortMode.RECENTLY_ADDED -> SortMode.NAME_ASC
        }
    }

    fun cycleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.LIST -> ViewMode.GRID
            ViewMode.GRID -> ViewMode.LARGE_GRID
            ViewMode.LARGE_GRID -> ViewMode.LIST
        }
    }

    // --- Dynamic category selection for split-pane ---
    private val _selectedCategoryId = MutableStateFlow(categoryId)
    val selectedCategoryId: StateFlow<Long> = _selectedCategoryId.asStateFlow()

    fun selectCategory(id: Long) {
        _selectedCategoryId.value = id
        // Clear inline search when switching categories
        _inlineSearchQuery.value = ""
    }

    // --- Categories by type ---

    val liveCategories: StateFlow<List<CategoryEntity>> =
        playlistRepository.observeActive()
            .flatMapLatest { playlist ->
                playlist?.let { categoryDao.observeByType(it.id, "live") } ?: flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val vodCategories: StateFlow<List<CategoryEntity>> =
        playlistRepository.observeActive()
            .flatMapLatest { playlist ->
                playlist?.let { categoryDao.observeByType(it.id, "vod") } ?: flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val seriesCategories: StateFlow<List<CategoryEntity>> =
        playlistRepository.observeActive()
            .flatMapLatest { playlist ->
                playlist?.let { categoryDao.observeByType(it.id, "series") } ?: flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Items within a selected category (reactive to _selectedCategoryId) ---

    val channels: StateFlow<List<ChannelEntity>> =
        _selectedCategoryId.flatMapLatest { catId ->
            if (catId == 0L) flowOf(emptyList())
            else channelRepository.observeByCategory(catId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val vodItems: StateFlow<List<VodEntity>> =
        _selectedCategoryId.flatMapLatest { catId ->
            if (catId == 0L) flowOf(emptyList())
            else vodDao.observeByCategory(catId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val seriesItems: StateFlow<List<SeriesEntity>> =
        _selectedCategoryId.flatMapLatest { catId ->
            if (catId == 0L) flowOf(emptyList())
            else seriesDao.observeByCategory(catId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Inline search (per-section, scoped to current content type) ---

    private val _inlineSearchQuery = MutableStateFlow("")
    val inlineSearchQuery: StateFlow<String> = _inlineSearchQuery.asStateFlow()

    fun updateInlineSearch(query: String) {
        _inlineSearchQuery.value = query
    }

    val filteredChannels: StateFlow<List<ChannelEntity>> =
        combine(channels, _inlineSearchQuery, _sortMode) { items, query, sort ->
            val filtered = if (query.length < 2) items
            else items.filter { it.name.contains(query, ignoreCase = true) }
            when (sort) {
                SortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
                SortMode.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
                SortMode.RECENTLY_ADDED -> filtered.sortedByDescending { it.id }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredVodItems: StateFlow<List<VodEntity>> =
        combine(vodItems, _inlineSearchQuery, _sortMode) { items, query, sort ->
            val filtered = if (query.length < 2) items
            else items.filter { it.name.contains(query, ignoreCase = true) }
            when (sort) {
                SortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
                SortMode.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
                SortMode.RECENTLY_ADDED -> filtered.sortedByDescending { it.id }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredSeriesItems: StateFlow<List<SeriesEntity>> =
        combine(seriesItems, _inlineSearchQuery, _sortMode) { items, query, sort ->
            val filtered = if (query.length < 2) items
            else items.filter { it.name.contains(query, ignoreCase = true) }
            when (sort) {
                SortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
                SortMode.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
                SortMode.RECENTLY_ADDED -> filtered.sortedByDescending { it.id }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Global Search ---

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<SearchResult>> =
        _searchQuery
            .debounce(300) // FIX: Wait 300ms after user stops typing before searching
            .flatMapLatest { query ->
                if (query.length < 2) {
                    flowOf(emptyList())
                } else {
                playlistRepository.observeActive().flatMapLatest { playlist ->
                    if (playlist == null) {
                        flowOf(emptyList())
                    } else {
                        combine(
                            channelRepository.search(playlist.id, query),
                            vodDao.search(playlist.id, query),
                            seriesDao.search(playlist.id, query),
                        ) { channels, vods, series ->
                            val channelResults = channels.map { ch ->
                                SearchResult(ch.id, ch.name, ch.logoUrl, "channel")
                            }
                            val vodResults = vods.map { v ->
                                SearchResult(v.id, v.name, v.logoUrl, "vod")
                            }
                            val seriesResults = series.map { s ->
                                SearchResult(s.id, s.name, s.coverUrl, "series")
                            }
                            channelResults + vodResults + seriesResults
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Favorites ---

    fun toggleFavorite(channelId: Long, currentFavorite: Boolean) {
        viewModelScope.launch {
            channelRepository.setFavorite(channelId, !currentFavorite)
        }
    }
}
