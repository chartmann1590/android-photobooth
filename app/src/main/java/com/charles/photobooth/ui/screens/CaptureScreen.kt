package com.charles.photobooth.ui.screens

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.net.Uri
import android.widget.VideoView
import android.widget.Toast
import androidx.camera.video.VideoRecordEvent
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
import androidx.compose.foundation.layout.Arrangement
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
import com.charles.photobooth.BuildConfig
import com.charles.photobooth.R
import com.charles.photobooth.data.MediaType
import com.charles.photobooth.camera.CameraCaptureManager
import com.charles.photobooth.camera.CaptureUiState
import com.charles.photobooth.camera.CaptureViewModel
import com.charles.photobooth.camera.MAX_VIDEO_DURATION_SECONDS
import com.charles.photobooth.camera.PhotoFilter
import com.charles.photobooth.monetization.BillingUiState
import com.charles.photobooth.monetization.PhotoQuotaState
import com.charles.photobooth.monetization.RewardedAdState
import com.charles.photobooth.camera.UploadStatus
import com.charles.photobooth.camera.VideoCaptureManager
import com.charles.photobooth.camera.videoOutputDir
import com.charles.photobooth.settings.SettingsRepository
import com.charles.photobooth.settings.ThermalPrinterSettings
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
    val videoManager = remember { VideoCaptureManager(context) }
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
    var videoCaptureEnabled by rememberSaveable { mutableStateOf(false) }
    var videoModeSelected by rememberSaveable { mutableStateOf(false) }
    var isRecordingVideo by rememberSaveable { mutableStateOf(false) }
    var recordingSeconds by rememberSaveable { mutableIntStateOf(0) }
    var pendingVideoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var boothPhotoCount by rememberSaveable { mutableIntStateOf(4) }
    var selectedTemplateKey by rememberSaveable { mutableStateOf(BuiltInTemplates.KEY_NONE) }
    var disabledTemplateKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var frontScreenFlashEnabled by rememberSaveable { mutableStateOf(false) }
    var boothPhotosTaken by rememberSaveable { mutableIntStateOf(0) }
    val boothPhotoIds = remember { mutableStateListOf<Long>() }
    var pendingNextBoothShot by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf(PhotoFilter.NONE) }
    var cameraId by rememberSaveable { mutableStateOf<String?>(null) }
    var thermalPrinterSettings by remember { mutableStateOf(ThermalPrinterSettings()) }
    var settingsLoaded by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var quotaReservedForSession by rememberSaveable { mutableIntStateOf(0) }

    val uiState by captureViewModel.uiState.collectAsStateWithLifecycle()

    val multiPhotoTemplate = remember(selectedTemplateKey, eventName, eventDate) {
        BuiltInTemplates.fromKey(selectedTemplateKey, eventName, eventDate)
    }
    // Any selected template (even a 1-frame event preset like Birthday) needs the compose
    // path so the themed background, title, and date overlay actually get applied.
    val effectiveBoothMode = boothMode || multiPhotoTemplate != null
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
        eventName = if (BuildConfig.WEDDING_MODE) context.getString(R.string.wedding_event_name) else settings.event.eventName
        eventDate = if (BuildConfig.WEDDING_MODE) context.getString(R.string.wedding_event_date) else settings.event.eventDate
        selectedFrameId = settings.event.selectedFrameId
        watermarkConfig = if (settings.watermark.enabled && settings.watermark.imagePath.isNotBlank()) {
            WatermarkConfig(
                imagePath = settings.watermark.imagePath,
                position = settings.watermark.position,
                sizePercent = settings.watermark.sizePercent,
            )
        } else null
        uploadSettings = if (BuildConfig.WEDDING_MODE) {
            settings.upload.copy(autoUploadEnabled = true, useAnonymousHost = true)
        } else {
            settings.upload
        }
        boothMode = settings.captureMode.boothMode
        gifModeEnabled = settings.captureMode.gifModeEnabled
        videoCaptureEnabled = settings.captureMode.videoCaptureEnabled
        if (!settings.captureMode.videoCaptureEnabled) {
            videoModeSelected = false
        }
        boothPhotoCount = settings.captureMode.boothPhotoCount
        disabledTemplateKeys = settings.captureMode.disabledTemplateKeys
        val storedTemplate = settings.captureMode.selectedTemplate
        selectedTemplateKey = if (storedTemplate in settings.captureMode.disabledTemplateKeys) {
            BuiltInTemplates.KEY_NONE
        } else {
            storedTemplate
        }
        frontScreenFlashEnabled = settings.camera.frontScreenFlashEnabled
        selectedFilter = try { PhotoFilter.valueOf(settings.captureMode.selectedFilter) } catch (_: Exception) { PhotoFilter.NONE }
        cameraId = settings.camera.cameraId
        thermalPrinterSettings = settings.thermalPrinter
        settingsLoaded = true
    }

    LaunchedEffect(previewView, settingsLoaded, videoModeSelected) {
        val view = previewView ?: return@LaunchedEffect
        if (!settingsLoaded) return@LaunchedEffect
        try {
            if (videoModeSelected) {
                videoManager.bindToLifecycle(
                    lifecycleOwner = lifecycleOwner,
                    previewView = view,
                    useFrontCamera = useFrontCamera,
                    specificCameraId = cameraId,
                )
            } else {
                cameraManager.bindToLifecycle(
                    lifecycleOwner = lifecycleOwner,
                    previewView = view,
                    useFrontCamera = useFrontCamera,
                    specificCameraId = cameraId,
                )
            }
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

    LaunchedEffect(isRecordingVideo) {
        if (!isRecordingVideo) return@LaunchedEffect
        recordingSeconds = 0
        while (isRecordingVideo && recordingSeconds < MAX_VIDEO_DURATION_SECONDS) {
            delay(1000)
            recordingSeconds += 1
        }
        if (isRecordingVideo) {
            videoManager.stopRecording()
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
        } else if (uiState is CaptureUiState.Capturing && videoModeSelected) {
            // Video path: countdown finished, kick off the actual recording. The recording
            // is managed by VideoCaptureManager and runs for MAX_VIDEO_DURATION_SECONDS.
            val outputDir = videoOutputDir(context.applicationContext as Application)
            if (outputDir == null) {
                captureViewModel.resetToIdle()
                Toast.makeText(context, context.getString(R.string.capture_video_save_failed), Toast.LENGTH_LONG).show()
                return@LaunchedEffect
            }
            val videoFile = java.io.File(outputDir, "video_${System.currentTimeMillis()}.mp4")
            pendingVideoPath = videoFile.absolutePath

            val useFrontScreenFlashVideo = frontScreenFlashEnabled && useFrontCamera
            val activityV = context.findActivity()
            val originalBrightnessV = activityV?.window?.attributes?.screenBrightness
            if (useFrontScreenFlashVideo) {
                showFlash = true
                activityV?.let { act ->
                    act.window.attributes = act.window.attributes.apply {
                        screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                    }
                }
            }
            fun restoreVideoBrightness() {
                if (useFrontScreenFlashVideo) {
                    showFlash = false
                    activityV?.let { act ->
                        act.window.attributes = act.window.attributes.apply {
                            screenBrightness = originalBrightnessV ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        }
                    }
                }
            }
            val started = videoManager.startRecordingToFile(videoFile) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    isRecordingVideo = false
                    recordingSeconds = 0
                    restoreVideoBrightness()
                    val path = pendingVideoPath
                    pendingVideoPath = null
                    if (event.hasError()) {
                        android.util.Log.e(
                            "VideoCapture",
                            "Recording finalize failed: code=${event.error}",
                            event.cause,
                        )
                        runCatching { videoFile.delete() }
                        captureViewModel.resetToIdle()
                        Toast.makeText(context, event.cause?.message ?: context.getString(R.string.capture_video_save_failed), Toast.LENGTH_LONG).show()
                        return@startRecordingToFile
                    }
                    val savedFile = path?.let { java.io.File(it) } ?: videoFile
                    captureViewModel.saveCapturedVideo(savedFile, eventName) { id ->
                        finishOrPreview(id)
                    }
                }
            }
            if (started) {
                isRecordingVideo = true
                recordingSeconds = 0
            } else {
                restoreVideoBrightness()
                pendingVideoPath = null
                captureViewModel.resetToIdle()
                Toast.makeText(context, context.getString(R.string.capture_video_unavailable), Toast.LENGTH_LONG).show()
            }
        } else if (uiState is CaptureUiState.Capturing) {
            previewView ?: return@LaunchedEffect
            val outputDir = context.cacheDir
            val file = java.io.File(outputDir, "capture_${System.currentTimeMillis()}.jpg")
            val useFrontScreenFlash = frontScreenFlashEnabled && useFrontCamera
            val activity = context.findActivity()
            val originalBrightness = activity?.window?.attributes?.screenBrightness
            try {
                if (useFrontScreenFlash) {
                    showFlash = true
                    activity?.let { act ->
                        act.window.attributes = act.window.attributes.apply {
                            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                        }
                    }
                    delay(150)
                }
                val uri = cameraManager.takePicture(file)
                if (!useFrontScreenFlash) {
                    showFlash = true
                } else {
                    // Restore screen brightness after the shot is captured.
                    activity?.let { act ->
                        act.window.attributes = act.window.attributes.apply {
                            screenBrightness = originalBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        }
                    }
                }
                captureViewModel.saveCapturedPhoto(
                    uri = uri,
                    eventName = eventName,
                    templateId = null,
                    selectedFrameId = selectedFrameId,
                    watermarkConfig = watermarkConfig,
                    filter = selectedFilter,
                    thermalPrinterSettings = thermalPrinterSettings,
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
                                } else if (gifModeEnabled && boothPhotoIds.size >= 2) {
                                    // GIF mode takes priority over template composing: if the
                                    // user explicitly opted into an animated GIF, that's what
                                    // they want — even when a multi-frame template is selected.
                                    android.util.Log.i(
                                        "BoothFlow",
                                        "Booth complete → creating GIF from ${boothPhotoIds.size} photos (gifModeEnabled=true, template=${multiPhotoTemplate != null})",
                                    )
                                    captureViewModel.createGifFromPhotos(
                                        photoIds = boothPhotoIds.toList(),
                                        eventName = eventName,
                                    ) { gifId ->
                                        quotaReservedForSession = 0
                                        android.util.Log.i("BoothFlow", "GIF creation completed → gifId=$gifId")
                                        finishOrPreview(gifId ?: id)
                                    }
                                } else if (multiPhotoTemplate != null) {
                                    android.util.Log.i(
                                        "BoothFlow",
                                        "Booth complete → composing template (gifModeEnabled=$gifModeEnabled, photos=${boothPhotoIds.size})",
                                    )
                                    captureViewModel.composeTemplate(
                                        photoIds = boothPhotoIds.toList(),
                                        template = multiPhotoTemplate,
                                        eventName = eventName,
                                    ) { compositeId ->
                                        quotaReservedForSession = 0
                                        if (compositeId == null) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.capture_template_failed),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                        finishOrPreview(compositeId ?: id)
                                    }
                                } else {
                                    android.util.Log.i(
                                        "BoothFlow",
                                        "Booth complete → no GIF/template (gifModeEnabled=$gifModeEnabled, photos=${boothPhotoIds.size}, template=null)",
                                    )
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
                // Make sure we don't strand the screen at max brightness on failure.
                if (useFrontScreenFlash) {
                    activity?.let { act ->
                        act.window.attributes = act.window.attributes.apply {
                            screenBrightness = originalBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        }
                    }
                }
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

            if (selectedFilter != PhotoFilter.NONE) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Rose.copy(alpha = 0.85f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.capture_filter_badge, selectedFilter.displayName),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkBackground.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                val statusText = when (uiState) {
                    is CaptureUiState.Idle -> if (isRecordingVideo) {
                        stringResource(R.string.capture_recording, recordingSeconds, MAX_VIDEO_DURATION_SECONDS)
                    } else {
                        stringResource(R.string.capture_ready)
                    }
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
                if (effectiveBoothMode && effectiveBoothCount > 1) {
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
            enter = fadeIn(tween(60)),
            exit = fadeOut(tween(400)),
        ) {
            val flashAlpha = if (frontScreenFlashEnabled && useFrontCamera) 1f else 0.85f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha)),
            )
        }

        // Recording timer — drawn AFTER the flash overlay so it remains visible even
        // when front-screen flash is active during video recording.
        AnimatedVisibility(
            visible = isRecordingVideo,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            val secondsLeft = (MAX_VIDEO_DURATION_SECONDS - recordingSeconds).coerceAtLeast(0)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkBackground.copy(alpha = 0.78f))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Rose),
                    )
                    Text(
                        text = stringResource(R.string.capture_recording_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "${secondsLeft}s",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState is CaptureUiState.Idle) {
                if (videoCaptureEnabled) {
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        FilledTonalButton(
                            onClick = { videoModeSelected = false },
                            enabled = !isRecordingVideo,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (!videoModeSelected) Rose else DarkBackground.copy(alpha = 0.65f),
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(stringResource(R.string.capture_photo_mode))
                        }
                        FilledTonalButton(
                            onClick = { videoModeSelected = true },
                            enabled = !isRecordingVideo,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (videoModeSelected) Rose else DarkBackground.copy(alpha = 0.65f),
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(stringResource(R.string.capture_video_mode))
                        }
                    }
                }

                val allChips = listOf(
                    BuiltInTemplates.KEY_NONE to stringResource(R.string.template_single),
                    BuiltInTemplates.KEY_STRIP_2X2 to stringResource(R.string.template_strip_2x2),
                    BuiltInTemplates.KEY_STRIP_VERTICAL to stringResource(R.string.template_strip_vertical),
                    BuiltInTemplates.KEY_BIRTHDAY to stringResource(R.string.template_birthday),
                    BuiltInTemplates.KEY_WEDDING to stringResource(R.string.template_wedding),
                    BuiltInTemplates.KEY_ANNIVERSARY to stringResource(R.string.template_anniversary),
                    BuiltInTemplates.KEY_HOLIDAY to stringResource(R.string.template_holiday),
                    BuiltInTemplates.KEY_GENERIC to stringResource(R.string.template_generic),
                )
                if (!videoModeSelected) {
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                    ) {
                        PhotoFilter.entries.forEach { filter ->
                            val selected = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) Rose else DarkBackground.copy(alpha = 0.65f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        if (selectedFilter != filter) {
                                            selectedFilter = filter
                                            scope.launch {
                                                settingsRepo.updateCaptureModeSettings { it.copy(selectedFilter = filter.name) }
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = filter.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }

                if (!videoModeSelected) {
                    // NONE always remains so users can always select "single shot"
                    val templateChips = allChips.filter { (k, _) -> k == BuiltInTemplates.KEY_NONE || k !in disabledTemplateKeys }
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
                        if (videoModeSelected) {
                            if (isRecordingVideo) {
                                videoManager.stopRecording()
                            } else {
                                // Trigger the same 3-second countdown used for photos. The actual
                                // recording starts in LaunchedEffect(countdown) when it reaches 0.
                                showFlash = false
                                captureViewModel.startCountdown()
                                countdown = 3
                            }
                            return@clickable
                        }
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
                    painter = painterResource(id = if (videoModeSelected) android.R.drawable.presence_video_online else android.R.drawable.ic_menu_camera),
                    contentDescription = stringResource(if (videoModeSelected) R.string.capture_video_button else R.string.capture_button),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    isCountingDown -> ""
                    videoModeSelected && isRecordingVideo -> stringResource(R.string.capture_stop_recording)
                    videoModeSelected -> stringResource(R.string.capture_tap_record_video)
                    else -> stringResource(R.string.capture_tap)
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }

        (uiState as? CaptureUiState.Preview)?.let { preview ->
            PostCapturePreviewOverlay(
                preview = preview,
                thermalPrinterSettings = thermalPrinterSettings,
                onThermalPrint = {
                    captureViewModel.printPhotoThermal(preview.photoId, thermalPrinterSettings)
                },
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun PostCapturePreviewOverlay(
    preview: CaptureUiState.Preview,
    thermalPrinterSettings: ThermalPrinterSettings,
    onThermalPrint: () -> Unit,
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
        if (preview.mediaType == MediaType.VIDEO) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.fromFile(java.io.File(preview.photoPath)))
                        setOnPreparedListener { player ->
                            player.isLooping = true
                            start()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            )
        } else {
            AsyncImage(
                model = java.io.File(preview.photoPath),
                contentDescription = stringResource(R.string.capture_preview_photo),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentScale = ContentScale.Fit,
            )
        }

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
                    if (BuildConfig.WEDDING_MODE) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.wedding_gallery_password_label, BuildConfig.WEDDING_GALLERY_PASSWORD),
                            style = MaterialTheme.typography.titleMedium,
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (
                    preview.mediaType == MediaType.IMAGE &&
                    thermalPrinterSettings.enabled &&
                    thermalPrinterSettings.deviceAddress.isNotBlank()
                ) {
                    FilledTonalButton(
                        onClick = onThermalPrint,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Gold,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_send),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thermal Print", fontWeight = FontWeight.Medium)
                    }
                }
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
}
