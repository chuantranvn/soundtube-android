package com.soundtube.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalThemeIsDark = compositionLocalOf { false }

private val MilkyLightColorScheme = lightColorScheme(
    primary = AccentRed,
    secondary = AccentPurple,
    background = Color(0xFFEDF0FA),
    surface = Color(0xFFF5F8FD),
    surfaceVariant = Color(0xFFE2ECF7),
    onPrimary = Color.White,
    onBackground = Color(0xFF1A1528),
    onSurface = Color(0xFF1A1528),
    onSurfaceVariant = Color(0xFF5A5072)
)

private val MilkyDarkColorScheme = darkColorScheme(
    primary = AccentRed,
    secondary = AccentPurple,
    background = Color(0xFF141122),
    surface = Color(0xFF1B172E),
    surfaceVariant = Color(0xFF241F3C),
    onPrimary = Color.White,
    onBackground = Color(0xFFF4F3F8),
    onSurface = Color(0xFFF4F3F8),
    onSurfaceVariant = Color(0xFFA59EB8)
)

@Composable
fun SoundTubeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MilkyDarkColorScheme else MilkyLightColorScheme
    CompositionLocalProvider(LocalThemeIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
