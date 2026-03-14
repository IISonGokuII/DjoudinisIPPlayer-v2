package com.djoudini.iplayer.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS conference_profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                cooldown_enabled INTEGER NOT NULL DEFAULT 1,
                cooldown_seconds INTEGER NOT NULL DEFAULT 20,
                hold_seconds INTEGER NOT NULL DEFAULT 20,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS conference_match_mappings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                conference_id INTEGER NOT NULL,
                match_id INTEGER NOT NULL,
                competition_name TEXT NOT NULL,
                match_label TEXT NOT NULL,
                channel_id INTEGER NOT NULL,
                channel_name TEXT NOT NULL,
                priority INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(conference_id) REFERENCES conference_profiles(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_conference_match_mappings_conference_id ON conference_match_mappings(conference_id)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_conference_match_mappings_match_id ON conference_match_mappings(match_id)",
        )
    }
}
