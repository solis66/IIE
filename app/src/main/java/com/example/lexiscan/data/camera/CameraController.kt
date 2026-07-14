package com.example.lexiscan.data.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var onImageCaptured: ((Uri) -> Unit)? = null

    var isTorchOn: Boolean = false
        private set

    fun setOnImageCapturedListener(listener: (Uri) -> Unit) {
        onImageCaptured = listener
    }

    fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(android.util.Size(1920, 1920))  // 限制输出分辨率防止 OOM
                .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "相机绑定失败", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto() {
        val capture = imageCapture ?: return

        val outputDir = File(context.cacheDir, "captures")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val photoFile = File(outputDir, "capture_${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Log.d(TAG, "照片已保存: $savedUri")
                    onImageCaptured?.invoke(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "拍照失败", exception)
                }
            }
        )
    }

    fun toggleTorch(): Boolean {
        camera?.let { cam ->
            if (!cam.cameraInfo.hasFlashUnit()) {
                return false
            }
            isTorchOn = !isTorchOn
            cam.cameraControl.enableTorch(isTorchOn)
            return isTorchOn
        }
        return false
    }

    fun switchCameraFacing() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraController"
    }
}
