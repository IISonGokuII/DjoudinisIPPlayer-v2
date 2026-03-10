package com.djoudini.iplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Individual episode within a series.
 */
@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["series_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["series_id"]),
        Index(value = ["playlist_id"]),
        Index(value = ["series_id", "season_number", "episode_number"]),
        Index(value = ["playlist_id", "remote_id"], unique = true),
    ]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    @ColumnInfo(name = "series_id")
    val seriesId: Long,

    @ColumnInfo(name = "remote_id")
    val remoteId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "stream_url")
    val streamUrl: String,

    @ColumnInfo(name = "container_extension")
    val containerExtension: String? = null,

    @ColumnInfo(name = "season_number")
    val seasonNumber: Int = 1,

    @ColumnInfo(name = "episode_number")
    val episodeNumber: Int = 1,

    @ColumnInfo(name = "cover_url")
    val coverUrl: String? = null,

    @ColumnInfo(name = "plot")
    val plot: String? = null,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int? = null,

    @ColumnInfo(name = "rating")
    val rating: Float? = null,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
)
