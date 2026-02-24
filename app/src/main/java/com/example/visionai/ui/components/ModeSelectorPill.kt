package com.example.visionai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionai.CaptureMode
import com.example.visionai.ui.theme.Cyan
import com.example.visionai.ui.theme.CyanDim
import com.example.visionai.ui.theme.GlassBase

@Composable
fun ModeSelectorPill(
    currentMode: CaptureMode,
    onModeChange: (CaptureMode) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(GlassBase, shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeTab(
            label = "FOTO",
            selected = currentMode == CaptureMode.PHOTO,
            enabled = enabled,
            onClick = { onModeChange(CaptureMode.PHOTO) }
        )
        ModeTab(
            label = "VIDEO 3s",
            selected = currentMode == CaptureMode.VIDEO,
            enabled = enabled,
            onClick = { onModeChange(CaptureMode.VIDEO) }
        )
        ModeTab(
            label = "CONTINUO",
            selected = currentMode == CaptureMode.CONTINUOUS,
            enabled = enabled,
            onClick = { onModeChange(CaptureMode.CONTINUOUS) }
        )
    }
}

@Composable
private fun ModeTab(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (selected) CyanDim else Color.Transparent
    val textColor = when {
        selected -> Cyan
        !enabled -> Color.White.copy(alpha = 0.3f)
        else -> Color.White.copy(alpha = 0.6f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}
