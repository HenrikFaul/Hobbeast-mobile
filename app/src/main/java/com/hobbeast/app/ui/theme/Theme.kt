package com.hobbeast.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Warm Neutral Palette (HOB-163) ──────────────────────────────────────────

object HobbeastColors {
    // Warm-neutral surfaces
    val Sand50     = Color(0xFFFAF8F5)
    val Sand100    = Color(0xFFF2EDE6)
    val Sand200    = Color(0xFFE5DDD1)
    val Sand300    = Color(0xFFD0C3B0)

    // Accent – coral-amber
    val Coral500   = Color(0xFFE8602C)
    val Coral600   = Color(0xFFD04E1E)
    val Amber400   = Color(0xFFFFC940)
    val Amber500   = Color(0xFFF5A623)

    // Neutrals
    val Stone700   = Color(0xFF4A4035)
    val Stone800   = Color(0xFF2E261C)
    val Stone900   = Color(0xFF1A1209)

    // Semantic
    val Success    = Color(0xFF3D9970)
    val Warning    = Color(0xFFF5A623)
    val Error      = Color(0xFFD64933)
    val Info       = Color(0xFF4A90D9)

    // Dark surfaces
    val Dark900    = Color(0xFF1A1510)
    val Dark800    = Color(0xFF242018)
    val Dark700    = Color(0xFF302A21)
}

private val LightColorScheme = lightColorScheme(
    primary = HobbeastColors.Coral500,
    onPrimary = Color.White,
    primaryContainer = HobbeastColors.Amber400,
    onPrimaryContainer = HobbeastColors.Stone900,
    secondary = HobbeastColors.Amber500,
    onSecondary = HobbeastColors.Stone900,
    background = HobbeastColors.Sand50,
    onBackground = HobbeastColors.Stone800,
    surface = Color.White,
    onSurface = HobbeastColors.Stone800,
    surfaceVariant = HobbeastColors.Sand100,
    onSurfaceVariant = HobbeastColors.Stone700,
    outline = HobbeastColors.Sand300,
    error = HobbeastColors.Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = HobbeastColors.Coral500,
    onPrimary = Color.White,
    primaryContainer = HobbeastColors.Coral600,
    onPrimaryContainer = Color.White,
    secondary = HobbeastColors.Amber400,
    onSecondary = HobbeastColors.Dark900,
    background = HobbeastColors.Dark900,
    onBackground = HobbeastColors.Sand100,
    surface = HobbeastColors.Dark800,
    onSurface = HobbeastColors.Sand100,
    surfaceVariant = HobbeastColors.Dark700,
    onSurfaceVariant = HobbeastColors.Sand200,
    outline = HobbeastColors.Stone700,
    error = HobbeastColors.Error,
)

// ─── Typography ───────────────────────────────────────────────────────────────

val HobbeastTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp),
)

@Composable
fun HobbeastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = HobbeastTypography,
        content = content,
    )
}
