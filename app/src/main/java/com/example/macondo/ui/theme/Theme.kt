package com.example.macondo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Tonos HCT de Material Color Utilities a partir del azul, dorado y crema del sitio.
private val Claro = lightColorScheme(
    primary = AzulMacondo, onPrimary = Color.White,
    primaryContainer = Color(0xFFC4E7FF), onPrimaryContainer = Color(0xFF001E2D),
    secondary = Color(0xFF755B00), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE6AC), onSecondaryContainer = Color(0xFF241A00),
    tertiary = AzulProfundoMacondo, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCDE5FF), onTertiaryContainer = Color(0xFF001D32),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = CremaMacondo, onBackground = AzulProfundoMacondo,
    surface = CremaMacondo, onSurface = AzulProfundoMacondo,
    surfaceVariant = Color(0xFFF2E0C8), onSurfaceVariant = Color(0xFF514533),
    surfaceTint = AzulMacondo, surfaceBright = CremaMacondo, surfaceDim = Color(0xFFE9D7C0),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFF5EB),
    surfaceContainer = Color(0xFFFFF2E2), surfaceContainerHigh = Color(0xFFFFEED9),
    surfaceContainerHighest = Color(0xFFF8E6CD),
    inverseSurface = Color(0xFF24323E), inverseOnSurface = CremaMacondo,
    inversePrimary = Color(0xFF89CFF8),
    outline = Color(0xFF837561), outlineVariant = Color(0xFFD5C4AD), scrim = Color.Black
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF89CFF8), onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF004C6A), onPrimaryContainer = Color(0xFFC4E7FF),
    secondary = DoradoMacondo, onSecondary = Color(0xFF3E2E00),
    secondaryContainer = Color(0xFF594400), onSecondaryContainer = Color(0xFFFFDF92),
    tertiary = Color(0xFFABCAE9), onTertiary = Color(0xFF11334C),
    tertiaryContainer = Color(0xFF2A4964), onTertiaryContainer = Color(0xFFCDE5FF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF061520), onBackground = Color(0xFFD6E4F5),
    surface = Color(0xFF061520), onSurface = Color(0xFFD6E4F5),
    surfaceVariant = Color(0xFF3B4856), onSurfaceVariant = Color(0xFFBAC8D8),
    surfaceTint = Color(0xFF89CFF8), surfaceBright = Color(0xFF2D3B48), surfaceDim = Color(0xFF061520),
    surfaceContainerLowest = Color(0xFF020F1B), surfaceContainerLow = Color(0xFF0F1D29),
    surfaceContainer = Color(0xFF13212D), surfaceContainerHigh = Color(0xFF1E2B38),
    surfaceContainerHighest = Color(0xFF293643),
    inverseSurface = Color(0xFFD6E4F5), inverseOnSurface = Color(0xFF24323E),
    inversePrimary = AzulMacondo,
    outline = Color(0xFF8492A1), outlineVariant = Color(0xFF3B4856), scrim = Color.Black
)

@Composable
fun MacondoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
        typography = MacondoTypography
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
    }
}
