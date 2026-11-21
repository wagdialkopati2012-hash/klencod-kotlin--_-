package com.example.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.BackupRepository
import com.example.data.repository.BackupValidationSummary
import com.example.domain.model.ThemeMode
import com.example.domain.model.ThemePreset
import com.example.domain.model.ThemeSettings
import com.example.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SettingsBackupUiState(
    val themeSettings: ThemeSettings = ThemeSettings(),
    val storageBytes: Long = 0L,
    val isBusy: Boolean = false,
    val pendingRestoreValidation: BackupValidationSummary? = null,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsBackupViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val storageBytes = MutableStateFlow(0L)
    private val isBusy = MutableStateFlow(false)
    private val pendingRestore = MutableStateFlow<BackupValidationSummary?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    init {
        refreshStorageSize()
    }

    private val statusFlow = combine(storageBytes, isBusy, pendingRestore, message, error) { bytes, busy, pending, msg, err ->
        StatusBundle(bytes, busy, pending, msg, err)
    }

    val uiState: StateFlow<SettingsBackupUiState> = combine(
        preferencesManager.themeSettingsFlow,
        statusFlow
    ) { theme, status ->
        SettingsBackupUiState(
            themeSettings = theme,
            storageBytes = status.bytes,
            isBusy = status.busy,
            pendingRestoreValidation = status.pending,
            message = status.msg,
            error = status.err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsBackupUiState()
    )

    fun refreshStorageSize() {
        viewModelScope.launch {
            storageBytes.value = backupRepository.getDatabaseStorageBytes()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.updateThemeMode(mode)
        }
    }

    fun selectPreset(preset: ThemePreset) {
        viewModelScope.launch {
            preferencesManager.updateThemePreset(preset)
        }
    }

    fun applyCustomColors(
        primaryHex: Long,
        secondaryHex: Long,
        accentHex: Long,
        backgroundHex: Long
    ) {
        viewModelScope.launch {
            preferencesManager.updateCustomColors(primaryHex, secondaryHex, accentHex, backgroundHex)
            message.value = "تم تطبيق الألوان المخصصة فوراً."
        }
    }

    fun resetDefaultTheme() {
        viewModelScope.launch {
            preferencesManager.resetDefaultTheme()
            message.value = "تمت إعادة تعيين المظهر الافتراضي."
        }
    }

    fun exportBackup(actor: User, destinationUri: Uri) {
        viewModelScope.launch {
            isBusy.value = true
            backupRepository.exportBackupToUri(actor, destinationUri).fold(
                onSuccess = { summary ->
                    isBusy.value = false
                    message.value = "تم تصدير النسخة الاحتياطية بنجاح (${summary.productsCount} صنف، ${summary.quotationsCount} عرض سعر)."
                    refreshStorageSize()
                },
                onFailure = { err ->
                    isBusy.value = false
                    error.value = err.message ?: "تعذر تصدير النسخة الاحتياطية."
                }
            )
        }
    }

    fun validateBackupForRestore(sourceUri: Uri) {
        viewModelScope.launch {
            isBusy.value = true
            backupRepository.validateBackupFromUri(sourceUri).fold(
                onSuccess = { summary ->
                    isBusy.value = false
                    pendingRestore.value = summary
                },
                onFailure = { err ->
                    isBusy.value = false
                    error.value = err.message ?: "ملف النسخة الاحتياطية غير صالح أو تالف."
                }
            )
        }
    }

    fun dismissRestoreValidation() {
        pendingRestore.value = null
    }

    fun confirmRestoreBackup(actor: User) {
        val summary = pendingRestore.value ?: return
        viewModelScope.launch {
            isBusy.value = true
            pendingRestore.value = null
            backupRepository.restoreValidatedBackup(actor, summary).fold(
                onSuccess = {
                    isBusy.value = false
                    message.value = "تمت استعادة جميع البيانات والإعدادات بنجاح."
                    refreshStorageSize()
                },
                onFailure = { err ->
                    isBusy.value = false
                    error.value = err.message ?: "تعذر استعادة النسخة الاحتياطية."
                }
            )
        }
    }

    fun clearPdfCache() {
        viewModelScope.launch {
            val freed = backupRepository.clearPdfCache()
            refreshStorageSize()
            val kb = (freed / 1024L).coerceAtLeast(0L)
            message.value = "تم تنظيف ذاكرة PDF المؤقتة (${kb} كيلوبايت)."
        }
    }

    fun clearBanners() {
        message.value = null
        error.value = null
    }

    private data class StatusBundle(
        val bytes: Long,
        val busy: Boolean,
        val pending: BackupValidationSummary?,
        val msg: String?,
        val err: String?
    )
}
