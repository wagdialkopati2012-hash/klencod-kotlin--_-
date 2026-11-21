package com.example.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CompanyRepository
import com.example.domain.model.CompanyInfo
import com.example.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class CompanyUiState(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val additionalInfo: String = "",
    val logoPath: String? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class CompanyViewModel @Inject constructor(
    private val companyRepository: CompanyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyUiState())
    val uiState: StateFlow<CompanyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            companyRepository.companyInfo.collect { info ->
                _uiState.update { current ->
                    if (current.isEditing) {
                        current.copy(logoPath = info.logoPath)
                    } else {
                        current.copy(
                            name = info.name,
                            phone = info.phone,
                            address = info.address,
                            additionalInfo = info.additionalInfo,
                            logoPath = info.logoPath
                        )
                    }
                }
            }
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true, message = null, error = null) }
    }

    fun cancelEditing(currentInfo: CompanyInfo) {
        _uiState.update {
            it.copy(
                name = currentInfo.name,
                phone = currentInfo.phone,
                address = currentInfo.address,
                additionalInfo = currentInfo.additionalInfo,
                logoPath = currentInfo.logoPath,
                isEditing = false,
                error = null
            )
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value, error = null) }
    }

    fun onAddressChange(value: String) {
        _uiState.update { it.copy(address = value, error = null) }
    }

    fun onAdditionalInfoChange(value: String) {
        _uiState.update { it.copy(additionalInfo = value, error = null) }
    }

    fun saveCompanyInfo(actor: User) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null, error = null) }
            val result = companyRepository.saveCompanyInfo(
                actor = actor,
                name = s.name,
                phone = s.phone,
                address = s.address,
                additionalInfo = s.additionalInfo
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            message = "تم حفظ بيانات الشركة وتحديث جميع عروض الأسعار."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isSaving = false, error = err.message ?: "تعذر حفظ البيانات.")
                    }
                }
            )
        }
    }

    fun uploadLogo(actor: User, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null, error = null) }
            companyRepository.saveCompanyLogo(actor, uri).fold(
                onSuccess = { path ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            logoPath = path,
                            message = "تم تحديث شعار الشركة بنجاح."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isSaving = false, error = err.message ?: "تعذر رفع الشعار.")
                    }
                }
            )
        }
    }

    fun deleteLogo(actor: User) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null, error = null) }
            companyRepository.deleteCompanyLogo(actor).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            logoPath = null,
                            message = "تم حذف شعار الشركة."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isSaving = false, error = err.message ?: "تعذر حذف الشعار.")
                    }
                }
            )
        }
    }

    fun clearBanner() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
