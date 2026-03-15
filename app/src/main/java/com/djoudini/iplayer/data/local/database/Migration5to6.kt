package com.djoudini.iplayer.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE recordings ADD COLUMN cloudProvider TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE recordings ADD COLUMN cloudStatus TEXT NOT NULL DEFAULT 'local'")
        database.execSQL("ALTER TABLE recordings ADD COLUMN cloudRemotePath TEXT")
        database.execSQL("ALTER TABLE recordings ADD COLUMN cloudError TEXT")
        database.execSQL("ALTER TABLE recordings ADD COLUMN cloudUploadedAt INTEGER")
    }
}
