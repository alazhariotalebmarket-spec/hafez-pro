package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IslamicColorScheme = darkColorScheme(
    primary = IslamicGreenPrimary,
    onPrimary = Color.Black,
    primaryContainer = IslamicGreenContainer,
    onPrimaryContainer = IslamicGreenLight,
    secondary = IslamicGold,
    onSecondary = Color.Black,
    secondaryContainer = IslamicGoldContainer,
    onSecondaryContainer = IslamicGoldLight,
    tertiary = IslamicGreenLight,
    onTertiary = Color.Black,
    background = IslamicBlack,
    onBackground = IslamicTextPrimary,
    surface = IslamicDarkSurface,
    onSurface = IslamicTextPrimary,
    surfaceVariant = IslamicDarkSurfaceVariant,
    onSurfaceVariant = IslamicTextSecondary,
    outline = IslamicBorder,
    outlineVariant = IslamicDarkSurfaceVariant,
    error = IslamicRed,
    onError = Color.White,
    errorContainer = IslamicRedContainer,
    onErrorContainer = Color(0xFFFCA5A5)
)

@Composable
fun HafezProTheme(
    content: @Composable () -> Unit
) {
    // Hafez Pro defaults strictly to the dark Islamic emerald theme requested
    MaterialTheme(
        colorScheme = IslamicColorScheme,
        typography = Typography,
        content = content
    )
}
