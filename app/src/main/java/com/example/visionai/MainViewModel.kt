package com.example.visionai

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.visionai.inference.LlamaModel
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class ModelState {
    NOT_LOADED, LOADING, READY, ERROR
}

enum class InferenceState {
    IDLE, RUNNING, DONE, ERROR
}

enum class CaptureMode {
    PHOTO, VIDEO, CONTINUOUS
}

enum class AppLanguage { ENGLISH, SPANISH }

enum class ChatRole { SYSTEM_DESCRIPTION, USER_QUESTION, ASSISTANT_ANSWER }
data class ChatMessage(val role: ChatRole, val text: String, val translatedText: String = "")

data class UiState(
    val modelState: ModelState = ModelState.NOT_LOADED,
    val inferenceState: InferenceState = InferenceState.IDLE,
    val statusText: String = "Modelo no cargado",
    val downloadProgress: Float = 0f,
    val downloadLabel: String = "",
    val responseText: String = "",
    val translatedText: String = "",
    val isTranslating: Boolean = false,
    val selectedBitmap: Bitmap? = null,
    val selectedVideoUri: Uri? = null,
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val isRecording: Boolean = false,
    val isContinuousRunning: Boolean = false,
    val continuousCount: Int = 0,
    val errorMessage: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val qaCount: Int = 0,
    val isQaMode: Boolean = false,
    val qaInputText: String = "",
    val isSpeaking: Boolean = false,
    val ttsReady: Boolean = false,
    val isListening: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH
)

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val llamaModel = LlamaModel()
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var tts: TextToSpeech? = null

    private var enToEsTranslator: com.google.mlkit.nl.translate.Translator? = null
    private var esEnTranslator: com.google.mlkit.nl.translate.Translator? = null
    private var enToEsReady = false
    private var esEnReady = false
    private var continuousJob: Job? = null

    init {
        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs.getString(KEY_LANGUAGE, null)
        if (savedLang == null) {
            _uiState.value = _uiState.value.copy(showLanguageDialog = true)
        } else {
            val lang = if (savedLang == "es") AppLanguage.SPANISH else AppLanguage.ENGLISH
            _uiState.value = _uiState.value.copy(language = lang)
            setupForLanguage(lang)
        }

        loadModel()
    }

    private fun setupForLanguage(lang: AppLanguage) {
        // Initialize TTS with the correct locale
        tts?.shutdown()
        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = if (lang == AppLanguage.SPANISH) Locale("es", "ES") else Locale.US
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(if (lang == AppLanguage.SPANISH) Locale("es") else Locale.ENGLISH)
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _uiState.value = _uiState.value.copy(isSpeaking = true)
                    }
                    override fun onDone(utteranceId: String?) {
                        _uiState.value = _uiState.value.copy(isSpeaking = false)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _uiState.value = _uiState.value.copy(isSpeaking = false)
                    }
                })
                _uiState.value = _uiState.value.copy(ttsReady = true)
                Log.i("VisionAI", "TTS ready (${if (lang == AppLanguage.SPANISH) "español" else "english"})")
            } else {
                Log.e("VisionAI", "TTS init failed: $status")
            }
        }

        // Initialize translators only for Spanish mode
        if (lang == AppLanguage.SPANISH) {
            val conditions = DownloadConditions.Builder().build()

            enToEsTranslator?.close()
            enToEsTranslator = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(TranslateLanguage.SPANISH)
                    .build()
            )
            enToEsTranslator!!.downloadModelIfNeeded(conditions)
                .addOnSuccessListener { enToEsReady = true; Log.i("VisionAI", "EN→ES translator ready") }
                .addOnFailureListener { Log.e("VisionAI", "EN→ES translator download failed", it) }

            esEnTranslator?.close()
            esEnTranslator = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.SPANISH)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build()
            )
            esEnTranslator!!.downloadModelIfNeeded(conditions)
                .addOnSuccessListener { esEnReady = true; Log.i("VisionAI", "ES→EN translator ready") }
                .addOnFailureListener { Log.e("VisionAI", "ES→EN translator download failed", it) }
        } else {
            enToEsTranslator?.close()
            enToEsTranslator = null
            enToEsReady = false
            esEnTranslator?.close()
            esEnTranslator = null
            esEnReady = false
        }
    }

    fun setLanguage(lang: AppLanguage) {
        app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, if (lang == AppLanguage.SPANISH) "es" else "en").apply()
        _uiState.value = _uiState.value.copy(
            showLanguageDialog = false,
            language = lang
        )
        setupForLanguage(lang)
    }

    /** Recycle the old bitmap before replacing it */
    private fun updateBitmap(newBitmap: Bitmap?) {
        val old = _uiState.value.selectedBitmap
        if (old != null && old !== newBitmap) {
            old.recycle()
        }
    }

    fun loadModel() {
        if (_uiState.value.modelState == ModelState.LOADING) return

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            _uiState.value = _uiState.value.copy(
                modelState = ModelState.LOADING,
                statusText = "Iniciando...",
                errorMessage = null,
                downloadProgress = 0f,
                downloadLabel = ""
            )

            try {
                val modelsDir = File(app.filesDir, "models")
                modelsDir.mkdirs()
                val modelFile = File(modelsDir, MODEL_FILENAME)
                val mmprojFile = File(modelsDir, MMPROJ_FILENAME)

                // Download if missing
                if (!modelFile.exists()) {
                    downloadFile(
                        url = "$HF_BASE_URL$MODEL_FILENAME",
                        dest = modelFile,
                        label = "Modelo principal"
                    )
                }
                if (!mmprojFile.exists()) {
                    downloadFile(
                        url = "$HF_BASE_URL$MMPROJ_FILENAME",
                        dest = mmprojFile,
                        label = "Proyector visual"
                    )
                }

                _uiState.value = _uiState.value.copy(
                    statusText = "Cargando modelo...",
                    downloadProgress = 0f,
                    downloadLabel = ""
                )

                val cores = Runtime.getRuntime().availableProcessors()
                val nThreads = (cores / 2).coerceIn(2, 4)

                llamaModel.load(
                    modelPath = modelFile.absolutePath,
                    mmprojPath = mmprojFile.absolutePath,
                    nThreads = nThreads,
                    contextSize = 4096
                )

                // Ensure splash is visible for at least 2 seconds
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < MIN_SPLASH_MS) {
                    kotlinx.coroutines.delay(MIN_SPLASH_MS - elapsed)
                }

                _uiState.value = _uiState.value.copy(
                    modelState = ModelState.READY,
                    statusText = "Modelo listo ($nThreads hilos)"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    modelState = ModelState.ERROR,
                    statusText = "Error",
                    errorMessage = e.message
                )
            }
        }
    }

    private suspend fun downloadFile(url: String, dest: File, label: String) =
        withContext(Dispatchers.IO) {
            val tmp = File(dest.parentFile, "${dest.name}.tmp")
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP ${connection.responseCode} al descargar $label")
                }

                val totalBytes = connection.contentLengthLong
                var downloaded = 0L

                connection.inputStream.use { input ->
                    tmp.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            val progress = if (totalBytes > 0) {
                                (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                            } else 0f
                            _uiState.value = _uiState.value.copy(
                                downloadProgress = progress,
                                downloadLabel = label,
                                statusText = "Descargando $label..."
                            )
                        }
                    }
                }

                tmp.renameTo(dest)
                Log.i("VisionAI", "Downloaded $label: ${dest.length()} bytes")
            } catch (e: Exception) {
                tmp.delete()
                throw e
            }
        }

    companion object {
        private const val MIN_SPLASH_MS = 2500L
        private const val HF_BASE_URL =
            "https://huggingface.co/ggml-org/SmolVLM2-500M-Video-Instruct-GGUF/resolve/main/"
        private const val MODEL_FILENAME = "SmolVLM2-500M-Video-Instruct-Q8_0.gguf"
        private const val MMPROJ_FILENAME = "mmproj-SmolVLM2-500M-Video-Instruct-Q8_0.gguf"
        private const val PREFS_NAME = "scenesense_prefs"
        private const val KEY_LANGUAGE = "app_language"
    }

    fun setCaptureMode(mode: CaptureMode) {
        if (_uiState.value.isContinuousRunning) {
            stopContinuous()
        }
        _uiState.value = _uiState.value.copy(
            captureMode = mode,
            responseText = "",
            translatedText = "",
            inferenceState = InferenceState.IDLE,
            errorMessage = null,
            chatMessages = emptyList(),
            qaCount = 0,
            isQaMode = false,
            qaInputText = ""
        )
    }

    fun onImageSelected(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(app.contentResolver, uri)
            val scaled = scaleBitmap(bitmap, maxDim = 512)
            if (scaled !== bitmap) bitmap.recycle()

            updateBitmap(scaled)
            _uiState.value = _uiState.value.copy(
                selectedBitmap = scaled,
                selectedVideoUri = null,
                inferenceState = InferenceState.IDLE,
                responseText = "",
                translatedText = "",
                chatMessages = emptyList(),
                qaCount = 0,
                isQaMode = false,
                qaInputText = ""
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error al cargar imagen: ${e.message}"
            )
        }
    }

    fun onVideoSelected(uri: Uri) {
        updateBitmap(null)
        cleanupTempVideos(uri)
        _uiState.value = _uiState.value.copy(
            selectedVideoUri = uri,
            selectedBitmap = null,
            inferenceState = InferenceState.IDLE,
            responseText = "",
            translatedText = "",
            chatMessages = emptyList(),
            qaCount = 0,
            isQaMode = false,
            qaInputText = ""
        )

        // Extract first frame as thumbnail
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(app, uri)
            val thumb = retriever.getFrameAtTime(0)
            retriever.release()
            if (thumb != null) {
                val scaled = scaleBitmap(thumb, 512)
                if (scaled !== thumb) thumb.recycle()
                _uiState.value = _uiState.value.copy(selectedBitmap = scaled)
            }
        } catch (_: Exception) {}
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        val scaled = scaleBitmap(bitmap, maxDim = 512)
        if (scaled !== bitmap) bitmap.recycle()

        updateBitmap(scaled)
        _uiState.value = _uiState.value.copy(
            selectedBitmap = scaled,
            selectedVideoUri = null,
            inferenceState = InferenceState.IDLE,
            responseText = "",
            translatedText = "",
            chatMessages = emptyList(),
            qaCount = 0,
            isQaMode = false,
            qaInputText = ""
        )
    }

    /** Capture photo and immediately start inference */
    fun onPhotoCapturedAndDescribe(bitmap: Bitmap) {
        onPhotoCaptured(bitmap)
        describe()
    }

    fun onVideoCaptured(uri: Uri) {
        onVideoSelected(uri)
    }

    /** Capture video and immediately start inference */
    fun onVideoCapturedAndDescribe(uri: Uri) {
        onVideoSelected(uri)
        describe()
    }

    fun setRecording(recording: Boolean) {
        _uiState.value = _uiState.value.copy(isRecording = recording)
    }

    fun startContinuous(context: Context, imageCapture: ImageCapture?) {
        if (imageCapture == null || !llamaModel.isLoaded) return
        if (continuousJob?.isActive == true) return

        _uiState.value = _uiState.value.copy(
            isContinuousRunning = true,
            continuousCount = 0,
            responseText = "",
            translatedText = "",
            errorMessage = null
        )

        continuousJob = viewModelScope.launch {
            var count = 0
            try {
                while (_uiState.value.isContinuousRunning) {
                    count++

                    _uiState.value = _uiState.value.copy(
                        inferenceState = InferenceState.RUNNING,
                        responseText = "Capturando frame $count...",
                        translatedText = ""
                    )

                    val bitmap = captureFrame(context, imageCapture)
                    if (!_uiState.value.isContinuousRunning) {
                        bitmap.recycle()
                        break
                    }

                    val scaled = scaleBitmap(bitmap, maxDim = 512)
                    if (scaled !== bitmap) bitmap.recycle()

                    // Keep one copy for UI state, another for inference.
                    // describeImage() internally re-scales to 384px and recycles
                    // the bitmap we pass in, so we can't share the same object.
                    val forInference = scaled.copy(scaled.config ?: Bitmap.Config.ARGB_8888, false)

                    updateBitmap(scaled)
                    _uiState.value = _uiState.value.copy(
                        selectedBitmap = scaled,
                        selectedVideoUri = null,
                        responseText = "Analizando frame $count...",
                        errorMessage = null
                    )

                    val response = llamaModel.describeImage(
                        forInference,
                        prompt = "Describe this image."
                    )

                    if (!_uiState.value.isContinuousRunning) break

                    val isSpanish = _uiState.value.language == AppLanguage.SPANISH
                    val displayResponse = if (isSpanish) {
                        translateEnToEs(response) ?: response
                    } else response

                    _uiState.value = _uiState.value.copy(
                        inferenceState = InferenceState.DONE,
                        responseText = "[Frame $count] $displayResponse",
                        continuousCount = count
                    )

                    // Auto-speak the result for hands-free navigation
                    tts?.let { engine ->
                        val toSpeak = displayResponse.trim()
                        if (toSpeak.isNotEmpty()) {
                            engine.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "continuous_$count")
                        }
                    }

                    // Pause so user hears the result before next frame
                    delay(3000)
                }
            } catch (e: Exception) {
                Log.e("VisionAI", "Continuous mode error at frame $count", e)
                _uiState.value = _uiState.value.copy(
                    inferenceState = InferenceState.ERROR,
                    errorMessage = "Error frame $count: ${e.message}"
                )
            }

            _uiState.value = _uiState.value.copy(isContinuousRunning = false)
            continuousJob = null
        }
    }

    fun stopContinuous() {
        _uiState.value = _uiState.value.copy(isContinuousRunning = false)
    }

    private suspend fun captureFrame(
        context: Context,
        imageCapture: ImageCapture
    ): Bitmap = suspendCancellableCoroutine { cont ->
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
                    cont.resume(rotated)
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            }
        )
    }

    fun describe() {
        val state = _uiState.value
        if (!llamaModel.isLoaded) return
        if (state.inferenceState == InferenceState.RUNNING) return

        if (state.selectedVideoUri != null) {
            describeVideo(state.selectedVideoUri)
        } else if (state.selectedBitmap != null) {
            describePhoto(state.selectedBitmap)
        }
    }

    private suspend fun translateEnToEs(text: String): String? {
        val t = enToEsTranslator ?: return null
        if (!enToEsReady) return null
        return try { t.translate(text).await() } catch (e: Exception) {
            Log.e("VisionAI", "EN→ES translation failed", e)
            null
        }
    }

    private suspend fun translateEsToEn(text: String): String? {
        val t = esEnTranslator ?: return null
        if (!esEnReady) return null
        return try { t.translate(text).await() } catch (e: Exception) {
            Log.e("VisionAI", "ES→EN translation failed", e)
            null
        }
    }

    fun speakText(text: String) {
        val engine = tts ?: return
        if (engine.isSpeaking) {
            engine.stop()
            _uiState.value = _uiState.value.copy(isSpeaking = false)
            return
        }
        // responseText is already in the correct language
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "scenesense_tts")
    }

    fun speakChatMessage(index: Int) {
        val engine = tts ?: return
        val state = _uiState.value
        val messages = state.chatMessages
        if (index < 0 || index >= messages.size) return
        val msg = messages[index]

        if (engine.isSpeaking) {
            engine.stop()
            _uiState.value = state.copy(isSpeaking = false)
            return
        }
        val toSpeak = if (state.language == AppLanguage.SPANISH && msg.translatedText.isNotEmpty())
            msg.translatedText else msg.text
        engine.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "scenesense_tts_$index")
    }

    fun stopSpeaking() {
        tts?.stop()
        _uiState.value = _uiState.value.copy(isSpeaking = false)
    }

    private var speechRecognizer: SpeechRecognizer? = null

    fun toggleVoiceInput() {
        if (_uiState.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            Log.e("VisionAI", "Speech recognition not available")
            return
        }

        // Stop TTS if playing so it doesn't interfere
        tts?.stop()

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(app).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _uiState.value = _uiState.value.copy(isListening = true)
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _uiState.value = _uiState.value.copy(isListening = false)
                }
                override fun onError(error: Int) {
                    Log.e("VisionAI", "Speech recognition error: $error")
                    _uiState.value = _uiState.value.copy(isListening = false)
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(qaInputText = text)
                    }
                    _uiState.value = _uiState.value.copy(isListening = false)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(qaInputText = text)
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val speechLang = if (_uiState.value.language == AppLanguage.SPANISH) "es-ES" else "en-US"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    private fun describePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            val isSpanish = _uiState.value.language == AppLanguage.SPANISH
            _uiState.value = _uiState.value.copy(
                inferenceState = InferenceState.RUNNING,
                responseText = if (isSpanish) "Analizando imagen..." else "Analyzing image...",
                translatedText = "",
                errorMessage = null
            )
            try {
                val response = llamaModel.describeImage(bitmap)
                val isContinuous = _uiState.value.captureMode == CaptureMode.CONTINUOUS

                if (isSpanish) {
                    val translated = translateEnToEs(response) ?: response
                    _uiState.value = _uiState.value.copy(
                        inferenceState = InferenceState.DONE,
                        responseText = translated,
                        chatMessages = if (!isContinuous) listOf(
                            ChatMessage(ChatRole.SYSTEM_DESCRIPTION, response, translatedText = translated)
                        ) else emptyList(),
                        isQaMode = !isContinuous,
                        qaCount = 0,
                        qaInputText = ""
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        inferenceState = InferenceState.DONE,
                        responseText = response,
                        chatMessages = if (!isContinuous) listOf(
                            ChatMessage(ChatRole.SYSTEM_DESCRIPTION, response)
                        ) else emptyList(),
                        isQaMode = !isContinuous,
                        qaCount = 0,
                        qaInputText = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    inferenceState = InferenceState.ERROR,
                    responseText = "",
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    private fun describeVideo(uri: Uri) {
        viewModelScope.launch {
            val isSpanish = _uiState.value.language == AppLanguage.SPANISH
            _uiState.value = _uiState.value.copy(
                inferenceState = InferenceState.RUNNING,
                responseText = if (isSpanish) "Analizando video..." else "Analyzing video...",
                translatedText = "",
                errorMessage = null
            )
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(app, uri)
                val response = llamaModel.describeVideo(uri, retriever)
                retriever.release()

                if (isSpanish) {
                    val translated = translateEnToEs(response) ?: response
                    _uiState.value = _uiState.value.copy(
                        inferenceState = InferenceState.DONE,
                        responseText = translated,
                        chatMessages = listOf(
                            ChatMessage(ChatRole.SYSTEM_DESCRIPTION, response, translatedText = translated)
                        ),
                        isQaMode = true,
                        qaCount = 0,
                        qaInputText = ""
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        inferenceState = InferenceState.DONE,
                        responseText = response,
                        chatMessages = listOf(
                            ChatMessage(ChatRole.SYSTEM_DESCRIPTION, response)
                        ),
                        isQaMode = true,
                        qaCount = 0,
                        qaInputText = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    inferenceState = InferenceState.ERROR,
                    responseText = "",
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun updateQaInput(text: String) {
        _uiState.value = _uiState.value.copy(qaInputText = text)
    }

    private fun buildQaPrompt(messages: List<ChatMessage>, newQuestion: String): String {
        val sb = StringBuilder()
        sb.append("Previous conversation about this image:\n")
        for (msg in messages) {
            when (msg.role) {
                ChatRole.SYSTEM_DESCRIPTION -> sb.append("Assistant: ${msg.text}\n")
                ChatRole.USER_QUESTION -> sb.append("User: ${msg.text}\n")
                ChatRole.ASSISTANT_ANSWER -> sb.append("Assistant: ${msg.text}\n")
            }
        }
        sb.append("User: $newQuestion\n")
        sb.append("Answer the user's question based on what you see in the image and the conversation above.")
        return sb.toString()
    }

    fun askFollowUp() {
        val state = _uiState.value
        if (!llamaModel.isLoaded) return
        if (state.inferenceState == InferenceState.RUNNING) return
        if (state.qaCount >= 3) return
        val question = state.qaInputText.trim()
        if (question.isEmpty()) return

        val isSpanish = state.language == AppLanguage.SPANISH

        viewModelScope.launch {
            // If Spanish: translate question ES→EN for the model, keep original as translatedText
            val questionEn: String
            val userMessage: ChatMessage
            if (isSpanish) {
                questionEn = translateEsToEn(question) ?: question
                userMessage = ChatMessage(ChatRole.USER_QUESTION, questionEn, translatedText = question)
            } else {
                questionEn = question
                userMessage = ChatMessage(ChatRole.USER_QUESTION, question)
            }

            val updatedMessages = state.chatMessages + userMessage
            val newQaCount = state.qaCount + 1

            _uiState.value = state.copy(
                chatMessages = updatedMessages,
                qaCount = newQaCount,
                qaInputText = "",
                inferenceState = InferenceState.RUNNING,
                errorMessage = null
            )

            val qaPrompt = buildQaPrompt(state.chatMessages, questionEn)

            try {
                val response = if (state.selectedVideoUri != null) {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(app, state.selectedVideoUri)
                    val result = llamaModel.describeVideo(state.selectedVideoUri, retriever, prompt = qaPrompt)
                    retriever.release()
                    result
                } else if (state.selectedBitmap != null) {
                    val bitmapCopy = state.selectedBitmap.copy(
                        state.selectedBitmap.config ?: Bitmap.Config.ARGB_8888, false
                    )
                    llamaModel.describeImage(bitmapCopy, prompt = qaPrompt)
                } else {
                    throw IllegalStateException("No image or video available")
                }

                val answerMessage = if (isSpanish) {
                    val translated = translateEnToEs(response) ?: response
                    ChatMessage(ChatRole.ASSISTANT_ANSWER, response, translatedText = translated)
                } else {
                    ChatMessage(ChatRole.ASSISTANT_ANSWER, response)
                }

                val displayText = if (isSpanish) answerMessage.translatedText.ifEmpty { response } else response
                val finalMessages = _uiState.value.chatMessages + answerMessage
                _uiState.value = _uiState.value.copy(
                    chatMessages = finalMessages,
                    inferenceState = InferenceState.DONE,
                    responseText = displayText
                )
            } catch (e: Exception) {
                Log.e("VisionAI", "Q&A follow-up failed", e)
                _uiState.value = _uiState.value.copy(
                    inferenceState = InferenceState.ERROR,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun translateMessage(index: Int) {
        val state = _uiState.value
        if (!enToEsReady || index < 0 || index >= state.chatMessages.size) return
        val msg = state.chatMessages[index]
        if (msg.translatedText.isNotEmpty()) return

        viewModelScope.launch {
            val translated = translateEnToEs(msg.text) ?: return@launch
            val updated = _uiState.value.chatMessages.toMutableList()
            updated[index] = msg.copy(translatedText = translated)
            _uiState.value = _uiState.value.copy(chatMessages = updated)
        }
    }

    /** Delete old temp video files, keeping the current one */
    private fun cleanupTempVideos(keepUri: Uri? = null) {
        val keepPath = keepUri?.path
        app.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("capture_") && file.name.endsWith(".mp4")) {
                if (file.absolutePath != keepPath) {
                    file.delete()
                }
            }
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val scale = maxDim.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.value = _uiState.value.copy(isContinuousRunning = false)
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        updateBitmap(null)
        cleanupTempVideos()
        llamaModel.free()
        enToEsTranslator?.close()
        esEnTranslator?.close()
    }
}
