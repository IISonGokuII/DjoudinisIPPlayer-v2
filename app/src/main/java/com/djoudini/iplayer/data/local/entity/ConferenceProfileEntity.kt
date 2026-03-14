package com.djoudini.iplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conference_profiles")
data class ConferenceProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "cooldown_enabled")
    val cooldownEnabled: Boolean = true,
    @ColumnInfo(name = "cooldown_seconds")
    val cooldownSeconds: Int = 20,
    @ColumnInfo(name = "hold_seconds")
    val holdSeconds: Int = 20,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
