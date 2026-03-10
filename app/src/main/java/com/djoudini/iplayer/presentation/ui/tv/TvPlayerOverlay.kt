package com.djoudini.iplayer.presentation.ui.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.djoudini.iplayer.domain.model.WatchContentType
import com.djoudini.iplayer.presentation.viewmodel.PlayerUiState

/**
 * TV-optimized player overlay with D-Pad key handling.
 *
 * Key mappings for Live TV (CHANNEL):
 * - DPAD_CENTER / Enter: Play/Pause
 * - DPAD_LEFT: Rewind 10s (LONG PRESS: Recent channels)
 * - DPAD_RIGHT: Forward 10s
 * - DPAD_UP: Previous channel (zap)
 * - DPAD_DOWN: Next channel (zap)
 * - NUMBER KEYS (0-9): Direct channel number input
 * - BACK: Exit player
 *
 * Key mappings for VOD/Episodes:
 * - DPAD_CENTER / Enter: Play/Pause
 * - DPAD_LEFT: Rewind 10s
 * - DPAD_RIGHT: Forward 10s (LONG PRESS: Next Episode)
 * - DPAD_UP/DOWN: Show/hide controls
 * - BACK: Exit player
 */
@Composable
fun TvPlayerOverlay(
    uiState: PlayerUiState,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onShowControls: () -> Unit,
    onBack: () -> Unit,
    onPreviousChannel: (() -> Unit)? = null,
    onNextChannel: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onShowRecentChannels: (() -> Unit)? = null,
    onInputChannelNumber: ((String) -> Unit)? = null,
    onToggleAudioSelection: (() -> Unit)? = null,
    onToggleSubtitleSelection: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isLiveTv = uiState.contentType == WatchContentType.CHANNEL
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                
                when (event.key) {
                    Key.DirectionCenter, Key.Enter -> {
                        onPlayPause()
                        true
                    }
                    Key.DirectionLeft -> {
                        onSeekBackward()
                        true
                    }
                    Key.DirectionRight -> {
                        onSeekForward()
                        true
                    }
                    Key.DirectionUp -> {
                        if (isLiveTv && onPreviousChannel != null) {
                            onPreviousChannel()
                        } else {
                            onShowControls()
                        }
                        true
                    }
                    Key.DirectionDown -> {
                        if (isLiveTv && onNextChannel != null) {
                            onNextChannel()
                        } else {
                            onShowControls()
                        }
                        true
                    }
                    // Number keys for channel input (Live TV only)
                    Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, 
                    Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine -> {
                        if (isLiveTv && onInputChannelNumber != null) {
                            val digit = when (event.key) {
                                Key.Zero -> "0"
                                Key.One -> "1"
                                Key.Two -> "2"
                                Key.Three -> "3"
                                Key.Four -> "4"
                                Key.Five -> "5"
                                Key.Six -> "6"
                                Key.Seven -> "7"
                                Key.Eight -> "8"
                                Key.Nine -> "9"
                                else -> null
                            }
                            digit?.let { onInputChannelNumber(it) }
                            true
                        } else false
                    }
                    Key.Back, Key.Escape -> {
                        onBack()
                        true
                    }
                    else -> false
                }
            },
    ) {
        // Channel Number Input Overlay
        AnimatedVisibility(
            visible = uiState.showChannelNumberInput,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Kanal",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.channelNumberInput,
                        color = Color.White,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "OK zum Bestätigen",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar: Title + EPG + Favorite + Audio/Subtitle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .padding(32.dp),
                ) {
                    // Title row with favorite and track buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Title
                        Text(
                            text = uiState.title,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        
                        // Favorite button (Live TV only)
                        if (isLiveTv && onToggleFavorite != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.isFavorite) 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        else 
                                            Color.Black.copy(alpha = 0.5f)
                                    )
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (uiState.isFavorite) 
                                        Icons.Default.Favorite 
                                    else 
                                        Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (uiState.isFavorite) Color.White else Color.White.copy(alpha = 0.7f),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        
                        // Audio track button
                        if (onToggleAudioSelection != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = "Audio",
                                    tint = Color.White,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        
                        // Subtitle button
                        if (onToggleSubtitleSelection != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClosedCaption,
                                    contentDescription = "Subtitles",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // EPG Info
                    uiState.currentProgram?.let { prog ->
                        Text(
                            text = "Jetzt: ${prog.title}",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    uiState.nextProgram?.let { next ->
                        Text(
                            text = "Danach: ${next.title}",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // Center: Play/Pause with visual focus
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Channel Zap Indicator (Live TV only)
                    if (isLiveTv) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.NavigateNext,
                                contentDescription = "Previous Channel",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(4.dp),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "HOCH",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind",
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(8.dp),
                    )

                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                            .padding(12.dp),
                    )

                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward",
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(8.dp),
                    )

                    // Channel Zap Indicator (Live TV only)
                    if (isLiveTv) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Channel",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(4.dp),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "RUNTER",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                // Bottom: Time / duration + Next Episode button for series
                if (uiState.durationMs > 0 || uiState.hasNextEpisode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                            .padding(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (uiState.durationMs > 0) {
                            Text(
                                text = formatTvDuration(uiState.currentPositionMs),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        
                        // Next Episode button for series
                        if (uiState.hasNextEpisode && onNextEpisode != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Episode",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Nächste Episode",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (uiState.durationMs > 0) {
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }
                        
                        if (uiState.durationMs > 0) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = formatTvDuration(uiState.durationMs),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTvDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
