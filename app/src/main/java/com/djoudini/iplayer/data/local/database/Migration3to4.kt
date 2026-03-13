package com.djoudini.iplayer.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recordings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                channelName TEXT NOT NULL,
                channelId INTEGER NOT NULL,
                filePath TEXT NOT NULL,
                startTimeMs INTEGER NOT NULL,
                durationMs INTEGER NOT NULL DEFAULT 0,
                fileSizeBytes INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'recording'
            )
            """.trimIndent()
        )
    }
}
