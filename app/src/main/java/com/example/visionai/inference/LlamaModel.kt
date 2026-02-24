package com.example.visionai.inference

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LlamaModel {

    companion object {
        private const val TAG = "VisionAI"
        private const val VIDEO_DURATION_MS = 3000L
        private const val VIDEO_NUM_FRAMES = 3
        private const val FRAME_MAX_DIM = 512

        init {
            System.loadLibrary("visionai")
        }
    }

    private var nativePtr: Long = 0L

    val isLoaded: Boolean get() = nativePtr != 0L

    suspend fun load(
        modelPath: String,
        mmprojPath: String,
        nThreads: Int = 4,
        contextSize: Int = 2048
    ) = withContext(Dispatchers.IO) {
        require(File(modelPath).exists()) { "Model file not found: $modelPath" }
        require(File(mmprojPath).exists()) { "Projector file not found: $mmprojPath" }

        if (nativePtr != 0L) {
            free()
        }

        nativePtr = loadModel(modelPath, mmprojPath, nThreads, contextSize)
    }

    /** Single image inference */
    suspend fun describeImage(
        bitmap: Bitmap,
        prompt: String = "Describe this image."
    ): String = withContext(Dispatchers.IO) {
        require(nativePtr != 0L) { "Model not loaded" }
        val scaled = scaleBitmap(bitmap, FRAME_MAX_DIM)
        val rgbBytes = bitmapToRgb(scaled)
        val result = runInference(nativePtr, rgbBytes, scaled.width, scaled.height, prompt)
        if (scaled !== bitmap) scaled.recycle()
        result
    }

    /** Video inference: extract frames from video URI */
    suspend fun describeVideo(
        videoUri: Uri,
        retriever: MediaMetadataRetriever,
        prompt: String = "What is the main action or notable event happening in this segment? Describe it in one brief sentence."
    ): String = withContext(Dispatchers.IO) {
        require(nativePtr != 0L) { "Model not loaded" }

        val rawFrames = extractFrames(retriever)
        if (rawFrames.isEmpty()) {
            throw IllegalStateException("Could not extract frames from video")
        }

        Log.i(TAG, "Extracted ${rawFrames.size} frames from video")

        // Scale frames once, extract RGB, then recycle
        val scaledFrames = rawFrames.map { scaleBitmap(it, FRAME_MAX_DIM) }
        val widths = IntArray(scaledFrames.size) { scaledFrames[it].width }
        val heights = IntArray(scaledFrames.size) { scaledFrames[it].height }
        val rgbArrays = Array(scaledFrames.size) { bitmapToRgb(scaledFrames[it]) }

        // Recycle all bitmaps
        scaledFrames.forEach { scaled ->
            if (scaled !in rawFrames) scaled.recycle()
        }
        rawFrames.forEach { it.recycle() }

        runVideoInference(nativePtr, rgbArrays, widths, heights, prompt)
    }

    fun free() {
        if (nativePtr != 0L) {
            freeModel(nativePtr)
            nativePtr = 0L
        }
    }

    /** Extract evenly-spaced frames from video */
    private fun extractFrames(retriever: MediaMetadataRetriever): List<Bitmap> {
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: VIDEO_DURATION_MS
        val actualDuration = minOf(durationMs, VIDEO_DURATION_MS)

        val frames = mutableListOf<Bitmap>()
        val interval = actualDuration / VIDEO_NUM_FRAMES

        for (i in 0 until VIDEO_NUM_FRAMES) {
            val timeUs = (i * interval * 1000) // microseconds
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            if (frame != null) {
                frames.add(frame)
                Log.i(TAG, "Frame $i at ${i * interval}ms: ${frame.width}x${frame.height}")
            }
        }

        return frames
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap

        val scale = maxDim.toFloat() / maxOf(w, h)
        val newW = (w * scale).toInt()
        val newH = (h * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun bitmapToRgb(bitmap: Bitmap): ByteArray {
        val argbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        val width = argbBitmap.width
        val height = argbBitmap.height
        val pixels = IntArray(width * height)
        argbBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val rgb = ByteArray(width * height * 3)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            rgb[i * 3]     = ((pixel shr 16) and 0xFF).toByte()
            rgb[i * 3 + 1] = ((pixel shr 8) and 0xFF).toByte()
            rgb[i * 3 + 2] = (pixel and 0xFF).toByte()
        }

        if (argbBitmap !== bitmap) {
            argbBitmap.recycle()
        }

        return rgb
    }

    // Native methods
    private external fun loadModel(
        modelPath: String, mmprojPath: String,
        nThreads: Int, contextSize: Int
    ): Long

    private external fun runInference(
        ctxPtr: Long, imageBytes: ByteArray,
        width: Int, height: Int, prompt: String
    ): String

    private external fun runVideoInference(
        ctxPtr: Long, frames: Array<ByteArray>,
        widths: IntArray, heights: IntArray, prompt: String
    ): String

    private external fun freeModel(ctxPtr: Long)
}
