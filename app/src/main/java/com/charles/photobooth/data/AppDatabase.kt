package com.charles.photobooth.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.charles.photobooth.BuildConfig

@Database(
    entities = [
        PhotoEntity::class,
        TemplateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "photobooth.db",
                )
                    .addMigrations(*Migrations.ALL)
                    // Debug only: schema mismatches wipe the DB so iterating on the
                    // schema is fast. Release builds intentionally crash instead — a
                    // noisy startup failure is recoverable; silent data loss is not.
                    .apply {
                        if (BuildConfig.DEBUG) fallbackToDestructiveMigration(true)
                    }
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
