package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.AuthRepository
import com.example.data.repository.CompanyRepository
import com.example.domain.model.CompanyInfo
import com.example.domain.model.ThemeSettings
import com.example.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data class Unauthenticated(val needsInitialOwnerSetup: Boolean) : SessionState
    data class Authenticated(val user: User) : SessionState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    companyRepository: CompanyRepository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    val themeSettings: StateFlow<ThemeSettings> = preferencesManager.themeSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeSettings()
        )

    val companyInfo: StateFlow<CompanyInfo> = companyRepository.companyInfo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CompanyInfo()
        )

    val sessionState: StateFlow<SessionState> = combine(
        authRepository.hasAnyUser,
        authRepository.currentUser
    ) { hasUsers, currentUser ->
        when {
            currentUser != null -> SessionState.Authenticated(currentUser)
            else -> SessionState.Unauthenticated(needsInitialOwnerSetup = !hasUsers)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionState.Loading
    )

    fun logout() {
        val activeUser = (sessionState.value as? SessionState.Authenticated)?.user
        viewModelScope.launch {
            authRepository.logout(activeUser)
        }
    }
}
