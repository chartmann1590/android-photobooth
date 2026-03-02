package com.example.photobooth.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val backgroundImagePath: String?, // nullable for solid color/background-less templates
    val layoutJson: String, // JSON describing frames and overlays
    val isBuiltIn: Boolean = false,
)

