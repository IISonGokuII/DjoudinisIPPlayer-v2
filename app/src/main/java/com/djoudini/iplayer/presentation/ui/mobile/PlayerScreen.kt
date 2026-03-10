package com.djoudini.iplayer.presentation.ui.mobile

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.app.Activity
import android.view.KeyEvent
import android.view.WindowInsetsController
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.djoudini.iplayer.R
import com.djoudini.iplayer.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    var isFullscreen by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
    var currentPlaybackSpeed by remember { mutableStateOf(1f) }

    // Immersive fullscreen mode
    LaunchedEffect(isFullscreen) {
        activity?.window?.let { window ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val controller = window.insetsController
                if (isFullscreen) {
                    controller?.hide(android.view.WindowInsets.Type.systemBars())
                    controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller?.show(android.view.WindowInsets.Type.systemBars())
                }
            } else {
                @Suppress("DEPRECATION")
                if (isFullscreen) {
                    window.decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
                } else {
                    window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    // Restore system bars when leaving
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let { window ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    window.insetsController?.show(android.view.WindowInsets.Type.systemBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    // Don't start playback while resume dialog is shown
    val shouldPlay = uiState.streamUrl.isNotBlank() && !uiState.showResumeDialog

    // Create player only when stream URL is available and dialog dismissed
    val exoPlayer = remember(uiState.streamUrl, shouldPlay) {
        if (!shouldPlay) return@remember null
        viewModel.playerFactory.create(
            userAgentOverride = uiState.userAgent,
        )
    }

    // Set media item and play
    LaunchedEffect(uiState.streamUrl, exoPlayer) {
        if (!shouldPlay || exoPlayer == null) return@LaunchedEffect

        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // If error is from renderer (e.g. bad track selection), clear overrides and retry
                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED ||
                    error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
                ) {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                        .build()
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                    return
                }

                // Try fallback URL for LiveTV before showing error
                val nextUrl = viewModel.tryNextFallback()
                if (nextUrl != null) {
                    exoPlayer.setMediaItem(MediaItem.fromUri(nextUrl))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                } else {
                    viewModel.onPlaybackError(error.localizedMessage ?: "Playback failed")
                }
            }
        })

        val mediaItem = MediaItem.fromUri(uiState.streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        // Resume position for VOD/episodes
        if (uiState.resumePositionMs > 0) {
            exoPlayer.seekTo(uiState.resumePositionMs)
        }

        exoPlayer.playWhenReady = true
        viewModel.startProgressTracking()
    }

    // Track playback state
    LaunchedEffect(exoPlayer) {
        if (exoPlayer == null) return@LaunchedEffect
        while (true) {
            viewModel.updatePlaybackState(
                isPlaying = exoPlayer.isPlaying,
                positionMs = exoPlayer.currentPosition,
                durationMs = exoPlayer.duration.coerceAtLeast(0),
            )
            delay(1000)
        }
    }

    // Auto-hide controls after 4 seconds when playing
    LaunchedEffect(uiState.controlsVisible, uiState.isPlaying) {
        if (uiState.controlsVisible && uiState.isPlaying) {
            delay(4000)
            viewModel.hideControls()
        }
    }

    // Pause/resume with lifecycle (stop playback when leaving screen)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayer?.mediaItemCount ?: 0 > 0) exoPlayer?.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopProgressTracking()
            exoPlayer?.release()
        }
    }

    // Resume dialog
    if (uiState.showResumeDialog) {
        val mins = uiState.resumePositionMs / 60_000
        AlertDialog(
            onDismissRequest = { viewModel.onResumeChoice(false) },
            title = { Text(stringResource(R.string.continue_watching_title)) },
            text = {
                Text(stringResource(R.string.resume_message, mins))
            },
            confirmButton = {
                Button(onClick = { viewModel.onResumeChoice(true) }) {
                    Text(stringResource(R.string.continue_btn))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.onResumeChoice(false) }) {
                    Text(stringResource(R.string.start_over))
                }
            },
        )
    }

    // Audio track selection dialog
    if (showAudioTrackDialog && exoPlayer != null) {
        val trackGroups = exoPlayer.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO }
        AlertDialog(
            onDismissRequest = { showAudioTrackDialog = false },
            title = { Text(stringResource(R.string.audio_track)) },
            text = {
                Column {
                    if (trackGroups.isEmpty()) {
                        Text(stringResource(R.string.no_audio_tracks))
                    } else {
                        trackGroups.forEachIndexed { _, group ->
                            for (trackIdx in 0 until group.length) {
                                val format = group.getTrackFormat(trackIdx)
                                val isSelected = group.isTrackSelected(trackIdx)
                                val trackLabel = format.language?.uppercase() ?: stringResource(R.string.track_number, trackIdx + 1)
                                val label = buildString {
                                    append(trackLabel)
                                    format.label?.let { append(" - $it") }
                                    format.channelCount.let { if (it > 0) append(" (${it}ch)") }
                                }
                                Text(
                                    text = if (isSelected) "* $label" else "  $label",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Save current position and parameters for recovery
                                            val savedPosition = exoPlayer.currentPosition
                                            val savedParams = exoPlayer.trackSelectionParameters
                                            try {
                                                exoPlayer.trackSelectionParameters = savedParams
                                                    .buildUpon()
                                                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                    .addOverride(
                                                        TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIdx))
                                                    )
                                                    .build()
                                            } catch (_: Exception) {
                                                // Restore previous parameters if override fails
                                                exoPlayer.trackSelectionParameters = savedParams
                                                exoPlayer.seekTo(savedPosition)
                                            }
                                            showAudioTrackDialog = false
                                        }
                                        .padding(vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioTrackDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    // Playback speed dialog
    if (showPlaybackSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
        AlertDialog(
            onDismissRequest = { showPlaybackSpeedDialog = false },
            title = { Text(stringResource(R.string.playback_speed)) },
            text = {
                Column {
                    speeds.forEach { speed ->
                        val isSelected = currentPlaybackSpeed == speed
                        Text(
                            text = if (isSelected) "* ${speed}x" else "  ${speed}x",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    exoPlayer?.setPlaybackSpeed(speed)
                                    currentPlaybackSpeed = speed
                                    showPlaybackSpeedDialog = false
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaybackSpeedDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    val focusRequester = remember { FocusRequester() }

    // Request focus for D-pad key handling
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        exoPlayer?.let { it.playWhenReady = !it.isPlaying }
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        exoPlayer?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        exoPlayer?.let { it.seekTo(it.currentPosition + 10_000) }
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        viewModel.toggleControls()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        exoPlayer?.playWhenReady = true
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        exoPlayer?.playWhenReady = false
                        true
                    }
                    else -> false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { viewModel.toggleControls() },
            ),
    ) {
        // Video surface
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
        }

        // Error display with retry button
        uiState.error?.let { error ->
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.playback_error),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = error,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Retry from beginning with original URL
                        viewModel.onPlaybackError("") // clear error
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(stringResource(R.string.retry))
                }
            }
        }

        // Overlay controls
        AnimatedVisibility(
            visible = uiState.controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top gradient + back button + title
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            uiState.currentProgram?.let { prog ->
                                Text(
                                    text = prog.title,
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                )
                            }
                        }
                        IconButton(onClick = {
                            showAudioTrackDialog = true
                            viewModel.showControls()
                        }) {
                            Icon(Icons.Default.Audiotrack, stringResource(R.string.audio_track), tint = Color.White)
                        }
                        IconButton(onClick = {
                            showPlaybackSpeedDialog = true
                            viewModel.showControls()
                        }) {
                            Icon(Icons.Default.Speed, stringResource(R.string.speed), tint = Color.White)
                        }
                        IconButton(onClick = { isFullscreen = !isFullscreen }) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.fullscreen),
                                tint = Color.White,
                            )
                        }
                    }
                }

                // Center play/pause controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = {
                            exoPlayer?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        Icon(Icons.Default.Replay10, stringResource(R.string.rewind), tint = Color.White)
                    }

                    // Play/Pause
                    IconButton(
                        onClick = {
                            exoPlayer?.let { it.playWhenReady = !it.isPlaying }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.play_pause),
                            tint = Color.White,
                            modifier = Modifier.size(40.dp),
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            exoPlayer?.let { it.seekTo(it.currentPosition + 10_000) }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        Icon(Icons.Default.Forward10, stringResource(R.string.forward), tint = Color.White)
                    }
                }

                // Bottom gradient + seek bar + time
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(16.dp),
                ) {
                    // Seek bar (only for VOD/episodes with duration)
                    if (uiState.durationMs > 0) {
                        Slider(
                            value = uiState.currentPositionMs.toFloat(),
                            onValueChange = { newPos ->
                                exoPlayer?.seekTo(newPos.toLong())
                            },
                            valueRange = 0f..uiState.durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.Gray,
                            ),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = formatDuration(uiState.currentPositionMs),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                text = formatDuration(uiState.durationMs),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    // EPG: next program
                    uiState.nextProgram?.let { next ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.next_program, next.title),
                            color = Color.LightGray,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
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
