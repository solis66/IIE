package com.example.lexiscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val iosBlue = Color(0xFF007AFF)
val iosGreen = Color(0xFF34C759)
val iosBg = Color(0xFFF2F2F7)
val iosYellow = Color(0xFFFACC15)

private val DarkColorScheme = darkColorScheme(
    primary = iosBlue,
    secondary = iosGreen,
    background = Color.Black,
    surface = Color(0xFF1A1A1A)
)

private val LightColorScheme = lightColorScheme(
    primary = iosBlue,
    secondary = iosGreen,
    background = iosBg,
    surface = Color.White
)

@Composable
fun LexiScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}