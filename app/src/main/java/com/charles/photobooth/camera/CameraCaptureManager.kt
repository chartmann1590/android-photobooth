package com.charles.photobooth.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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

data class CameraInfo(
    val id: String,
    val displayName: String,
    val isFrontFacing: Boolean,
)

class CameraCaptureManager(
    private val context: Context,
) {
    private var imageCapture: ImageCapture? = null

    fun getAvailableCameras(): List<CameraInfo> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return emptyList()
        return cameraManager.cameraIdList.map { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                ?: CameraCharacteristics.LENS_FACING_BACK
            val isFront = facing == CameraCharacteristics.LENS_FACING_FRONT
            CameraInfo(
                id = id,
                displayName = if (isFront) "Front Camera" else "Back Camera ($id)",
                isFrontFacing = isFront,
            )
        }
    }

    suspend fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean,
        specificCameraId: String? = null,
    ) {
        val cameraProvider = getCameraProvider()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val selectors = mutableListOf<CameraSelector>()
        if (specificCameraId != null) {
            selectors.add(
                CameraSelector.Builder()
                    .addCameraFilter { cameras ->
                        val filtered = cameras.filter { cam ->
                            cam.cameraSelector.toString().contains(specificCameraId)
                        }
                        if (filtered.isEmpty()) cameras else filtered
                    }
                    .build()
            )
        }
        selectors.add(if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA)
        selectors.add(if (useFrontCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA)

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        cameraProvider.unbindAll()
        var lastError: Exception? = null
        for (selector in selectors) {
            try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture,
                )
                this.imageCapture = imageCapture
                return
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("No camera available")
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
