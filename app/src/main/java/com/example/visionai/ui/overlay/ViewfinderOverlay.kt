package com.example.visionai.ui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.visionai.ui.components.CyanCornerBrackets

@Composable
fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        CyanCornerBrackets()
    }
}
