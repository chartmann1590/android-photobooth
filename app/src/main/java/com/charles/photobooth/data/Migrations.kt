package com.charles.photobooth.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Registry of Room schema migrations.
 *
 * To add a migration: bump `@Database(version = N)` in `AppDatabase`, write a
 * `Migration(N-1, N)` here, append it to `ALL`, and commit the new
 * `app/schemas/.../N.json` that the build emits. Without an entry here, release
 * builds will crash at startup rather than silently wipe user data.
 */
object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE photos ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'IMAGE'")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE photos ADD COLUMN filter TEXT NOT NULL DEFAULT 'NONE'")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
