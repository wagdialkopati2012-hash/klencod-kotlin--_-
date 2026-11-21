package com.example.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode(val arabicLabel: String) {
    LIGHT("الوضع الفاتح"),
    DARK("الوضع الداكن"),
    SYSTEM("الوضع التلقائي حسب النظام")
}

@Serializable
enum class ThemePreset(
    val arabicName: String,
    val styleDescription: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long,
    val backgroundLightHex: Long
) {
    PREMIUM_NAVY(
        arabicName = "الأزرق الملكي الفاخر",
        styleDescription = "طابع مؤسسي احترافي",
        primaryHex = 0xFF0F294A,
        secondaryHex = 0xFF1E56A0,
        accentHex = 0xFF0EA5A4,
        backgroundLightHex = 0xFFF7F9FC
    ),
    EMERALD_LUXURY(
        arabicName = "الزمردي الفاخر",
        styleDescription = "فخامة الأعمال",
        primaryHex = 0xFF064E3B,
        secondaryHex = 0xFF059669,
        accentHex = 0xFFD4AF37,
        backgroundLightHex = 0xFFFAF9F5
    ),
    ROYAL_PURPLE(
        arabicName = "الأرجواني الملكي",
        styleDescription = "عصري راقٍ",
        primaryHex = 0xFF3B0764,
        secondaryHex = 0xFF7C3AED,
        accentHex = 0xFFA78BFA,
        backgroundLightHex = 0xFFF8F7FC
    ),
    OCEAN_BLUE(
        arabicName = "أزرق المحيط",
        styleDescription = "تقني نقي",
        primaryHex = 0xFF0369A1,
        secondaryHex = 0xFF0891B2,
        accentHex = 0xFF14B8A6,
        backgroundLightHex = 0xFFF2F7FA
    ),
    BURGUNDY_LUXURY(
        arabicName = "العنابي الفاخر",
        styleDescription = "أناقة كلاسيكية فاخرة",
        primaryHex = 0xFF6B0F1A,
        secondaryHex = 0xFF991B1B,
        accentHex = 0xFFC9A227,
        backgroundLightHex = 0xFFFBF9F4
    ),
    MINIMAL_GRAY(
        arabicName = "الرمادي البسيط",
        styleDescription = "بساطة ونقاء",
        primaryHex = 0xFF1F2937,
        secondaryHex = 0xFF475569,
        accentHex = 0xFF64748B,
        backgroundLightHex = 0xFFFFFFFF
    ),
    MODERN_TEAL(
        arabicName = "التيل العصري",
        styleDescription = "مالي حديث",
        primaryHex = 0xFF0F4C5C,
        secondaryHex = 0xFF0D9488,
        accentHex = 0xFF2DD4BF,
        backgroundLightHex = 0xFFF4FAFA
    )
}

@Immutable
@Serializable
data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val preset: ThemePreset = ThemePreset.PREMIUM_NAVY,
    val useCustomColors: Boolean = false,
    val customPrimaryHex: Long = ThemePreset.PREMIUM_NAVY.primaryHex,
    val customSecondaryHex: Long = ThemePreset.PREMIUM_NAVY.secondaryHex,
    val customAccentHex: Long = ThemePreset.PREMIUM_NAVY.accentHex,
    val customBackgroundHex: Long = ThemePreset.PREMIUM_NAVY.backgroundLightHex
) {
    val activePrimaryHex: Long
        get() = if (useCustomColors) customPrimaryHex else preset.primaryHex

    val activeSecondaryHex: Long
        get() = if (useCustomColors) customSecondaryHex else preset.secondaryHex

    val activeAccentHex: Long
        get() = if (useCustomColors) customAccentHex else preset.accentHex

    val activeBackgroundLightHex: Long
        get() = if (useCustomColors) customBackgroundHex else preset.backgroundLightHex
}
