package com.charles.photobooth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.charles.photobooth.template.TemplateDefinition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.charles.photobooth.data.TemplateEntity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.charles.photobooth.printing.BluetoothDevicePickerDialog
import com.charles.photobooth.printing.BluetoothDeviceInfo
import com.charles.photobooth.settings.AllSettings
import com.charles.photobooth.settings.CaptureModeSettings
import com.charles.photobooth.settings.ShareSettings
import com.charles.photobooth.settings.ThermalPrinterSettings
import com.charles.photobooth.settings.WatermarkSettings
import com.charles.photobooth.BuildConfig
import com.charles.photobooth.template.BuiltInTemplates
import com.charles.photobooth.template.WatermarkPosition
import com.charles.photobooth.ui.components.TemplatePreview
import com.charles.photobooth.camera.PhotoFilter
import com.charles.photobooth.ui.theme.CardSurface
import com.charles.photobooth.ui.theme.CardSurfaceLight
import com.charles.photobooth.ui.theme.DarkBackground
import com.charles.photobooth.ui.theme.Gold
import com.charles.photobooth.ui.theme.Rose
import com.charles.photobooth.ui.theme.Success
import com.charles.photobooth.R
import com.charles.photobooth.ui.theme.TextSecondary
import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonColors
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.charles.photobooth.settings.BugReport
import com.charles.photobooth.network.GitHubComment
import com.charles.photobooth.network.GitHubIssue


@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenFrameDesigner: () -> Unit = {},
    onOpenWebsite: () -> Unit = {},
) {
    val vm: SettingsViewModel = viewModel()
    val state by vm.settings.collectAsState()
    val status by vm.testStatus.collectAsState()
    val frames by vm.frames.collectAsState()

    val bugReports by vm.bugReports.collectAsState()
    val isSubmitting by vm.isSubmitting.collectAsState()
    val submitError by vm.submitError.collectAsState()
    val submitSuccess by vm.submitSuccess.collectAsState()
    val comments by vm.comments.collectAsState()
    val commentsLoading by vm.commentsLoading.collectAsState()
    val commentsError by vm.commentsError.collectAsState()
    val issueDetails by vm.issueDetails.collectAsState()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Rose,
        unfocusedBorderColor = CardSurfaceLight,
        focusedLabelColor = Rose,
        unfocusedLabelColor = TextSecondary,
        cursorColor = Rose,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardSurfaceLight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = stringResource(R.string.capture_back),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }

        // Status message
        status?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (it.contains("ok", ignoreCase = true) || it.contains("success", ignoreCase = true))
                            Success.copy(alpha = 0.15f)
                        else
                            Rose.copy(alpha = 0.15f),
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (it.contains("ok", ignoreCase = true) || it.contains("success", ignoreCase = true))
                        Success
                    else
                        Rose,
                )
            }
        }

        // Settings content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EventSettingsSection(state, onEventChange = vm::updateEvent, textFieldColors = textFieldColors)
            CameraSettingsSection(
                state,
                onCameraChange = vm::updateCamera,
                onCameraIdChange = vm::updateCameraId,
                onFrontScreenFlashChange = vm::updateFrontScreenFlash,
            )
            FrameSettingsSection(
                state = state,
                frames = frames,
                onFrameSelected = vm::updateSelectedFrame,
                onOpenFrameDesigner = onOpenFrameDesigner,
            )
            CaptureModeSection(
                state = state,
                onCaptureModeChange = vm::updateCaptureMode,
            )
            WatermarkSection(
                state = state,
                onWatermarkChange = vm::updateWatermark,
                textFieldColors = textFieldColors,
            )
            UploadSettingsSection(state, onUploadChange = vm::updateUpload, textFieldColors = textFieldColors)
            ShareSettingsSection(state, onShareChange = vm::updateShare)
            ThermalPrinterSettingsSection(
                state = state,
                onThermalPrinterChange = vm::updateThermalPrinter,
                onTestConnection = vm::testThermalPrinterConnection,
                textFieldColors = textFieldColors,
            )
            SmsSettingsSection(
                state,
                onSmsChange = vm::updateSms,
                onTestSms = vm::testSms,
                onSendTestSms = vm::sendTestSms,
                textFieldColors = textFieldColors,
            )
            SmtpSettingsSection(state, onSmtpChange = vm::updateSmtp, onTestEmail = vm::testEmail, textFieldColors = textFieldColors)
            SupportAndFeedbackSection(
                bugReports = bugReports,
                isSubmitting = isSubmitting,
                submitError = submitError,
                submitSuccess = submitSuccess,
                comments = comments,
                commentsLoading = commentsLoading,
                commentsError = commentsError,
                issueDetails = issueDetails,
                onSubmitBugReport = vm::submitBugReport,
                onLoadIssue = vm::loadIssueDetailsAndComments,
                onPostReply = vm::postReply,
                onClearSubmitStatus = vm::clearSubmitStatus,
                textFieldColors = textFieldColors,
            )
            AboutSection(onOpenWebsite = onOpenWebsite)
            if (BuildConfig.DEBUG) {
                DebugSection(onResetQuota = vm::resetDailyPhotoQuota)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    iconRes: Int,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Rose.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Rose,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            content()
        }
    }
}

