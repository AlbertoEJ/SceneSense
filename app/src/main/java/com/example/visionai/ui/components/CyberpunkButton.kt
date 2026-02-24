package com.example.visionai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.visionai.ui.theme.Cyan
import com.example.visionai.ui.theme.CyanGlow
import com.example.visionai.ui.theme.ErrorRed

@Composable
fun CyberpunkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
    isContinuousActive: Boolean = false,
    enabled: Boolean = true
) {
    val outerColor = if (enabled) Cyan else Cyan.copy(alpha = 0.3f)
    val innerColor = when {
        isContinuousActive -> Cyan
        isRecording -> ErrorRed
        else -> Color.White
    }

    val pulseTransition = rememberInfiniteTransition(label = "recPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recPulseScale"
    )
    val innerScale = if (isRecording || isContinuousActive) pulseScale else 1f

    Box(
        modifier = modifier
            .size(72.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = CyanGlow,
                spotColor = CyanGlow
            )
            .border(2.dp, outerColor, CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(innerScale)
                .background(innerColor, CircleShape)
        )
    }
}
