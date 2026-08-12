package com.charles.photobooth.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
    IMAGE,
    VIDEO,
}

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventName: String,
    val takenAtEpochSeconds: Long = System.currentTimeMillis() / 1_000L,
    val localPath: String,
    val uploadedUrl: String? = null,
    val templateId: Long? = null,
    val mediaType: MediaType = MediaType.IMAGE,
    val filter: String = "NONE",
)

