package com.example.visionai.ui.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionai.InferenceState
import com.example.visionai.ui.theme.Cyan
import com.example.visionai.ui.theme.NeonGreen

@Composable
fun ResponseContent(
    responseText: String,
    inferenceState: InferenceState,
    errorMessage: String?,
    isContinuousRunning: Boolean = false,
    isSpeaking: Boolean = false,
    ttsReady: Boolean = false,
    isSpanishMode: Boolean = false,
    onSpeak: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(20.dp)) {
        // Scrollable text area
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            if (inferenceState == InferenceState.RUNNING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Cyan,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = responseText,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Fixed buttons at the bottom (hidden in continuous mode)
        if (!isContinuousRunning && inferenceState == InferenceState.DONE) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speak button
                if (ttsReady) {
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = if (isSpeaking) "\u23F9" else "\uD83D\uDD0A",
                            fontSize = 20.sp,
                            color = if (isSpeaking) NeonGreen else Cyan
                        )
                    }
                }
            }
        }

        PoweredByFooter()
    }
}
