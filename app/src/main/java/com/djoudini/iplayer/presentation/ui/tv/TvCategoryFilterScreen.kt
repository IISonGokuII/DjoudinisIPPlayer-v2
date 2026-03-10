package com.djoudini.iplayer.presentation.ui.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.CategoryEntity
import com.djoudini.iplayer.presentation.components.FocusableCard
import com.djoudini.iplayer.presentation.viewmodel.OnboardingViewModel

/**
 * TV-optimized category filter screen.
 * 
 * Key improvements for TV:
 * - Fixed height category list (no need to scroll through all to reach buttons)
 * - Always visible navigation buttons at bottom
 * - Larger touch targets for D-Pad navigation
 * - Select All / None buttons prominently placed
 */
@Composable
fun TvCategoryFilterScreen(
    playlistId: Long,
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()

    LaunchedEffect(playlistId) {
        viewModel.loadCategories(playlistId)
    }

    LaunchedEffect(Unit) {
        viewModel.syncComplete.collect {
            onComplete()
        }
    }
    
    // Check for errors and progress
    val errorMessage = loginState.error
    val isSyncing = filterState.isSyncing
    val hasCategories = filterState.liveCategories.isNotEmpty() || 
                        filterState.vodCategories.isNotEmpty() || 
                        filterState.seriesCategories.isNotEmpty()

    val stepTitles = listOf(
        stringResource(R.string.live_tv),
        stringResource(R.string.movies_vod),
        stringResource(R.string.series)
    )
    val currentStep = filterState.currentStep

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        // Show error if present
        if (errorMessage != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Error: $errorMessage",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(24.dp))
                FocusableCard(
                    onClick = { viewModel.clearError() },
                    modifier = Modifier
                        .width(200.dp)
                        .height(56.dp),
                    focusScale = 1.05f
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            return@Box
        }
        
        if (filterState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    stringResource(R.string.loading_categories),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        } else if (filterState.isSyncing) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = syncProgress.phase.ifEmpty { stringResource(R.string.syncing) },
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (syncProgress.processedItems > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.items_count, syncProgress.processedItems),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                if (!syncProgress.isIndeterminate && syncProgress.isActive) {
                    LinearProgressIndicator(
                        progress = { syncProgress.progress },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(8.dp),
                    )
                } else if (syncProgress.isActive) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(8.dp),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with step indicator
                TvCategoryFilterHeader(
                    stepTitles = stepTitles,
                    currentStep = currentStep,
                    onBack = { if (currentStep > 0) viewModel.previousStep() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Instruction text
                Text(
                    text = stringResource(R.string.select_categories_format, stepTitles[currentStep]),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Select All / None buttons in a row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    FocusableCard(
                        onClick = { viewModel.selectAllInStep(true) },
                        modifier = Modifier
                            .width(160.dp)
                            .height(48.dp),
                        focusScale = 1.05f
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.all),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    FocusableCard(
                        onClick = { viewModel.selectAllInStep(false) },
                        modifier = Modifier
                            .width(160.dp)
                            .height(48.dp),
                        focusScale = 1.05f
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.none),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category list - FIXED HEIGHT so buttons are always visible
                val categories = when (currentStep) {
                    0 -> filterState.liveCategories
                    1 -> filterState.vodCategories
                    2 -> filterState.seriesCategories
                    else -> emptyList()
                }

                AnimatedContent(
                    targetState = currentStep,
                    modifier = Modifier.height(320.dp), // Fixed height for ~6 items
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        } else {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        }
                    },
                    label = "step",
                ) { step ->
                    val stepCategories = when (step) {
                        0 -> filterState.liveCategories
                        1 -> filterState.vodCategories
                        2 -> filterState.seriesCategories
                        else -> emptyList()
                    }

                    if (stepCategories.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_categories_format, stepTitles[step]),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        TvCategoryCheckboxList(
                            categories = stepCategories,
                            onToggle = { id, checked -> viewModel.toggleCategory(id, checked) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom navigation buttons - ALWAYS VISIBLE
                val selectedCount = when (currentStep) {
                    0 -> filterState.liveCategories.count { it.isSelected }
                    1 -> filterState.vodCategories.count { it.isSelected }
                    2 -> filterState.seriesCategories.count { it.isSelected }
                    else -> 0
                }
                TvCategoryFilterNavigation(
                    currentStep = currentStep,
                    totalSteps = 3,
                    onPrevious = { viewModel.previousStep() },
                    onNext = { viewModel.nextStep() },
                    onSync = { viewModel.syncSelectedContent(playlistId) },
                    selectedCount = selectedCount
                )
            }
        }
    }
}

@Composable
private fun TvCategoryFilterHeader(
    stepTitles: List<String>,
    currentStep: Int,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title row with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (currentStep > 0) {
                FocusableCard(
                    onClick = onBack,
                    modifier = Modifier.size(56.dp),
                    focusScale = 1.1f
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(24.dp))
            }

            Text(
                text = stringResource(R.string.step_format, stepTitles[currentStep], currentStep + 1),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step indicator dots/bars
        Row(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            stepTitles.forEachIndexed { index, _ ->
                val color = when {
                    index == currentStep -> MaterialTheme.colorScheme.primary
                    index < currentStep -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
                LinearProgressIndicator(
                    progress = { if (index <= currentStep) 1f else 0f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun TvCategoryCheckboxList(
    categories: List<CategoryEntity>,
    onToggle: (Long, Boolean) -> Unit,
) {
    LazyColumn {
        items(categories, key = { it.id }) { category ->
            TvCategoryCheckboxItem(
                category = category,
                onToggle = { checked -> onToggle(category.id, checked) }
            )
        }
    }
}

@Composable
private fun TvCategoryCheckboxItem(
    category: CategoryEntity,
    onToggle: (Boolean) -> Unit,
) {
    FocusableCard(
        onClick = { onToggle(!category.isSelected) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        focusScale = 1.02f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Custom checkbox indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (category.isSelected) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            
            // Selection indicator
            if (category.isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TvCategoryFilterNavigation(
    currentStep: Int,
    totalSteps: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSync: () -> Unit,
    selectedCount: Int = 0,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Show selected count
        if (selectedCount > 0) {
            Text(
                text = "$selectedCount Kategorien ausgewählt",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            // Back button (if not first step)
            if (currentStep > 0) {
                FocusableCard(
                    onClick = onPrevious,
                    modifier = Modifier
                        .width(180.dp)
                        .height(56.dp),
                    focusScale = 1.05f
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.back),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Next or Sync button
            if (currentStep < totalSteps - 1) {
                FocusableCard(
                    onClick = onNext,
                    modifier = Modifier
                        .width(180.dp)
                        .height(56.dp),
                    focusScale = 1.05f
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.next),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(24.dp))
                    }
                }
            } else {
                FocusableCard(
                    onClick = onSync,
                    modifier = Modifier
                        .width(240.dp)
                        .height(56.dp),
                    focusScale = 1.05f
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.sync_and_continue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
