package com.djoudini.iplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Live TV channel within a category.
 * Stores stream URL, EPG mapping (tvg-id), and channel metadata.
 */
@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlist_id"]),
        Index(value = ["category_id"]),
        Index(value = ["playlist_id", "remote_id"], unique = true),
        Index(value = ["tvg_id"]),
        Index(value = ["is_favorite"]),
        // Optimized for getRecentlyWatched() query
        Index(value = ["playlist_id", "last_watched_at"]),
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    /** Remote stream ID from Xtream or generated hash */
    @ColumnInfo(name = "remote_id")
    val remoteId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "stream_url")
    val streamUrl: String,

    /** Logo/icon URL */
    @ColumnInfo(name = "logo_url")
    val logoUrl: String? = null,

    /** EPG mapping key from tvg-id attribute in M3U */
    @ColumnInfo(name = "tvg_id")
    val tvgId: String? = null,

    @ColumnInfo(name = "tvg_name")
    val tvgName: String? = null,

    /** Container extension: ts, m3u8, mpegts, etc. */
    @ColumnInfo(name = "container_extension")
    val containerExtension: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    /** Custom user-agent for this specific channel, if needed */
    @ColumnInfo(name = "user_agent")
    val userAgent: String? = null,

    /** Catch-up / Timeshift support type: "default", "append", "shift", or null */
    @ColumnInfo(name = "catchup_type")
    val catchupType: String? = null,

    @ColumnInfo(name = "catchup_source")
    val catchupSource: String? = null,

    @ColumnInfo(name = "catchup_days")
    val catchupDays: Int? = null,

    @ColumnInfo(name = "last_watched_at")
    val lastWatchedAt: Long? = null,
)
