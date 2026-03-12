package com.djoudini.iplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Series metadata. Episodes are stored as VodEntity items linked via series_id.
 */
@Entity(
    tableName = "series",
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
        // Optimiert für observeByCategory mit ORDER BY name
        Index(value = ["category_id", "name"]),
    ]
)
data class SeriesEntity(
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

    @ColumnInfo(name = "cover_url")
    val coverUrl: String? = null,

    @ColumnInfo(name = "plot")
    val plot: String? = null,

    @ColumnInfo(name = "cast")
    val cast: String? = null,

    @ColumnInfo(name = "genre")
    val genre: String? = null,

    @ColumnInfo(name = "release_date")
    val releaseDate: String? = null,

    @ColumnInfo(name = "rating")
    val rating: Float? = null,

    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int? = null,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
)
