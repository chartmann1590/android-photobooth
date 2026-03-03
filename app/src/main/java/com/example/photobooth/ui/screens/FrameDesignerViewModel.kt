package com.example.photobooth.ui.screens

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photobooth.data.AppDatabase
import com.example.photobooth.data.TemplateEntity
import com.example.photobooth.template.OUTPUT_4X6_HEIGHT
import com.example.photobooth.template.OUTPUT_4X6_WIDTH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FrameDesignerViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val templateDao = AppDatabase.getInstance(application).templateDao()

    val frames: StateFlow<List<TemplateEntity>> =
        templateDao.getAllTemplates().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun saveFrame(context: Context, name: String, imageUri: Uri, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val framesDir = File(context.filesDir, "frames")
                if (!framesDir.exists()) framesDir.mkdirs()

                val destFile = File(framesDir, "frame_${System.currentTimeMillis()}.png")

                // Decode source, scale to 4x6 output size, save as PNG
                val source = context.contentResolver.openInputStream(imageUri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return@withContext

                val scaled = Bitmap.createScaledBitmap(
                    source, OUTPUT_4X6_WIDTH, OUTPUT_4X6_HEIGHT, true,
                )
                if (scaled !== source) source.recycle()

                destFile.outputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                scaled.recycle()

                templateDao.insert(
                    TemplateEntity(
                        name = name.ifBlank { "Untitled Frame" },
                        backgroundImagePath = destFile.absolutePath,
                        layoutJson = "{}",
                    )
                )
            }
            onDone()
        }
    }

    fun deleteFrame(frame: TemplateEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Delete the image file
                frame.backgroundImagePath?.let { path ->
                    File(path).delete()
                }
                templateDao.deleteById(frame.id)
            }
        }
    }
}
