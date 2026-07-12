package com.sikamikaniko.sonora.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF8A6CF2)
private val AccentDim = Color(0xFF6C4CF1)

private val SonoraColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentDim,
    background = Color(0xFF0E0F13),
    onBackground = Color(0xFFECECF1),
    surface = Color(0xFF16181F),
    onSurface = Color(0xFFECECF1),
    surfaceVariant = Color(0xFF20232D),
    onSurfaceVariant = Color(0xFFA9AEC0),
    outline = Color(0xFF2C303C)
)

@Composable
fun SonoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SonoraColors,
        typography = Typography(),
        content = content
    )
}
