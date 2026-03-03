package com.example.photobooth.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photobooth.data.AppDatabase
import com.example.photobooth.data.TemplateEntity
import com.example.photobooth.settings.AllSettings
import com.example.photobooth.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.photobooth.network.SmtpEmailClient
import com.example.photobooth.network.SmsGatewayClient
import kotlinx.coroutines.flow.MutableStateFlow

sealed interface SmtpFieldChange {
    data class Host(val value: String) : SmtpFieldChange
    data class Port(val value: Int) : SmtpFieldChange
    data class UseTls(val value: Boolean) : SmtpFieldChange
    data class Username(val value: String) : SmtpFieldChange
    data class Password(val value: String) : SmtpFieldChange
    data class FromAddress(val value: String) : SmtpFieldChange
    data class FromName(val value: String) : SmtpFieldChange
}

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application)
    private val templateDao = AppDatabase.getInstance(application).templateDao()
    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus: kotlinx.coroutines.flow.StateFlow<String?> = _testStatus

    val settings: StateFlow<AllSettings> =
        repo.settingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AllSettings(),
        )

    val frames: StateFlow<List<TemplateEntity>> =
        templateDao.getAllTemplates().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun updateEvent(eventName: String, pattern: String) {
        viewModelScope.launch {
            repo.updateEventSettings {
                it.copy(eventName = eventName, filenamePattern = pattern)
            }
        }
    }

    fun updateCamera(useFrontCamera: Boolean) {
        viewModelScope.launch {
            repo.updateCameraSettings { it.copy(useFrontCamera = useFrontCamera) }
        }
    }

    fun updateSelectedFrame(frameId: Long?) {
        viewModelScope.launch {
            repo.updateSelectedFrame(frameId)
        }
    }

    fun updateUpload(
        useAnonymous: Boolean,
        immichBase: String,
        immichToken: String,
        immichAlbum: String,
    ) {
        viewModelScope.launch {
            repo.updateUploadSettings {
                it.copy(
                    useAnonymousHost = useAnonymous,
                    immichBaseUrl = immichBase,
                    immichApiToken = immichToken,
                    immichAlbumId = immichAlbum,
                )
            }
        }
    }

    fun updateSms(
        baseUrl: String,
        username: String,
        password: String,
        useCloud: Boolean,
    ) {
        viewModelScope.launch {
            repo.updateSmsSettings {
                it.copy(
                    baseUrl = baseUrl,
                    username = username,
                    password = password,
                    useCloudServer = useCloud,
                )
            }
        }
    }

    fun updateSmtp(change: SmtpFieldChange) {
        viewModelScope.launch {
            repo.updateSmtpSettings { current ->
                when (change) {
                    is SmtpFieldChange.Host -> current.copy(host = change.value)
                    is SmtpFieldChange.Port -> current.copy(port = change.value)
                    is SmtpFieldChange.UseTls -> current.copy(useSslTls = change.value)
                    is SmtpFieldChange.Username -> current.copy(username = change.value)
                    is SmtpFieldChange.Password -> current.copy(password = change.value)
                    is SmtpFieldChange.FromAddress -> current.copy(fromAddress = change.value)
                    is SmtpFieldChange.FromName -> current.copy(fromName = change.value)
                }
            }
        }
    }

    fun testSms() {
        viewModelScope.launch {
            val current = settings.value.sms
            if (current.baseUrl.isBlank()) {
                _testStatus.value = "Set SMS base URL first"
                return@launch
            }
            try {
                val client = SmsGatewayClient(current)
                client.sendSms(listOf("+10000000000"), "Photobooth SMS test")
                _testStatus.value = "Test SMS request sent successfully"
            } catch (e: Exception) {
                _testStatus.value = "Test SMS failed: ${e.message}"
            }
        }
    }

    fun testEmail() {
        viewModelScope.launch {
            val smtp = settings.value.smtp
            if (smtp.host.isBlank()) {
                _testStatus.value = "Set SMTP host first"
                return@launch
            }
            try {
                val client = SmtpEmailClient(getApplication(), smtp)
                val result = client.testConnection()
                _testStatus.value = if (result.success) {
                    "SMTP DNS test ok: ${result.message}"
                } else {
                    "SMTP DNS test failed: ${result.message}"
                }
            } catch (e: Exception) {
                _testStatus.value = "SMTP test error: ${e.message}"
            }
        }
    }
}

