package com.example.photobooth.camera

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photobooth.data.AppDatabase
import com.example.photobooth.data.PhotoEntity
import com.example.photobooth.template.renderSimple4x6
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface CaptureUiState {
    data object Idle : CaptureUiState
    data class CountingDown(val secondsLeft: Int) : CaptureUiState
    data object Capturing : CaptureUiState
    data class Saved(val photoId: Long) : CaptureUiState
    data class Error(val message: String) : CaptureUiState
}

class CaptureViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState

    private val db = AppDatabase.getInstance(application)
    private val photoDao = db.photoDao()
    private val templateDao = db.templateDao()

    fun resetToIdle() {
        _uiState.value = CaptureUiState.Idle
    }

    fun startCountdown() {
        if (_uiState.value is CaptureUiState.CountingDown || _uiState.value is CaptureUiState.Capturing) {
            return
        }
        _uiState.value = CaptureUiState.CountingDown(3)
    }

    fun tickCountdown(nextSeconds: Int) {
        if (nextSeconds > 0) {
            _uiState.value = CaptureUiState.CountingDown(nextSeconds)
        } else {
            _uiState.value = CaptureUiState.Capturing
        }
    }

    fun saveCapturedPhoto(
        context: Context,
        uri: Uri,
        eventName: String,
        templateId: Long?,
        selectedFrameId: Long?,
        onComplete: (Long?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val outputDir = File(context.filesDir, "photos")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                val destFile = File(outputDir, "photo_${System.currentTimeMillis()}.jpg")

                withContext(Dispatchers.IO) {
                    // Read EXIF rotation from the original file
                    val exifRotation = context.contentResolver.openInputStream(uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL,
                        )
                    } ?: ExifInterface.ORIENTATION_NORMAL

                    // Decode with downsampling for very large images
                    val rawBytes = context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes()
                    } ?: throw IllegalStateException("Cannot read captured photo")

                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)

                    // Downsample if much larger than 1200x1800 target
                    var sampleSize = 1
                    while (opts.outWidth / sampleSize > 2400 || opts.outHeight / sampleSize > 3600) {
                        sampleSize *= 2
                    }

                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val rawBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                        ?: throw IllegalStateException("Failed to decode photo")

                    // Apply EXIF rotation so pixels match actual orientation
                    val corrected = applyExifRotation(rawBitmap, exifRotation)
                    if (corrected !== rawBitmap) rawBitmap.recycle()

                    // Look up frame overlay path if a frame is selected
                    val frameOverlayPath = selectedFrameId?.let { id ->
                        templateDao.getTemplateByIdSync(id)?.backgroundImagePath
                    }

                    val processed = renderSimple4x6(corrected, frameOverlayPath)
                    if (processed !== corrected) corrected.recycle()

                    destFile.outputStream().use { out ->
                        processed.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    processed.recycle()
                }

                val entity = PhotoEntity(
                    eventName = eventName,
                    localPath = destFile.absolutePath,
                    templateId = templateId,
                )
                val id = photoDao.insert(entity)
                _uiState.value = CaptureUiState.Saved(id)
                onComplete(id)
            } catch (e: Exception) {
                _uiState.value = CaptureUiState.Error(e.message ?: "Failed to save photo")
                onComplete(null)
            }
        }
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap // NORMAL or UNDEFINED — no rotation needed
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
