package com.djoudini.iplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks watch progress for resume-playback.
 * Stores second-accurate position for any stream (live, VOD, episode).
 * Also holds Trakt.tv sync state.
 */
@Entity(
    tableName = "watch_progress",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlist_id"]),
        Index(value = ["playlist_id", "content_type", "content_id"], unique = true),
        // Optimiert für observeContinueWatching: Filter nach is_completed, Sortierung nach last_watched_at
        Index(value = ["playlist_id", "is_completed", "last_watched_at"]),
        Index(value = ["last_watched_at"]),
        // Optimiert für Trakt-Sync
        Index(value = ["trakt_synced"]),
    ]
)
data class WatchProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    /** "channel", "vod", or "episode" */
    @ColumnInfo(name = "content_type")
    val contentType: String,

    /** ID of the channel, VOD item, or episode */
    @ColumnInfo(name = "content_id")
    val contentId: Long,

    /** Display name of the content (movie title, episode name, channel name) */
    @ColumnInfo(name = "content_name", defaultValue = "")
    val contentName: String = "",

    /** Poster/logo URL for display in continue watching */
    @ColumnInfo(name = "poster_url", defaultValue = "")
    val posterUrl: String = "",

    /** Current playback position in milliseconds */
    @ColumnInfo(name = "position_ms")
    val positionMs: Long = 0L,

    /** Total duration in milliseconds (0 for live streams) */
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0L,

    /** 0.0 to 1.0 progress percentage */
    @ColumnInfo(name = "progress_percent")
    val progressPercent: Float = 0f,

    /** Whether this item has been watched to completion (>= 90%) */
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "last_watched_at")
    val lastWatchedAt: Long = System.currentTimeMillis(),

    // --- Trakt.tv sync ---
    @ColumnInfo(name = "trakt_synced")
    val traktSynced: Boolean = false,

    @ColumnInfo(name = "trakt_id")
    val traktId: Int? = null,
)
