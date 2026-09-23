package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YouTubeColorScheme = darkColorScheme(
    primary = YouTubeRed,
    onPrimary = Color.White,
    primaryContainer = YouTubeRedDark,
    onPrimaryContainer = Color.White,
    secondary = YouTubeBlue,
    onSecondary = Color.Black,
    background = YouTubeDarkBg,
    onBackground = YouTubeTextPrimary,
    surface = YouTubeDarkSurface,
    onSurface = YouTubeTextPrimary,
    surfaceVariant = YouTubeDarkCard,
    onSurfaceVariant = YouTubeTextSecondary,
    outline = YouTubeDarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // We enforce YouTube's signature dark aesthetic for optimal video viewing experience
    MaterialTheme(
        colorScheme = YouTubeColorScheme,
        typography = Typography,
        content = content
    )
}
