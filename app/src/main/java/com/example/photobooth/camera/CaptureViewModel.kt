package com.example.photobooth.camera

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photobooth.data.AppDatabase
import com.example.photobooth.data.PhotoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
        onComplete: (Long?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val outputDir = File(context.filesDir, "photos")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                val destFile = File(outputDir, "photo_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
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
}

