package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = DeepPurple,
    primaryContainer = DeepPurple,
    onPrimaryContainer = AccentPurple,
    secondary = AccentGreen,
    onSecondary = DarkGreenBg,
    secondaryContainer = DarkGreenBg,
    onSecondaryContainer = AccentGreen,
    tertiary = AccentPurple,
    onTertiary = DeepPurple,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkBg,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = AccentError,
    onError = DarkErrorBg
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SophisticatedDarkColorScheme,
        typography = Typography,
        content = content
    )
}
