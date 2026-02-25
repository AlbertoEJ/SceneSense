package com.example.visionai.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.visionai.ui.theme.Cyan
import com.example.visionai.ui.theme.CyanDim
import com.example.visionai.ui.theme.NeonGreen

@Composable
fun VoiceCommandButton(
    isActive: Boolean,
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor: Color
    val bgColor: Color
    val iconColor: Color
    val borderWidth: Float

    if (isActive) {
        iconColor = NeonGreen
        bgColor = NeonGreen.copy(alpha = 0.12f)
        if (isListening) {
            val transition = rememberInfiniteTransition(label = "voicePulse")
            val pulse by transition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "voiceBorderPulse"
            )
            borderColor = NeonGreen.copy(alpha = pulse)
            borderWidth = 2f
        } else {
            borderColor = NeonGreen
            borderWidth = 1.5f
        }
    } else {
        borderColor = Cyan.copy(alpha = 0.4f)
        bgColor = CyanDim.copy(alpha = 0.1f)
        iconColor = Cyan.copy(alpha = 0.6f)
        borderWidth = 1f
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor, CircleShape)
            .border(borderWidth.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Draw microphone icon with Canvas
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)

            // Mic body (rounded rect)
            val bodyW = w * 0.32f
            val bodyH = h * 0.42f
            val bodyLeft = (w - bodyW) / 2f
            val bodyTop = h * 0.08f
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(bodyW / 2f, bodyW / 2f)
            )

            // Arc around mic
            val arcW = w * 0.52f
            val arcH = h * 0.48f
            val arcLeft = (w - arcW) / 2f
            val arcTop = h * 0.12f
            drawArc(
                color = iconColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(arcLeft, arcTop),
                size = Size(arcW, arcH),
                style = stroke
            )

            // Stem line
            val stemX = w / 2f
            val stemTop = arcTop + arcH / 2f + arcH * 0.28f
            val stemBottom = stemTop + h * 0.16f
            drawLine(iconColor, Offset(stemX, stemTop), Offset(stemX, stemBottom), stroke.width)

            // Base line
            val baseY = stemBottom
            val baseHalf = w * 0.14f
            drawLine(iconColor, Offset(stemX - baseHalf, baseY), Offset(stemX + baseHalf, baseY), stroke.width)

            // Slash for inactive state
            if (!isActive) {
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.18f, h * 0.18f),
                    end = Offset(w * 0.82f, h * 0.82f),
                    strokeWidth = w * 0.1f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
