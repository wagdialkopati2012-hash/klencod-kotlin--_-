package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.domain.model.User
import com.example.domain.model.UserPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class UsersUiState(
    val users: List<User> = emptyList(),
    val searchQuery: String = "",
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UsersUiState> = combine(
        authRepository.allUsers,
        searchQuery,
        message,
        error
    ) { users, q, msg, err ->
        val clean = q.trim().lowercase()
        val filtered = if (clean.isEmpty()) users else users.filter {
            it.fullName.lowercase().contains(clean) ||
                it.username.lowercase().contains(clean) ||
                it.roleLabel.lowercase().contains(clean)
        }
        UsersUiState(
            users = filtered,
            searchQuery = q,
            message = msg,
            error = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UsersUiState()
    )

    fun onSearchChange(value: String) {
        searchQuery.value = value
    }

    fun addUser(
        actor: User,
        username: String,
        fullName: String,
        password: String,
        roleLabel: String,
        isSuperAdmin: Boolean,
        permissions: UserPermissions,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            authRepository.addUser(
                actor = actor,
                username = username,
                fullName = fullName,
                password = password,
                roleLabel = roleLabel,
                isSuperAdmin = isSuperAdmin,
                permissions = permissions
            ).fold(
                onSuccess = {
                    message.value = "تمت إضافة المستخدم «$fullName» بنجاح."
                    onDone()
                },
                onFailure = { err ->
                    error.value = err.message ?: "تعذر إضافة المستخدم."
                }
            )
        }
    }

    fun updateUserDetails(
        actor: User,
        targetUserId: Long,
        fullName: String,
        roleLabel: String,
        isSuperAdmin: Boolean,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            authRepository.updateUserDetails(actor, targetUserId, fullName, roleLabel, isSuperAdmin).fold(
                onSuccess = {
                    message.value = "تم تحديث بيانات المستخدم."
                    onDone()
                },
                onFailure = { err ->
                    error.value = err.message ?: "تعذر تحديث بيانات المستخدم."
                }
            )
        }
    }

    fun changePassword(actor: User, targetUserId: Long, newPassword: String, onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.changeUserPassword(actor, targetUserId, newPassword).fold(
                onSuccess = {
                    message.value = "تم تغيير كلمة المرور بنجاح."
                    onDone()
                },
                onFailure = { err ->
                    error.value = err.message ?: "تعذر تغيير كلمة المرور."
                }
            )
        }
    }

    fun toggleUserActive(actor: User, targetUser: User) {
        viewModelScope.launch {
            authRepository.setUserActiveStatus(actor, targetUser.id, !targetUser.isActive).fold(
                onSuccess = {
                    message.value = if (!targetUser.isActive) {
                        "تم تفعيل حساب ${targetUser.fullName}."
                    } else {
                        "تم تعليق حساب ${targetUser.fullName}."
                    }
                },
                onFailure = { err ->
                    error.value = err.message ?: "تعذر تغيير حالة الحساب."
                }
            )
        }
    }

    fun updatePermissions(actor: User, targetUserId: Long, permissions: UserPermissions, onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.updateUserPermissions(actor, targetUserId, permissions).fold(
                onSuccess = {
                    message.value = "تم تحديث مصفوفة الصلاحيات."
                    onDone()
                },
                onFailure = { err ->
                    error.value = err.message ?: "تعذر تحديث الصلاحيات."
                }
            )
        }
    }

    fun deleteUser(actor: User, targetUser: User) {
        viewModelScope.launch {
            authRepository.deleteUser(actor, targetUser.id).fold(
                onSuccess = {
                    message.value = "تم حذف المستخدم «${targetUser.fullName}»."
                },
                onFailure = { err ->
                    error.value = err.message ?: "تعذر حذف المستخدم."
                }
            )
        }
    }

    fun clearBanners() {
        message.value = null
        error.value = null
    }
}
