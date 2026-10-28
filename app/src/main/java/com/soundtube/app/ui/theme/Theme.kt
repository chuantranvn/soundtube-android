package com.soundtube.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MilkyLightColorScheme = lightColorScheme(
    primary = AccentRed,
    secondary = AccentPurple,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun SoundTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MilkyLightColorScheme,
        typography = Typography,
        content = content
    )
}
