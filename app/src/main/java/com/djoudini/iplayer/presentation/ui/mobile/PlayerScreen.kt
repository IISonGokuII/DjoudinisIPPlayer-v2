package com.djoudini.iplayer.presentation.ui.mobile

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import android.view.MotionEvent
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.djoudini.iplayer.R
import com.djoudini.iplayer.domain.model.WatchContentType
import com.djoudini.iplayer.presentation.viewmodel.AspectRatio
import com.djoudini.iplayer.presentation.viewmodel.PlayerViewModel
import com.djoudini.iplayer.presentation.viewmodel.SleepTimerPreset
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAudioDelayDialog by remember { mutableStateOf(false) }
    var currentPlaybackSpeed by remember { mutableStateOf(1f) }

    // Immersive fullscreen mode with smooth transition
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
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
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

    // CRITICAL FIX: Create player once and keep it alive
    // The player is created regardless of shouldPlay state, then media is loaded when ready
    val exoPlayer = remember {
        viewModel.playerFactory.create(
            userAgentOverride = uiState.userAgent,
        )
    }

    // Set media item and play on streamUrl change
    LaunchedEffect(uiState.streamUrl, exoPlayer) {
        // Wait for valid stream URL
        if (uiState.streamUrl.isBlank()) return@LaunchedEffect
        
        // Don't start if resume dialog is shown
        if (uiState.showResumeDialog) return@LaunchedEffect

        // CRITICAL: Stop current playback before loading new stream
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        // Add error listener
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

    // CRITICAL: Release player when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            exoPlayer?.release()
        }
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

    // Audio Track Info Dialog (simplified)
    if (showAudioTrackDialog) {
        AlertDialog(
            onDismissRequest = { showAudioTrackDialog = false },
            title = { Text(stringResource(R.string.audio_track)) },
            text = {
                Column {
                    Text(
                        text = "Audio-Spur Information:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Die Audio-Spur wird automatisch vom Stream geladen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bei den meisten IPTV-Streams ist nur eine Audio-Spur verfügbar. Wenn mehrere Audio-Spuren vorhanden sind (z.B. bei Filmen mit Mehrsprachigkeit), werden diese automatisch von ExoPlayer erkannt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tipp: Wenn kein Ton zu hören ist, überprüfe die Lautstärke deines Geräts oder versuche den Stream neu zu laden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
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

    // NEW: Sleep Timer Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column {
                    SleepTimerPreset.entries.forEach { preset ->
                        val isSelected = uiState.sleepTimerActive && 
                            (preset.minutes * 60 == uiState.sleepTimerRemainingSeconds || 
                             (preset != SleepTimerPreset.OFF && uiState.sleepTimerActive))
                        Text(
                            text = if (isSelected) "✓ ${preset.label}" else preset.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSleepTimer(preset)
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    // NEW: Audio Delay Dialog
    if (showAudioDelayDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDelayDialog = false },
            title = { Text("Audio-Synchronisation") },
            text = {
                Column {
                    Text(
                        text = "Aktuelle Verzögerung: ${uiState.audioDelayMs}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Minus button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.adjustAudioDelay(-100) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    "Weniger",
                                    modifier = Modifier.rotate(180f),
                                )
                            }
                            Text("-100ms", style = MaterialTheme.typography.labelSmall)
                        }
                        // Reset button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.resetAudioDelay() },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Default.Refresh, "Zurücksetzen")
                            }
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                        // Plus button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.adjustAudioDelay(100) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Mehr")
                            }
                            Text("+100ms", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tipp: Bei Lip-Sync-Problemen schrittweise anpassen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioDelayDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    // NEW: Auto-Play Countdown Overlay
    if (uiState.showAutoPlayCountdown) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Nächste Folge in ${uiState.autoPlayCountdownSeconds}s",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Button(onClick = { viewModel.cancelAutoPlay() }) {
                        Text("Abbrechen")
                    }
                    Button(onClick = { viewModel.startAutoPlayCountdown(0) }) {
                        Text("Jetzt abspielen")
                    }
                }
            }
        }
    }

    val focusRequester = remember { FocusRequester() }

    // EPG Overlay visibility - shows for 5 seconds when channel/program changes
    var showEpgOverlay by remember { mutableStateOf(false) }
    var lastProgramChangeTime by remember { mutableStateOf(0L) }

    // Show EPG overlay when program changes
    LaunchedEffect(uiState.currentProgram, uiState.nextProgram) {
        val now = System.currentTimeMillis()
        if (now - lastProgramChangeTime > 1000) { // Debounce
            showEpgOverlay = true
            lastProgramChangeTime = now
            // Hide after 5 seconds
            kotlinx.coroutines.delay(5000)
            showEpgOverlay = false
        }
    }

    // Long-press detector for TV remote
    var longPressAction by remember { mutableStateOf<String?>(null) }

    // Execute long press actions
    LaunchedEffect(longPressAction) {
        when (longPressAction) {
            "aspect_ratio" -> viewModel.cycleAspectRatio()
            "sleep_timer" -> showSleepTimerDialog = true
            "audio_delay_down" -> viewModel.adjustAudioDelay(-100)
            "audio_delay_up" -> viewModel.adjustAudioDelay(100)
        }
        longPressAction = null
    }

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
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            // Long press for aspect ratio toggle
                            longPressAction = "aspect_ratio"
                            exoPlayer?.let { it.playWhenReady = !it.isPlaying }
                            viewModel.showControls()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (uiState.contentType == WatchContentType.CHANNEL) {
                                viewModel.playPreviousChannel()
                            } else {
                                // Long press for sleep timer
                                longPressAction = "sleep_timer"
                                viewModel.toggleControls()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (uiState.contentType == WatchContentType.CHANNEL) {
                                viewModel.playNextChannel()
                            } else {
                                viewModel.toggleControls()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            // Long press for audio delay decrease
                            longPressAction = "audio_delay_down"
                            exoPlayer?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
                            viewModel.showControls()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            // Long press for audio delay increase
                            longPressAction = "audio_delay_up"
                            if (uiState.contentType == WatchContentType.EPISODE && uiState.hasNextEpisode) {
                                exoPlayer?.let {
                                    if (uiState.durationMs > 0 && it.currentPosition >= uiState.durationMs - 5000) {
                                        viewModel.startAutoPlayCountdown(0)
                                    } else {
                                        it.seekTo(it.currentPosition + 10_000)
                                    }
                                }
                            } else {
                                exoPlayer?.let { it.seekTo(it.currentPosition + 10_000) }
                            }
                            viewModel.showControls()
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
                        KeyEvent.KEYCODE_MEDIA_STOP -> {
                            exoPlayer?.stop()
                            true
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            false // Let back button navigate normally
                        }
                        else -> false
                    }
                } else if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                    // Cancel long press on key release
                    longPressAction = null
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_RIGHT -> true
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { viewModel.toggleControls() },
            ),
    ) {
        // Video surface with Pinch-to-Zoom and Aspect Ratio
        if (exoPlayer != null) {
            val playerView = remember { mutableStateOf<PlayerView?>(null) }
            
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        // Set aspect ratio mode based on current setting
                        resizeMode = when (uiState.aspectRatio) {
                            AspectRatio.FIT_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatio.FIT_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatio.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatio.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatio.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        }
                        playerView.value = this
                    }
                },
                update = { playerView ->
                    // Update resizeMode when aspectRatio changes
                    playerView.resizeMode = when (uiState.aspectRatio) {
                        AspectRatio.FIT_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatio.FIT_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatio.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatio.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatio.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = uiState.videoScale
                        scaleY = uiState.videoScale
                    }
                    .pointerInput(uiState.videoScale) {
                        // Simple pinch-to-zoom
                        awaitEachGesture {
                            var zooming = false
                            var initialSpan = 0f
                            val initialScale = uiState.videoScale

                            do {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                if (changes.size >= 2) {
                                    val p0 = changes[0].position
                                    val p1 = changes[1].position
                                    val dx = p1.x - p0.x
                                    val dy = p1.y - p0.y
                                    val span = kotlin.math.sqrt(dx * dx + dy * dy)
                                    if (!zooming) {
                                        initialSpan = span
                                        zooming = true
                                    } else {
                                        val scaleChange = span / initialSpan
                                        val newScale = (initialScale * scaleChange).coerceIn(0.5f, 3.0f)
                                        viewModel.setVideoScale(newScale)
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
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
                        // NEW: Sleep Timer
                        IconButton(onClick = { showSleepTimerDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (uiState.sleepTimerActive) MaterialTheme.colorScheme.error else Color.White,
                            )
                        }
                        // NEW: Audio Delay
                        IconButton(onClick = { showAudioDelayDialog = true }) {
                            Icon(
                                Icons.Default.Audiotrack,
                                contentDescription = "Audio Delay",
                                tint = if (uiState.audioDelayMs != 0) MaterialTheme.colorScheme.error else Color.White,
                            )
                        }
                        // NEW: Aspect Ratio
                        IconButton(onClick = { viewModel.cycleAspectRatio() }) {
                            Text(
                                text = uiState.aspectRatio.label,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // PREVIOUS: Channel (LiveTV) or Episode (Series)
                    if (uiState.contentType == WatchContentType.CHANNEL || 
                        (uiState.contentType == WatchContentType.EPISODE && uiState.hasNextEpisode)) {
                        IconButton(
                            onClick = {
                                if (uiState.contentType == WatchContentType.CHANNEL) {
                                    viewModel.playPreviousChannel()
                                } else {
                                    viewModel.loadNextEpisode()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (uiState.contentType == WatchContentType.CHANNEL) "Vorheriger Sender" else "Vorherige Folge",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

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

                    // NEXT: Channel (LiveTV) or Episode (Series)
                    if (uiState.contentType == WatchContentType.CHANNEL || 
                        (uiState.contentType == WatchContentType.EPISODE && uiState.hasNextEpisode)) {
                        IconButton(
                            onClick = {
                                if (uiState.contentType == WatchContentType.CHANNEL) {
                                    viewModel.playNextChannel()
                                } else {
                                    // Start auto-play countdown or load next episode immediately
                                    if (uiState.hasNextEpisode) {
                                        viewModel.startAutoPlayCountdown(0) // Immediate
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (uiState.contentType == WatchContentType.CHANNEL) "Nächster Sender" else "Nächste Folge",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(28.dp)
                                    .rotate(180f),
                            )
                        }
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

        // Modern EPG Info Banner at bottom of screen
        if (uiState.contentType == WatchContentType.CHANNEL) {
            EpgInfoBanner(
                visible = showEpgOverlay,
                channelName = uiState.title,
                channelLogo = uiState.logoUrl,
                currentProgram = uiState.currentProgram,
                nextProgram = uiState.nextProgram,
                onDismiss = { showEpgOverlay = false },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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

/**
 * Modernes EPG-Infobanner – erscheint unten im Player für 5 Sekunden.
 * Zeigt Kanalname, aktuelles Programm mit Fortschrittsbalken und nächstes Programm.
 */
@Composable
private fun EpgInfoBanner(
    visible: Boolean,
    channelName: String,
    channelLogo: String?,
    currentProgram: com.djoudini.iplayer.data.local.entity.EpgProgramEntity?,
    nextProgram: com.djoudini.iplayer.data.local.entity.EpgProgramEntity?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)),
                        startY = 0f,
                        endY = 200f,
                    )
                )
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Channel header row: Logo + Name + Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Channel logo
                    if (!channelLogo.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = channelLogo,
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Schließen",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                currentProgram?.let { current ->
                    Spacer(modifier = Modifier.height(14.dp))

                    // "JETZT" label
                    Text(
                        text = "JETZT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // Program title + time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${timeFormat.format(Date(current.startTime))} – ${timeFormat.format(Date(current.stopTime))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }

                    // Progress bar: current position within program
                    val now = System.currentTimeMillis()
                    val totalDuration = (current.stopTime - current.startTime).toFloat()
                    val elapsed = (now - current.startTime).toFloat()
                    val progress = if (totalDuration > 0) (elapsed / totalDuration).coerceIn(0f, 1f) else 0f
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                }

                nextProgram?.let { next ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "DANACH  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = next.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeFormat.format(Date(next.startTime)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.45f),
                        )
                    }
                }
            }
        }
    }
}
