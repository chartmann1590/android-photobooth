package com.charles.photobooth.camera

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charles.photobooth.data.AppDatabase
import com.charles.photobooth.data.MediaType
import com.charles.photobooth.data.PhotoEntity
import com.charles.photobooth.network.AnonymousUploader
import com.charles.photobooth.network.ImageUploader
import com.charles.photobooth.network.ImmichUploader
import com.charles.photobooth.printing.ThermalPrinterClient
import com.charles.photobooth.settings.ThermalPrinterSettings
import com.charles.photobooth.settings.UploadSettings
import com.charles.photobooth.template.GifEncoder
import com.charles.photobooth.template.TemplateDefinition
import com.charles.photobooth.template.TemplateRenderer
import com.charles.photobooth.template.WatermarkConfig
import com.charles.photobooth.template.renderSimple4x6
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal fun photoOutputDir(app: Application): File? {
    val external = app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val target = external ?: File(app.filesDir, "photos")
    return if (target.exists() || target.mkdirs()) target else null
}

internal fun videoOutputDir(app: Application): File? {
    val external = app.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
    val target = external ?: File(app.filesDir, "videos")
    return if (target.exists() || target.mkdirs()) target else null
}

sealed interface CaptureUiState {
    data object Idle : CaptureUiState
    data class CountingDown(val secondsLeft: Int) : CaptureUiState
    data object Capturing : CaptureUiState
    data class Saved(val photoId: Long) : CaptureUiState
    data class Preview(
        val photoId: Long,
        val photoPath: String,
        val mediaType: MediaType = MediaType.IMAGE,
        val uploadStatus: UploadStatus,
    ) : CaptureUiState
    data class Error(val message: String) : CaptureUiState
}

sealed interface UploadStatus {
    data object Uploading : UploadStatus
    data class Complete(val url: String) : UploadStatus
    data class Failed(val message: String) : UploadStatus
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

    fun enterPreviewAndUpload(photoId: Long, uploadSettings: UploadSettings) {
        viewModelScope.launch {
            val photo = photoDao.getPhotosByIds(listOf(photoId)).firstOrNull()
            if (photo == null) {
                _uiState.value = CaptureUiState.Error("Photo not found for preview")
                return@launch
            }
            _uiState.value = CaptureUiState.Preview(
                photoId = photo.id,
                photoPath = photo.localPath,
                mediaType = photo.mediaType,
                uploadStatus = UploadStatus.Uploading,
            )
            try {
                val uploader = uploaderFor(uploadSettings)
                val file = File(photo.localPath)
                val url = withContext(Dispatchers.IO) { uploader.upload(file) }
                if (url.isBlank()) {
                    updatePreviewStatus(photoId, UploadStatus.Failed("Upload returned empty URL"))
                    return@launch
                }
                photoDao.updateUploadedUrl(photo.id, url)
                updatePreviewStatus(photoId, UploadStatus.Complete(url))
            } catch (e: Exception) {
                updatePreviewStatus(photoId, UploadStatus.Failed(e.message ?: "Upload failed"))
            }
        }
    }

    private fun updatePreviewStatus(photoId: Long, status: UploadStatus) {
        val current = _uiState.value
        if (current is CaptureUiState.Preview && current.photoId == photoId) {
            _uiState.value = current.copy(uploadStatus = status)
        }
    }

    private fun uploaderFor(uploadSettings: UploadSettings): ImageUploader =
        if (!uploadSettings.useAnonymousHost && uploadSettings.isImmichConfigured) {
            ImmichUploader(uploadSettings)
        } else {
            AnonymousUploader()
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
        thermalPrinterSettings: ThermalPrinterSettings = ThermalPrinterSettings(),
        onComplete: (Long?) -> Unit,
    ) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                val outputDir = photoOutputDir(app)
                if (outputDir == null) {
                    _uiState.value = CaptureUiState.Error("Failed to create photo directory")
                    onComplete(null)
                    return@launch
                }

                val destFile = File(outputDir, "photo_${System.currentTimeMillis()}.jpg")

