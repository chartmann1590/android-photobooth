package com.charles.photobooth.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
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

    suspend fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean,
        specificCameraId: String? = null,
    ) {
        val cameraProvider = getCameraProvider()

        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        val videoCapture = VideoCapture.withOutput(recorder)

        val selector = if (specificCameraId != null) {
            CameraSelector.Builder()
                .addCameraFilter { cameras ->
                    cameras.filter { cam ->
                        cam.cameraSelector.toString().contains(specificCameraId)
                    }
                }
                .build()
        } else if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            selector,
            preview,
            videoCapture,
        )
        this.videoCapture = videoCapture
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
        val capture = videoCapture ?: return false
        val outputOptions = FileOutputOptions.Builder(outputFile).build()
        activeRecording = capture.output.prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
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
