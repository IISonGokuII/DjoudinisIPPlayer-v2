package com.djoudini.iplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * EPG program entry, matched to channels via tvg_id / epg_channel_id.
 * Supports XMLTV data with start/stop times.
 */
@Entity(
    tableName = "epg_programs",
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
        Index(value = ["epg_channel_id"]),
        Index(value = ["start_time"]),
        Index(value = ["stop_time"]),  // Für getCurrentProgram Queries
        Index(value = ["epg_channel_id", "start_time"]),
        Index(value = ["playlist_id", "epg_channel_id", "start_time"], unique = true),
        // Optimiert für getCurrentProgram: channel + time range lookup
        Index(value = ["playlist_id", "epg_channel_id", "stop_time"]),
        // Optimiert für batch queries im EPG Grid
        Index(value = ["playlist_id", "start_time", "stop_time"]),
    ]
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    /** Maps to Channel.tvgId for matching */
    @ColumnInfo(name = "epg_channel_id")
    val epgChannelId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    /** Epoch millis */
    @ColumnInfo(name = "start_time")
    val startTime: Long,

    /** Epoch millis */
    @ColumnInfo(name = "stop_time")
    val stopTime: Long,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "icon_url")
    val iconUrl: String? = null,

    @ColumnInfo(name = "language")
    val language: String? = null,

    /** Catch-up archive URL if available */
    @ColumnInfo(name = "catchup_url")
    val catchupUrl: String? = null,
)
