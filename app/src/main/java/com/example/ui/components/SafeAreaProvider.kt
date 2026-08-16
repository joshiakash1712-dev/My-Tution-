package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Global SafeAreaProvider / layout wrapper component.
 * Uses standard platform window insets to automatically apply safe top padding
 * to the main application container. This guarantees that all screens, app bars,
 * headers, greeting texts, and buttons start rendering below the device status bar and notch,
 * preventing any overlaps with punch-holes, waterdrops, or custom notches.
 */
@Composable
fun SafeAreaProvider(
    content: @Composable () -> Unit
) {
    // Retrieve top insets of the status bar and notch dynamically
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding)
    ) {
        content()
    }
}
