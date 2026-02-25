package com.example.visionai.ui.state

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionai.AppLanguage
import com.example.visionai.ModelState
import com.example.visionai.ui.theme.Cyan
import com.example.visionai.ui.theme.DarkBg

@Composable
fun ModelLoadOverlay(
    modelState: ModelState,
    errorMessage: String?,
    downloadProgress: Float,
    downloadLabel: String,
    statusText: String,
    language: AppLanguage,
    onLoadModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (modelState == ModelState.READY) return

    val isDownloading = downloadLabel.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Grid pattern
        GridBackground()

        // Radial glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Cyan.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 600f
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App title
            Text(
                text = "SCENESENSE",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // AI disclaimer
            Text(
                text = if (language == AppLanguage.SPANISH)
                    "Las descripciones generadas por IA son aproximaciones que pueden contener errores, imprecisiones u omisiones. Los resultados no sustituyen el criterio humano ni constituyen información verificada. Tu privacidad está protegida: el procesamiento se realiza íntegramente en tu dispositivo."
                else
                    "AI-generated descriptions are approximations that may contain errors, inaccuracies, or omissions. Results do not replace human judgment and should not be considered verified information. Your privacy is protected: all processing is performed entirely on your device.",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (modelState) {
                ModelState.LOADING, ModelState.NOT_LOADED -> {
                    if (isDownloading) {
                        // Determinate progress bar
                        DownloadProgressBar(progress = downloadProgress)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Percentage
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            color = Cyan,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        StatusText(text = "DOWNLOADING_${downloadLabel.uppercase().replace(" ", "_")}")
                    } else {
                        // Scanning bar for model load phase
                        ScanningProgressBar()

                        Spacer(modifier = Modifier.height(24.dp))

                        StatusText(text = "LOADING_NEURAL_CORE")
                    }
                }
                ModelState.ERROR -> {
                    StatusText(text = "ERROR_LOADING_MODEL")

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(onClick = onLoadModel) {
                        Text(
                            text = "[ REINTENTAR ]",
                            color = Cyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                }
                ModelState.READY -> {}
            }
        }
    }
}

@Composable
private fun DownloadProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .width(192.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Cyan.copy(alpha = 0.8f))
        )
    }
}

@Composable
private fun GridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 40f
        val lineColor = Cyan.copy(alpha = 0.04f)
        val w = size.width
        val h = size.height

        var x = 0f
        while (x <= w) {
            drawLine(lineColor, Offset(x, 0f), Offset(x, h), 1f)
            x += gridSize
        }
        var y = 0f
        while (y <= h) {
            drawLine(lineColor, Offset(0f, y), Offset(w, y), 1f)
            y += gridSize
        }
    }
}

@Composable
private fun ScanningProgressBar() {
    val transition = rememberInfiniteTransition(label = "progress")
    val offsetFraction by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progressSweep"
    )

    Box(
        modifier = Modifier
            .width(192.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.33f)
                .height(3.dp)
                .padding(start = (192 * offsetFraction.coerceIn(0f, 0.67f)).dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Cyan.copy(alpha = 0.6f))
        )
    }
}

@Composable
private fun StatusText(text: String) {
    val blinkTransition = rememberInfiniteTransition(label = "blink")
    val cursorAlpha by blinkTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = Cyan.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(12.dp)
                .alpha(cursorAlpha)
                .background(Cyan)
        )
    }
}
