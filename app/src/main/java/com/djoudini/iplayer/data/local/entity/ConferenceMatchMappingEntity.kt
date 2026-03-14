package com.djoudini.iplayer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conference_match_mappings",
    foreignKeys = [
        ForeignKey(
            entity = ConferenceProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["conference_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["conference_id"]),
        Index(value = ["match_id"]),
    ],
)
data class ConferenceMatchMappingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "conference_id")
    val conferenceId: Long,
    @ColumnInfo(name = "match_id")
    val matchId: Int,
    @ColumnInfo(name = "competition_name")
    val competitionName: String,
    @ColumnInfo(name = "match_label")
    val matchLabel: String,
    @ColumnInfo(name = "channel_id")
    val channelId: Long,
    @ColumnInfo(name = "channel_name")
    val channelName: String,
    @ColumnInfo(name = "priority")
    val priority: Int = 0,
)
