package com.djoudini.iplayer.presentation.ui.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djoudini.iplayer.R
import com.djoudini.iplayer.presentation.components.ContentCard
import com.djoudini.iplayer.presentation.viewmodel.ContentListViewModel

@Composable
fun ChannelListScreen(
    onChannelClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ContentListViewModel = hiltViewModel(),
) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.channels_count, channels.size)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(channels, key = { it.channel.id }) { channelWithEpg ->
                val epgNow = channelWithEpg.currentProgram?.let { program ->
                    "${program.title} (${program.startTime / 60000} min)"
                }
                ContentCard(
                    name = channelWithEpg.channel.name,
                    logoUrl = channelWithEpg.channel.logoUrl,
                    onClick = { onChannelClick(channelWithEpg.channel.id) },
                    isFavorite = channelWithEpg.channel.isFavorite,
                    onFavoriteClick = { viewModel.toggleFavorite(channelWithEpg.channel.id, channelWithEpg.channel.isFavorite) },
                    epgNow = epgNow,
                )
            }
        }
    }
}
