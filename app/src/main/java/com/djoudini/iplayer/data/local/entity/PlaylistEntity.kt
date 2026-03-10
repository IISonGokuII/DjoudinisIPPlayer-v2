package com.djoudini.iplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a stored IPTV playlist/account.
 * Supports both Xtream Codes API connections and M3U file/URL sources.
 */
@Entity(
    tableName = "playlists",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["type"]),
    ]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    /** "xtream" or "m3u" */
    @ColumnInfo(name = "type")
    val type: String,

    // --- Xtream Codes fields ---
    @ColumnInfo(name = "server_url")
    val serverUrl: String? = null,

    @ColumnInfo(name = "username")
    val username: String? = null,

    @ColumnInfo(name = "password")
    val password: String? = null,

    // --- M3U fields ---
    @ColumnInfo(name = "m3u_url")
    val m3uUrl: String? = null,

    @ColumnInfo(name = "m3u_local_path")
    val m3uLocalPath: String? = null,

    // --- Account info ---
    @ColumnInfo(name = "expiration_date")
    val expirationDate: Long? = null,

    @ColumnInfo(name = "max_connections")
    val maxConnections: Int? = null,

    @ColumnInfo(name = "active_connections")
    val activeConnections: Int? = null,

    @ColumnInfo(name = "is_trial")
    val isTrial: Boolean = false,

    @ColumnInfo(name = "status")
    val status: String = "active",

    // --- Sync metadata ---
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = 0L,

    @ColumnInfo(name = "epg_url")
    val epgUrl: String? = null,

    @ColumnInfo(name = "epg_last_synced_at")
    val epgLastSyncedAt: Long = 0L,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
)
