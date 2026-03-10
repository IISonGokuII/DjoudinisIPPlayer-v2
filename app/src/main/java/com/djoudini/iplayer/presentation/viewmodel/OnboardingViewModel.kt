package com.djoudini.iplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.dao.CategoryDao
import com.djoudini.iplayer.data.local.entity.CategoryEntity
import com.djoudini.iplayer.domain.model.SyncProgress
import com.djoudini.iplayer.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class CategoryFilterState(
    val liveCategories: List<CategoryEntity> = emptyList(),
    val vodCategories: List<CategoryEntity> = emptyList(),
    val seriesCategories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val currentStep: Int = 0, // 0=Live, 1=VOD, 2=Series
    val isSyncing: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val categoryDao: CategoryDao,
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    val syncProgress: StateFlow<SyncProgress> = playlistRepository.syncProgress

    private val _loginSuccess = MutableSharedFlow<Long>()
    val loginSuccess: SharedFlow<Long> = _loginSuccess.asSharedFlow()

    private val _filterState = MutableStateFlow(CategoryFilterState())
    val filterState: StateFlow<CategoryFilterState> = _filterState.asStateFlow()

    private val _syncComplete = MutableSharedFlow<Unit>()
    val syncComplete: SharedFlow<Unit> = _syncComplete.asSharedFlow()

    private var categoryCollectJob: Job? = null

    fun loginXtream(name: String, serverUrl: String, username: String, password: String) {
        if (name.isBlank() || serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            _loginState.update { it.copy(error = "All fields are required") }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, error = null) }
            try {
                val playlistId = playlistRepository.addXtreamPlaylist(name, serverUrl, username, password)
                playlistRepository.setActive(playlistId)
                // Phase 1: Only fetch categories (fast!)
                playlistRepository.syncCategoriesOnly(playlistId)
                _loginState.update { it.copy(isLoading = false) }
                _loginSuccess.emit(playlistId)
            } catch (e: Exception) {
                _loginState.update {
                    it.copy(isLoading = false, error = e.localizedMessage ?: "Login failed")
                }
            }
        }
    }

    fun loginM3u(name: String, m3uUrl: String) {
        if (name.isBlank() || m3uUrl.isBlank()) {
            _loginState.update { it.copy(error = "All fields are required") }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, error = null) }
            try {
                val playlistId = playlistRepository.addM3uPlaylist(name, m3uUrl)
                playlistRepository.setActive(playlistId)
                playlistRepository.syncCategoriesOnly(playlistId)
                _loginState.update { it.copy(isLoading = false) }
                _loginSuccess.emit(playlistId)
            } catch (e: Exception) {
                _loginState.update {
                    it.copy(isLoading = false, error = e.localizedMessage ?: "Import failed")
                }
            }
        }
    }

    fun loadCategories(playlistId: Long) {
        categoryCollectJob?.cancel()
        categoryCollectJob = viewModelScope.launch {
            _filterState.update { it.copy(isLoading = true) }
            // Single snapshot load instead of 3 racing Flow collectors
            val allCategories = categoryDao.getAllByPlaylist(playlistId)
            _filterState.update {
                it.copy(
                    liveCategories = allCategories.filter { c -> c.categoryType == "live" },
                    vodCategories = allCategories.filter { c -> c.categoryType == "vod" },
                    seriesCategories = allCategories.filter { c -> c.categoryType == "series" },
                    isLoading = false,
                )
            }
        }
    }

    fun toggleCategory(categoryId: Long, selected: Boolean) {
        viewModelScope.launch {
            categoryDao.setSelected(categoryId, selected)
            // Update local state directly (no re-query needed)
            _filterState.update { state ->
                state.copy(
                    liveCategories = state.liveCategories.map {
                        if (it.id == categoryId) it.copy(isSelected = selected) else it
                    },
                    vodCategories = state.vodCategories.map {
                        if (it.id == categoryId) it.copy(isSelected = selected) else it
                    },
                    seriesCategories = state.seriesCategories.map {
                        if (it.id == categoryId) it.copy(isSelected = selected) else it
                    },
                )
            }
        }
    }

    fun selectAllInStep(selected: Boolean) {
        viewModelScope.launch {
            val currentCategories = when (_filterState.value.currentStep) {
                0 -> _filterState.value.liveCategories
                1 -> _filterState.value.vodCategories
                2 -> _filterState.value.seriesCategories
                else -> emptyList()
            }
            currentCategories.forEach { cat ->
                categoryDao.setSelected(cat.id, selected)
            }
            _filterState.update { state ->
                when (state.currentStep) {
                    0 -> state.copy(liveCategories = state.liveCategories.map { it.copy(isSelected = selected) })
                    1 -> state.copy(vodCategories = state.vodCategories.map { it.copy(isSelected = selected) })
                    2 -> state.copy(seriesCategories = state.seriesCategories.map { it.copy(isSelected = selected) })
                    else -> state
                }
            }
        }
    }

    fun nextStep() {
        _filterState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(2)) }
    }

    fun previousStep() {
        _filterState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun syncSelectedContent(playlistId: Long) {
        viewModelScope.launch {
            _filterState.update { it.copy(isSyncing = true) }
            try {
                playlistRepository.syncSelectedStreams(playlistId)
                _filterState.update { it.copy(isSyncing = false) }
                _syncComplete.emit(Unit)
            } catch (e: Exception) {
                _filterState.update { it.copy(isSyncing = false) }
                _loginState.update {
                    it.copy(error = e.localizedMessage ?: "Sync failed")
                }
            }
        }
    }

    fun clearError() {
        _loginState.update { it.copy(error = null) }
    }
}
