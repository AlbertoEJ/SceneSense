package com.example.visionai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import com.example.visionai.ui.theme.Cyan

@Composable
fun CyanCornerBrackets(
    modifier: Modifier = Modifier,
    bracketLength: Float = 40f,
    strokeWidth: Float = 3f,
    margin: Float = 16f
) {
    val density = LocalDensity.current
    val insets = WindowInsets.safeDrawing
    val topInset = with(density) { insets.getTop(this).toFloat() }
    val bottomInset = with(density) { insets.getBottom(this).toFloat() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val len = bracketLength
        val s = strokeWidth
        val cap = StrokeCap.Round
        val color = Cyan

        val left = margin
        val right = w - margin
        val top = topInset + margin
        val bottom = h - bottomInset - margin

        // Top-left
        drawLine(color, Offset(left, top), Offset(left + len, top), s, cap)
        drawLine(color, Offset(left, top), Offset(left, top + len), s, cap)

        // Top-right
        drawLine(color, Offset(right, top), Offset(right - len, top), s, cap)
        drawLine(color, Offset(right, top), Offset(right, top + len), s, cap)

        // Bottom-left
        drawLine(color, Offset(left, bottom), Offset(left + len, bottom), s, cap)
        drawLine(color, Offset(left, bottom), Offset(left, bottom - len), s, cap)

        // Bottom-right
        drawLine(color, Offset(right, bottom), Offset(right - len, bottom), s, cap)
        drawLine(color, Offset(right, bottom), Offset(right, bottom - len), s, cap)
    }
}
