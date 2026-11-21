package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ThemeMode
import com.example.domain.model.ThemePreset
import com.example.domain.model.ThemeSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tendering_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val USE_CUSTOM_COLORS = booleanPreferencesKey("use_custom_colors")
        val CUSTOM_PRIMARY = longPreferencesKey("custom_primary")
        val CUSTOM_SECONDARY = longPreferencesKey("custom_secondary")
        val CUSTOM_ACCENT = longPreferencesKey("custom_accent")
        val CUSTOM_BACKGROUND = longPreferencesKey("custom_background")
        val REMEMBER_USERNAME = booleanPreferencesKey("remember_username")
        val SAVED_USERNAME = stringPreferencesKey("saved_username")
        val ACTIVE_USER_ID = longPreferencesKey("active_user_id")
    }

    val themeSettingsFlow: Flow<ThemeSettings> = context.dataStore.data.map { prefs ->
        val mode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.LIGHT
        val preset = prefs[Keys.THEME_PRESET]?.let { runCatching { ThemePreset.valueOf(it) }.getOrNull() }
            ?: ThemePreset.PREMIUM_NAVY
        ThemeSettings(
            themeMode = mode,
            preset = preset,
            useCustomColors = prefs[Keys.USE_CUSTOM_COLORS] ?: false,
            customPrimaryHex = prefs[Keys.CUSTOM_PRIMARY] ?: preset.primaryHex,
            customSecondaryHex = prefs[Keys.CUSTOM_SECONDARY] ?: preset.secondaryHex,
            customAccentHex = prefs[Keys.CUSTOM_ACCENT] ?: preset.accentHex,
            customBackgroundHex = prefs[Keys.CUSTOM_BACKGROUND] ?: preset.backgroundLightHex
        )
    }

    val rememberedUsernameFlow: Flow<Pair<Boolean, String>> = context.dataStore.data.map { prefs ->
        val remember = prefs[Keys.REMEMBER_USERNAME] ?: false
        val username = if (remember) prefs[Keys.SAVED_USERNAME].orEmpty() else ""
        remember to username
    }

    val activeUserIdFlow: Flow<Long?> = context.dataStore.data.map { prefs ->
        val id = prefs[Keys.ACTIVE_USER_ID] ?: -1L
        if (id > 0L) id else null
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateThemePreset(preset: ThemePreset) {
        context.dataStore.edit {
            it[Keys.THEME_PRESET] = preset.name
            it[Keys.USE_CUSTOM_COLORS] = false
        }
    }

    suspend fun updateCustomColors(
        primaryHex: Long,
        secondaryHex: Long,
        accentHex: Long,
        backgroundHex: Long
    ) {
        context.dataStore.edit {
            it[Keys.USE_CUSTOM_COLORS] = true
            it[Keys.CUSTOM_PRIMARY] = primaryHex
            it[Keys.CUSTOM_SECONDARY] = secondaryHex
            it[Keys.CUSTOM_ACCENT] = accentHex
            it[Keys.CUSTOM_BACKGROUND] = backgroundHex
        }
    }

    suspend fun resetDefaultTheme() {
        val defaultPreset = ThemePreset.PREMIUM_NAVY
        context.dataStore.edit {
            it[Keys.THEME_MODE] = ThemeMode.LIGHT.name
            it[Keys.THEME_PRESET] = defaultPreset.name
            it[Keys.USE_CUSTOM_COLORS] = false
            it[Keys.CUSTOM_PRIMARY] = defaultPreset.primaryHex
            it[Keys.CUSTOM_SECONDARY] = defaultPreset.secondaryHex
            it[Keys.CUSTOM_ACCENT] = defaultPreset.accentHex
            it[Keys.CUSTOM_BACKGROUND] = defaultPreset.backgroundLightHex
        }
    }

    suspend fun applyThemeSettings(settings: ThemeSettings) {
        context.dataStore.edit {
            it[Keys.THEME_MODE] = settings.themeMode.name
            it[Keys.THEME_PRESET] = settings.preset.name
            it[Keys.USE_CUSTOM_COLORS] = settings.useCustomColors
            it[Keys.CUSTOM_PRIMARY] = settings.customPrimaryHex
            it[Keys.CUSTOM_SECONDARY] = settings.customSecondaryHex
            it[Keys.CUSTOM_ACCENT] = settings.customAccentHex
            it[Keys.CUSTOM_BACKGROUND] = settings.customBackgroundHex
        }
    }

    suspend fun saveSession(userId: Long, username: String, rememberUsername: Boolean) {
        context.dataStore.edit {
            it[Keys.ACTIVE_USER_ID] = userId
            it[Keys.REMEMBER_USERNAME] = rememberUsername
            if (rememberUsername) {
                it[Keys.SAVED_USERNAME] = username
            } else {
                it.remove(Keys.SAVED_USERNAME)
            }
        }
    }

    suspend fun clearActiveSession() {
        context.dataStore.edit {
            it.remove(Keys.ACTIVE_USER_ID)
        }
    }
}
