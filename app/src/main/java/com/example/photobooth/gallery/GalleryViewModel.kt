package com.example.photobooth.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photobooth.data.AppDatabase
import com.example.photobooth.data.PhotoEntity
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
    private val uploader: ImageUploader = ZeroX0Uploader()
    private val settingsRepo = SettingsRepository(application)

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
        viewModelScope.launch {
            try {
                _actionState.value = GalleryActionState.Uploading
                val file = File(photo.localPath)
                val url = uploader.upload(file)
                photoDao.updateUploadedUrl(photo.id, url)
                _actionState.value = GalleryActionState.Idle
            } catch (e: Exception) {
                _actionState.value = GalleryActionState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun sendPhotoByEmail(photo: PhotoEntity, to: String) {
        viewModelScope.launch {
            try {
                _actionState.value = GalleryActionState.Sending
                val file = File(photo.localPath)
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
        viewModelScope.launch {
            try {
                _actionState.value = GalleryActionState.Sending
                val smsSettings = settingsRepo.getCurrentSettings().sms
                val client = SmsGatewayClient(smsSettings)
                val message = "Your photo from ${photo.eventName}: ${photo.uploadedUrl ?: "uploaded locally"}"
                client.sendSms(listOf(phone), message)
                _actionState.value = GalleryActionState.Idle
            } catch (e: Exception) {
                _actionState.value = GalleryActionState.Error(e.message ?: "SMS failed")
            }
        }
    }

    fun deletePhoto(id: Long) {
        viewModelScope.launch {
            photoDao.deleteById(id)
        }
    }
}

