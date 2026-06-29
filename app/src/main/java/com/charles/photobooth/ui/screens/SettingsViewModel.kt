package com.charles.photobooth.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charles.photobooth.data.AppDatabase
import com.charles.photobooth.data.TemplateEntity
import com.charles.photobooth.monetization.PhotoQuotaRepository
import com.charles.photobooth.settings.AllSettings
import com.charles.photobooth.settings.CaptureModeSettings
import com.charles.photobooth.settings.SettingsRepository
import com.charles.photobooth.settings.ShareSettings
import com.charles.photobooth.settings.ThermalPrinterSettings
import com.charles.photobooth.settings.WatermarkSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.charles.photobooth.R
import com.charles.photobooth.network.SmtpEmailClient
import com.charles.photobooth.network.SmsGatewayClient
import kotlinx.coroutines.flow.MutableStateFlow
import android.net.Uri
import com.charles.photobooth.network.GitHubComment
import com.charles.photobooth.network.GitHubIssue
import com.charles.photobooth.network.GitHubService
import com.charles.photobooth.settings.BugReport
import com.charles.photobooth.settings.BugReportRepository
import com.charles.photobooth.util.DiagnosticsHelper

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
    private val quotaRepo = PhotoQuotaRepository(application)
    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus: kotlinx.coroutines.flow.StateFlow<String?> = _testStatus

    private val bugReportRepo = BugReportRepository(application)
    private val gitHubService = GitHubService()

    val bugReports: StateFlow<List<BugReport>> =
        bugReportRepo.bugReports.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _submitError = MutableStateFlow<String?>(null)
    val submitError: StateFlow<String?> = _submitError

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess

    private val _comments = MutableStateFlow<List<GitHubComment>>(emptyList())
    val comments: StateFlow<List<GitHubComment>> = _comments

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading

    private val _commentsError = MutableStateFlow<String?>(null)
    val commentsError: StateFlow<String?> = _commentsError

    private val _issueDetails = MutableStateFlow<GitHubIssue?>(null)
    val issueDetails: StateFlow<GitHubIssue?> = _issueDetails

    fun clearSubmitStatus() {
        _submitSuccess.value = false
        _submitError.value = null
        _isSubmitting.value = false
    }

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

    fun updateEvent(eventName: String, eventDate: String, pattern: String) {
        viewModelScope.launch {
            repo.updateEventSettings {
                it.copy(eventName = eventName, eventDate = eventDate, filenamePattern = pattern)
            }
        }
    }

    fun updateCamera(useFrontCamera: Boolean) {
        viewModelScope.launch {
            repo.updateCameraSettings { it.copy(useFrontCamera = useFrontCamera) }
        }
    }

    fun updateCameraId(cameraId: String?) {
        viewModelScope.launch {
            repo.updateCameraSettings { it.copy(cameraId = cameraId) }
        }
    }

    fun updateFrontScreenFlash(enabled: Boolean) {
        viewModelScope.launch {
            repo.updateCameraSettings { it.copy(frontScreenFlashEnabled = enabled) }
        }
    }

    /** Debug-only: clear today's photo-quota counters back to base. */
    fun resetDailyPhotoQuota() {
        viewModelScope.launch {
            quotaRepo.resetDailyQuota()
            _testStatus.value = "Daily photo quota reset"
        }
    }

    fun updateWatermark(block: (WatermarkSettings) -> WatermarkSettings) {
        viewModelScope.launch {
            repo.updateWatermarkSettings(block)
        }
    }

    fun updateCaptureMode(block: (CaptureModeSettings) -> CaptureModeSettings) {
        viewModelScope.launch {
            repo.updateCaptureModeSettings(block)
        }
    }

    fun updateSelectedFrame(frameId: Long?) {
        viewModelScope.launch {
            repo.updateSelectedFrame(frameId)
        }
    }

    fun updateUpload(
        autoUploadEnabled: Boolean,
        useAnonymous: Boolean,
        immichBase: String,
        immichToken: String,
        immichAlbumSyncEnabled: Boolean,
        immichAlbum: String,
    ) {
        viewModelScope.launch {
            repo.updateUploadSettings {
                it.copy(
                    autoUploadEnabled = autoUploadEnabled,
                    useAnonymousHost = useAnonymous,
                    immichBaseUrl = immichBase,
                    immichApiToken = immichToken,
                    immichAlbumSyncEnabled = immichAlbumSyncEnabled,
                    immichAlbumId = immichAlbum,
                )
            }
        }
    }

    fun updateShare(block: (ShareSettings) -> ShareSettings) {
        viewModelScope.launch {
            repo.updateShareSettings(block)
        }
    }

    fun updateThermalPrinter(block: (ThermalPrinterSettings) -> ThermalPrinterSettings) {
        viewModelScope.launch {
            repo.updateThermalPrinterSettings(block)
        }
    }

    fun testThermalPrinterConnection() {
        viewModelScope.launch {
            val settings = repo.getCurrentSettings().thermalPrinter
            if (settings.deviceAddress.isBlank()) {
                _testStatus.value = "Select a printer first"
                return@launch
            }
            _testStatus.value = "Connecting to ${settings.deviceName.ifBlank { settings.deviceAddress }}…"
            try {
                val result = com.charles.photobooth.printing.ThermalPrinterClient(settings).testConnection()
                _testStatus.value = if (result.isSuccess) {
                    "Printer connection ok"
                } else {
                    "Connection failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _testStatus.value = "Connection error: ${e.message}"
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
            _testStatus.value = "Testing SMS gateway…"
            try {
                val client = SmsGatewayClient(current)
                val result = client.testConnection()
                _testStatus.value = if (result.success) {
                    "SMS test ok: ${result.message}"
                } else {
                    "SMS test failed: ${result.message}"
                }
            } catch (e: Exception) {
                _testStatus.value = "SMS test error: ${e.message}"
            }
        }
    }

    fun sendTestSms(phone: String) {
        viewModelScope.launch {
            val current = settings.value.sms
            if (current.baseUrl.isBlank()) {
                _testStatus.value = "Set SMS base URL first"
                return@launch
            }
            val trimmed = phone.trim()
            if (trimmed.isBlank() || !trimmed.matches(Regex("^\\+?[0-9 \\-]{6,}$"))) {
                _testStatus.value = "Enter a valid test phone number"
                return@launch
            }
            _testStatus.value = "Sending real test SMS…"
            try {
                SmsGatewayClient(current).sendSms(listOf(trimmed), "Photobooth test")
                _testStatus.value = "Test SMS sent to $trimmed"
            } catch (e: Exception) {
                _testStatus.value = "Test SMS send failed: ${e.message}"
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
            _testStatus.value = "Testing SMTP connection…"
            try {
                val client = SmtpEmailClient(getApplication(), smtp)
                val result = client.testConnection()
                _testStatus.value = if (result.success) {
                    "SMTP test ok: ${result.message}"
                } else {
                    "SMTP test failed: ${result.message}"
                }
            } catch (e: Exception) {
                _testStatus.value = "SMTP test error: ${e.message}"
            }
        }
    }

    fun submitBugReport(
        title: String,
        description: String,
        name: String,
        email: String,
        includeDiagnostics: Boolean,
        screenshotUri: Uri?
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isSubmitting.value = true
            _submitError.value = null
            _submitSuccess.value = false
            try {
                var screenshotUrl: String? = null
                if (screenshotUri != null) {
                    val contentResolver = getApplication<Application>().contentResolver
                    val bytes = contentResolver.openInputStream(screenshotUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val filename = "screenshot_${System.currentTimeMillis()}.png"
                        val uploadResult = gitHubService.uploadAsset(filename, base64Data)
                        screenshotUrl = uploadResult.getOrThrow()
                    }
                }

                val bodyBuilder = StringBuilder()
                if (name.isNotBlank() || email.isNotBlank()) {
                    bodyBuilder.append("### Reporter Info\n")
                    if (name.isNotBlank()) bodyBuilder.append("- **Name**: $name\n")
                    if (email.isNotBlank()) bodyBuilder.append("- **Email**: $email\n")
                    bodyBuilder.append("\n")
                }
                bodyBuilder.append("### Description\n")
                bodyBuilder.append(description)
                bodyBuilder.append("\n\n")

                if (screenshotUrl != null) {
                    bodyBuilder.append("### Attachments\n")
                    bodyBuilder.append("![Screenshot]($screenshotUrl)\n\n")
                }

                if (includeDiagnostics) {
                    val diagnostics = DiagnosticsHelper.gatherDiagnostics(getApplication(), true)
                    bodyBuilder.append(diagnostics)
                }

                val issueResult = gitHubService.createIssue(title, bodyBuilder.toString())
                val issue = issueResult.getOrThrow()

                // Save locally
                val localReport = BugReport(
                    number = issue.number,
                    title = issue.title,
                    status = issue.state,
                    createdAt = issue.createdAt,
                    htmlUrl = issue.htmlUrl
                )
                bugReportRepo.saveBugReport(localReport)

                _submitSuccess.value = true
            } catch (e: Exception) {
                _submitError.value = e.localizedMessage ?: "Failed to submit bug report"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun loadIssueDetailsAndComments(issueNumber: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _commentsLoading.value = true
            _commentsError.value = null
            try {
                val issueResult = gitHubService.getIssue(issueNumber)
                val issue = issueResult.getOrThrow()
                _issueDetails.value = issue

                // Sync status
                bugReportRepo.updateBugReportStatus(issue.number, issue.state)

                val commentsResult = gitHubService.getComments(issueNumber)
                val commentsList = commentsResult.getOrThrow()
                _comments.value = commentsList
            } catch (e: Exception) {
                _commentsError.value = e.localizedMessage ?: "Failed to load thread details"
            } finally {
                _commentsLoading.value = false
            }
        }
    }

    fun postReply(issueNumber: Int, commentText: String, screenshotUri: Uri?, onComplete: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _commentsLoading.value = true
            _commentsError.value = null
            try {
                var screenshotUrl: String? = null
                if (screenshotUri != null) {
                    val contentResolver = getApplication<Application>().contentResolver
                    val bytes = contentResolver.openInputStream(screenshotUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val filename = "screenshot_${System.currentTimeMillis()}.png"
                        val uploadResult = gitHubService.uploadAsset(filename, base64Data)
                        screenshotUrl = uploadResult.getOrThrow()
                    }
                }

                val bodyBuilder = StringBuilder()
                bodyBuilder.append("**[User Reply from App]**\n\n")
                bodyBuilder.append(commentText)
                bodyBuilder.append("\n\n")

                if (screenshotUrl != null) {
                    bodyBuilder.append("### Attachments\n")
                    bodyBuilder.append("![Screenshot]($screenshotUrl)\n\n")
                }

                gitHubService.postComment(issueNumber, bodyBuilder.toString()).getOrThrow()

                // Reload details and comments
                loadIssueDetailsAndComments(issueNumber)

                launch(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                _commentsError.value = e.localizedMessage ?: "Failed to post comment"
            } finally {
                _commentsLoading.value = false
            }
        }
    }
}

