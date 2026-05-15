package com.charles.photobooth.data

import androidx.room.migration.Migration

/**
 * Registry of Room schema migrations.
 *
 * To add a migration: bump `@Database(version = N)` in `AppDatabase`, write a
 * `Migration(N-1, N)` here, append it to `ALL`, and commit the new
 * `app/schemas/.../N.json` that the build emits. Without an entry here, release
 * builds will crash at startup rather than silently wipe user data.
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf()
}
