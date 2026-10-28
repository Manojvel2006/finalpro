package com.repsense.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Slate950 = Color(0xFF0F172A)
val Slate900 = Color(0xFF1E293B)
val Slate800 = Color(0xFF334155)
val Slate700 = Color(0xFF475569)
val Slate400 = Color(0xFF94A3B8)
val Slate100 = Color(0xFFF1F5F9)

val PrimaryBlue = Color(0xFF2563EB)
val PrimaryBlueLight = Color(0xFF3B82F6)
val PrimaryContainer = Color(0xFF1D4ED8)

val NeonCyan = Color(0xFF06B6D4)
val NeonLime = Color(0xFF84CC16)
val AmberAccent = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF10B981)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Slate900,
    onPrimaryContainer = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Slate800,
    onSecondaryContainer = Color.White,
    tertiary = AmberAccent,
    onTertiary = Color.Black,
    background = Slate950,
    onBackground = Color.White,
    surface = Slate900,
    onSurface = Color.White,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = PrimaryBlue,
    secondary = NeonCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF0E7490),
    tertiary = AmberAccent,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun RepSenseTheme(
    darkTheme: Boolean = true, // Default to sleek dark fitness mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
