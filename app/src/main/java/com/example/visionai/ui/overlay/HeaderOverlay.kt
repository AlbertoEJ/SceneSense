package com.example.visionai.ui.overlay

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionai.ModelState
import com.example.visionai.ui.components.GlassSurface
import com.example.visionai.ui.components.PulsingDot
import com.example.visionai.ui.theme.Cyan
import com.example.visionai.ui.theme.ErrorRed
import com.example.visionai.ui.theme.NeonGreen

@Composable
fun HeaderOverlay(
    modelState: ModelState,
    modifier: Modifier = Modifier
) {
    val dotColor = when (modelState) {
        ModelState.READY -> NeonGreen
        ModelState.LOADING -> Cyan
        ModelState.NOT_LOADED, ModelState.ERROR -> ErrorRed
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassSurface(shape = RoundedCornerShape(20.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PulsingDot(color = dotColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SCENESENSE",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
