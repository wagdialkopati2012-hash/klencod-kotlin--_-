package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ThemeMode
import com.example.domain.model.ThemePreset
import com.example.domain.model.User
import com.example.domain.util.DateFormatter
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumTopBar
import com.example.ui.theme.LocalTenderingColors
import com.example.ui.viewmodel.SettingsBackupViewModel

@Composable
fun SettingsScreen(
    currentUser: User,
    onBack: () -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenCompany: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsBackupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val perms = currentUser.effectivePermissions()
    val ext = LocalTenderingColors.current

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        PremiumTopBar(
            title = "الإعدادات",
            subtitle = "المظهر، التخزين، النسخ الاحتياطي، ومعلومات النظام",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "msg") {
                AnimatedVisibility(visible = state.message != null) {
                    Surface(
                        color = ext.success.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.message.orEmpty(),
                                color = ext.success,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::clearBanners, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = ext.success)
                            }
                        }
                    }
                }
            }

            item(key = "appearance") {
                MoreMenuRow(
                    title = "المظهر والألوان",
                    subtitle = "${state.themeSettings.themeMode.arabicLabel} • ${state.themeSettings.preset.arabicName}",
                    icon = Icons.Default.ColorLens,
                    tint = MaterialTheme.colorScheme.primary,
                    testTag = "settings_theme_row",
                    onClick = onOpenThemeSettings
                )
            }

            if (perms.canViewCompany) {
                item(key = "company") {
                    MoreMenuRow(
                        title = "بيانات الشركة",
                        subtitle = "تحديث الاسم والهاتف والعنوان وشعار الترويسة",
                        icon = Icons.Default.Business,
                        tint = MaterialTheme.colorScheme.secondary,
                        testTag = "settings_company_row",
                        onClick = onOpenCompany
                    )
                }
            }

            if (perms.canManageBackup) {
                item(key = "backup") {
                    MoreMenuRow(
                        title = "النسخ الاحتياطي والاستعادة",
                        subtitle = "حفظ واستعادة قاعدة البيانات والصور والإعدادات",
                        icon = Icons.Default.Backup,
                        tint = ext.accent,
                        testTag = "settings_backup_row",
                        onClick = onOpenBackupRestore
                    )
                }
            }

            item(key = "storage") {
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إدارة التخزين المحلي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val sizeKb = (state.storageBytes / 1024L).coerceAtLeast(1L)
                    val sizeLabel = if (sizeKb >= 1024L) {
                        String.format("%.2f ميجابايت", sizeKb / 1024f)
                    } else {
                        "$sizeKb كيلوبايت"
                    }
                    Text(
                        text = "حجم قاعدة البيانات والملفات المحلية: $sizeLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = viewModel::clearPdfCache,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنظيف ذاكرة PDF المؤقتة")
                    }
                }
            }

            item(key = "app_info") {
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ext.accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "معلومات التطبيق",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("التطبيق: عرض سعر", style = MaterialTheme.typography.bodyMedium)
                    Text("الإصدار: 1.0.0 (Native Kotlin + Jetpack Compose)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("التخزين: Room + DataStore (100% بدون إنترنت)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item(key = "logout") {
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsBackupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.themeSettings
    val ext = LocalTenderingColors.current

    var editingColorSlot by remember { mutableStateOf<ColorSlot?>(null) }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        PremiumTopBar(
            title = "المظهر والألوان",
            subtitle = "تطبيق فوري على جميع الشاشات والتقارير",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "theme_banner") {
                AnimatedVisibility(visible = state.message != null) {
                    Surface(
                        color = ext.success.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message.orEmpty(),
                            color = ext.success,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // A. Theme Mode Selection
            item(key = "theme_mode") {
                PremiumCard {
                    Text(
                        text = "وضع المظهر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val icon = when (mode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            }
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.arabicLabel, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_${mode.name}")
                            )
                        }
                    }
                }
            }

            // B & C. 7 Ready-Made Color Themes Visual Grid
            item(key = "presets_header") {
                Text(
                    text = "الثيمات الجاهزة (معاينة حية)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val presetRows = ThemePreset.entries.chunked(2)
            items(presetRows.size, key = { "preset_row_$it" }) { rowIdx ->
                val pair = presetRows[rowIdx]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { preset ->
                        val isSelected = !settings.useCustomColors && settings.preset == preset
                        ThemePresetPreviewCard(
                            preset = preset,
                            selected = isSelected,
                            onClick = { viewModel.selectPreset(preset) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_preset_${preset.name}")
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // D. Custom Color Selection
            item(key = "custom_colors") {
                PremiumCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تخصيص الألوان يدوياً",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val primaryColor = settings.activePrimaryHex
                    val secondaryColor = settings.activeSecondaryHex
                    val accentColor = settings.activeAccentHex
                    val bgColor = settings.activeBackgroundLightHex

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomColorPickerRow("اللون الأساسي (Primary)", primaryColor) {
                            editingColorSlot = ColorSlot.PRIMARY
                        }
                        CustomColorPickerRow("اللون الثانوي (Secondary)", secondaryColor) {
                            editingColorSlot = ColorSlot.SECONDARY
                        }
                        CustomColorPickerRow("لون التمييز (Accent)", accentColor) {
                            editingColorSlot = ColorSlot.ACCENT
                        }
                        CustomColorPickerRow("لون الخلفية (Background)", bgColor) {
                            editingColorSlot = ColorSlot.BACKGROUND
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = viewModel::resetDefaultTheme,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_default_theme_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة تعيين المظهر الافتراضي", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    editingColorSlot?.let { slot ->
        val initialHex = when (slot) {
            ColorSlot.PRIMARY -> settings.activePrimaryHex
            ColorSlot.SECONDARY -> settings.activeSecondaryHex
            ColorSlot.ACCENT -> settings.activeAccentHex
            ColorSlot.BACKGROUND -> settings.activeBackgroundLightHex
        }
        ColorPickerDialog(
            title = slot.arabicLabel,
            initialHex = initialHex,
            onDismiss = { editingColorSlot = null },
            onSelect = { chosenHex ->
                val p = if (slot == ColorSlot.PRIMARY) chosenHex else settings.activePrimaryHex
                val s = if (slot == ColorSlot.SECONDARY) chosenHex else settings.activeSecondaryHex
                val a = if (slot == ColorSlot.ACCENT) chosenHex else settings.activeAccentHex
                val b = if (slot == ColorSlot.BACKGROUND) chosenHex else settings.activeBackgroundLightHex
                viewModel.applyCustomColors(p, s, a, b)
                editingColorSlot = null
            }
        )
    }
}

private enum class ColorSlot(val arabicLabel: String) {
    PRIMARY("اختيار اللون الأساسي"),
    SECONDARY("اختيار اللون الثانوي"),
    ACCENT("اختيار لون التمييز"),
    BACKGROUND("اختيار لون الخلفية")
}

@Composable
private fun ThemePresetPreviewCard(
    preset: ThemePreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = Color(preset.primaryHex)
    val s = Color(preset.secondaryHex)
    val a = Color(preset.accentHex)
    val bg = Color(preset.backgroundLightHex)

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.5.dp else 1.dp,
            color = if (selected) p else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (selected) 6.dp else 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Mini UI Preview inside card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(0.6.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(p)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(s.copy(alpha = 0.22f))
                        )
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(a)
                        )
                    }
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "محدد",
                        tint = p,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preset.arabicName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = preset.styleDescription,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ColorDot(p)
                ColorDot(s)
                ColorDot(a)
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
            .border(0.6.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
    )
}

@Composable
private fun CustomColorPickerRow(
    label: String,
    colorHex: Long,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(colorHex))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initialHex: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    val initialColor = Color(initialHex)
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }

    val curatedSwatches = listOf(
        0xFF0F294A, 0xFF1E56A0, 0xFF0EA5A4, 0xFF064E3B,
        0xFF059669, 0xFFD4AF37, 0xFF3B0764, 0xFF7C3AED,
        0xFF0369A1, 0xFF6B0F1A, 0xFF1F2937, 0xFFF7F9FC,
        0xFFFAF9F5, 0xFFFFFFFF
    )

    val previewColor = Color(red, green, blue, 1f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )

                // Quick swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    curatedSwatches.take(7).forEach { hex ->
                        val c = Color(hex)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable {
                                    red = c.red; green = c.green; blue = c.blue
                                }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    curatedSwatches.drop(7).forEach { hex ->
                        val c = Color(hex)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable {
                                    red = c.red; green = c.green; blue = c.blue
                                }
                        )
                    }
                }

                Text("الأحمر (R): ${(red * 255).toInt()}", style = MaterialTheme.typography.labelSmall)
                Slider(value = red, onValueChange = { red = it })
                Text("الأخضر (G): ${(green * 255).toInt()}", style = MaterialTheme.typography.labelSmall)
                Slider(value = green, onValueChange = { green = it })
                Text("الأزرق (B): ${(blue * 255).toInt()}", style = MaterialTheme.typography.labelSmall)
                Slider(value = blue, onValueChange = { blue = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                val argbLong = previewColor.toArgb().toLong() and 0xFFFFFFFFL
                onSelect(argbLong)
            }) {
                Text("تطبيق")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun BackupRestoreScreen(
    currentUser: User,
    onBack: () -> Unit,
    viewModel: SettingsBackupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = LocalTenderingColors.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(currentUser, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.validateBackupForRestore(uri)
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        PremiumTopBar(
            title = "النسخ الاحتياطي والاستعادة",
            subtitle = "تصدير واستيراد محلي آمن عبر Storage Access Framework",
            onBack = onBack
        )

        if (state.isBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "banners") {
                AnimatedVisibility(visible = state.message != null) {
                    Surface(
                        color = ext.success.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message.orEmpty(),
                            color = ext.success,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                AnimatedVisibility(visible = state.error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item(key = "export_card") {
                PremiumCard {
                    Text(
                        text = "إنشاء نسخة احتياطية كاملة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تشمل النسخة الاحتياطية: الأصناف، العملاء، عروض الأسعار وتفاصيلها، عداد الترقيم، المستخدمين والصلاحيات، بيانات وشعار الشركة، سجل العمليات، وإعدادات المظهر.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            exportLauncher.launch("نسخة_نظام_المناقصات_${System.currentTimeMillis()}.json")
                        },
                        enabled = !state.isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("export_backup_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير نسخة احتياطية", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item(key = "restore_card") {
                PremiumCard {
                    Text(
                        text = "استعادة نسخة احتياطية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يتم التحقق من سلامة ملف النسخة الاحتياطية وبنيته تلقائياً قبل استبدال أي بيانات حالية، ولن يتم الكتابة فوق البيانات إلا بعد تأكيدك الصريح.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        enabled = !state.isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("restore_backup_button")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار ملف للاستعادة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Validation & Explicit Confirmation Dialog before overwriting existing data
    state.pendingRestoreValidation?.let { summary ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreValidation,
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("تم التحقق من سلامة ملف النسخة الاحتياطية بنجاح:")
                    Text("• تاريخ النسخة: ${DateFormatter.formatDateTime(summary.exportedAt)}")
                    Text("• المستخدمون: ${summary.usersCount}")
                    Text("• الأصناف: ${summary.productsCount}")
                    Text("• عروض الأسعار: ${summary.quotationsCount} (آخر رقم: #${summary.quotationCounter})")
                    Text("• العملاء: ${summary.customersCount}")
                    Text("• سجلات العمليات: ${summary.auditLogsCount}")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تحذير: ستؤدي الاستعادة إلى استبدال البيانات الحالية بمحتويات هذه النسخة. هل تريد المتابعة؟",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRestoreBackup(currentUser) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الاستعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRestoreValidation) { Text("إلغاء") }
            }
        )
    }
}