@Composable
private fun EventSettingsSection(
    state: AllSettings,
    onEventChange: (String, String, String) -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    SettingsCard(
        title = stringResource(R.string.settings_event),
        iconRes = android.R.drawable.ic_menu_today,
    ) {
        StableTextField(
            value = state.event.eventName,
            onValueChange = { onEventChange(it, state.event.eventDate, state.event.filenamePattern) },
            label = stringResource(R.string.settings_event_name),
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
        )
        StableTextField(
            value = state.event.eventDate,
            onValueChange = { onEventChange(state.event.eventName, it, state.event.filenamePattern) },
            label = stringResource(R.string.settings_event_date),
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
        )
        StableTextField(
            value = state.event.filenamePattern,
            onValueChange = { onEventChange(state.event.eventName, state.event.eventDate, it) },
            label = stringResource(R.string.settings_filename_pattern),
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
        )
    }
}

@Composable
private fun UploadSettingsSection(
    state: AllSettings,
    onUploadChange: (Boolean, Boolean, String, String, Boolean, String) -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    SettingsCard(
        title = stringResource(R.string.settings_upload),
        iconRes = android.R.drawable.ic_menu_upload,
    ) {
        StyledSwitch(
            label = stringResource(R.string.settings_auto_upload),
            checked = state.upload.autoUploadEnabled,
            onCheckedChange = {
                onUploadChange(it, state.upload.useAnonymousHost, state.upload.immichBaseUrl, state.upload.immichApiToken, state.upload.immichAlbumSyncEnabled, state.upload.immichAlbumId)
            },
        )
        StyledSwitch(
            label = stringResource(R.string.settings_use_anon_host),
            checked = state.upload.useAnonymousHost,
            onCheckedChange = {
                onUploadChange(state.upload.autoUploadEnabled, it, state.upload.immichBaseUrl, state.upload.immichApiToken, state.upload.immichAlbumSyncEnabled, state.upload.immichAlbumId)
            },
        )
        if (!state.upload.useAnonymousHost) {
            StableTextField(
                value = state.upload.immichBaseUrl,
                onValueChange = { onUploadChange(state.upload.autoUploadEnabled, state.upload.useAnonymousHost, it, state.upload.immichApiToken, state.upload.immichAlbumSyncEnabled, state.upload.immichAlbumId) },
                label = stringResource(R.string.settings_immich_base_url),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
            )
            StableTextField(
                value = state.upload.immichApiToken,
                onValueChange = { onUploadChange(state.upload.autoUploadEnabled, state.upload.useAnonymousHost, state.upload.immichBaseUrl, it, state.upload.immichAlbumSyncEnabled, state.upload.immichAlbumId) },
                label = stringResource(R.string.settings_immich_api_token),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                visualTransformation = PasswordVisualTransformation(),
            )
            StyledSwitch(
                label = stringResource(R.string.settings_immich_album_sync),
                checked = state.upload.immichAlbumSyncEnabled,
                onCheckedChange = {
                    onUploadChange(state.upload.autoUploadEnabled, state.upload.useAnonymousHost, state.upload.immichBaseUrl, state.upload.immichApiToken, it, state.upload.immichAlbumId)
                },
            )
            if (state.upload.immichAlbumSyncEnabled) {
                StableTextField(
                    value = state.upload.immichAlbumId,
                    onValueChange = { onUploadChange(state.upload.autoUploadEnabled, state.upload.useAnonymousHost, state.upload.immichBaseUrl, state.upload.immichApiToken, state.upload.immichAlbumSyncEnabled, it) },
                    label = stringResource(R.string.settings_immich_album_id),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                )
            }
        }
    }
}

