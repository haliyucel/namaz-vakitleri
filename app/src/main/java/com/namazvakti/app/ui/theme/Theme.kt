package com.namazvakti.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EmeraldDeep = Color(0xFF0A3A2D)
val EmeraldPrimary = Color(0xFF0F5643)
val EmeraldLight = Color(0xFF1A7A5E)
val GoldAccent = Color(0xFFD9A441)
val GoldSoft = Color(0xFFF6DF9E)
val SurfaceLight = Color(0xFFF5F7F4)
val CardLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF091612)
val CardDark = Color(0xFF112620)
val AmberWarn = Color(0xFFE08E22)

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EFE5),
    onPrimaryContainer = EmeraldDeep,
    secondary = GoldAccent,
    onSecondary = Color(0xFF2B1E00),
    secondaryContainer = Color(0xFFFFEFC7),
    onSecondaryContainer = Color(0xFF3E2C00),
    background = SurfaceLight,
    onBackground = Color(0xFF14211C),
    surface = CardLight,
    onSurface = Color(0xFF14211C),
    surfaceVariant = Color(0xFFE9EFEA),
    onSurfaceVariant = Color(0xFF43574F),
    outline = Color(0xFFCAD6D0)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4EC5A0),
    onPrimary = Color(0xFF003829),
    primaryContainer = Color(0xFF0F4F3E),
    onPrimaryContainer = Color(0xFFC5F2E2),
    secondary = GoldSoft,
    onSecondary = Color(0xFF3A2800),
    secondaryContainer = Color(0xFF533B00),
    onSecondaryContainer = Color(0xFFFFEFC7),
    background = SurfaceDark,
    onBackground = Color(0xFFE7F0EC),
    surface = CardDark,
    onSurface = Color(0xFFE7F0EC),
    surfaceVariant = Color(0xFF19352D),
    onSurfaceVariant = Color(0xFFAAC0B7),
    outline = Color(0xFF2B4B40)
)

@Composable
fun NamazTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
