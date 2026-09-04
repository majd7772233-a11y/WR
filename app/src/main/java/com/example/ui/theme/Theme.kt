package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WallRushColorScheme = darkColorScheme(
    primary = Player1Primary,
    onPrimary = Color.Black,
    primaryContainer = Player1Dark,
    onPrimaryContainer = Color.White,
    secondary = Player2Primary,
    onSecondary = Color.White,
    secondaryContainer = Player2Dark,
    onSecondaryContainer = Color.White,
    tertiary = WallWoodPrimary,
    onTertiary = Color.Black,
    background = DeepSlateBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardLight,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun WallRushTheme(
    darkTheme: Boolean = true, // Default to sleek dark game aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WallRushColorScheme,
        typography = Typography,
        content = content
    )
}
