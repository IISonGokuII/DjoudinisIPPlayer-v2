package com.djoudini.iplayer.presentation.ui.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.local.entity.CategoryEntity
import com.djoudini.iplayer.presentation.viewmodel.OnboardingViewModel

@Composable
fun CategoryFilterScreen(
    playlistId: Long,
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()

    LaunchedEffect(playlistId) {
        viewModel.loadCategories(playlistId)
    }

    LaunchedEffect(Unit) {
        viewModel.syncComplete.collect {
            onComplete()
        }
    }

    val stepTitles = listOf(stringResource(R.string.live_tv), stringResource(R.string.movies_vod), stringResource(R.string.series))
    val currentStep = filterState.currentStep

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.step_format, stepTitles[currentStep], currentStep + 1))
                },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = { viewModel.previousStep() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.selectAllInStep(true) }) {
                        Text(stringResource(R.string.all))
                    }
                    TextButton(onClick = { viewModel.selectAllInStep(false) }) {
                        Text(stringResource(R.string.none))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (filterState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.loading_categories))
                }
            } else if (filterState.isSyncing) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = syncProgress.phase.ifEmpty { stringResource(R.string.syncing) },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (syncProgress.processedItems > 0) {
                        Text(
                            text = stringResource(R.string.items_count, syncProgress.processedItems),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!syncProgress.isIndeterminate && syncProgress.isActive) {
                        LinearProgressIndicator(
                            progress = { syncProgress.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                        )
                    } else if (syncProgress.isActive) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                        )
                    }
                }
            } else {
                // Step indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    stepTitles.forEachIndexed { index, _ ->
                        val color = if (index == currentStep) {
                            MaterialTheme.colorScheme.primary
                        } else if (index < currentStep) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                        LinearProgressIndicator(
                            progress = { if (index <= currentStep) 1f else 0f },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            color = color,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.select_categories_format, stepTitles[currentStep]),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )

                val categories = when (currentStep) {
                    0 -> filterState.liveCategories
                    1 -> filterState.vodCategories
                    2 -> filterState.seriesCategories
                    else -> emptyList()
                }

                AnimatedContent(
                    targetState = currentStep,
                    modifier = Modifier.weight(1f),
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
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.no_categories_format, stepTitles[step]),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        CategoryCheckboxList(
                            categories = stepCategories,
                            onToggle = { id, checked -> viewModel.toggleCategory(id, checked) },
                        )
                    }
                }

                // Bottom navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { viewModel.previousStep() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.back))
                        }
                    }

                    if (currentStep < 2) {
                        Button(
                            onClick = { viewModel.nextStep() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                        ) {
                            Text(stringResource(R.string.next))
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.syncSelectedContent(playlistId) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                        ) {
                            Text(stringResource(R.string.sync_and_continue))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCheckboxList(
    categories: List<CategoryEntity>,
    onToggle: (Long, Boolean) -> Unit,
) {
    LazyColumn {
        items(categories, key = { it.id }) { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = category.isSelected,
                    onCheckedChange = { checked ->
                        onToggle(category.id, checked)
                    },
                )
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
