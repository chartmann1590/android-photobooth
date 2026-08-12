package com.charles.photobooth.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface VideoCaptureState {
    data object Idle : VideoCaptureState
    data object Recording : VideoCaptureState
    data class Finished(val uri: android.net.Uri) : VideoCaptureState
    data class Error(val message: String) : VideoCaptureState
}

const val MAX_VIDEO_DURATION_SECONDS = 8

class VideoCaptureManager(
    private val context: Context,
) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    @SuppressLint("UnsafeOptInUsageError")
    suspend fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean,
        specificCameraId: String? = null,
    ) {
        val cameraProvider = getCameraProvider()

        // Force 16:9 on the preview so it shares aspect ratio with the video stream.
        // Pixel 8 Pro's Tensor encoder rejects buffers when preview and video have
        // mismatched dimensions (e.g. 1600x1200 preview + HD video → ERROR_NO_VALID_DATA).
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()
        val preview = androidx.camera.core.Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // SD-first ordering. On Tensor (Pixel 8 Pro) the MFC encoder intermittently
        // rejects HD/FHD buffer formats with VIDIOC_QBUF EINVAL → ERROR_NO_VALID_DATA.
        // SD (480p) is the universally supported encoder profile.
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.fromOrderedList(
                    listOf(Quality.SD, Quality.HD),
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
                )
            )
            .build()
        val videoCapture = VideoCapture.withOutput(recorder)

        val selectors = mutableListOf<CameraSelector>()
        if (specificCameraId != null) {
            selectors.add(
                CameraSelector.Builder()
                    .addCameraFilter { cameras ->
                        val filtered = cameras.filter { cam ->
                            Camera2CameraInfo.from(cam).cameraId == specificCameraId
                        }
                        if (filtered.isEmpty()) cameras else filtered
                    }
                    .build()
            )
        }
        selectors.add(if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA)
        selectors.add(if (useFrontCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA)

        // ViewPort forces Preview and VideoCapture to share the same crop rect, which
        // ensures the encoder gets buffers it can actually process. Without this, Pixel 8
        // Pro picks mismatched preview/video dimensions and the encoder rejects buffers.
        val viewPort = ViewPort.Builder(Rational(16, 9), Surface.ROTATION_0)
            .setScaleType(ViewPort.FILL_CENTER)
            .build()
        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(videoCapture)
            .setViewPort(viewPort)
            .build()

        cameraProvider.unbindAll()
        var lastError: Exception? = null
        for (selector in selectors) {
            try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    useCaseGroup,
                )
                this.videoCapture = videoCapture
                Log.i("VideoCapture", "Bound video pipeline (selector=${selector}, SD-first quality, 16:9 viewport)")
                return
            } catch (e: Exception) {
                Log.w("VideoCapture", "Bind failed for selector $selector: ${e.message}")
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("No camera available for video")
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        context: Context,
        onEvent: (VideoRecordEvent) -> Unit,
    ): Boolean {
        val capture = videoCapture ?: return false
        val recorder = capture.output

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "photobooth_video_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
            .setContentValues(contentValues)
            .build()

        activeRecording = recorder.prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                onEvent(event)
            }
        return true
    }

    @SuppressLint("MissingPermission")
    fun startRecordingToFile(
        outputFile: java.io.File,
        onEvent: (VideoRecordEvent) -> Unit,
    ): Boolean {
        val capture = videoCapture ?: run {
            Log.e("VideoCapture", "startRecordingToFile: videoCapture is null (not bound)")
            return false
        }
        val outputOptions = FileOutputOptions.Builder(outputFile).build()
        Log.i("VideoCapture", "Starting recording to ${outputFile.absolutePath}")
        activeRecording = capture.output.prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> Log.i("VideoCapture", "Event: Start")
                    is VideoRecordEvent.Status -> Log.d(
                        "VideoCapture",
                        "Event: Status duration=${event.recordingStats.recordedDurationNanos / 1_000_000}ms bytes=${event.recordingStats.numBytesRecorded}",
                    )
                    is VideoRecordEvent.Finalize -> Log.i(
                        "VideoCapture",
                        "Event: Finalize error=${event.error} bytes=${event.outputResults.outputUri} cause=${event.cause?.message}",
                    )
                    else -> Log.d("VideoCapture", "Event: ${event::class.simpleName}")
                }
                onEvent(event)
            }
        return true
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        cont.resume(future.get())
                    } catch (e: Exception) {
                        if (cont.isActive) {
                            cont.resumeWithException(e)
                        }
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
}
