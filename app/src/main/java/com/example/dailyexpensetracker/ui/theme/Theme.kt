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
    onSurfaceVariant = Color.Gray
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = PastelPeach,
    tertiary = SuccessGreen,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = ErrorRed,
    surfaceVariant = LightCard,
    onSurfaceVariant = Color.Gray
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
    get() = if (colorScheme.background == DarkBackground) Color(0xFF2C2C2E) else Color(0xFFEEEEEE)
