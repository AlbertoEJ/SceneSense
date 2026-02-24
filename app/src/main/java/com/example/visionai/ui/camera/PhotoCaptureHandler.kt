package com.example.visionai.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat

object PhotoCaptureHandler {

    fun capture(
        context: Context,
        imageCapture: ImageCapture?,
        onCaptured: (Bitmap) -> Unit
    ) {
        imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = imageProxy.toBitmap()
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val rotated = if (rotation != 0) {
                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                        Bitmap.createBitmap(
                            bitmap, 0, 0,
                            bitmap.width, bitmap.height,
                            matrix, true
                        )
                    } else bitmap
                    imageProxy.close()
                    onCaptured(rotated)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("VisionAI", "Photo capture failed", exception)
                }
            }
        )
    }
}
