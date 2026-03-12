package com.djoudini.iplayer.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration von Version 2 auf 3:
 * Fügt is_favorite Spalte zur series Tabelle hinzu
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add is_favorite column with default value 0 (false)
        database.execSQL("ALTER TABLE series ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
    }
}
