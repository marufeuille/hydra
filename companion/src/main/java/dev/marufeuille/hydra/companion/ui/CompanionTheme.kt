package dev.marufeuille.hydra.companion.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            darkColorScheme(
                primary = Color(0xFF6EA8FF),
                background = Color(0xFF0B1220),
                surface = Color(0xFF111827),
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF2563EB),
                background = Color(0xFFF8FAFC),
                surface = Color.White,
            )
        },
        content = content,
    )
}
