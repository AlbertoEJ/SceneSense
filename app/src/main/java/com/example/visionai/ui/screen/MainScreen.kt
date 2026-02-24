package com.example.visionai.ui.screen

import android.Manifest
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import com.example.visionai.AppLanguage
import com.example.visionai.CaptureMode
import com.example.visionai.InferenceState
import com.example.visionai.MainViewModel
import com.example.visionai.ModelState
import com.example.visionai.ui.components.GlassSurface
import com.example.visionai.ui.theme.Cyan
import com.example.visionai.ui.theme.DarkBg
import com.example.visionai.ui.theme.NeonGreen
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.visionai.ui.camera.CameraPreview
import com.example.visionai.ui.camera.PhotoCaptureHandler
import com.example.visionai.ui.camera.VideoCaptureHandler
import com.example.visionai.ui.overlay.BottomControlsOverlay
import com.example.visionai.ui.overlay.HeaderOverlay
import com.example.visionai.ui.overlay.ViewfinderOverlay
import com.example.visionai.ui.sheet.ResponseBottomSheet
import com.example.visionai.ui.state.ModelLoadOverlay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    val isProcessing = state.inferenceState == InferenceState.RUNNING
    val modelReady = state.modelState == ModelState.READY
    val isContinuousRunning = state.isContinuousRunning
    val sheetVisible = state.responseText.isNotEmpty() ||
        state.inferenceState == InferenceState.RUNNING ||
        (state.isQaMode && state.chatMessages.isNotEmpty())

    // Auto-request camera permission when model is ready
    LaunchedEffect(modelReady) {
        if (modelReady && !cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    // Auto-start voice input after audio permission is granted
    LaunchedEffect(audioPermission.status.isGranted) {
        if (audioPermission.status.isGranted && state.isQaMode && !state.isListening) {
            viewModel.toggleVoiceInput()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Layer 1: Camera preview
        if (cameraPermission.status.isGranted) {
            CameraPreview(
                onBound = { img, vid ->
                    imageCapture = img
                    videoCapture = vid
                }
            )
        }

        // Layer 2: Viewfinder brackets
        ViewfinderOverlay()

        // Layer 3: Header pill
        HeaderOverlay(
            modelState = state.modelState,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Layer 4: Bottom controls + Response sheet (Column so sheet pushes controls up)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BottomControlsOverlay(
                captureMode = state.captureMode,
                isRecording = state.isRecording,
                isProcessing = isProcessing,
                isContinuousRunning = isContinuousRunning,
                continuousCount = state.continuousCount,
                enabled = modelReady && !isProcessing,
                onModeChange = { mode ->
                    viewModel.setCaptureMode(mode)
                },
                onShutterClick = {
                    when (state.captureMode) {
                        CaptureMode.PHOTO -> {
                            PhotoCaptureHandler.capture(context, imageCapture) { bitmap ->
                                viewModel.onPhotoCapturedAndDescribe(bitmap)
                            }
                        }
                        CaptureMode.VIDEO -> {
                            if (activeRecording != null) {
                                VideoCaptureHandler.stopRecording(activeRecording)
                                activeRecording = null
                            } else {
                                VideoCaptureHandler.startRecording(
                                    context = context,
                                    videoCapture = videoCapture,
                                    onRecordingStarted = { recording ->
                                        activeRecording = recording
                                        viewModel.setRecording(true)
                                    },
                                    onRecordingFinished = { uri ->
                                        viewModel.setRecording(false)
                                        activeRecording = null
                                        viewModel.onVideoCapturedAndDescribe(uri)
                                    },
                                    onError = { error ->
                                        viewModel.setRecording(false)
                                        activeRecording = null
                                    }
                                )
                            }
                        }
                        CaptureMode.CONTINUOUS -> {
                            if (isContinuousRunning) {
                                viewModel.stopContinuous()
                            } else {
                                viewModel.startContinuous(context, imageCapture)
                            }
                        }
                    }
                }
            )

            ResponseBottomSheet(
                visible = sheetVisible,
                responseText = state.responseText,
                translatedText = state.translatedText,
                inferenceState = state.inferenceState,
                isTranslating = state.isTranslating,
                errorMessage = state.errorMessage,
                isContinuousRunning = isContinuousRunning,
                isQaMode = state.isQaMode,
                chatMessages = state.chatMessages,
                qaCount = state.qaCount,
                qaInputText = state.qaInputText,
                isSpeaking = state.isSpeaking,
                ttsReady = state.ttsReady,
                isListening = state.isListening,
                isSpanishMode = state.language == AppLanguage.SPANISH,
                onTranslate = {},
                onSpeak = { viewModel.speakText(state.responseText) },
                onSpeakMessage = { viewModel.speakChatMessage(it) },
                onQaInputChange = { viewModel.updateQaInput(it) },
                onQaSend = { viewModel.askFollowUp() },
                onTranslateMessage = { viewModel.translateMessage(it) },
                onToggleVoice = {
                    if (audioPermission.status.isGranted) {
                        viewModel.toggleVoiceInput()
                    } else {
                        audioPermission.launchPermissionRequest()
                    }
                }
            )
        }

        // Layer 5: Model load overlay
        ModelLoadOverlay(
            modelState = state.modelState,
            errorMessage = state.errorMessage,
            downloadProgress = state.downloadProgress,
            downloadLabel = state.downloadLabel,
            statusText = state.statusText,
            onLoadModel = { viewModel.loadModel() }
        )

        // Layer 6: Language selection dialog (first launch)
        if (state.showLanguageDialog) {
            LanguageSelectionDialog(
                onSelectLanguage = { viewModel.setLanguage(it) }
            )
        }
    }
}

@Composable
private fun LanguageSelectionDialog(onSelectLanguage: (AppLanguage) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SELECT LANGUAGE",
                    color = Cyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "SELECCIONAR IDIOMA",
                    color = Cyan.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onSelectLanguage(AppLanguage.ENGLISH) },
                    border = BorderStroke(1.dp, Cyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "English",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                OutlinedButton(
                    onClick = { onSelectLanguage(AppLanguage.SPANISH) },
                    border = BorderStroke(1.dp, NeonGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Espa\u00F1ol",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
