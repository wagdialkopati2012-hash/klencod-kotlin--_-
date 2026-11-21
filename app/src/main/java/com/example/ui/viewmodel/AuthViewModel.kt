package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val rememberUsername: Boolean = false,
    val showPassword: Boolean = false,
    val ownerFullName: String = "",
    val ownerUsername: String = "",
    val ownerPassword: String = "",
    val ownerConfirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val (remember, savedUsername) = authRepository.rememberedUsername.first()
            if (remember && savedUsername.isNotBlank()) {
                _uiState.update {
                    it.copy(username = savedUsername, rememberUsername = true)
                }
            }
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onRememberToggle(checked: Boolean) {
        _uiState.update { it.copy(rememberUsername = checked) }
    }

    fun onToggleShowPassword() {
        _uiState.update { it.copy(showPassword = !it.showPassword) }
    }

    fun onOwnerFullNameChange(value: String) {
        _uiState.update { it.copy(ownerFullName = value, errorMessage = null) }
    }

    fun onOwnerUsernameChange(value: String) {
        _uiState.update { it.copy(ownerUsername = value, errorMessage = null) }
    }

    fun onOwnerPasswordChange(value: String) {
        _uiState.update { it.copy(ownerPassword = value, errorMessage = null) }
    }

    fun onOwnerConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(ownerConfirmPassword = value, errorMessage = null) }
    }

    fun login() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = authRepository.login(s.username, s.password, s.rememberUsername)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, password = "") }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun createInitialOwner() {
        val s = _uiState.value
        if (s.ownerPassword != s.ownerConfirmPassword) {
            _uiState.update { it.copy(errorMessage = "كلمتا المرور غير متطابقتين.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = authRepository.createInitialOwner(
                fullName = s.ownerFullName,
                username = s.ownerUsername,
                password = s.ownerPassword
            )) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            ownerPassword = "",
                            ownerConfirmPassword = ""
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
            }
        }
    }
}
