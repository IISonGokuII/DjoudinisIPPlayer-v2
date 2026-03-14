package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.CategoryDao
import com.djoudini.iplayer.data.local.dao.ChannelDao
import com.djoudini.iplayer.data.local.entity.CategoryEntity
import com.djoudini.iplayer.data.local.entity.ChannelEntity
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LiveTvManagementViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val categoryDao: CategoryDao,
    private val channelDao: ChannelDao,
) : ViewModel() {

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> =
        playlistRepository.observeActive()
            .flatMapLatest { playlist ->
                playlist?.let { categoryDao.observeAllByType(it.id, "live") } ?: flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val channels: StateFlow<List<ChannelEntity>> =
        selectedCategoryId.flatMapLatest { categoryId ->
            categoryId?.let { channelDao.observeByCategory(it) } ?: flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            categories.collect { categoryList ->
                val current = _selectedCategoryId.value
                val stillExists = current != null && categoryList.any { it.id == current }
                if (!stillExists) {
                    _selectedCategoryId.value = categoryList.firstOrNull()?.id
                }
            }
        }
    }

    fun selectCategory(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    fun toggleFavorite(channelId: Long, currentFavorite: Boolean) {
        viewModelScope.launch {
            channelDao.setFavorite(channelId, !currentFavorite)
        }
    }

    fun toggleCategorySelected(categoryId: Long, selected: Boolean) {
        viewModelScope.launch {
            categoryDao.setSelected(categoryId, selected)
        }
    }

    fun moveCategoryUp(categoryId: Long) {
        reorderCategories(categoryId, -1)
    }

    fun moveCategoryDown(categoryId: Long) {
        reorderCategories(categoryId, 1)
    }

    fun moveChannelUp(channelId: Long) {
        reorderChannels(channelId, -1)
    }

    fun moveChannelDown(channelId: Long) {
        reorderChannels(channelId, 1)
    }

    private fun reorderCategories(categoryId: Long, delta: Int) {
        viewModelScope.launch {
            val currentCategories = categories.value
            val index = currentCategories.indexOfFirst { it.id == categoryId }
            if (index == -1) return@launch

            val targetIndex = (index + delta).coerceIn(0, currentCategories.lastIndex)
            if (targetIndex == index) return@launch

            val reordered = currentCategories.toMutableList().apply {
                add(targetIndex, removeAt(index))
            }.mapIndexed { newIndex, category ->
                category.copy(sortOrder = newIndex)
            }
            categoryDao.updateAll(reordered)
        }
    }

    private fun reorderChannels(channelId: Long, delta: Int) {
        viewModelScope.launch {
            val currentChannels = channels.value
            val index = currentChannels.indexOfFirst { it.id == channelId }
            if (index == -1) return@launch

            val targetIndex = (index + delta).coerceIn(0, currentChannels.lastIndex)
            if (targetIndex == index) return@launch

            val reordered = currentChannels.toMutableList().apply {
                add(targetIndex, removeAt(index))
            }.mapIndexed { newIndex, channel ->
                channel.copy(sortOrder = newIndex)
            }
            channelDao.updateAll(reordered)
        }
    }
}
