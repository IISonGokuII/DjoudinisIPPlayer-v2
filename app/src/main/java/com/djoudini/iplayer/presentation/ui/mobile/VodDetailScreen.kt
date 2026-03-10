package com.djoudini.iplayer.presentation.ui.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.djoudini.iplayer.R
import com.djoudini.iplayer.presentation.viewmodel.VodDetailViewModel

@Composable
fun VodDetailScreen(
    onPlay: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: VodDetailViewModel = hiltViewModel(),
) {
    val vod by viewModel.vod.collectAsStateWithLifecycle()
    val resumePosition by viewModel.resumePositionMs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vod?.name ?: stringResource(R.string.movie)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            vod?.let { movie ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    if (!movie.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = movie.logoUrl,
                            contentDescription = movie.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(140.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = movie.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        movie.year?.let {
                            Text(
                                text = stringResource(R.string.year_format, it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        movie.genre?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        movie.rating?.let {
                            Text(
                                text = stringResource(R.string.rating_format, it),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        movie.durationSeconds?.let { secs ->
                            val hours = secs / 3600
                            val mins = (secs % 3600) / 60
                            val durationText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                            Text(
                                text = stringResource(R.string.duration_format, durationText),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        movie.director?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.director_format, it),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        movie.cast?.let {
                            Text(
                                text = stringResource(R.string.cast_format, it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Plot
                movie.plot?.let { plot ->
                    Text(
                        text = plot,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Play button
                Button(
                    onClick = { onPlay(movie.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (resumePosition > 0) {
                        val mins = resumePosition / 60_000
                        Text(stringResource(R.string.continue_from_format, mins))
                    } else {
                        Text(stringResource(R.string.play_movie))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
