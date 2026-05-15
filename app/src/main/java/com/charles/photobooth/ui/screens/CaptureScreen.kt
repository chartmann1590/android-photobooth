package com.charles.photobooth.ui.screens

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.lifecycle.LifecycleEventObserver
import com.charles.photobooth.R
import com.charles.photobooth.camera.CameraCaptureManager
import com.charles.photobooth.camera.CaptureUiState
import com.charles.photobooth.camera.CaptureViewModel
import com.charles.photobooth.camera.PhotoFilter
import com.charles.photobooth.monetization.BillingUiState
import com.charles.photobooth.monetization.PhotoQuotaState
import com.charles.photobooth.monetization.RewardedAdState
import com.charles.photobooth.camera.UploadStatus
import com.charles.photobooth.settings.SettingsRepository
import com.charles.photobooth.settings.UploadSettings
import com.charles.photobooth.template.BuiltInTemplates
import com.charles.photobooth.template.WatermarkConfig
import com.charles.photobooth.ui.components.TemplatePreview
import com.charles.photobooth.util.QrCodeGenerator
import com.charles.photobooth.ui.theme.DarkBackground
import com.charles.photobooth.ui.theme.Gold
import com.charles.photobooth.ui.theme.Rose
import com.charles.photobooth.ui.theme.RoseLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onFinishedCapture: (Long?) -> Unit,
    quotaState: PhotoQuotaState = PhotoQuotaState(),
    rewardedAdState: RewardedAdState = RewardedAdState(),
    billingState: BillingUiState = BillingUiState(),
    onReservePhotos: suspend (Int) -> Boolean = { true },
    onRefundPhotos: (Int) -> Unit = {},
    onWatchRewardedAd: () -> Unit = {},
    onBuyUnlimited: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val captureViewModel: CaptureViewModel = viewModel()
    val settingsRepo = remember { SettingsRepository(context) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val cameraManager = remember { CameraCaptureManager(context) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var countdown by rememberSaveable { mutableIntStateOf(0) }
    var useFrontCamera by rememberSaveable { mutableStateOf(true) }
    var eventName by rememberSaveable { mutableStateOf(context.getString(R.string.default_event_name)) }
    var eventDate by rememberSaveable { mutableStateOf("") }
    var selectedFrameId by rememberSaveable { mutableStateOf<Long?>(null) }
    var watermarkConfig by remember { mutableStateOf<WatermarkConfig?>(null) }
    var uploadSettings by remember { mutableStateOf(UploadSettings()) }
    var boothMode by rememberSaveable { mutableStateOf(false) }
    var gifModeEnabled by rememberSaveable { mutableStateOf(false) }
    var boothPhotoCount by rememberSaveable { mutableIntStateOf(4) }
    var selectedTemplateKey by rememberSaveable { mutableStateOf(BuiltInTemplates.KEY_NONE) }
    var boothPhotosTaken by rememberSaveable { mutableIntStateOf(0) }
    val boothPhotoIds = remember { mutableStateListOf<Long>() }
    var pendingNextBoothShot by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf(PhotoFilter.NONE) }
    var cameraId by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsLoaded by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var quotaReservedForSession by rememberSaveable { mutableIntStateOf(0) }

    val uiState by captureViewModel.uiState.collectAsStateWithLifecycle()

    val multiPhotoTemplate = remember(selectedTemplateKey, eventName, eventDate) {
        BuiltInTemplates.fromKey(selectedTemplateKey, eventName, eventDate)
    }
    val effectiveBoothMode = boothMode || (multiPhotoTemplate?.frames?.size ?: 0) > 1
    val effectiveBoothCount = multiPhotoTemplate?.frames?.size ?: boothPhotoCount

    fun finishOrPreview(photoId: Long?) {
        if (photoId != null && uploadSettings.autoUploadEnabled && uploadSettings.isAnyUploadDestinationReady) {
            captureViewModel.enterPreviewAndUpload(photoId, uploadSettings)
        } else {
            onFinishedCapture(photoId)
        }
    }

    LaunchedEffect(Unit) {
        val settings = settingsRepo.settingsFlow.first()
        useFrontCamera = settings.camera.useFrontCamera
        eventName = settings.event.eventName
        eventDate = settings.event.eventDate
        selectedFrameId = settings.event.selectedFrameId
        watermarkConfig = if (settings.watermark.enabled && settings.watermark.imagePath.isNotBlank()) {
            WatermarkConfig(
                imagePath = settings.watermark.imagePath,
                position = settings.watermark.position,
                sizePercent = settings.watermark.sizePercent,
            )
        } else null
        uploadSettings = settings.upload
        boothMode = settings.captureMode.boothMode
        gifModeEnabled = settings.captureMode.gifModeEnabled
        boothPhotoCount = settings.captureMode.boothPhotoCount
        selectedTemplateKey = settings.captureMode.selectedTemplate
        selectedFilter = try { PhotoFilter.valueOf(settings.captureMode.selectedFilter) } catch (_: Exception) { PhotoFilter.NONE }
        cameraId = settings.camera.cameraId
        settingsLoaded = true
    }

    LaunchedEffect(previewView, settingsLoaded) {
        val view = previewView ?: return@LaunchedEffect
        if (!settingsLoaded) return@LaunchedEffect
        try {
            cameraManager.bindToLifecycle(
                lifecycleOwner = lifecycleOwner,
                previewView = view,
                useFrontCamera = useFrontCamera,
                specificCameraId = cameraId,
            )
        } catch (_: Exception) {
        }
    }

    val isCountingDown = uiState is CaptureUiState.CountingDown

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                captureViewModel.resetToIdle()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
        onDispose {
            tts?.shutdown()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Error) {
            Toast.makeText(context, (uiState as CaptureUiState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(pendingNextBoothShot) {
        if (pendingNextBoothShot) {
            delay(2000)
            showFlash = false
            pendingNextBoothShot = false
            captureViewModel.startCountdown()
            countdown = 3
        }
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            if (countdown == 1) {
                tts?.speak(context.getString(R.string.capture_smile), TextToSpeech.QUEUE_FLUSH, null, "SMILE_PROMPT")
            }
            delay(1000)
            val next = countdown - 1
            captureViewModel.tickCountdown(next)
            countdown = next
        } else if (uiState is CaptureUiState.Capturing) {
            previewView ?: return@LaunchedEffect
            val outputDir = context.cacheDir
            val file = java.io.File(outputDir, "capture_${System.currentTimeMillis()}.jpg")
            try {
                val uri = cameraManager.takePicture(file)
                showFlash = true
                captureViewModel.saveCapturedPhoto(
                    uri = uri,
                    eventName = eventName,
                    templateId = null,
                    selectedFrameId = selectedFrameId,
                    watermarkConfig = watermarkConfig,
                    filter = selectedFilter,
                    onComplete = { id ->
                        if (id == null) {
                            val refundCount = (quotaReservedForSession - boothPhotosTaken).coerceAtLeast(0)
                            if (refundCount > 0) onRefundPhotos(refundCount)
                            quotaReservedForSession = 0
                            onFinishedCapture(null)
                            return@saveCapturedPhoto
                        }
                        if (effectiveBoothMode) {
                                boothPhotoIds.add(id)
                                boothPhotosTaken++
                                if (boothPhotosTaken < effectiveBoothCount) {
                                    captureViewModel.resetToIdle()
                                    pendingNextBoothShot = true
                                } else if (multiPhotoTemplate != null) {
                                    captureViewModel.composeTemplate(
                                        photoIds = boothPhotoIds.toList(),
                                        template = multiPhotoTemplate,
                                        eventName = eventName,
                                    ) { compositeId ->
                                        quotaReservedForSession = 0
                                        finishOrPreview(compositeId ?: id)
                                    }
                                } else if (gifModeEnabled && boothPhotoIds.size >= 2) {
                                    captureViewModel.createGifFromPhotos(
                                        photoIds = boothPhotoIds.toList(),
                                        eventName = eventName,
                                    ) { gifId ->
                                        quotaReservedForSession = 0
                                        finishOrPreview(gifId ?: id)
                                    }
                                } else {
                                    quotaReservedForSession = 0
                                    finishOrPreview(id)
                                }
                        } else {
                            quotaReservedForSession = 0
                            finishOrPreview(id)
                        }
                    },
                )
            } catch (e: Exception) {
                val refundCount = (quotaReservedForSession - boothPhotosTaken).coerceAtLeast(0)
                if (refundCount > 0) onRefundPhotos(refundCount)
                quotaReservedForSession = 0
                captureViewModel.resetToIdle()
                Toast.makeText(context, context.getString(R.string.capture_error) + ": ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView = it }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground.copy(alpha = 0.7f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DarkBackground.copy(alpha = 0.8f),
                        ),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkBackground.copy(alpha = 0.5f))
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
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkBackground.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (quotaState.hasUnlimitedPhotos) {
                        stringResource(R.string.capture_counter_unlimited)
                    } else {
                        stringResource(R.string.capture_counter_remaining, quotaState.remainingPhotos)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (quotaState.hasUnlimitedPhotos) Gold else Color.White,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkBackground.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                val statusText = when (uiState) {
                    is CaptureUiState.Idle -> stringResource(R.string.capture_ready)
                    is CaptureUiState.CountingDown -> stringResource(R.string.capture_get_ready)
                    is CaptureUiState.Capturing -> stringResource(R.string.capture_capturing)
                    is CaptureUiState.Saved -> stringResource(R.string.capture_saved)
                    is CaptureUiState.Preview -> stringResource(R.string.capture_saved)
                    is CaptureUiState.Error -> stringResource(R.string.capture_error)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = when (uiState) {
                        is CaptureUiState.Saved -> Gold
                        is CaptureUiState.Error -> MaterialTheme.colorScheme.error
                        else -> Color.White
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = isCountingDown,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            val seconds = (uiState as? CaptureUiState.CountingDown)?.secondsLeft ?: 0
            val pulseScale by animateFloatAsState(
                targetValue = if (isCountingDown) 1f else 0.5f,
                animationSpec = tween(300),
                label = "pulse",
            )
            val ringColor = when (seconds) {
                3 -> Color(0xFF42A5F5)
                2 -> Gold
                else -> Rose
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ringColor.copy(alpha = 0.9f),
                                    ringColor.copy(alpha = 0.2f),
                                ),
                            ),
                        )
                        .border(4.dp, ringColor.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = seconds.toString(),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (seconds == 1) stringResource(R.string.capture_smile) else stringResource(R.string.capture_get_ready),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold,
                )
                if (effectiveBoothMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${boothPhotosTaken + 1}/$effectiveBoothCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gold,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showFlash,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(400)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f)),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState is CaptureUiState.Idle) {
                val templateChips = listOf(
                    BuiltInTemplates.KEY_NONE to stringResource(R.string.template_single),
                    BuiltInTemplates.KEY_STRIP_2X2 to stringResource(R.string.template_strip_2x2),
                    BuiltInTemplates.KEY_STRIP_VERTICAL to stringResource(R.string.template_strip_vertical),
                    BuiltInTemplates.KEY_BIRTHDAY to stringResource(R.string.template_birthday),
                    BuiltInTemplates.KEY_WEDDING to stringResource(R.string.template_wedding),
                    BuiltInTemplates.KEY_ANNIVERSARY to stringResource(R.string.template_anniversary),
                    BuiltInTemplates.KEY_HOLIDAY to stringResource(R.string.template_holiday),
                    BuiltInTemplates.KEY_GENERIC to stringResource(R.string.template_generic),
                )
                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 0.dp)
                        .padding(bottom = 12.dp),
                ) {
                    templateChips.forEach { (key, label) ->
                        val selected = selectedTemplateKey == key
                        val previewTemplate = when (key) {
                            BuiltInTemplates.KEY_NONE -> null
                            BuiltInTemplates.KEY_STRIP_2X2 -> BuiltInTemplates.photoStrip2x2(eventName)
                            BuiltInTemplates.KEY_STRIP_VERTICAL -> BuiltInTemplates.photoStripVertical(eventName)
                            else -> BuiltInTemplates.fromKey(key, eventName, eventDate.ifBlank { "Date" })
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Rose else DarkBackground.copy(alpha = 0.65f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { selectedTemplateKey = key }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            TemplatePreview(
                                template = previewTemplate,
                                modifier = Modifier.width(64.dp),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(
                        width = 4.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Rose, RoseLight),
                        ),
                        shape = CircleShape,
                    )
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCountingDown) Rose.copy(alpha = 0.5f)
                        else Rose,
                    )
                    .clickable(
                        enabled = uiState is CaptureUiState.Idle,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        scope.launch {
                            val requiredPhotos = if (effectiveBoothMode) effectiveBoothCount else 1
                            if (onReservePhotos(requiredPhotos)) {
                                quotaReservedForSession = requiredPhotos
                                showFlash = false
                                boothPhotosTaken = 0
                                boothPhotoIds.clear()
                                pendingNextBoothShot = false
                                captureViewModel.startCountdown()
                                countdown = 3
                            } else {
                                showPaywall = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_camera),
                    contentDescription = stringResource(R.string.capture_button),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isCountingDown) "" else stringResource(R.string.capture_tap),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }

        (uiState as? CaptureUiState.Preview)?.let { preview ->
            PostCapturePreviewOverlay(
                preview = preview,
                onDone = {
                    captureViewModel.resetToIdle()
                    onFinishedCapture(preview.photoId)
                },
            )
        }

        if (showPaywall) {
            PaywallDialog(
                quotaState = quotaState,
                billingState = billingState,
                rewardedAdState = rewardedAdState,
                onWatchAd = onWatchRewardedAd,
                onBuyUnlimited = onBuyUnlimited,
                onDismiss = { showPaywall = false },
            )
        }
    }
}

@Composable
private fun PostCapturePreviewOverlay(
    preview: CaptureUiState.Preview,
    onDone: () -> Unit,
) {
    var minPreviewElapsed by remember(preview.photoId) { mutableStateOf(false) }
    LaunchedEffect(preview.photoId) {
        kotlinx.coroutines.delay(5000)
        minPreviewElapsed = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        AsyncImage(
            model = java.io.File(preview.photoPath),
            contentDescription = stringResource(R.string.capture_preview_photo),
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentScale = ContentScale.Fit,
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val status = preview.uploadStatus
            when {
                status is UploadStatus.Complete && minPreviewElapsed -> {
                    val qrBitmap = remember(status.url) {
                        QrCodeGenerator.generate(status.url, 360)
                    }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Box(
                            modifier = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.gallery_qr_code),
                                modifier = Modifier.size(180.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.capture_scan_to_get_photo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                }
                status is UploadStatus.Failed -> {
                    Text(
                        text = stringResource(R.string.capture_upload_failed, status.message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Rose,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.capture_preview_uploading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            FilledTonalButton(
                onClick = onDone,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Rose,
                    contentColor = Color.White,
                ),
            ) {
                Text(stringResource(R.string.capture_preview_done), fontWeight = FontWeight.Medium)
            }
        }
    }
}
