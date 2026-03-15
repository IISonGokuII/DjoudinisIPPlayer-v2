package com.djoudini.iplayer.presentation.ui.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
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
import android.os.PowerManager
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
import androidx.compose.material3.HorizontalDivider
import com.djoudini.iplayer.R
import com.djoudini.iplayer.data.service.RecordingService
import com.djoudini.iplayer.domain.model.WatchContentType
import com.djoudini.iplayer.presentation.viewmodel.AspectRatio
import com.djoudini.iplayer.presentation.viewmodel.AudioTrackInfo
import com.djoudini.iplayer.presentation.viewmodel.PlayerViewModel
import com.djoudini.iplayer.presentation.viewmodel.SleepTimerPreset
import com.djoudini.iplayer.presentation.viewmodel.SubtitleTrackInfo
import com.djoudini.iplayer.util.AutoFrameRateManager
import com.djoudini.iplayer.util.startCompatService
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
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
    val preferredAudioLanguage by viewModel.preferredAudioLanguage.collectAsStateWithLifecycle()
    val preferredSubtitleLanguage by viewModel.preferredSubtitleLanguage.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }

    // --- Recording state ---
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0L) }

    // Track recording elapsed time while active
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0L
            while (isRecording) {
                delay(1_000)
                recordingSeconds++
            }
        }
    }

    // Listen for recording completed broadcast
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == RecordingService.ACTION_RECORDING_COMPLETED) {
                    isRecording = false
                }
            }
        }
        val filter = IntentFilter(RecordingService.ACTION_RECORDING_COMPLETED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    // --- Gesture HUD state ---
    var gestureHudType by remember { mutableStateOf("") } // "brightness", "volume", "seek"
    var gestureHudValue by remember { mutableStateOf(0f) } // 0..1 for br/vol, seconds for seek
    var gestureHudSeekDelta by remember { mutableStateOf(0L) } // seconds for seek display

    LaunchedEffect(gestureHudType) {
        if (gestureHudType.isNotEmpty()) {
            delay(1_500)
            gestureHudType = ""
        }
    }
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAudioDelayDialog by remember { mutableStateOf(false) }
    var currentPlaybackSpeed by remember { mutableStateOf(1f) }
    var showSubtitleTrackDialog by remember { mutableStateOf(false) }
    val autoFrameRateManager = remember { AutoFrameRateManager() }
    var appliedContentFrameRate by remember { mutableStateOf<Float?>(null) }
    var frameRateStatusMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(frameRateStatusMessage) {
        if (frameRateStatusMessage != null) {
            delay(2_500)
            frameRateStatusMessage = null
        }
    }
    // TV Options Panel (long press center on Fire TV)
    val scope = rememberCoroutineScope()
    var showTvOptionsPanel by remember { mutableStateOf(false) }
    var tvOptionIndex by remember { mutableStateOf(0) } // 0=Bildformat, 1=Ton, 2=Untertitel, 3=Sleeptimer, [4=Aufnahme if Live]
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    // Maps flat track index → (TrackGroup, trackIndexInGroup) for ExoPlayer selection
    var audioTrackGroupMap by remember { mutableStateOf<List<Pair<androidx.media3.common.TrackGroup, Int>>>(emptyList()) }
    var subtitleTrackGroupMap by remember { mutableStateOf<List<Pair<androidx.media3.common.TrackGroup, Int>>>(emptyList()) }

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

    // Keep screen awake for the whole player session. Fire TV can still launch
    // the screensaver if we only react to playback state toggles.
    DisposableEffect(activity, uiState.streamUrl) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.window?.decorView?.keepScreenOn = true
        
        // Also acquire WakeLock for extra safety (Fire TV specific)
        val powerManager = activity?.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DjoudinisIPPlayer::PlaybackLock"
        )
        wakeLock?.acquire(10*60*60*1000L) // 10 hours max
        
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.decorView?.keepScreenOn = false
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
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

    // Don't start playback while the user has not decided whether to resume.
    var resumeChoice by remember(uiState.contentType, uiState.contentId) { mutableStateOf<Boolean?>(null) }

    // CRITICAL FIX: Create player once and keep it alive
    // The player is created regardless of shouldPlay state, then media is loaded when ready
    val exoPlayer = remember {
        viewModel.playerFactory.create(
            userAgentOverride = uiState.userAgent,
        )
    }

    DisposableEffect(exoPlayer, viewModel) {
        val errorListener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
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

                val nextUrl = viewModel.tryNextFallback()
                if (nextUrl != null) {
                    exoPlayer.setMediaItem(MediaItem.fromUri(nextUrl))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                } else {
                    viewModel.onPlaybackError(error.localizedMessage ?: "Playback failed")
                }
            }
        }

        exoPlayer.addListener(errorListener)
        onDispose { exoPlayer.removeListener(errorListener) }
    }

    LaunchedEffect(exoPlayer, preferredAudioLanguage, preferredSubtitleLanguage) {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setPreferredAudioLanguage(preferredAudioLanguage.ifBlank { null })
            .setPreferredTextLanguage(preferredSubtitleLanguage.ifBlank { null })
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, preferredSubtitleLanguage.isBlank())
            .build()
    }

    // Set media item and play on streamUrl change
    LaunchedEffect(uiState.streamUrl, exoPlayer, uiState.showResumeDialog, resumeChoice) {
        if (uiState.streamUrl.isBlank()) return@LaunchedEffect
        if (uiState.showResumeDialog && !uiState.autoResume && resumeChoice == null && uiState.resumePositionMs > 10_000L) {
            return@LaunchedEffect
        }

        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        val mediaItem = MediaItem.fromUri(uiState.streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        // Resume position for VOD/episodes - only seek if user chose to resume
        if (uiState.resumePositionMs > 0 && (resumeChoice == true || uiState.autoResume)) {
            exoPlayer.seekTo(uiState.resumePositionMs)
        } else if (uiState.resumePositionMs > 0 && !uiState.autoResume && resumeChoice == null) {
            // User hasn't made a choice yet, don't start playback
            return@LaunchedEffect
        }

        exoPlayer.playWhenReady = true
        viewModel.startProgressTracking()
    }

    // CRITICAL: Release player when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopProgressTracking()
            activity?.let { currentActivity ->
                autoFrameRateManager.restoreOriginalFrameRate(currentActivity)
            }
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    // Track playback state
    LaunchedEffect(exoPlayer) {
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
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayer.mediaItemCount > 0) exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Track changes listener: reads audio/subtitle tracks from ExoPlayer and updates ViewModel.
    // Also auto-selects a compatible audio track if the default one can't be decoded.
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            private fun applyAutoFrameRateIfPossible() {
                val currentActivity = activity ?: return
                val videoFormat = exoPlayer.videoFormat ?: return
                val frameRate = videoFormat.frameRate
                if (frameRate <= 0f) return

                val currentApplied = appliedContentFrameRate
                if (currentApplied != null && kotlin.math.abs(currentApplied - frameRate) < 0.5f) {
                    return
                }

                autoFrameRateManager.matchFrameRate(
                    activity = currentActivity,
                    contentFrameRate = frameRate,
                )
                appliedContentFrameRate = frameRate
                frameRateStatusMessage = "Auto Frame Rate: ${String.format(Locale.US, "%.2f", frameRate)} fps"
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val audioTracks = mutableListOf<AudioTrackInfo>()
                val subtitleTracks = mutableListOf<SubtitleTrackInfo>()
                val newAudioMap = mutableListOf<Pair<androidx.media3.common.TrackGroup, Int>>()
                val newSubtitleMap = mutableListOf<Pair<androidx.media3.common.TrackGroup, Int>>()

                tracks.groups.forEach { group ->
                    when (group.type) {
                        C.TRACK_TYPE_AUDIO -> {
                            for (i in 0 until group.length) {
                                val fmt = group.getTrackFormat(i)
                                val codec = fmt.sampleMimeType?.substringAfterLast('/') ?: ""
                                audioTracks.add(AudioTrackInfo(
                                    index = audioTracks.size,
                                    language = fmt.language ?: "und",
                                    label = buildString {
                                        append(fmt.label ?: fmt.language ?: "Audio ${audioTracks.size + 1}")
                                        if (codec.isNotBlank()) append(" [$codec]")
                                        if (fmt.channelCount > 0) append(" ${fmt.channelCount}ch")
                                    },
                                    isSelected = group.isTrackSelected(i),
                                ))
                                newAudioMap.add(Pair(group.mediaTrackGroup, i))
                            }
                        }
                        C.TRACK_TYPE_TEXT -> {
                            for (i in 0 until group.length) {
                                val fmt = group.getTrackFormat(i)
                                subtitleTracks.add(SubtitleTrackInfo(
                                    index = subtitleTracks.size,
                                    language = fmt.language ?: "und",
                                    label = fmt.label ?: fmt.language ?: "Untertitel ${subtitleTracks.size + 1}",
                                    isSelected = group.isTrackSelected(i),
                                ))
                                newSubtitleMap.add(Pair(group.mediaTrackGroup, i))
                            }
                        }
                        else -> {}
                    }
                }

                viewModel.updateTracks(audioTracks, subtitleTracks)
                audioTrackGroupMap = newAudioMap
                subtitleTrackGroupMap = newSubtitleMap

                // Auto-fallback: if all selected audio tracks are Dolby/DTS (not supported on
                // most mobile devices), prefer AAC/MP3/Opus instead.
                val dolbyMimes = setOf("audio/ac3", "audio/eac3", "audio/x-e-ac-3", "audio/true-hd")
                val selectedAudio = tracks.groups
                    .filter { it.type == C.TRACK_TYPE_AUDIO }
                    .any { g -> (0 until g.length).any { g.isTrackSelected(it) && (g.getTrackFormat(it).sampleMimeType in dolbyMimes) } }

                if (selectedAudio) {
                    // Try to find a non-Dolby audio group
                    val aacGroup = tracks.groups
                        .filter { it.type == C.TRACK_TYPE_AUDIO }
                        .find { g -> (0 until g.length).any { g.getTrackFormat(it).sampleMimeType !in dolbyMimes } }

                    if (aacGroup != null) {
                        val trackIdx = (0 until aacGroup.length)
                            .first { aacGroup.getTrackFormat(it).sampleMimeType !in dolbyMimes }
                        try {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .addOverride(TrackSelectionOverride(aacGroup.mediaTrackGroup, trackIdx))
                                .build()
                        } catch (_: Exception) {}
                    }
                }

                applyAutoFrameRateIfPossible()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    applyAutoFrameRateIfPossible()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Resume dialog
        if (uiState.showResumeDialog) {
            val mins = uiState.resumePositionMs / 60_000
            AlertDialog(
            onDismissRequest = {
                resumeChoice = false
                viewModel.onResumeChoice(false)
            },
                title = { Text(stringResource(R.string.continue_watching_title)) },
                text = {
                    Text(stringResource(R.string.resume_message, mins))
                },
                confirmButton = {
                Button(onClick = {
                    resumeChoice = true
                    viewModel.onResumeChoice(true)
                }) {
                        Text(stringResource(R.string.continue_btn))
                    }
                },
                dismissButton = {
                OutlinedButton(onClick = {
                    resumeChoice = false
                    viewModel.onResumeChoice(false)
                }) {
                        Text(stringResource(R.string.start_over))
                    }
                },
        )
    }

    // Audio Track Selection Dialog
    if (showAudioTrackDialog) {
        AlertDialog(
            onDismissRequest = { showAudioTrackDialog = false },
            title = { Text(stringResource(R.string.audio_track)) },
            text = {
                Column {
                    if (uiState.audioTracks.isEmpty()) {
                        Text(
                            text = "Keine Audio-Spuren erkannt. Starte den Stream neu oder warte kurz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // Auto option
                        Text(
                            text = if (!uiState.audioTracks.any { it.isSelected }) "✓ Automatisch" else "  Automatisch",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (!uiState.audioTracks.any { it.isSelected }) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                        .build()
                                    viewModel.updateTracks(
                                        uiState.audioTracks.map { it.copy(isSelected = false) },
                                        uiState.subtitleTracks,
                                    )
                                    showAudioTrackDialog = false
                                }
                                .padding(vertical = 10.dp),
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        uiState.audioTracks.forEachIndexed { idx, track ->
                            Text(
                                text = if (track.isSelected) "✓ ${track.label}" else "  ${track.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (track.isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val (group, trackIdx) = audioTrackGroupMap.getOrNull(idx) ?: return@clickable
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                            .addOverride(TrackSelectionOverride(group, trackIdx))
                                            .build()
                                        viewModel.selectAudioTrack(idx)
                                        showAudioTrackDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                            )
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

    // Subtitle Track Selection Dialog
    if (showSubtitleTrackDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleTrackDialog = false },
            title = { Text(stringResource(R.string.subtitles)) },
            text = {
                Column {
                    // "Off" option
                    Text(
                        text = if (uiState.subtitleTracks.none { it.isSelected }) "✓ Aus" else "  Aus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.subtitleTracks.none { it.isSelected }) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED)
                                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                    .build()
                                viewModel.updateTracks(uiState.audioTracks, uiState.subtitleTracks.map { it.copy(isSelected = false) })
                                showSubtitleTrackDialog = false
                            }
                            .padding(vertical = 10.dp),
                    )
                    if (uiState.subtitleTracks.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        uiState.subtitleTracks.forEachIndexed { idx, track ->
                            Text(
                                text = if (track.isSelected) "✓ ${track.label}" else "  ${track.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (track.isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val (group, trackIdx) = subtitleTrackGroupMap.getOrNull(idx) ?: return@clickable
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                            .addOverride(TrackSelectionOverride(group, trackIdx))
                                            .build()
                                        viewModel.selectSubtitleTrack(idx)
                                        showSubtitleTrackDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleTrackDialog = false }) {
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
                        KeyEvent.KEYCODE_MENU -> {
                            showTvOptionsPanel = !showTvOptionsPanel
                            if (showTvOptionsPanel) {
                                tvOptionIndex = 0
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (showTvOptionsPanel) {
                                // Activate the focused option in the TV options panel
                                // indices: 0=Bildformat, 1=Ton, 2=Untertitel, 3=Sleeptimer, 4=Aufnahme(live)
                                when (tvOptionIndex) {
                                    0 -> viewModel.cycleAspectRatio()
                                    1 -> { showAudioTrackDialog = true; showTvOptionsPanel = false }
                                    2 -> { showSubtitleTrackDialog = true; showTvOptionsPanel = false }
                                    3 -> { showSleepTimerDialog = true; showTvOptionsPanel = false }
                                    4 -> {
                                        // Recording toggle (only reachable for Live TV)
                                        if (isRecording) {
                                            context.startService(
                                                Intent(context, RecordingService::class.java).apply {
                                                    action = RecordingService.ACTION_STOP
                                                }
                                            )
                                            isRecording = false
                                        } else {
                                            context.startCompatService(
                                                Intent(context, RecordingService::class.java).apply {
                                                    action = RecordingService.ACTION_START
                                                    putExtra(RecordingService.EXTRA_STREAM_URL, uiState.streamUrl)
                                                    putExtra(RecordingService.EXTRA_CHANNEL_NAME, uiState.title)
                                                    putExtra(RecordingService.EXTRA_CHANNEL_ID, uiState.contentId)
                                                }
                                            )
                                            isRecording = true
                                        }
                                        showTvOptionsPanel = false
                                    }
                                }
                            } else {
                                // Start long-press timer: 600ms → show TV options panel
                                val job = scope.launch {
                                    delay(600)
                                    tvOptionIndex = 0
                                    showTvOptionsPanel = true
                                }
                                longPressJob = job
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (uiState.contentType == WatchContentType.CHANNEL) {
                                viewModel.playPreviousChannel()
                            } else {
                                if (showTvOptionsPanel) {
                                    val count = if (uiState.contentType == WatchContentType.CHANNEL) 5 else 4
                                    tvOptionIndex = (tvOptionIndex - 1 + count) % count
                                } else {
                                    viewModel.toggleControls()
                                }
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (uiState.contentType == WatchContentType.CHANNEL) {
                                viewModel.playNextChannel()
                            } else {
                                if (showTvOptionsPanel) {
                                    val count = if (uiState.contentType == WatchContentType.CHANNEL) 5 else 4
                                    tvOptionIndex = (tvOptionIndex + 1) % count
                                } else {
                                    viewModel.toggleControls()
                                }
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (showTvOptionsPanel) {
                                val count = if (uiState.contentType == WatchContentType.CHANNEL) 5 else 4
                                tvOptionIndex = (tvOptionIndex - 1 + count) % count
                            } else {
                                exoPlayer?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
                                viewModel.showControls()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (showTvOptionsPanel) {
                                val count = if (uiState.contentType == WatchContentType.CHANNEL) 5 else 4
                                tvOptionIndex = (tvOptionIndex + 1) % count
                            } else {
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
                            }
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
                            if (showTvOptionsPanel) {
                                showTvOptionsPanel = false
                                true
                            } else {
                                false // Let back button navigate normally
                            }
                        }
                        else -> false
                    }
                } else if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            // Short press: job still active → toggle play/pause
                            val job = longPressJob
                            longPressJob = null
                            if (job != null && job.isActive) {
                                job.cancel()
                                exoPlayer?.let { it.playWhenReady = !it.isPlaying }
                                viewModel.showControls()
                            }
                            true
                        }
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val forcedAspectRatio = forcedAspectRatio(uiState.aspectRatio)
            val videoSurfaceModifier = when (forcedAspectRatio) {
                null -> Modifier.fillMaxSize()
                else -> {
                    val containerRatio = maxWidth / maxHeight
                    if (containerRatio > forcedAspectRatio) {
                        Modifier.fillMaxHeight().aspectRatio(forcedAspectRatio)
                    } else {
                        Modifier.fillMaxWidth().aspectRatio(forcedAspectRatio)
                    }
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        keepScreenOn = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        resizeMode = playerResizeMode(uiState.aspectRatio)
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = playerResizeMode(uiState.aspectRatio)
                },
                modifier = videoSurfaceModifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = uiState.videoScale
                        scaleY = uiState.videoScale
                    }
                    .pointerInput(uiState.videoScale) {
                        // Single-finger swipe gestures: brightness (left), volume (right), seek (horizontal)
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startX = down.position.x
                            val startY = down.position.y
                            var totalDx = 0f
                            var totalDy = 0f
                            var gestureDecided = false
                            var isHorizontal = false

                            do {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                if (changes.size == 1) {
                                    val pos = changes[0].position
                                    totalDx = pos.x - startX
                                    totalDy = pos.y - startY

                                    if (!gestureDecided && (kotlin.math.abs(totalDx) > 20f || kotlin.math.abs(totalDy) > 20f)) {
                                        gestureDecided = true
                                        isHorizontal = kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy)
                                    }

                                    if (gestureDecided) {
                                        if (isHorizontal) {
                                            // Seek: left = back, right = forward (100px = 10s)
                                            val seekDelta = (totalDx / 100f * 10f).toLong()
                                            gestureHudType = "seek"
                                            gestureHudSeekDelta = seekDelta
                                        } else {
                                            // Vertical: left side = brightness, right side = volume
                                            val fraction = (-totalDy / size.height.toFloat()).coerceIn(-1f, 1f)
                                            if (startX < size.width / 2f) {
                                                // Brightness
                                                val currentBr = activity?.window?.attributes?.screenBrightness
                                                    ?.takeIf { it >= 0f } ?: 0.5f
                                                val newBr = (currentBr + fraction * 0.5f).coerceIn(0.01f, 1f)
                                                activity?.window?.let { win ->
                                                    win.attributes = win.attributes.apply { screenBrightness = newBr }
                                                }
                                                gestureHudType = "brightness"
                                                gestureHudValue = newBr
                                            } else {
                                                // Volume
                                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                val newVol = (currentVol + (fraction * maxVolume * 0.5f).toInt())
                                                    .coerceIn(0, maxVolume)
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                gestureHudType = "volume"
                                                gestureHudValue = newVol.toFloat() / maxVolume
                                            }
                                        }
                                    }
                                } else if (changes.size >= 2) {
                                    // Pinch-to-zoom
                                    val p0 = changes[0].position
                                    val p1 = changes[1].position
                                    val dx2 = p1.x - p0.x; val dy2 = p1.y - p0.y
                                    val span = kotlin.math.sqrt(dx2 * dx2 + dy2 * dy2)
                                    val initialScale2 = uiState.videoScale
                                    val initialSpan2 = span
                                    if (initialSpan2 > 0f) {
                                        val scaleChange = span / initialSpan2
                                        val newScale = (initialScale2 * scaleChange).coerceIn(0.5f, 3.0f)
                                        viewModel.setVideoScale(newScale)
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            // Apply seek on gesture end
                            if (gestureDecided && isHorizontal && gestureHudSeekDelta != 0L) {
                                val seekMs = gestureHudSeekDelta * 1_000L
                                exoPlayer?.let { player ->
                                    val newPos = (player.currentPosition + seekMs).coerceAtLeast(0L)
                                    player.seekTo(newPos)
                                }
                            }
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

        AnimatedVisibility(
            visible = frameRateStatusMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = frameRateStatusMessage ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
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
                        // Recording button (Live TV only)
                        if (uiState.contentType == WatchContentType.CHANNEL) {
                            IconButton(onClick = {
                                if (isRecording) {
                                    context.startService(
                                        Intent(context, RecordingService::class.java).apply {
                                            action = RecordingService.ACTION_STOP
                                        }
                                    )
                                    isRecording = false
                                } else {
                                    context.startCompatService(
                                        Intent(context, RecordingService::class.java).apply {
                                            action = RecordingService.ACTION_START
                                            putExtra(RecordingService.EXTRA_STREAM_URL, uiState.streamUrl)
                                            putExtra(RecordingService.EXTRA_CHANNEL_NAME, uiState.title)
                                            putExtra(RecordingService.EXTRA_CHANNEL_ID, uiState.contentId)
                                        }
                                    )
                                    isRecording = true
                                }
                                viewModel.showControls()
                            }) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                    contentDescription = if (isRecording) "Aufnahme stoppen" else "Aufnahme starten",
                                    tint = if (isRecording) Color.Red else Color.White,
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
                            showSubtitleTrackDialog = true
                            viewModel.showControls()
                        }) {
                            Icon(
                                Icons.Default.Subtitles,
                                stringResource(R.string.subtitles),
                                tint = if (uiState.subtitleTracks.any { it.isSelected }) MaterialTheme.colorScheme.primary else Color.White,
                            )
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

        // Gesture HUD overlay (center, auto-hides after 1.5s)
        AnimatedVisibility(
            visible = gestureHudType.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            GestureHud(type = gestureHudType, value = gestureHudValue, seekDelta = gestureHudSeekDelta)
        }

        // Recording indicator (top-left, visible while recording)
        if (isRecording) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 72.dp, top = 16.dp)
                    .background(Color.Red.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.FiberManualRecord, null, tint = Color.White, modifier = Modifier.size(10.dp))
                val recH = recordingSeconds / 3600
                val recM = (recordingSeconds % 3600) / 60
                val recS = recordingSeconds % 60
                val recTime = if (recH > 0) "%d:%02d:%02d".format(recH, recM, recS) else "%02d:%02d".format(recM, recS)
                Text("REC  $recTime", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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

        // TV Options Panel — long-press center on Fire TV remote
        AnimatedVisibility(
            visible = showTvOptionsPanel,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .padding(horizontal = 32.dp, vertical = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val sleepTimerLabel = when {
                        uiState.sleepTimerActive -> {
                            val rem = uiState.sleepTimerRemainingSeconds
                            "${rem / 60}m ${rem % 60}s"
                        }
                        else -> "Aus"
                    }
                    val options = buildList {
                        add("Bildformat" to uiState.aspectRatio.label)
                        add("Ton" to (uiState.audioTracks.firstOrNull { it.isSelected }?.let { it.label ?: it.language } ?: "—"))
                        add("Untertitel" to (uiState.subtitleTracks.firstOrNull { it.isSelected }?.let { it.label ?: it.language } ?: "Aus"))
                        add("Sleeptimer" to sleepTimerLabel)
                        if (uiState.contentType == WatchContentType.CHANNEL) {
                            add("Aufnahme" to if (isRecording) "●  Läuft" else "Aus")
                        }
                    }
                    options.forEachIndexed { index, (title, value) ->
                        val isFocused = tvOptionIndex == index
                        val isRecordingTile = title == "Aufnahme" && isRecording
                        val isTimerActive = title == "Sleeptimer" && uiState.sleepTimerActive
                        val accentColor = when {
                            isRecordingTile -> Color.Red.copy(alpha = 0.85f)
                            isTimerActive && isFocused -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                            isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            else -> Color.White.copy(alpha = 0.08f)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor)
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = title,
                                color = if (isFocused || isRecordingTile) Color.White else Color.White.copy(alpha = 0.55f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = value,
                                color = if (isRecordingTile) Color.White else Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isFocused || isRecordingTile) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                Text(
                    text = "MENU oeffnet  •  ← → waehlen  •  OK bestaetigen  •  ↑ ↓ Sender wechseln  •  ZURUECK schliessen",
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
private fun playerResizeMode(aspectRatio: AspectRatio): Int {
    return when (aspectRatio) {
        AspectRatio.FIT_16_9,
        AspectRatio.FIT_4_3,
        AspectRatio.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        AspectRatio.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        AspectRatio.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }
}

private fun forcedAspectRatio(aspectRatio: AspectRatio): Float? {
    return when (aspectRatio) {
        AspectRatio.FIT_16_9 -> 16f / 9f
        AspectRatio.FIT_4_3 -> 4f / 3f
        else -> null
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Gesture HUD — shows brightness, volume or seek delta as a pill overlay.
 */
@Composable
private fun GestureHud(type: String, value: Float, seekDelta: Long) {
    val icon = when (type) {
        "brightness" -> "☀"
        "volume" -> "🔊"
        "seek" -> if (seekDelta >= 0) "⏩" else "⏪"
        else -> ""
    }
    val label = when (type) {
        "brightness" -> "${(value * 100).toInt()}%"
        "volume" -> "${(value * 100).toInt()}%"
        "seek" -> if (seekDelta >= 0) "+${seekDelta}s" else "${seekDelta}s"
        else -> ""
    }
    Column(
        modifier = Modifier
            .wrapContentSize()
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(icon, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        if (type == "brightness" || type == "volume") {
            LinearProgressIndicator(
                progress = { value },
                modifier = Modifier.width(100.dp).height(4.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
            )
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
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