@Composable
private fun ShareSettingsSection(
    state: AllSettings,
    onShareChange: (ShareSettings.() -> ShareSettings) -> Unit,
) {
    SettingsCard(
        title = stringResource(R.string.settings_share_options),
        iconRes = android.R.drawable.ic_menu_share,
    ) {
        StyledSwitch(
            label = stringResource(R.string.settings_share_email),
            checked = state.share.enableEmailShare,
            onCheckedChange = { onShareChange { copy(enableEmailShare = it) } },
        )
        StyledSwitch(
            label = stringResource(R.string.settings_share_sms),
            checked = state.share.enableSmsShare,
            onCheckedChange = { onShareChange { copy(enableSmsShare = it) } },
        )
        StyledSwitch(
            label = stringResource(R.string.settings_share_print),
            checked = state.share.enablePrintShare,
            onCheckedChange = { onShareChange { copy(enablePrintShare = it) } },
        )
    }
}

@Composable
private fun ThermalPrinterSettingsSection(
    state: AllSettings,
    onThermalPrinterChange: (ThermalPrinterSettings.() -> ThermalPrinterSettings) -> Unit,
    onTestConnection: () -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    val context = LocalContext.current
    val settings = state.thermalPrinter
    var showPicker by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val btPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            pendingAction?.invoke()
        }
        pendingAction = null
    }

    fun withBtPermissions(action: () -> Unit) {
        val allGranted = btPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(btPermissions)
        }
    }

    if (showPicker) {
        BluetoothDevicePickerDialog(
            onDeviceSelected = { device: BluetoothDeviceInfo ->
                onThermalPrinterChange { copy(deviceAddress = device.address, deviceName = device.name) }
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }

    SettingsCard(
        title = "Bluetooth Thermal Printer",
        iconRes = android.R.drawable.ic_menu_send,
    ) {
        StyledSwitch(
            label = "Enable thermal printer",
            checked = settings.enabled,
            onCheckedChange = { onThermalPrinterChange { copy(enabled = it) } },
        )

        if (settings.enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (settings.deviceName.isNotBlank()) settings.deviceName else "No printer selected",
                        color = if (settings.deviceName.isNotBlank()) Color.White else TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (settings.deviceAddress.isNotBlank()) {
                        Text(
                            text = settings.deviceAddress,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                ElevatedButton(
                    onClick = { withBtPermissions { showPicker = true } },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Rose,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Scan & Select")
                }
            }

            if (settings.deviceAddress.isNotBlank()) {
                ElevatedButton(
                    onClick = { withBtPermissions(onTestConnection) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Gold,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text("Test Connection", fontWeight = FontWeight.Medium)
                }
            }

            StyledSwitch(
                label = "Auto-print after each capture",
                checked = settings.autoPrintAfterCapture,
                onCheckedChange = { onThermalPrinterChange { copy(autoPrintAfterCapture = it) } },
            )

            StableTextField(
                value = settings.footerText,
                onValueChange = { onThermalPrinterChange { copy(footerText = it) } },
                label = "Footer text (optional)",
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
            )
        }
    }
}

@Composable
private fun SmsSettingsSection(
    state: AllSettings,
    onSmsChange: (String, String, String, Boolean) -> Unit,
    onTestSms: () -> Unit,
    onSendTestSms: (String) -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    var testPhone by rememberSaveable { mutableStateOf("") }
    SettingsCard(
        title = stringResource(R.string.settings_sms_gateway),
        iconRes = android.R.drawable.ic_dialog_email,
    ) {
        StableTextField(
            value = state.sms.baseUrl,
            onValueChange = { onSmsChange(it, state.sms.username, state.sms.password, state.sms.useCloudServer) },
            label = stringResource(R.string.settings_base_url),
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
        )
        StableTextField(
            value = state.sms.username,
            onValueChange = { onSmsChange(state.sms.baseUrl, it, state.sms.password, state.sms.useCloudServer) },
            label = stringResource(R.string.settings_username),
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
        )
        StableTextField(
            value = state.sms.password,
            onValueChange = { onSmsChange(state.sms.baseUrl, state.sms.username, it, state.sms.useCloudServer) },
            label = stringResource(R.string.settings_password),
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
            visualTransformation = PasswordVisualTransformation(),
        )
        StyledSwitch(
            label = stringResource(R.string.settings_cloud_server),
            checked = state.sms.useCloudServer,
            onCheckedChange = {
                onSmsChange(state.sms.baseUrl, state.sms.username, state.sms.password, it)
            },
        )
        ElevatedButton(
            onClick = onTestSms,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Gold,
                contentColor = Color.Black,
            ),
        ) {
            Text(stringResource(R.string.settings_test_sms), fontWeight = FontWeight.Medium)
        }
        StableTextField(
            value = testPhone,
            onValueChange = { testPhone = it },
            label = stringResource(R.string.settings_sms_test_phone_hint),
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
        )
        ElevatedButton(
            onClick = { onSendTestSms(testPhone) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Gold,
                contentColor = Color.Black,
            ),
        ) {
            Text(stringResource(R.string.settings_sms_send_test), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SmtpSettingsSection(
    state: AllSettings,
    onSmtpChange: (SmtpFieldChange) -> Unit,
    onTestEmail: () -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    SettingsCard(
        title = stringResource(R.string.settings_smtp_email),
        iconRes = android.R.drawable.ic_dialog_email,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StableTextField(
                value = state.smtp.host,
                onValueChange = { onSmtpChange(SmtpFieldChange.Host(it)) },
                label = stringResource(R.string.settings_smtp_host),
                modifier = Modifier.weight(2f),
                colors = textFieldColors,
            )
            StableTextField(
                value = state.smtp.port.toString(),
                onValueChange = { onSmtpChange(SmtpFieldChange.Port(it.toIntOrNull() ?: state.smtp.port)) },
                label = stringResource(R.string.settings_port),
                modifier = Modifier.weight(1f),
                colors = textFieldColors,
            )
        }
        StyledSwitch(
            label = stringResource(R.string.settings_use_tls),
            checked = state.smtp.useSslTls,
            onCheckedChange = { onSmtpChange(SmtpFieldChange.UseTls(it)) },
        )
        StableTextField(
            value = state.smtp.username,
            onValueChange = { onSmtpChange(SmtpFieldChange.Username(it)) },
            label = stringResource(R.string.settings_username),
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
        )
        StableTextField(
            value = state.smtp.password,
            onValueChange = { onSmtpChange(SmtpFieldChange.Password(it)) },
            label = stringResource(R.string.settings_password),
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
            visualTransformation = PasswordVisualTransformation(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StableTextField(
                value = state.smtp.fromAddress,
                onValueChange = { onSmtpChange(SmtpFieldChange.FromAddress(it)) },
                label = stringResource(R.string.settings_from_address),
                modifier = Modifier.weight(1f),
                colors = textFieldColors,
            )
            StableTextField(
                value = state.smtp.fromName,
                onValueChange = { onSmtpChange(SmtpFieldChange.FromName(it)) },
                label = stringResource(R.string.settings_from_name),
                modifier = Modifier.weight(1f),
                colors = textFieldColors,
            )
        }
        ElevatedButton(
            onClick = onTestEmail,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Gold,
                contentColor = Color.Black,
            ),
        ) {
            Text(stringResource(R.string.settings_test_email), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CameraSettingsSection(
    state: AllSettings,
    onCameraChange: (Boolean) -> Unit,
    onCameraIdChange: (String?) -> Unit,
    onFrontScreenFlashChange: (Boolean) -> Unit,
) {
    SettingsCard(
        title = stringResource(R.string.settings_camera),
        iconRes = android.R.drawable.ic_menu_camera,
    ) {
        StyledSwitch(
            label = stringResource(R.string.settings_use_front_camera),
            checked = state.camera.useFrontCamera,
            onCheckedChange = onCameraChange,
        )
        StyledSwitch(
            label = stringResource(R.string.settings_front_screen_flash),
            checked = state.camera.frontScreenFlashEnabled,
            onCheckedChange = onFrontScreenFlashChange,
        )
    }
}

@Composable
private fun CaptureModeSection(
    state: AllSettings,
    onCaptureModeChange: (CaptureModeSettings.() -> CaptureModeSettings) -> Unit,
) {
    SettingsCard(
        title = stringResource(R.string.settings_capture_mode),
        iconRes = android.R.drawable.ic_menu_crop,
    ) {
        StyledSwitch(
            label = stringResource(R.string.settings_booth_mode),
            checked = state.captureMode.boothMode,
            onCheckedChange = { onCaptureModeChange { copy(boothMode = it) } },
        )
        StyledSwitch(
            label = stringResource(R.string.settings_video_capture),
            checked = state.captureMode.videoCaptureEnabled,
            onCheckedChange = { onCaptureModeChange { copy(videoCaptureEnabled = it) } },
        )
        if (state.captureMode.boothMode) {
            StyledSwitch(
                label = stringResource(R.string.settings_gif_mode),
                checked = state.captureMode.gifModeEnabled,
                onCheckedChange = { onCaptureModeChange { copy(gifModeEnabled = it) } },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_photo_count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ElevatedButton(
                        onClick = { onCaptureModeChange { copy(boothPhotoCount = maxOf(2, boothPhotoCount - 1)) } },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = CardSurfaceLight,
                            contentColor = Color.White,
                        ),
                    ) { Text("-") }
                    Text(
                        text = " ${state.captureMode.boothPhotoCount} ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    ElevatedButton(
                        onClick = { onCaptureModeChange { copy(boothPhotoCount = minOf(8, boothPhotoCount + 1)) } },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = CardSurfaceLight,
                            contentColor = Color.White,
                        ),
                    ) { Text("+") }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_photo_filter),
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PhotoFilter.entries.forEach { filter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (state.captureMode.selectedFilter == filter.name) Rose.copy(alpha = 0.2f)
                            else Color.Transparent,
                        )
                        .clickable { onCaptureModeChange { copy(selectedFilter = filter.name) } }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = filter.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.captureMode.selectedFilter == filter.name) Color.White else TextSecondary,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_template_layout),
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
            Text(
                text = stringResource(R.string.settings_template_tap_to_preview),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.7f),
            )
        }
        val templateOptions = listOf(
            "NONE",
            "SINGLE",
            "STRIP_2x2",
            "STRIP_VERTICAL",
            BuiltInTemplates.KEY_BIRTHDAY,
            BuiltInTemplates.KEY_WEDDING,
            BuiltInTemplates.KEY_ANNIVERSARY,
            BuiltInTemplates.KEY_HOLIDAY,
            BuiltInTemplates.KEY_GENERIC,
        )
        val templateNames = mapOf(
            "NONE" to stringResource(R.string.settings_template_none),
            "SINGLE" to stringResource(R.string.settings_template_single),
            "STRIP_2x2" to stringResource(R.string.settings_template_2x2),
            "STRIP_VERTICAL" to stringResource(R.string.settings_template_vertical),
            BuiltInTemplates.KEY_BIRTHDAY to stringResource(R.string.settings_template_birthday),
            BuiltInTemplates.KEY_WEDDING to stringResource(R.string.settings_template_wedding),
            BuiltInTemplates.KEY_ANNIVERSARY to stringResource(R.string.settings_template_anniversary),
            BuiltInTemplates.KEY_HOLIDAY to stringResource(R.string.settings_template_holiday),
            BuiltInTemplates.KEY_GENERIC to stringResource(R.string.settings_template_generic),
        )
        val previewEventName = state.event.eventName.ifBlank { "Event Title" }
        val previewEventDate = state.event.eventDate.ifBlank { "Date here" }
        var zoomedKey by remember { mutableStateOf<String?>(null) }
        templateOptions.forEach { key ->
            val previewTemplate = when (key) {
                "NONE" -> null
                "SINGLE" -> BuiltInTemplates.singlePortrait(previewEventName)
                "STRIP_2x2" -> BuiltInTemplates.photoStrip2x2(previewEventName)
                "STRIP_VERTICAL" -> BuiltInTemplates.photoStripVertical(previewEventName)
                else -> BuiltInTemplates.fromKey(key, previewEventName, previewEventDate)
            }
            val isDisabled = key in state.captureMode.disabledTemplateKeys
            val canToggle = key != "NONE"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (state.captureMode.selectedTemplate == key) Rose.copy(alpha = 0.2f)
                        else Color.Transparent,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TemplatePreview(
                    template = previewTemplate,
                    modifier = Modifier
                        .width(80.dp)
                        .clickable { zoomedKey = key },
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = templateNames[key] ?: key,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isDisabled -> TextSecondary.copy(alpha = 0.5f)
                        state.captureMode.selectedTemplate == key -> Color.White
                        else -> TextSecondary
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isDisabled) {
                            onCaptureModeChange { copy(selectedTemplate = key) }
                        }
                        .padding(vertical = 8.dp),
                )
                if (canToggle) {
                    Switch(
                        checked = !isDisabled,
                        onCheckedChange = { wantEnabled ->
                            onCaptureModeChange {
                                val newDisabled = if (wantEnabled) {
                                    disabledTemplateKeys - key
                                } else {
                                    disabledTemplateKeys + key
                                }
                                // If we just disabled the currently selected template,
                                // fall back to NONE so capture isn't locked out.
                                val nextSelected = if (!wantEnabled && selectedTemplate == key) "NONE" else selectedTemplate
                                copy(disabledTemplateKeys = newDisabled, selectedTemplate = nextSelected)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Rose,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardSurfaceLight,
                            uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }
        }

        if (zoomedKey != null) {
            val key = zoomedKey!!
            val zoomedTemplate = when (key) {
                "NONE" -> null
                "SINGLE" -> BuiltInTemplates.singlePortrait(previewEventName)
                "STRIP_2x2" -> BuiltInTemplates.photoStrip2x2(previewEventName)
                "STRIP_VERTICAL" -> BuiltInTemplates.photoStripVertical(previewEventName)
                else -> BuiltInTemplates.fromKey(key, previewEventName, previewEventDate)
            }
            FullScreenTemplatePreviewDialog(
                template = zoomedTemplate,
                title = templateNames[key] ?: key,
                isSelected = state.captureMode.selectedTemplate == key,
                onSelect = {
                    onCaptureModeChange { copy(selectedTemplate = key) }
                    zoomedKey = null
                },
                onDismiss = { zoomedKey = null },
            )
        }
    }
}

@Composable
private fun WatermarkSection(
    state: AllSettings,
    onWatermarkChange: (WatermarkSettings.() -> WatermarkSettings) -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    SettingsCard(
        title = stringResource(R.string.settings_watermark),
        iconRes = android.R.drawable.ic_menu_edit,
    ) {
        StyledSwitch(
            label = stringResource(R.string.settings_watermark_enabled),
            checked = state.watermark.enabled,
            onCheckedChange = { onWatermarkChange { copy(enabled = it) } },
        )
        if (state.watermark.enabled) {
            StableTextField(
                value = state.watermark.imagePath,
                onValueChange = { onWatermarkChange { copy(imagePath = it) } },
                label = stringResource(R.string.settings_watermark_path),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
            )
            Text(
                text = stringResource(R.string.settings_watermark_position),
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
            WatermarkPosition.entries.forEach { pos ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (state.watermark.position == pos) Rose.copy(alpha = 0.2f)
                            else Color.Transparent,
                        )
                        .clickable { onWatermarkChange { copy(position = pos) } }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pos.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.watermark.position == pos) Color.White else TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FrameSettingsSection(
    state: AllSettings,
    frames: List<TemplateEntity>,
    onFrameSelected: (Long?) -> Unit,
    onOpenFrameDesigner: () -> Unit,
) {
    SettingsCard(
        title = stringResource(R.string.settings_frame_overlay),
        iconRes = android.R.drawable.ic_menu_gallery,
    ) {
        // None option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (state.event.selectedFrameId == null) Rose.copy(alpha = 0.2f)
                    else Color.Transparent,
                )
                .clickable { onFrameSelected(null) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_no_frame),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.event.selectedFrameId == null) Color.White else TextSecondary,
            )
        }
        // Frame list
        frames.forEach { frame ->
            val isSelected = state.event.selectedFrameId == frame.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) Rose.copy(alpha = 0.2f) else Color.Transparent,
                    )
                    .clickable { onFrameSelected(frame.id) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = frame.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) Color.White else TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (isSelected) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.checkbox_on_background),
                        contentDescription = stringResource(R.string.settings_selected),
                        tint = Rose,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        ElevatedButton(
            onClick = onOpenFrameDesigner,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Gold,
                contentColor = Color.Black,
            ),
        ) {
            Text(stringResource(R.string.settings_manage_frames), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun FullScreenTemplatePreviewDialog(
    template: TemplateDefinition?,
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                TemplatePreview(
                    template = template,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!isSelected) {
                        ElevatedButton(
                            onClick = onSelect,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Rose,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(stringResource(R.string.settings_template_use_this), fontWeight = FontWeight.Medium)
                        }
                    }
                    ElevatedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = CardSurfaceLight,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(stringResource(R.string.gallery_close), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSection(
    onOpenWebsite: () -> Unit,
) {
    SettingsCard(
        title = stringResource(R.string.settings_about),
        iconRes = android.R.drawable.ic_dialog_info,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Text(
            text = stringResource(R.string.about_website_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        ElevatedButton(
            onClick = onOpenWebsite,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Gold,
                contentColor = Color.Black,
            ),
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_view),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.about_visit_website), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DebugSection(
    onResetQuota: () -> Unit,
) {
    SettingsCard(
        title = stringResource(R.string.settings_debug),
        iconRes = android.R.drawable.ic_menu_manage,
    ) {
        Text(
            text = stringResource(R.string.settings_debug_caption),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        ElevatedButton(
            onClick = onResetQuota,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Gold,
                contentColor = Color.Black,
            ),
        ) {
            Text(stringResource(R.string.settings_debug_reset_quota), fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * OutlinedTextField wrapper that keeps the editing buffer in local state. Without this,
 * round-tripping every keystroke through DataStore -> Flow -> recompose causes characters
 * to drop and the cursor to jump during fast typing. External value changes still
 * propagate when the field isn't focused.
 */
@Composable
private fun StableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.TextFieldColors,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var buffer by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (!isFocused && buffer != value) buffer = value
    }
    OutlinedTextField(
        value = buffer,
        onValueChange = {
            buffer = it
            onValueChange(it)
        },
        label = { Text(label) },
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(12.dp),
        colors = colors,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
    )
}

@Composable
private fun StyledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Rose,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = CardSurfaceLight,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SupportAndFeedbackSection(
    bugReports: List<BugReport>,
    isSubmitting: Boolean,
    submitError: String?,
    submitSuccess: Boolean,
    comments: List<GitHubComment>,
    commentsLoading: Boolean,
    commentsError: String?,
    issueDetails: GitHubIssue?,
    onSubmitBugReport: (String, String, String, String, Boolean, Uri?) -> Unit,
    onLoadIssue: (Int) -> Unit,
    onPostReply: (Int, String, Uri?, () -> Unit) -> Unit,
    onClearSubmitStatus: () -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedIssueNumber by remember { mutableStateOf<Int?>(null) }

    SettingsCard(
        title = "Support & Feedback",
        iconRes = android.R.drawable.ic_menu_help,
    ) {
        Text(
            text = "Have questions or found a bug? Report it here! Issues are tracked publicly on GitHub.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        ElevatedButton(
            onClick = {
                onClearSubmitStatus()
                showReportDialog = true
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Rose,
                contentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Report a Problem", fontWeight = FontWeight.Medium)
        }

        if (bugReports.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your Submitted Reports:",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bugReports.forEach { report ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardSurfaceLight)
                            .clickable {
                                selectedIssueNumber = report.number
                                onLoadIssue(report.number)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${report.number} ${report.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Submitted on ${report.createdAt.take(10)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val isOpen = report.status.equals("open", ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isOpen) Success.copy(alpha = 0.2f) else TextSecondary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isOpen) "Open" else "Closed",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOpen) Success else TextSecondary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportFormDialog(
            isSubmitting = isSubmitting,
            submitError = submitError,
            submitSuccess = submitSuccess,
            onSubmit = onSubmitBugReport,
            onDismiss = {
                showReportDialog = false
                onClearSubmitStatus()
            },
            textFieldColors = textFieldColors,
        )
    }

    if (selectedIssueNumber != null) {
        IssueDetailDialog(
            issueNumber = selectedIssueNumber!!,
            issueDetails = issueDetails,
            comments = comments,
            isLoading = commentsLoading,
            error = commentsError,
            onPostReply = { text, uri, onComplete ->
                onPostReply(selectedIssueNumber!!, text, uri, onComplete)
            },
            onDismiss = { selectedIssueNumber = null },
            textFieldColors = textFieldColors,
        )
    }
}

@Composable
private fun ReportFormDialog(
    isSubmitting: Boolean,
    submitError: String?,
    submitSuccess: Boolean,
    onSubmit: (String, String, String, String, Boolean, Uri?) -> Unit,
    onDismiss: () -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(true) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!isSubmitting) onDismiss() }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Report a Problem",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )

                    if (submitSuccess) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Success.copy(alpha = 0.15f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Thank you!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Success,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your bug report has been submitted successfully.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                ElevatedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = Success,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Close")
                                }
                            }
                        }
                    } else {
                        submitError?.let {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Rose.copy(alpha = 0.15f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Rose,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Rose.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .background(Rose.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "⚠️ Warning: Anything submitted here will be posted publicly to a GitHub repository issue tracker, including any screenshots or diagnostics you attach.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Rose,
                            )
                        }

                        StableTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = "Title / Subject",
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors,
                        )

                        StableTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "Description / Steps to Reproduce",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = textFieldColors,
                            singleLine = false,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StableTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = "Name (Optional)",
                                modifier = Modifier.weight(1f),
                                colors = textFieldColors,
                            )
                            StableTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = "Email (Optional)",
                                modifier = Modifier.weight(1f),
                                colors = textFieldColors,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeDiagnostics,
                                onCheckedChange = { includeDiagnostics = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Rose,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Include Device Diagnostics & Model config",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ElevatedButton(
                                onClick = {
                                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = CardSurfaceLight,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Attach Screenshot")
                            }

                            selectedImageUri?.let { uri ->
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Selected screenshot preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Rose)
                                            .align(Alignment.TopEnd)
                                            .clickable { selectedImageUri = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                            contentDescription = "Remove screenshot",
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color.White,
                                    disabledContentColor = TextSecondary
                                )
                            ) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            ElevatedButton(
                                onClick = {
                                    if (title.isNotBlank() && description.isNotBlank()) {
                                        onSubmit(title, description, name, email, includeDiagnostics, selectedImageUri)
                                    }
                                },
                                enabled = !isSubmitting && title.isNotBlank() && description.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = Rose,
                                    contentColor = Color.White,
                                    disabledContainerColor = CardSurfaceLight,
                                    disabledContentColor = TextSecondary
                                )
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Submit")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueDetailDialog(
    issueNumber: Int,
    issueDetails: GitHubIssue?,
    comments: List<GitHubComment>,
    isLoading: Boolean,
    error: String?,
    onPostReply: (String, Uri?, () -> Unit) -> Unit,
    onDismiss: () -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    var replyText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = issueDetails?.title ?: "Loading Issue #$issueNumber...",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val stateText = issueDetails?.state ?: "open"
                                val isOpen = stateText.equals("open", ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isOpen) Success.copy(alpha = 0.2f) else TextSecondary.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isOpen) "Open" else "Closed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOpen) Success else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GitHub Issue Tracker",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (issueDetails != null) {
                                ElevatedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(issueDetails.htmlUrl))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = Gold,
                                        contentColor = Color.Black,
                                    )
                                ) {
                                    Text("View on GitHub", fontWeight = FontWeight.Medium)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CardSurfaceLight)
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                    contentDescription = "Close dialog",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (isLoading && comments.isEmpty() && issueDetails == null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Rose)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                issueDetails?.body?.let { bodyText ->
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(CardSurfaceLight)
                                                .padding(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Original Description",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Gold,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Text(
                                                    text = issueDetails.createdAt.take(10),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary,
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = bodyText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White,
                                            )
                                        }
                                    }
                                }

                                items(comments) { comment ->
                                    val isAppUserReply = comment.body.startsWith("**[User Reply from App]**")
                                    val displayBody = if (isAppUserReply) {
                                        comment.body.removePrefix("**[User Reply from App]**").trim()
                                    } else {
                                        comment.body
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isAppUserReply) Rose.copy(alpha = 0.15f)
                                                else CardSurfaceLight
                                            )
                                            .border(
                                                1.dp,
                                                if (isAppUserReply) Rose.copy(alpha = 0.3f) else Color.Transparent,
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isAppUserReply) "You (from App)" else comment.user.login,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isAppUserReply) Rose else Gold,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                text = comment.createdAt.take(10),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = displayBody,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }

                        error?.let {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Rose.copy(alpha = 0.15f))
                                    .padding(12.dp)
                            ) {
                                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Rose)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = { Text("Write a reply...") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                maxLines = 3,
                            )

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardSurfaceLight)
                                    .clickable {
                                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_camera),
                                    contentDescription = "Attach screenshot to reply",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            ElevatedButton(
                                onClick = {
                                    if (replyText.isNotBlank()) {
                                        onPostReply(replyText, selectedImageUri) {
                                            replyText = ""
                                            selectedImageUri = null
                                        }
                                    }
                                },
                                enabled = !isLoading && replyText.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = Rose,
                                    contentColor = Color.White,
                                    disabledContainerColor = CardSurfaceLight,
                                    disabledContentColor = TextSecondary
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                if (isLoading && replyText.isNotBlank()) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Post")
                                }
                            }
                        }

                        selectedImageUri?.let { uri ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardSurfaceLight)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Reply screenshot preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Text(
                                        text = "Screenshot attached",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Rose)
                                        .clickable { selectedImageUri = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                        contentDescription = "Remove screenshot",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
