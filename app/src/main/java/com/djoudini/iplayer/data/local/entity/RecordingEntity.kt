package com.djoudini.iplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelName: String,
    val channelId: Long,
    /** MediaStore URI (Android 10+) or file path (Android 9-) as string */
    val filePath: String,
    val startTimeMs: Long,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    /** "recording" | "completed" | "failed" */
    val status: String = "recording",
)
