package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Immutable
data class TenderingExtendedColors(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val success: Color,
    val warning: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val subtleSurface: Color,
    val glassBorder: Color,
    val isDark: Boolean
)

val LocalTenderingColors = staticCompositionLocalOf {
    TenderingExtendedColors(
        primary = Color(0xFF0F294A),
        secondary = Color(0xFF1E56A0),
        accent = Color(0xFF0EA5A4),
        success = Color(0xFF10B981),
        warning = Color(0xFFF59E0B),
        heroStart = Color(0xFF0F294A),
        heroEnd = Color(0xFF0EA5A4),
        subtleSurface = Color(0xFFF1F5F9),
        glassBorder = Color(0xFFE2E8F0),
        isDark = false
    )
}

fun Color.contrastOnColor(): Color =
    if (luminance() > 0.45f) Color(0xFF0F172A) else Color.White

fun Color.blendWith(other: Color, ratio: Float): Color {
    val inv = 1f - ratio
    return Color(
        red = red * inv + other.red * ratio,
        green = green * inv + other.green * ratio,
        blue = blue * inv + other.blue * ratio,
        alpha = 1f
    )
}
