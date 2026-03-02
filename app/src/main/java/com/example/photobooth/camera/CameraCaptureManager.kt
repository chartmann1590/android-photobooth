package com.example.photobooth.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraCaptureManager(
    private val context: Context,
) {
    private var imageCapture: ImageCapture? = null

    suspend fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean,
    ) {
        val cameraProvider = getCameraProvider()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val selector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            selector,
            preview,
            imageCapture,
        )
        this.imageCapture = imageCapture
    }

    suspend fun takePicture(outputFile: File): Uri = suspendCancellableCoroutine { cont ->
        val capture = imageCapture
        if (capture == null) {
            cont.resumeWithException(IllegalStateException("ImageCapture not bound"))
            return@suspendCancellableCoroutine
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    if (cont.isActive) {
                        cont.resumeWithException(exception)
                    }
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = outputFileResults.savedUri ?: Uri.fromFile(outputFile)
                    if (cont.isActive) {
                        cont.resume(uri)
                    }
                }
            },
        )
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

