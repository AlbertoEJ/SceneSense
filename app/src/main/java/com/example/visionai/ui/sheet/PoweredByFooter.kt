package com.example.visionai.ui.sheet

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PoweredByFooter(modifier: Modifier = Modifier) {
    Text(
        text = "POWERED BY SMOLVLM2",
        modifier = modifier.padding(top = 12.dp),
        color = Color.White.copy(alpha = 0.3f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.sp
    )
}
