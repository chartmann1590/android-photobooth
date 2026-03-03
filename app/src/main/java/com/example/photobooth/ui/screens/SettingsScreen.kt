package com.example.photobooth.ui.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.photobooth.data.TemplateEntity
import com.example.photobooth.settings.AllSettings
import com.example.photobooth.ui.theme.CardSurface
import com.example.photobooth.ui.theme.CardSurfaceLight
import com.example.photobooth.ui.theme.DarkBackground
import com.example.photobooth.ui.theme.Gold
import com.example.photobooth.ui.theme.Rose
import com.example.photobooth.ui.theme.Success
import com.example.photobooth.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenFrameDesigner: () -> Unit = {},
) {
    val vm: SettingsViewModel = viewModel()
    val state by vm.settings.collectAsState()
    val status by vm.testStatus.collectAsState()
    val frames by vm.frames.collectAsState()

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
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Settings",
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
            CameraSettingsSection(state, onCameraChange = vm::updateCamera)
            FrameSettingsSection(
                state = state,
                frames = frames,
                onFrameSelected = vm::updateSelectedFrame,
                onOpenFrameDesigner = onOpenFrameDesigner,
            )
            UploadSettingsSection(state, onUploadChange = vm::updateUpload, textFieldColors = textFieldColors)
            SmsSettingsSection(state, onSmsChange = vm::updateSms, onTestSms = vm::testSms, textFieldColors = textFieldColors)
            SmtpSettingsSection(state, onSmtpChange = vm::updateSmtp, onTestEmail = vm::testEmail, textFieldColors = textFieldColors)
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
    onEventChange: (String, String) -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    SettingsCard(
        title = "Event",
        iconRes = android.R.drawable.ic_menu_today,
    ) {
        OutlinedTextField(
            value = state.event.eventName,
            onValueChange = { onEventChange(it, state.event.filenamePattern) },
            label = { Text("Event name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            singleLine = true,
        )
        OutlinedTextField(
            value = state.event.filenamePattern,
            onValueChange = { onEventChange(state.event.eventName, it) },
            label = { Text("Filename pattern") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            singleLine = true,
        )
    }
}

@Composable
private fun UploadSettingsSection(
    state: AllSettings,
    onUploadChange: (Boolean, String, String, String) -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    SettingsCard(
        title = "Upload",
        iconRes = android.R.drawable.ic_menu_upload,
    ) {
        StyledSwitch(
            label = "Use anonymous host (0x0.st)",
            checked = state.upload.useAnonymousHost,
            onCheckedChange = {
                onUploadChange(it, state.upload.immichBaseUrl, state.upload.immichApiToken, state.upload.immichAlbumId)
            },
        )
        OutlinedTextField(
            value = state.upload.immichBaseUrl,
            onValueChange = { onUploadChange(state.upload.useAnonymousHost, it, state.upload.immichApiToken, state.upload.immichAlbumId) },
            label = { Text("Immich base URL") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            singleLine = true,
        )
        OutlinedTextField(
            value = state.upload.immichApiToken,
            onValueChange = { onUploadChange(state.upload.useAnonymousHost, state.upload.immichBaseUrl, it, state.upload.immichAlbumId) },
            label = { Text("Immich API token") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            singleLine = true,
        )
        OutlinedTextField(
            value = state.upload.immichAlbumId,
            onValueChange = { onUploadChange(state.upload.useAnonymousHost, state.upload.immichBaseUrl, state.upload.immichApiToken, it) },
            label = { Text("Immich album ID") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            singleLine = true,
        )
    }
}

@Composable
private fun SmsSettingsSection(
    state: AllSettings,
    onSmsChange: (String, String, String, Boolean) -> Unit,
    onTestSms: () -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
) {
    SettingsCard(
        title = "SMS Gateway",
        iconRes = android.R.drawable.ic_dialog_email,
    ) {
        OutlinedTextField(
            value = state.sms.baseUrl,
            onValueChange = { onSmsChange(it, state.sms.username, state.sms.password, state.sms.useCloudServer) },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            singleLine = true,
        )
        OutlinedTextField(
            value = state.sms.username,
            onValueChange = { onSmsChange(state.sms.baseUrl, it, state.sms.password, state.sms.useCloudServer) },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            singleLine = true,
        )
        OutlinedTextField(
            value = state.sms.password,
            onValueChange = { onSmsChange(state.sms.baseUrl, state.sms.username, it, state.sms.useCloudServer) },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            singleLine = true,
        )
        StyledSwitch(
            label = "Cloud server mode",
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
            Text("Test SMS", fontWeight = FontWeight.Medium)
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
        title = "SMTP / Email",
        iconRes = android.R.drawable.ic_dialog_email,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.smtp.host,
                onValueChange = { onSmtpChange(SmtpFieldChange.Host(it)) },
                label = { Text("SMTP host") },
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true,
            )
            OutlinedTextField(
                value = state.smtp.port.toString(),
                onValueChange = { onSmtpChange(SmtpFieldChange.Port(it.toIntOrNull() ?: state.smtp.port)) },
                label = { Text("Port") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true,
            )
        }
        StyledSwitch(
            label = "Use TLS",
            checked = state.smtp.useSslTls,
            onCheckedChange = { onSmtpChange(SmtpFieldChange.UseTls(it)) },
        )
        OutlinedTextField(
            value = state.smtp.username,
            onValueChange = { onSmtpChange(SmtpFieldChange.Username(it)) },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            singleLine = true,
        )
        OutlinedTextField(
            value = state.smtp.password,
            onValueChange = { onSmtpChange(SmtpFieldChange.Password(it)) },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.smtp.fromAddress,
                onValueChange = { onSmtpChange(SmtpFieldChange.FromAddress(it)) },
                label = { Text("From address") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true,
            )
            OutlinedTextField(
                value = state.smtp.fromName,
                onValueChange = { onSmtpChange(SmtpFieldChange.FromName(it)) },
                label = { Text("From name") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true,
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
            Text("Test Email / DNS", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CameraSettingsSection(
    state: AllSettings,
    onCameraChange: (Boolean) -> Unit,
) {
    SettingsCard(
        title = "Camera",
        iconRes = android.R.drawable.ic_menu_camera,
    ) {
        StyledSwitch(
            label = "Use front camera",
            checked = state.camera.useFrontCamera,
            onCheckedChange = onCameraChange,
        )
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
        title = "Frame Overlay",
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
                text = "None (no frame overlay)",
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
                        contentDescription = "Selected",
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
            Text("Manage Frames", fontWeight = FontWeight.Medium)
        }
    }
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
