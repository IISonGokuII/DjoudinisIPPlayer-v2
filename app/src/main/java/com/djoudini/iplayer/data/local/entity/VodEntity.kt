package com.djoudini.iplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Video-on-Demand entry (Movie or single episode).
 * Linked to a category and playlist.
 */
@Entity(
    tableName = "vod_items",
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
        Index(value = ["tmdb_id"]),
        Index(value = ["is_favorite"]),
    ]
)
data class VodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "remote_id")
    val remoteId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "stream_url")
    val streamUrl: String,

    @ColumnInfo(name = "container_extension")
    val containerExtension: String? = null,

    @ColumnInfo(name = "logo_url")
    val logoUrl: String? = null,

    @ColumnInfo(name = "plot")
    val plot: String? = null,

    @ColumnInfo(name = "cast")
    val cast: String? = null,

    @ColumnInfo(name = "director")
    val director: String? = null,

    @ColumnInfo(name = "genre")
    val genre: String? = null,

    @ColumnInfo(name = "release_date")
    val releaseDate: String? = null,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int? = null,

    @ColumnInfo(name = "rating")
    val rating: Float? = null,

    /** TMDB ID for Trakt.tv sync */
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int? = null,

    @ColumnInfo(name = "imdb_id")
    val imdbId: String? = null,

    @ColumnInfo(name = "year")
    val year: Int? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
)
