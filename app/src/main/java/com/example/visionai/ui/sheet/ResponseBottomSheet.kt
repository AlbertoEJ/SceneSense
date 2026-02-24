package com.example.visionai.ui.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.visionai.ChatMessage
import com.example.visionai.InferenceState
import com.example.visionai.ui.components.GlassSurface

@Composable
fun ResponseBottomSheet(
    visible: Boolean,
    responseText: String,
    translatedText: String,
    inferenceState: InferenceState,
    isTranslating: Boolean,
    errorMessage: String?,
    isContinuousRunning: Boolean = false,
    isQaMode: Boolean = false,
    chatMessages: List<ChatMessage> = emptyList(),
    qaCount: Int = 0,
    qaInputText: String = "",
    isSpeaking: Boolean = false,
    ttsReady: Boolean = false,
    isListening: Boolean = false,
    isSpanishMode: Boolean = false,
    onTranslate: () -> Unit,
    onSpeak: () -> Unit = {},
    onSpeakMessage: (Int) -> Unit = {},
    onQaInputChange: (String) -> Unit = {},
    onQaSend: () -> Unit = {},
    onTranslateMessage: (Int) -> Unit = {},
    onToggleVoice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        GlassSurface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .animateContentSize(animationSpec = tween(300))
                .heightIn(max = if (isQaMode) 450.dp else 300.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            if (isQaMode && chatMessages.isNotEmpty()) {
                ChatContent(
                    chatMessages = chatMessages,
                    qaCount = qaCount,
                    qaInputText = qaInputText,
                    inferenceState = inferenceState,
                    ttsReady = ttsReady,
                    isListening = isListening,
                    isSpanishMode = isSpanishMode,
                    onInputChange = onQaInputChange,
                    onSend = onQaSend,
                    onTranslateMessage = onTranslateMessage,
                    onSpeakMessage = onSpeakMessage,
                    onToggleVoice = onToggleVoice
                )
            } else {
                ResponseContent(
                    responseText = responseText,
                    inferenceState = inferenceState,
                    errorMessage = errorMessage,
                    isContinuousRunning = isContinuousRunning,
                    isSpeaking = isSpeaking,
                    ttsReady = ttsReady,
                    isSpanishMode = isSpanishMode,
                    onSpeak = onSpeak
                )
            }
        }
    }
}
