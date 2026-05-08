package com.example.photobooth.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photobooth.data.AppDatabase
import com.example.photobooth.data.PhotoEntity
import com.example.photobooth.network.ImmichUploader
import com.example.photobooth.network.ImageUploader
import com.example.photobooth.network.ZeroX0Uploader
import com.example.photobooth.network.SmtpEmailClient
import com.example.photobooth.network.SmsGatewayClient
import com.example.photobooth.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface GalleryActionState {
    data object Idle : GalleryActionState
    data object Uploading : GalleryActionState
    data object Sending : GalleryActionState
    data class Error(val message: String) : GalleryActionState
}

class GalleryViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val photoDao = db.photoDao()
    private val settingsRepo = SettingsRepository(application)

    private fun getUploader(): ImageUploader {
        val uploadSettings = settingsRepo.getCurrentSettingsBlocking().upload
        return if (uploadSettings.useAnonymousHost) {
            ZeroX0Uploader()
        } else {
            ImmichUploader(uploadSettings)
        }
    }

    val photos: StateFlow<List<PhotoEntity>> =
        photoDao.getAllPhotos()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
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
                    _actionState.value = GalleryActionState.Error("Photo file not found")
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
                _actionState.value = GalleryActionState.Sending
                val file = File(photo.localPath)
                if (!file.exists()) {
                    _actionState.value = GalleryActionState.Error("Photo file not found")
                    return@launch
                }
                val settings = settingsRepo.getCurrentSettings().smtp
                val client = SmtpEmailClient(getApplication(), settings)
                val subject = settings.defaultSubjectTemplate.replace("{eventName}", photo.eventName)
                val body = settings.defaultBodyTemplate.replace("{eventName}", photo.eventName)
                client.sendPhotoEmail(to, subject, body, file)
                _actionState.value = GalleryActionState.Idle
            } catch (e: Exception) {
                _actionState.value = GalleryActionState.Error(e.message ?: "Email failed")
            }
        }
    }

    fun sendPhotoBySms(photo: PhotoEntity, phone: String) {
        if (_actionState.value is GalleryActionState.Uploading || _actionState.value is GalleryActionState.Sending) return
        viewModelScope.launch {
            try {
                _actionState.value = GalleryActionState.Sending
                val smsSettings = settingsRepo.getCurrentSettings().sms
                val client = SmsGatewayClient(smsSettings)
                val urlOrLocal = photo.uploadedUrl ?: getApplication<Application>().getString(com.example.photobooth.R.string.sms_uploaded_locally)
                val message = getApplication<Application>().getString(com.example.photobooth.R.string.sms_body_template, photo.eventName, urlOrLocal)
                client.sendSms(listOf(phone), message)
                _actionState.value = GalleryActionState.Idle
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

    fun clearActionState() {
        _actionState.value = GalleryActionState.Idle
    }
}