                withContext(Dispatchers.IO) {
                    val exifRotation = app.contentResolver.openInputStream(uri)?.use { stream ->
                        androidx.exifinterface.media.ExifInterface(stream).getAttributeInt(
                            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL,
                        )
                    } ?: androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL

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
                    filter = filter.name,
                )
                val id = photoDao.insert(entity)
                _uiState.value = CaptureUiState.Saved(id)
                if (thermalPrinterSettings.enabled && thermalPrinterSettings.autoPrintAfterCapture &&
                    thermalPrinterSettings.deviceAddress.isNotBlank()
                ) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val bm = BitmapFactory.decodeFile(destFile.absolutePath)
                        bm?.let { ThermalPrinterClient(thermalPrinterSettings).print(it) }
                        bm?.recycle()
                    }
                }
                onComplete(id)
            } catch (e: Exception) {
                _uiState.value = CaptureUiState.Error(e.message ?: "Failed to save photo")
                onComplete(null)
            }
        }
    }

    fun saveCapturedVideo(
        videoFile: File,
        eventName: String,
        onComplete: (Long?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.i(
                    "VideoSave",
                    "saveCapturedVideo path=${videoFile.absolutePath} exists=${videoFile.exists()} size=${if (videoFile.exists()) videoFile.length() else -1L}",
                )
                if (!videoFile.exists() || videoFile.length() <= 0L) {
                    _uiState.value = CaptureUiState.Error("Failed to save video")
                    onComplete(null)
                    return@launch
                }
                val entity = PhotoEntity(
                    eventName = eventName,
                    localPath = videoFile.absolutePath,
                    templateId = null,
                    mediaType = MediaType.VIDEO,
                )
                val id = photoDao.insert(entity)
                _uiState.value = CaptureUiState.Saved(id)
                onComplete(id)
            } catch (e: Exception) {
                _uiState.value = CaptureUiState.Error(e.message ?: "Failed to save video")
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
        android.util.Log.i("GifFlow", "createGifFromPhotos called with ${photoIds.size} photoIds=$photoIds")
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    val photosById = photoDao.getPhotosByIds(photoIds).associateBy { it.id }
                    val orderedPhotos = photoIds.mapNotNull { photosById[it] }
                    android.util.Log.i("GifFlow", "Resolved ${orderedPhotos.size}/${photoIds.size} photos from DB")
                    if (orderedPhotos.size < 2) {
                        throw IllegalStateException("At least two photos are required to create a GIF")
                    }

                    val outputDir = photoOutputDir(app)
                        ?: throw IllegalStateException("Failed to create photo directory")

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
                            val raw = BitmapFactory.decodeFile(photo.localPath)
                                ?: throw IllegalStateException("Failed to decode ${photo.localPath}")
                            val rotated = applyExifRotation(raw, readExifOrientation(photo.localPath))
                            try {
                                encoder.addFrame(rotated)
                            } finally {
                                if (rotated !== raw) raw.recycle()
                                rotated.recycle()
                            }
                        }
                        encoder.finish()
                    }

                    // Sidecar file listing the source frame paths. The gallery uses this for
                    // in-app animated playback because our hand-rolled GIF encoder produces
                    // files that both ImageDecoder and Movie reject. The GIF file itself
                    // remains valid for export/sharing.
                    val sidecar = File(outputDir, destFile.name + ".frames.txt")
                    sidecar.writeText(orderedPhotos.joinToString("\n") { it.localPath })

                    photoDao.insert(
                        PhotoEntity(
                            eventName = eventName,
                            localPath = destFile.absolutePath,
                            templateId = null,
                        ),
                    )
                }
                android.util.Log.i("GifFlow", "GIF inserted into DB with id=$id")
                _uiState.value = CaptureUiState.Saved(id)
                onComplete(id)
            } catch (e: Exception) {
                android.util.Log.e("GifFlow", "createGifFromPhotos failed", e)
                _uiState.value = CaptureUiState.Error(e.message ?: "Failed to create GIF")
                onComplete(null)
            }
        }
    }

    fun composeTemplate(
        photoIds: List<Long>,
        template: TemplateDefinition,
        eventName: String,
        onComplete: (Long?) -> Unit,
    ) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    val photosById = photoDao.getPhotosByIds(photoIds).associateBy { it.id }
                    val orderedPhotos = photoIds.mapNotNull { photosById[it] }
                    if (orderedPhotos.isEmpty()) {
                        throw IllegalStateException("No photos available to compose template")
                    }

                    val outputDir = photoOutputDir(app)
                        ?: throw IllegalStateException("Failed to create photo directory")

                    val bitmaps = orderedPhotos.map { photo ->
                        val raw = BitmapFactory.decodeFile(photo.localPath)
                            ?: throw IllegalStateException("Failed to decode ${photo.localPath}")
                        val rotated = applyExifRotation(raw, readExifOrientation(photo.localPath))
                        if (rotated !== raw) raw.recycle()
                        rotated
                    }
                    val compositeId = try {
                        val composite = TemplateRenderer(app).render(template, bitmaps)
                        val destFile = File(outputDir, "template_${System.currentTimeMillis()}.jpg")
                        try {
                            destFile.outputStream().use { out ->
                                composite.compress(Bitmap.CompressFormat.JPEG, 95, out)
                            }
                        } finally {
                            composite.recycle()
                        }
                        photoDao.insert(
                            PhotoEntity(
                                eventName = eventName,
                                localPath = destFile.absolutePath,
                                templateId = null,
                            ),
                        )
                    } finally {
                        bitmaps.forEach { it.recycle() }
                    }

                    // The composite is the finished photo; remove the raw source photos
                    // so the gallery shows only the framed result instead of the un-framed
                    // intermediates the user never asked for.
                    orderedPhotos.forEach { photo ->
                        runCatching { File(photo.localPath).delete() }
                        runCatching { photoDao.deleteById(photo.id) }
                    }

                    compositeId
                }
                _uiState.value = CaptureUiState.Saved(id)
                onComplete(id)
            } catch (e: Exception) {
                android.util.Log.e("CaptureViewModel", "composeTemplate failed", e)
                _uiState.value = CaptureUiState.Error(e.message ?: "Failed to compose template")
                onComplete(null)
            }
        }
    }

}
