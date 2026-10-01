package com.example.traductor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = NavySurface,
    onPrimaryContainer = Color.White,
    secondary = ElectricBlueLight,
    onSecondary = Color.White,
    background = NavyBackground,
    onBackground = Color.White,
    surface = NavySurface,
    onSurface = Color.White,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = NavyBorder
)

@Composable
fun TraductorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
