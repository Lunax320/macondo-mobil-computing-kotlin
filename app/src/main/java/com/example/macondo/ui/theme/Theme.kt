package com.example.macondo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Claro = lightColorScheme(
    primary = Color(0xFF16394D), onPrimary = Color.White,
    secondary = Color(0xFF785425), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E1BD), onSecondaryContainer = Color(0xFF39260F),
    background = Color(0xFFFFFAF1), onBackground = Color(0xFF172C38),
    surface = Color(0xFFFFFAF1), onSurface = Color(0xFF172C38),
    surfaceVariant = Color(0xFFF2EADE), onSurfaceVariant = Color(0xFF514B43),
    outlineVariant = Color(0xFFD5C7B7)
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFB4D8F0), onPrimary = Color(0xFF102D40),
    secondary = Color(0xFFF0CC8F), secondaryContainer = Color(0xFF4C361A),
    onSecondaryContainer = Color(0xFFF9E5C4), background = Color(0xFF101B23),
    surface = Color(0xFF101B23), surfaceVariant = Color(0xFF22313A), onSurface = Color(0xFFF8F0E3)
)

@Composable
fun MacondoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
        typography = MacondoTypography,
        content = content
    )
}
