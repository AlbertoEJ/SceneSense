package com.example.visionai.ui.camera

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.File

object VideoCaptureHandler {

    private const val RECORD_DURATION_MS = 3000L

    fun startRecording(
        context: Context,
        videoCapture: VideoCapture<Recorder>?,
        onRecordingStarted: (Recording) -> Unit,
        onRecordingFinished: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val vc = videoCapture ?: return

        val videoFile = File(
            context.cacheDir,
            "capture_${System.currentTimeMillis()}.mp4"
        )
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        @Suppress("MissingPermission")
        val recording = vc.output
            .prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            onRecordingFinished(videoFile.toUri())
                        } else {
                            onError("Video error: ${event.error}")
                            Log.e("VisionAI", "Video error: ${event.error}")
                        }
                    }
                }
            }

        onRecordingStarted(recording)

        // Auto-stop after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                recording.stop()
            } catch (_: Exception) {
                // Already stopped
            }
        }, RECORD_DURATION_MS)
    }

    fun stopRecording(recording: Recording?) {
        try {
            recording?.stop()
        } catch (_: Exception) {
            // Already stopped
        }
    }
}
