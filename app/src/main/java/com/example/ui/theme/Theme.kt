package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemeMode
import com.example.domain.model.ThemeSettings

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun TenderingTheme(
    settings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val isDark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val rawPrimary = Color(settings.activePrimaryHex)
    val rawSecondary = Color(settings.activeSecondaryHex)
    val rawAccent = Color(settings.activeAccentHex)
    val rawLightBg = Color(settings.activeBackgroundLightHex)

    val springSpec = spring<Color>(stiffness = Spring.StiffnessMediumLow)

    val primaryTarget = if (isDark) rawSecondary.blendWith(Color.White, 0.22f) else rawPrimary
    val secondaryTarget = if (isDark) rawAccent.blendWith(Color.White, 0.15f) else rawSecondary
    val tertiaryTarget = rawAccent
    val bgTarget = if (isDark) Color(0xFF0B1220) else rawLightBg
    val surfaceTarget = if (isDark) Color(0xFF131C2E) else Color.White
    val surfaceVariantTarget = if (isDark) Color(0xFF1B273D) else rawLightBg.blendWith(rawPrimary, 0.06f)

    val primary by animateColorAsState(primaryTarget, springSpec, label = "primary")
    val secondary by animateColorAsState(secondaryTarget, springSpec, label = "secondary")
    val tertiary by animateColorAsState(tertiaryTarget, springSpec, label = "tertiary")
    val background by animateColorAsState(bgTarget, springSpec, label = "background")
    val surface by animateColorAsState(surfaceTarget, springSpec, label = "surface")
    val surfaceVariant by animateColorAsState(surfaceVariantTarget, springSpec, label = "surfaceVariant")

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = primary.contrastOnColor(),
            primaryContainer = rawPrimary.blendWith(Color(0xFF0F172A), 0.35f),
            onPrimaryContainer = Color(0xFFE2E8F0),
            secondary = secondary,
            onSecondary = secondary.contrastOnColor(),
            secondaryContainer = rawSecondary.blendWith(Color(0xFF0F172A), 0.45f),
            onSecondaryContainer = Color(0xFFE2E8F0),
            tertiary = tertiary,
            onTertiary = tertiary.contrastOnColor(),
            background = background,
            onBackground = Color(0xFFF1F5F9),
            surface = surface,
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = Color(0xFF334155),
            outlineVariant = Color(0xFF1E293B),
            error = Color(0xFFF87171),
            onError = Color(0xFF450A0A)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = primary.contrastOnColor(),
            primaryContainer = primary.blendWith(Color.White, 0.86f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = secondary.contrastOnColor(),
            secondaryContainer = secondary.blendWith(Color.White, 0.86f),
            onSecondaryContainer = secondary,
            tertiary = tertiary,
            onTertiary = tertiary.contrastOnColor(),
            background = background,
            onBackground = Color(0xFF0F172A),
            surface = surface,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            outlineVariant = Color(0xFFE2E8F0),
            error = Color(0xFFDC2626),
            onError = Color.White
        )
    }

    val extended = TenderingExtendedColors(
        primary = primary,
        secondary = secondary,
        accent = tertiary,
        success = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
        warning = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
        heroStart = rawPrimary,
        heroEnd = rawAccent,
        subtleSurface = if (isDark) Color(0xFF192438) else rawLightBg.blendWith(rawPrimary, 0.04f),
        glassBorder = if (isDark) Color(0xFF28354D) else Color(0xFFE2E8F0),
        isDark = isDark
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalTenderingColors provides extended
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
