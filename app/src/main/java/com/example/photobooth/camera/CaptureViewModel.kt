package com.example.photobooth.camera

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photobooth.data.AppDatabase
import com.example.photobooth.data.PhotoEntity
import com.example.photobooth.template.GifEncoder
import com.example.photobooth.template.WatermarkConfig
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
        uri: Uri,
        eventName: String,
        templateId: Long?,
        selectedFrameId: Long?,
        watermarkConfig: WatermarkConfig? = null,
        filter: PhotoFilter = PhotoFilter.NONE,
        onComplete: (Long?) -> Unit,
    ) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                val outputDir = File(app.filesDir, "photos")
                if (!outputDir.exists() && !outputDir.mkdirs()) {
                    _uiState.value = CaptureUiState.Error("Failed to create photo directory")
                    onComplete(null)
                    return@launch
                }

                val destFile = File(outputDir, "photo_${System.currentTimeMillis()}.jpg")

                withContext(Dispatchers.IO) {
                    val exifRotation = app.contentResolver.openInputStream(uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL,
                        )
                    } ?: ExifInterface.ORIENTATION_NORMAL

                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    app.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, opts)
                    }

                    var sampleSize = 1
                    while (opts.outWidth / sampleSize > 2400 || opts.outHeight / sampleSize > 3600) {
                        sampleSize *= 2
                    }

                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val rawBitmap = app.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOpts)
                    } ?: throw IllegalStateException("Failed to decode photo")

                    try {
                        val corrected = applyExifRotation(rawBitmap, exifRotation)
                        if (corrected !== rawBitmap) rawBitmap.recycle()

                        val filtered = applyFilter(corrected, filter)
                        if (filtered !== corrected) corrected.recycle()

                        val frameOverlayPath = selectedFrameId?.let { id ->
                            templateDao.getTemplateByIdSync(id)?.backgroundImagePath
                        }

                        val processed = renderSimple4x6(filtered, frameOverlayPath, watermarkConfig)
                        if (processed !== filtered) filtered.recycle()

                        destFile.outputStream().use { out ->
                            processed.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }
                        processed.recycle()
                    } catch (e: OutOfMemoryError) {
                        rawBitmap.recycle()
                        throw IllegalStateException("Not enough memory to process photo. Try again.")
                    }
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

    fun createGifFromPhotos(
        photoIds: List<Long>,
        eventName: String,
        onComplete: (Long?) -> Unit,
    ) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    val photosById = photoDao.getPhotosByIds(photoIds).associateBy { it.id }
                    val orderedPhotos = photoIds.mapNotNull { photosById[it] }
                    if (orderedPhotos.size < 2) {
                        throw IllegalStateException("At least two photos are required to create a GIF")
                    }

                    val outputDir = File(app.filesDir, "photos")
                    if (!outputDir.exists() && !outputDir.mkdirs()) {
                        throw IllegalStateException("Failed to create photo directory")
                    }

                    val firstBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(orderedPhotos.first().localPath, firstBounds)
                    if (firstBounds.outWidth <= 0 || firstBounds.outHeight <= 0) {
                        throw IllegalStateException("Failed to read GIF source photo")
                    }

                    val targetWidth = minOf(640, firstBounds.outWidth)
                    val targetHeight = (firstBounds.outHeight * (targetWidth.toFloat() / firstBounds.outWidth)).toInt()
                        .coerceAtLeast(1)
                    val destFile = File(outputDir, "gif_${System.currentTimeMillis()}.gif")
                    destFile.outputStream().use { out ->
                        val encoder = GifEncoder(targetWidth, targetHeight, delayMs = 500)
                        encoder.start(out)
                        orderedPhotos.forEach { photo ->
                            val bitmap = BitmapFactory.decodeFile(photo.localPath)
                                ?: throw IllegalStateException("Failed to decode ${photo.localPath}")
                            try {
                                encoder.addFrame(bitmap)
                            } finally {
                                bitmap.recycle()
                            }
                        }
                        encoder.finish()
                    }

                    photoDao.insert(
                        PhotoEntity(
                            eventName = eventName,
                            localPath = destFile.absolutePath,
                            templateId = null,
                        ),
                    )
                }
                _uiState.value = CaptureUiState.Saved(id)
                onComplete(id)
            } catch (e: Exception) {
                _uiState.value = CaptureUiState.Error(e.message ?: "Failed to create GIF")
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
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
