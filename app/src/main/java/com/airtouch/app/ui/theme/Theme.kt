package com.airtouch.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airtouch.app.model.ThemeSkin

@Immutable
data class AirPalette(
    val isDark: Boolean,
    val background: Color,
    val backgroundTop: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceGlass: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentStrong: Color,
    val accentSoft: Color,
    val border: Color,
    val borderStrong: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val shadow: Color
)

val LocalAirPalette = staticCompositionLocalOf { obsidianPalette }

private val obsidianPalette = AirPalette(
    isDark = true,
    background = Color(0xFF08090C),
    backgroundTop = Color(0xFF101217),
    surface = Color(0xFF13151B),
    surfaceRaised = Color(0xFF1B1E26),
    surfaceGlass = Color(0xCC1A1D25),
    textPrimary = Color(0xFFF4F6FA),
    textSecondary = Color(0xFFA2A9B6),
    textTertiary = Color(0xFF6D7481),
    accent = Color(0xFF8CA9FF),
    accentStrong = Color(0xFFAFC2FF),
    accentSoft = Color(0x298CA9FF),
    border = Color(0xFF2A2E38),
    borderStrong = Color(0xFF3A4050),
    success = Color(0xFF69D9A7),
    warning = Color(0xFFF0C46B),
    danger = Color(0xFFFF7C85),
    shadow = Color(0x66000000)
)

private val silverPalette = AirPalette(
    isDark = false,
    background = Color(0xFFF1F2F6),
    backgroundTop = Color(0xFFFAFBFF),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF5F6FA),
    surfaceGlass = Color(0xF2FFFFFF),
    textPrimary = Color(0xFF101217),
    textSecondary = Color(0xFF626978),
    textTertiary = Color(0xFF939BA9),
    accent = Color(0xFF0A7CFF),
    accentStrong = Color(0xFF005FD1),
    accentSoft = Color(0x1F0A7CFF),
    border = Color(0xFFDDE1E9),
    borderStrong = Color(0xFFC6CCD7),
    success = Color(0xFF168A62),
    warning = Color(0xFFB67B14),
    danger = Color(0xFFD9434B),
    shadow = Color(0x1A242A36)
)

private val midnightPalette = AirPalette(
    isDark = true,
    background = Color(0xFF050F1A),
    backgroundTop = Color(0xFF0A1B2B),
    surface = Color(0xFF0B1A29),
    surfaceRaised = Color(0xFF12283C),
    surfaceGlass = Color(0xCC0E2133),
    textPrimary = Color(0xFFEAF7FF),
    textSecondary = Color(0xFF91B4CC),
    textTertiary = Color(0xFF5F829A),
    accent = Color(0xFF5AC8FA),
    accentStrong = Color(0xFF8ADDFF),
    accentSoft = Color(0x295AC8FA),
    border = Color(0xFF1A354B),
    borderStrong = Color(0xFF28506C),
    success = Color(0xFF5BE0B3),
    warning = Color(0xFFFFD078),
    danger = Color(0xFFFF8098),
    shadow = Color(0x80000000)
)

private val rosePalette = AirPalette(
    isDark = true,
    background = Color(0xFF140E12),
    backgroundTop = Color(0xFF21161D),
    surface = Color(0xFF20161C),
    surfaceRaised = Color(0xFF2E2029),
    surfaceGlass = Color(0xCC261922),
    textPrimary = Color(0xFFFFF4F8),
    textSecondary = Color(0xFFC7A6B4),
    textTertiary = Color(0xFF90707E),
    accent = Color(0xFFFF9DB7),
    accentStrong = Color(0xFFFFC2D1),
    accentSoft = Color(0x2EFF9DB7),
    border = Color(0xFF3A2832),
    borderStrong = Color(0xFF523846),
    success = Color(0xFF72E2B5),
    warning = Color(0xFFF4C77B),
    danger = Color(0xFFFF7E8E),
    shadow = Color(0x80000000)
)

fun paletteFor(skin: ThemeSkin): AirPalette = when (skin) {
    ThemeSkin.OBSIDIAN -> obsidianPalette
    ThemeSkin.SILVER -> silverPalette
    ThemeSkin.MIDNIGHT -> midnightPalette
    ThemeSkin.ROSE -> rosePalette
}

private val AirTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        letterSpacing = (-1.4).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.4.sp
    )
)

private val AirShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp)
)

@Composable
fun AirTouchTheme(
    skin: ThemeSkin,
    content: @Composable () -> Unit
) {
    val palette = paletteFor(skin)
    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.background,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.textSecondary,
            onSecondary = palette.background,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceRaised,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.borderStrong,
            outlineVariant = palette.border,
            error = palette.danger
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.textSecondary,
            onSecondary = Color.White,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceRaised,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.borderStrong,
            outlineVariant = palette.border,
            error = palette.danger
        )
    }

    CompositionLocalProvider(LocalAirPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AirTypography,
            shapes = AirShapes,
            content = content
        )
    }
}

/** 快捷访问当前皮肤色板。 */
val airPalette: AirPalette
    @Composable get() = LocalAirPalette.current

@Suppress("unused")
private val unusedSystemDarkCheck: Boolean
    @Composable get() = isSystemInDarkTheme()
