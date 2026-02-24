package com.example.visionai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberpunkColorScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = DarkBg,
    primaryContainer = CyanDim,
    onPrimaryContainer = Cyan,
    secondary = Cyan,
    onSecondary = DarkBg,
    tertiary = NeonGreen,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = GlassBase,
    onSurfaceVariant = Color(0xFFB0BEC5),
    error = ErrorRed,
    onError = Color.White,
    outline = GlassBorder
)

@Composable
fun VisionAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberpunkColorScheme,
        typography = Typography,
        content = content
    )
}
