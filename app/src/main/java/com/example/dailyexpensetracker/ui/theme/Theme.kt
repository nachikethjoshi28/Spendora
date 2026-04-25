package com.example.dailyexpensetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = BlueSecondary,
    tertiary = SuccessGreen,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = ErrorRed,
    surfaceVariant = FintechSurface,
    onSurfaceVariant = Color.Gray,
    outline = Color(0xFF3A3A3C),                   // Subtle hairline borders in dark
    outlineVariant = Color(0xFF2C2C2E)
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),          // Soft indigo tint for filled accents
    onPrimaryContainer = Color(0xFF0F1A52),        // Deep ink on indigo tint
    secondary = BlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E5FF),
    onSecondaryContainer = Color(0xFF1F1D54),
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF1A1D29),              // Rich slate ink (premium, not pure black)
    surface = LightSurface,
    onSurface = Color(0xFF1A1D29),
    surfaceVariant = LightCard,
    onSurfaceVariant = Color(0xFF5D6275),          // Refined slate for secondary text
    outline = Color(0xFFD4D7E0),                   // Subtle hairline borders
    outlineVariant = Color(0xFFE6E8EF),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFE6E8),
    onErrorContainer = Color(0xFF7A0E14),
    scrim = Color(0xFF1A1D29)
)

@Composable
fun SpendoraTheme(
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

// Helper extension to get common colors easily
val MaterialTheme.isLight: Boolean
    @Composable
    get() = !isSystemInDarkTheme() // This is a fallback, better to use colorScheme

val MaterialTheme.fintechDeepDark: Color
    @Composable
    get() = colorScheme.background

val MaterialTheme.fintechCard: Color
    @Composable
    get() = colorScheme.surfaceVariant

val MaterialTheme.fintechSurface: Color
    @Composable
    get() = if (colorScheme.background == DarkBackground) Color(0xFF2C2C2E) else Color(0xFFEEF1F8)
