package com.example.visionai.ui.sheet

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionai.ChatMessage
import com.example.visionai.ChatRole
import com.example.visionai.InferenceState
import com.example.visionai.ui.theme.Cyan
import com.example.visionai.ui.theme.CyanDim
import com.example.visionai.ui.theme.GlassBase
import com.example.visionai.ui.theme.NeonGreen

@Composable
fun ChatContent(
    chatMessages: List<ChatMessage>,
    qaCount: Int,
    qaInputText: String,
    inferenceState: InferenceState,
    ttsReady: Boolean = false,
    isListening: Boolean = false,
    isSpanishMode: Boolean = false,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onTranslateMessage: (Int) -> Unit,
    onSpeakMessage: (Int) -> Unit = {},
    onToggleVoice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        QaCounterBadge(qaCount = qaCount, isSpanishMode = isSpanishMode)

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(chatMessages) { index, message ->
                ChatBubble(
                    message = message,
                    ttsReady = ttsReady,
                    isSpanishMode = isSpanishMode,
                    onTranslate = { onTranslateMessage(index) },
                    onSpeak = { onSpeakMessage(index) }
                )
            }

            if (inferenceState == InferenceState.RUNNING) {
                item { ThinkingIndicator(isSpanishMode = isSpanishMode) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (qaCount >= 3) {
            Text(
                text = if (isSpanishMode) "L\u00CDMITE DE PREGUNTAS ALCANZADO" else "QUESTION LIMIT REACHED",
                color = Cyan.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        } else {
            QaInputRow(
                text = qaInputText,
                enabled = inferenceState != InferenceState.RUNNING,
                isListening = isListening,
                isSpanishMode = isSpanishMode,
                onTextChange = onInputChange,
                onSend = onSend,
                onToggleVoice = onToggleVoice
            )
        }

        PoweredByFooter()
    }
}

@Composable
private fun QaCounterBadge(qaCount: Int, isSpanishMode: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CHAT Q&A",
            color = Cyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "$qaCount/3 ${if (isSpanishMode) "PREGUNTAS" else "QUESTIONS"}",
            color = if (qaCount >= 3) NeonGreen else Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    ttsReady: Boolean = false,
    isSpanishMode: Boolean = false,
    onTranslate: () -> Unit,
    onSpeak: () -> Unit = {}
) {
    val isAi = message.role != ChatRole.USER_QUESTION
    val bubbleColor = if (isAi) CyanDim else GlassBase
    val label = if (isAi) "SCENESENSE" else if (isSpanishMode) "T\u00DA" else "YOU"
    val labelColor = if (isAi) NeonGreen else Cyan

    // In Spanish mode, show translatedText if available; otherwise show text
    val displayText = if (isSpanishMode && message.translatedText.isNotEmpty())
        message.translatedText else message.text

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bubbleColor)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = labelColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            if (isAi && ttsReady) {
                Text(
                    text = "\uD83D\uDD0A",
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onSpeak)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = displayText,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ThinkingIndicator(isSpanishMode: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "thinking")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(Cyan)
            )
            if (i < 2) Spacer(modifier = Modifier.width(4.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isSpanishMode) "Pensando..." else "Thinking...",
            color = Cyan.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun QaInputRow(
    text: String,
    enabled: Boolean,
    isListening: Boolean = false,
    isSpanishMode: Boolean = false,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleVoice: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                if (isListening) NeonGreen.copy(alpha = 0.6f) else Cyan.copy(alpha = 0.3f),
                RoundedCornerShape(24.dp)
            )
            .background(GlassBase)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            enabled = enabled && !isListening,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(Cyan),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() }),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = if (isListening) {
                                if (isSpanishMode) "Escuchando..." else "Listening..."
                            } else {
                                if (isSpanishMode) "Pregunta sobre la imagen..." else "Ask about the image..."
                            },
                            color = if (isListening) NeonGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.3f),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        IconButton(
            onClick = onToggleVoice,
            enabled = enabled,
            modifier = Modifier.size(36.dp)
        ) {
            Text(
                text = "\uD83C\uDF99",
                fontSize = 18.sp,
                color = if (isListening) NeonGreen else Cyan.copy(alpha = 0.6f)
            )
        }

        IconButton(
            onClick = onSend,
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = if (isSpanishMode) "Enviar" else "Send",
                tint = if (enabled && text.isNotBlank()) Cyan else Cyan.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
