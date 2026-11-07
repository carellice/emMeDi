package com.emmdi.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Colori presi dall'icona: indaco/viola, con il verde menta della spunta.
private val Light = lightColorScheme(
    primary = Color(0xFF5B3FD9), onPrimary = Color.White,
    primaryContainer = Color(0xFFE7DEFF), onPrimaryContainer = Color(0xFF1D0B63),
    secondary = Color(0xFF00A676), onSecondary = Color.White,
    secondaryContainer = Color(0xFFC6F4E0), onSecondaryContainer = Color(0xFF002117),
    tertiary = Color(0xFFB4407A),
    background = Color(0xFFFBF9FF), onBackground = Color(0xFF1B1A22),
    surface = Color(0xFFFBF9FF), onSurface = Color(0xFF1B1A22),
    surfaceVariant = Color(0xFFE6E0F2), onSurfaceVariant = Color(0xFF5E5A6E),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F2FD),
    surfaceContainer = Color(0xFFF1ECFA), surfaceContainerHigh = Color(0xFFEBE5F6),
    surfaceContainerHighest = Color(0xFFE5DEF2),
    outline = Color(0xFF8E889E), outlineVariant = Color(0xFFD8D1E6),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFC6B6FF), onPrimary = Color(0xFF2B1683),
    primaryContainer = Color(0xFF42299F), onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFF52E0A6), onSecondary = Color(0xFF003827),
    secondaryContainer = Color(0xFF00513A), onSecondaryContainer = Color(0xFFC6F4E0),
    tertiary = Color(0xFFFFAFD3),
    background = Color(0xFF13111A), onBackground = Color(0xFFE6E1EE),
    surface = Color(0xFF13111A), onSurface = Color(0xFFE6E1EE),
    surfaceVariant = Color(0xFF2E2A3A), onSurfaceVariant = Color(0xFFB2ACC2),
    surfaceContainerLowest = Color(0xFF0E0C14), surfaceContainerLow = Color(0xFF1A1722),
    surfaceContainer = Color(0xFF1F1B28), surfaceContainerHigh = Color(0xFF292533),
    surfaceContainerHighest = Color(0xFF34303F),
    outline = Color(0xFF8C869B), outlineVariant = Color(0xFF3E394B),
)

val BrandGradient = Brush.linearGradient(listOf(Color(0xFF3B2F8F), Color(0xFF6A3FE0), Color(0xFF8B5CFF)))

private val Base = Typography()
private val AppTypography = Typography(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(lineHeight = 26.sp),
)

@Composable
fun EmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}

val MonoStyle = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp, lineHeight = 22.sp)
