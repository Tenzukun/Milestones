package com.nathan.milestones.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Pink theme for light mode */
private val LightColors = lightColorScheme(
    primary = Color(0xFFD63A6E),            // buttons, "NEXT UP", switches
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),   // soft pink: home background tint, detail cards
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFFF0642D),          // peach accent
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF380D00),
    background = Color(0xFFFFF8F9),
    surface = Color(0xFFFFFBFF)
)

/** Pink theme for dark mode */
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7B2949),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFFFB59D),
    onSecondary = Color(0xFF5C1900),
    secondaryContainer = Color(0xFF7D2D0F),
    onSecondaryContainer = Color(0xFFFFDBCF)
)

/**
 * The app's theme: always pink (no longer follows your wallpaper colours).
 * Follows your phone's light/dark mode.
 */
@Composable
fun MilestonesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, // from Type.kt in this same folder
        content = content
    )
}