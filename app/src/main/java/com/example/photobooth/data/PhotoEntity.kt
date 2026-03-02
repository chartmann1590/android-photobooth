package com.example.photobooth.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventName: String,
    val takenAtEpochSeconds: Long = System.currentTimeMillis() / 1_000L,
    val localPath: String,
    val uploadedUrl: String? = null,
    val templateId: Long? = null,
)

