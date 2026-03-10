package com.djoudini.iplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Category (group) within a playlist. Supports Live, VOD, and Series types.
 * Only user-selected categories are persisted (Smart Onboarding filter).
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        // Optimiert für observeByType mit is_selected Filter und sort_order, name Sortierung
        Index(value = ["playlist_id", "category_type", "is_selected", "sort_order", "name"]),
        Index(value = ["playlist_id", "remote_id"], unique = true),
        // Optimiert für observeSelected
        Index(value = ["playlist_id", "is_selected", "sort_order"]),
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    /** Remote category ID from Xtream API or generated hash for M3U group-title */
    @ColumnInfo(name = "remote_id")
    val remoteId: String,

    @ColumnInfo(name = "name")
    val name: String,

    /** "live", "vod", or "series" */
    @ColumnInfo(name = "category_type")
    val categoryType: String,

    /** Whether user selected this category during onboarding filter */
    @ColumnInfo(name = "is_selected")
    val isSelected: Boolean = true,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "parent_id")
    val parentId: Long? = null,
)
