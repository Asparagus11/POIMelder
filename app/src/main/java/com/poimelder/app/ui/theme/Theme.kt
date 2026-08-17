package com.poimelder.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Znuny-inspirierte Pastellpalette ────────────────────────────────────────
// Verspielt, freundlich, leichtgewichtig und modern.

// Lila-Familie (Hauptakzent)
private val Purple50 = Color(0xFFF5F2FF)
private val Purple100 = Color(0xFFEDE8FF)
private val Purple200 = Color(0xFFDCD4FF)
private val Purple300 = Color(0xFFC4B1FF)
private val Purple400 = Color(0xFFA885FF)
private val Purple500 = Color(0xFF8A4FFF)
private val Purple600 = Color(0xFF7F30F7)
private val Purple700 = Color(0xFF711EE3)

// Grün (Pastell-Sekundärfarbe – frisch & freundlich)
private val Green100 = Color(0xFFE7F9E7)
private val Green400 = Color(0xFF5ABF5A)
private val Green600 = Color(0xFF2E9E2E)

// Blau (Tertiär – leichtgewichtig & kühl)
private val Blue100 = Color(0xFFE3F2FF)
private val Blue200 = Color(0xFFC1E5F9)
private val Blue500 = Color(0xFF54A2FF)

// Orange (Akzent für Warnung/Aufmerksamkeit)
private val Orange500 = Color(0xFFFE8A26)

// ─── Light Color Scheme ──────────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary = Purple500,
    onPrimary = Color.White,
    primaryContainer = Purple100,
    onPrimaryContainer = Purple700,

    secondary = Green400,
    onSecondary = Color.White,
    secondaryContainer = Green100,
    onSecondaryContainer = Green600,

    tertiary = Blue500,
    onTertiary = Color.White,
    tertiaryContainer = Blue100,
    onTertiaryContainer = Color(0xFF1A4E7A),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Purple50,
    onBackground = Color(0xFF212327),
    surface = Color.White,
    onSurface = Color(0xFF212327),
    surfaceVariant = Purple100,
    onSurfaceVariant = Color(0xFF484E56),

    outline = Purple200,
    outlineVariant = Purple100,
)

// ─── Dark Color Scheme ───────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary = Purple300,
    onPrimary = Purple700,
    primaryContainer = Purple600,
    onPrimaryContainer = Purple100,

    secondary = Green400,
    onSecondary = Color(0xFF003E00),
    secondaryContainer = Color(0xFF1B5E1B),
    onSecondaryContainer = Green100,

    tertiary = Blue200,
    onTertiary = Color(0xFF003355),
    tertiaryContainer = Color(0xFF004A77),
    onTertiaryContainer = Blue100,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF1A1A2E),
    onBackground = Color(0xFFE8E4F0),
    surface = Color(0xFF212136),
    onSurface = Color(0xFFE8E4F0),
    surfaceVariant = Color(0xFF312F45),
    onSurfaceVariant = Purple200,

    outline = Purple400,
    outlineVariant = Color(0xFF3D3A52),
)

@Composable
fun POIMelderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
