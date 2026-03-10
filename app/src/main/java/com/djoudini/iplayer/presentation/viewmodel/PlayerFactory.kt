package com.djoudini.iplayer.presentation.viewmodel

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.djoudini.iplayer.data.local.entity.PlayerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating configured ExoPlayer instances.
 *
 * Features:
 * - User-Agent spoofing via custom HttpDataSource.Factory
 * - Configurable buffer sizes (LoadControl)
 * - Hardware/Software decoding toggle
 * - Tunneled playback support for Android TV
 * - Ready for multi-view (up to 4 instances)
 */
@Singleton
class PlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    /**
     * Creates a fully configured ExoPlayer instance.
     *
     * @param config Player configuration for buffer, UA, and decoding options.
     * @param userAgentOverride Per-channel UA override (takes priority over config).
     * @param videoScale Video scale factor for zoom (0.5f - 3.0f).
     * @return Configured [ExoPlayer] instance. Caller is responsible for releasing it.
     */
    @OptIn(UnstableApi::class)
    fun create(
        config: PlayerConfig = PlayerConfig(),
        userAgentOverride: String? = null,
        videoScale: Float = 1.0f,
    ): ExoPlayer {
        val effectiveUserAgent = userAgentOverride ?: config.userAgent

        // --- Network Layer: OkHttp-based DataSource with spoofed User-Agent ---
        val httpDataSourceFactory: DataSource.Factory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(effectiveUserAgent)
            .setDefaultRequestProperties(
                mapOf("User-Agent" to effectiveUserAgent)
            )

        // --- Buffer Control: Use preset or custom values ---
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                config.minBufferMs,
                config.maxBufferMs,
                config.bufferForPlaybackMs,
                config.bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // --- Renderer: Hardware vs Software Decoding ---
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                if (config.preferSoftwareDecoding) {
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                } else {
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                }
            )
            setEnableDecoderFallback(true)
        }

        // --- Track Selection ---
        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setTunnelingEnabled(config.enableTunneledPlayback)
                .setPreferredVideoMimeType(null) // Accept any
                .build()
        }

        // --- Media Source ---
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        // --- Build Player ---
        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }

    /**
     * Creates a lightweight player instance for multi-view (PiP / split-screen).
     * Reduced buffer to conserve memory when running 4 instances simultaneously.
     */
    @OptIn(UnstableApi::class)
    fun createForMultiView(
        config: PlayerConfig = PlayerConfig(),
        userAgentOverride: String? = null,
    ): ExoPlayer {
        val multiViewConfig = config.copy(
            minBufferMs = 5_000,
            maxBufferMs = 15_000,
            bufferForPlaybackMs = 1_500,
            bufferForPlaybackAfterRebufferMs = 3_000,
        )
        return create(multiViewConfig, userAgentOverride)
    }
}
