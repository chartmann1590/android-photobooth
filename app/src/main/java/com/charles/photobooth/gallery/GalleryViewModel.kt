package com.charles.photobooth.gallery

import android.app.Application
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charles.photobooth.BuildConfig
import com.charles.photobooth.data.AppDatabase
import com.charles.photobooth.data.MediaType
import com.charles.photobooth.data.PhotoEntity
import com.charles.photobooth.network.AnonymousUploader
import com.charles.photobooth.network.ImmichUploader
import com.charles.photobooth.network.ImageUploader
import com.charles.photobooth.network.WeddingUploaderProvider
import com.charles.photobooth.network.FirebaseWeddingEmailSender
import com.charles.photobooth.network.SmtpEmailClient
import com.charles.photobooth.network.SmsGatewayClient
import com.charles.photobooth.printing.ThermalPrinterClient
import com.charles.photobooth.settings.ShareSettings
import com.charles.photobooth.settings.SettingsRepository
import com.charles.photobooth.settings.ThermalPrinterSettings
import com.charles.photobooth.template.renderSimple4x6
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import java.io.File

sealed interface GalleryActionState {
    data object Idle : GalleryActionState
    data object Uploading : GalleryActionState
    data object Sending : GalleryActionState
    data class Success(val message: String) : GalleryActionState
    data class Error(val message: String) : GalleryActionState
}

enum class GalleryAction {
    UPLOAD,
    QR_CODE,
    EMAIL,
    SMS,
    PRINT,
    THERMAL_PRINT,
    ANDROID_SHARE,
    DELETE,
}

fun availableGalleryActions(
    photo: PhotoEntity,
    shareSettings: ShareSettings,
    thermalPrinterSettings: ThermalPrinterSettings = ThermalPrinterSettings(),
): Set<GalleryAction> {
    val actions = mutableSetOf(GalleryAction.UPLOAD, GalleryAction.DELETE)
    if (photo.uploadedUrl != null) {
        actions.add(GalleryAction.QR_CODE)
    }
    if (photo.mediaType == MediaType.VIDEO) {
        return actions
    }
    actions.add(GalleryAction.ANDROID_SHARE)
    if (shareSettings.enableEmailShare) actions.add(GalleryAction.EMAIL)
    if (shareSettings.enableSmsShare) actions.add(GalleryAction.SMS)
    if (shareSettings.enablePrintShare) actions.add(GalleryAction.PRINT)
    if (thermalPrinterSettings.enabled && thermalPrinterSettings.deviceAddress.isNotBlank()) {
        actions.add(GalleryAction.THERMAL_PRINT)
    }
    return actions
}

class GalleryViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val photoDao = db.photoDao()
    private val settingsRepo = SettingsRepository(application)

    private fun getUploader(): ImageUploader {
        val uploadSettings = settingsRepo.getCurrentSettingsBlocking().upload
        return if (BuildConfig.WEDDING_MODE) {
            WeddingUploaderProvider.uploaderOrNull() ?: AnonymousUploader()
        } else if (!uploadSettings.useAnonymousHost && uploadSettings.isImmichConfigured) {
            ImmichUploader(uploadSettings)
        } else {
            AnonymousUploader()
        }
    }

    val photos: StateFlow<List<PhotoEntity>> =
        photoDao.getAllPhotos()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    val shareSettings: StateFlow<ShareSettings> =
        settingsRepo.settingsFlow
            .map { it.share }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ShareSettings(),
            )

    val thermalPrinterSettings: StateFlow<ThermalPrinterSettings> =
        settingsRepo.settingsFlow
            .map { it.thermalPrinter }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ThermalPrinterSettings(),
            )

    private val _actionState = MutableStateFlow<GalleryActionState>(GalleryActionState.Idle)
    val actionState: StateFlow<GalleryActionState> = _actionState

    fun uploadPhoto(photo: PhotoEntity) {
        if (_actionState.value is GalleryActionState.Uploading || _actionState.value is GalleryActionState.Sending) return
        viewModelScope.launch {
            try {
                _actionState.value = GalleryActionState.Uploading
                val file = File(photo.localPath)
                if (!file.exists()) {
                    _actionState.value = GalleryActionState.Error("Media file not found")
                    return@launch
                }
                val uploader = getUploader()
                val url = uploader.upload(file)
                if (url.isBlank()) {
                    _actionState.value = GalleryActionState.Error("Upload returned empty URL")
                    return@launch
                }
                photoDao.updateUploadedUrl(photo.id, url)
                _actionState.value = GalleryActionState.Idle
            } catch (e: Exception) {
                _actionState.value = GalleryActionState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun sendPhotoByEmail(photo: PhotoEntity, to: String) {
        if (_actionState.value is GalleryActionState.Uploading || _actionState.value is GalleryActionState.Sending) return
        viewModelScope.launch {
            try {
                if (photo.mediaType == MediaType.VIDEO) {
                    _actionState.value = GalleryActionState.Error("Videos cannot be emailed")
                    return@launch
                }
                _actionState.value = GalleryActionState.Sending

                val currentPhoto = photoDao.getPhotosByIds(listOf(photo.id)).firstOrNull() ?: photo
                var url = currentPhoto.uploadedUrl

                if (url.isNullOrBlank()) {
                    _actionState.value = GalleryActionState.Uploading
                    val settings = settingsRepo.getCurrentSettings()
                    val uploadSettings = if (BuildConfig.WEDDING_MODE) {
                        settings.upload.copy(autoUploadEnabled = true, useAnonymousHost = true)
                    } else settings.upload

                    val uploader = if (BuildConfig.WEDDING_MODE) {
                        WeddingUploaderProvider.uploaderOrNull() ?: AnonymousUploader()
                    } else if (!uploadSettings.useAnonymousHost && uploadSettings.isImmichConfigured) {
                        ImmichUploader(uploadSettings)
                    } else AnonymousUploader()

                    val file = File(currentPhoto.localPath)
                    val uploadedUrl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        uploader.upload(file)
                    }
                    if (uploadedUrl.isNotBlank()) {
                        photoDao.updateUploadedUrl(currentPhoto.id, uploadedUrl)
                        url = uploadedUrl
                    }
                }

                if (url.isNullOrBlank()) {
                    _actionState.value = GalleryActionState.Error("Failed to get photo upload URL for email")
                    return@launch
                }

                _actionState.value = GalleryActionState.Sending
                var sentSuccess = false

                if (BuildConfig.WEDDING_MODE) {
                    val file = File(currentPhoto.localPath)
                    val smtpSettings = com.charles.photobooth.settings.SmtpSettings(
                        host = BuildConfig.WEDDING_SMTP_HOST,
                        port = BuildConfig.WEDDING_SMTP_PORT,
                        useSslTls = BuildConfig.WEDDING_SMTP_USE_TLS,
                        username = BuildConfig.WEDDING_SMTP_USERNAME,
                        password = BuildConfig.WEDDING_SMTP_PASSWORD,
                        fromAddress = BuildConfig.WEDDING_SMTP_FROM_ADDRESS,
                        fromName = BuildConfig.WEDDING_SMTP_FROM_NAME,
                        defaultSubjectTemplate = "Your Photobooth Picture from {eventName}",
                        defaultBodyTemplate = "Thank you for celebrating with us! Here is your photobooth picture.\n\nYou can also view the full gallery here:\n{galleryUrl}"
                    )
                    runCatching {
                        val client = SmtpEmailClient(getApplication(), smtpSettings)
                        val passwordNotice = if (BuildConfig.WEDDING_GALLERY_PASSWORD.isNotBlank()) {
                            "\n\nGallery Password: ${BuildConfig.WEDDING_GALLERY_PASSWORD}"
                        } else ""
                        val subject = smtpSettings.defaultSubjectTemplate.replace("{eventName}", currentPhoto.eventName)
                        val galleryUrlStr = if (BuildConfig.WEDDING_GALLERY_URL.isNotBlank()) BuildConfig.WEDDING_GALLERY_URL else (url ?: "")
                        val body = smtpSettings.defaultBodyTemplate.replace("{galleryUrl}", galleryUrlStr) + passwordNotice
                        client.sendPhotoEmail(to, subject, body, file)
                        sentSuccess = true
                    }.onFailure {
                        android.util.Log.e("GalleryViewModel", "SMTP email send failed", it)
                        _actionState.value = GalleryActionState.Error("Failed to send email: ${it.message}")
                        return@launch
                    }
                } else {
                    val file = File(currentPhoto.localPath)
                    val smtpSettings = settingsRepo.getCurrentSettings().smtp
                    if (smtpSettings.host.isNotBlank()) {
                        runCatching {
                            val client = SmtpEmailClient(getApplication(), smtpSettings)
                            val subject = smtpSettings.defaultSubjectTemplate.replace("{eventName}", currentPhoto.eventName)
                            val body = smtpSettings.defaultBodyTemplate.replace("{eventName}", currentPhoto.eventName)
                            client.sendPhotoEmail(to, subject, body, file)
                            sentSuccess = true
                        }
                    }
                }

                if (sentSuccess) {
                    _actionState.value = GalleryActionState.Success("Email sent to $to!")
                } else {
                    val errMsg = "Failed to send email. Check SMTP settings in Settings."
                    _actionState.value = GalleryActionState.Error(errMsg)
                }
            } catch (e: Exception) {
                _actionState.value = GalleryActionState.Error(e.message ?: "Email failed")
            }
        }
    }

    fun sendPhotoBySms(photo: PhotoEntity, phone: String) {
        if (_actionState.value is GalleryActionState.Uploading || _actionState.value is GalleryActionState.Sending) return
        viewModelScope.launch {
            try {
                if (photo.mediaType == MediaType.VIDEO) {
                    _actionState.value = GalleryActionState.Error("Videos cannot be sent by SMS")
                    return@launch
                }
                _actionState.value = GalleryActionState.Sending
                val currentPhoto = photoDao.getPhotosByIds(listOf(photo.id)).firstOrNull() ?: photo
                var url = currentPhoto.uploadedUrl

                if (url.isNullOrBlank()) {
                    _actionState.value = GalleryActionState.Uploading
                    val settings = settingsRepo.getCurrentSettings()
                    val uploadSettings = if (BuildConfig.WEDDING_MODE) {
                        settings.upload.copy(autoUploadEnabled = true, useAnonymousHost = true)
                    } else settings.upload

                    val uploader = if (BuildConfig.WEDDING_MODE) {
                        WeddingUploaderProvider.uploaderOrNull() ?: AnonymousUploader()
                    } else if (!uploadSettings.useAnonymousHost && uploadSettings.isImmichConfigured) {
                        ImmichUploader(uploadSettings)
                    } else AnonymousUploader()

                    val file = File(currentPhoto.localPath)
                    val uploadedUrl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        uploader.upload(file)
                    }
                    if (uploadedUrl.isNotBlank()) {
                        photoDao.updateUploadedUrl(currentPhoto.id, uploadedUrl)
                        url = uploadedUrl
                    }
                }

                if (url.isNullOrBlank()) {
                    _actionState.value = GalleryActionState.Error("Failed to get photo upload URL for SMS")
                    return@launch
                }

                _actionState.value = GalleryActionState.Sending
                val smsSettings = settingsRepo.getCurrentSettings().sms
                val client = SmsGatewayClient(smsSettings)
                val passwordNotice = if (BuildConfig.WEDDING_MODE && BuildConfig.WEDDING_GALLERY_PASSWORD.isNotBlank()) {
                    " (Gallery Password: ${BuildConfig.WEDDING_GALLERY_PASSWORD})"
                } else ""
                val baseMsg = getApplication<Application>().getString(com.charles.photobooth.R.string.sms_body_template, currentPhoto.eventName, url)
                val message = baseMsg + passwordNotice
                client.sendSms(listOf(phone), message)
                _actionState.value = GalleryActionState.Success("SMS sent to $phone!")
            } catch (e: Exception) {
                _actionState.value = GalleryActionState.Error(e.message ?: "SMS failed")
            }
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            try {
                val file = File(photo.localPath)
                if (file.exists()) file.delete()
                photoDao.deleteById(photo.id)
            } catch (_: Exception) {
            }
        }
    }

    fun printPhotoThermal(photo: PhotoEntity) {
        if (_actionState.value is GalleryActionState.Uploading || _actionState.value is GalleryActionState.Sending) return
        viewModelScope.launch {
            try {
                _actionState.value = GalleryActionState.Sending
                val file = File(photo.localPath)
                if (!file.exists()) {
                    _actionState.value = GalleryActionState.Error("Photo file not found")
                    return@launch
                }
                val bitmap = BitmapFactory.decodeFile(photo.localPath)
                    ?: run {
                        _actionState.value = GalleryActionState.Error("Failed to load photo for printing")
                        return@launch
                    }
                val appSettings = settingsRepo.getCurrentSettings()
                val frameOverlayPath = appSettings.event.selectedFrameId?.let { id ->
                    db.templateDao().getTemplateByIdSync(id)?.backgroundImagePath
                }
                val printable = if (frameOverlayPath.isNullOrBlank()) {
                    bitmap
                } else {
                    renderSimple4x6(bitmap, frameOverlayPath, watermark = null).also {
                        bitmap.recycle()
                    }
                }
                val thermalSettings = appSettings.thermalPrinter.copy(
                    footerText = appSettings.thermalPrinter.footerText
                        .ifBlank { listOf(photo.eventName, appSettings.event.eventDate).filter { it.isNotBlank() }.joinToString(" - ") }
                )
                val result = ThermalPrinterClient(thermalSettings, getApplication()).print(printable)
                printable.recycle()
                result.fold(
                    onSuccess = { _actionState.value = GalleryActionState.Idle },
                    onFailure = { _actionState.value = GalleryActionState.Error(it.message ?: "Thermal print failed") },
                )
            } catch (e: Exception) {
                _actionState.value = GalleryActionState.Error(e.message ?: "Thermal print failed")
            }
        }
    }

    fun clearActionState() {
        _actionState.value = GalleryActionState.Idle
    }

    fun getShareIntent(photo: PhotoEntity): Intent? {
        if (photo.mediaType == MediaType.VIDEO) return null
        val file = File(photo.localPath)
        if (!file.exists()) return null
        val mimeType = when (file.extension.lowercase()) {
            "gif" -> "image/gif"
            "png" -> "image/png"
            else -> "image/jpeg"
        }

        val uri = try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = getApplication<Application>().contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val contentUri = resolver.insert(collection, values) ?: return null
            resolver.openOutputStream(contentUri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(contentUri, values, null, null)
            contentUri
        } catch (_: Exception) {
            return null
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Photo from ${photo.eventName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
