package com.example.photobooth.ui.screens

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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import androidx.lifecycle.LifecycleEventObserver
import com.example.photobooth.R
import com.example.photobooth.camera.CameraCaptureManager
import com.example.photobooth.camera.CaptureUiState
import com.example.photobooth.camera.CaptureViewModel
import com.example.photobooth.settings.SettingsRepository
import com.example.photobooth.ui.theme.DarkBackground
import com.example.photobooth.ui.theme.Gold
import com.example.photobooth.ui.theme.Rose
import com.example.photobooth.ui.theme.RoseLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.Locale

@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onFinishedCapture: (Long?) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val captureViewModel: CaptureViewModel = viewModel()
    val settingsRepo = remember { SettingsRepository(context) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val cameraManager = remember { CameraCaptureManager(context) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var countdown by rememberSaveable { mutableIntStateOf(0) }
    var useFrontCamera by rememberSaveable { mutableStateOf(true) }
    var eventName by rememberSaveable { mutableStateOf(context.getString(R.string.default_event_name)) }
    var selectedFrameId by rememberSaveable { mutableStateOf<Long?>(null) }

    val uiState by captureViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val settings = settingsRepo.settingsFlow.first()
        useFrontCamera = settings.camera.useFrontCamera
        eventName = settings.event.eventName
        selectedFrameId = settings.event.selectedFrameId
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

    LaunchedEffect(previewView, useFrontCamera) {
        val view = previewView ?: return@LaunchedEffect
        cameraManager.bindToLifecycle(
            lifecycleOwner = lifecycleOwner,
            previewView = view,
            useFrontCamera = useFrontCamera,
        )
    }

    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Error) {
            Toast.makeText(context, (uiState as CaptureUiState.Error).message, Toast.LENGTH_LONG).show()
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
                captureViewModel.saveCapturedPhoto(
                    uri = uri,
                    eventName = eventName,
                    templateId = null,
                    selectedFrameId = selectedFrameId,
                    onComplete = onFinishedCapture,
                )
            } catch (e: Exception) {
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
                val statusText = when (uiState) {
                    is CaptureUiState.Idle -> stringResource(R.string.capture_ready)
                    is CaptureUiState.CountingDown -> stringResource(R.string.capture_get_ready)
                    is CaptureUiState.Capturing -> stringResource(R.string.capture_capturing)
                    is CaptureUiState.Saved -> stringResource(R.string.capture_saved)
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Rose.copy(alpha = 0.8f),
                                    Rose.copy(alpha = 0.2f),
                                ),
                            ),
                        )
                        .border(3.dp, RoseLight.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = seconds.toString(),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (seconds == 1) stringResource(R.string.capture_smile) else stringResource(R.string.capture_get_ready),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
                        captureViewModel.startCountdown()
                        countdown = 3
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
    }
}
