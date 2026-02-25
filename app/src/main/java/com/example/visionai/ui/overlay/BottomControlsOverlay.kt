package com.example.visionai.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionai.CaptureMode
import com.example.visionai.ui.components.CyberpunkButton
import com.example.visionai.ui.components.ModeSelectorPill
import com.example.visionai.ui.components.VoiceCommandButton
import com.example.visionai.ui.theme.Cyan
import com.example.visionai.ui.theme.ErrorRed

@Composable
fun BottomControlsOverlay(
    captureMode: CaptureMode,
    isRecording: Boolean,
    isProcessing: Boolean,
    isContinuousRunning: Boolean,
    continuousCount: Int,
    enabled: Boolean,
    isVoiceCommandMode: Boolean = false,
    isVoiceListening: Boolean = false,
    onToggleVoiceCommand: () -> Unit = {},
    onModeChange: (CaptureMode) -> Unit,
    onShutterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeSelectorPill(
                currentMode = captureMode,
                onModeChange = onModeChange,
                enabled = !isContinuousRunning
            )

            // Shutter row: VoiceCommandButton | Shutter | Spacer (for balance)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                VoiceCommandButton(
                    isActive = isVoiceCommandMode,
                    isListening = isVoiceListening,
                    onClick = onToggleVoiceCommand
                )

                Spacer(modifier = Modifier.width(16.dp))

                CyberpunkButton(
                    onClick = onShutterClick,
                    isRecording = isRecording,
                    isContinuousActive = isContinuousRunning,
                    enabled = enabled || isContinuousRunning
                )

                // Balance spacer (same width as VoiceCommandButton + gap)
                Spacer(modifier = Modifier.width(64.dp))
            }

            // Status label
            val statusText = when {
                isContinuousRunning -> "CONTINUO... ($continuousCount)"
                isRecording -> "GRABANDO..."
                isProcessing -> "PROCESANDO..."
                else -> null
            }
            val statusColor = when {
                isContinuousRunning -> Cyan
                isRecording -> ErrorRed
                else -> Cyan
            }

            AnimatedVisibility(
                visible = statusText != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = statusText ?: "",
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            // Reserve space when no label to avoid layout jump
            if (statusText == null) {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}
