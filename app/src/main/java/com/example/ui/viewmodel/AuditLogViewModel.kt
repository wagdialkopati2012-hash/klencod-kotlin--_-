package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuditLogRepository
import com.example.domain.model.AuditLogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Immutable
data class AuditLogUiState(
    val logs: List<AuditLogEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "الكل",
    val categories: List<String> = listOf("الكل", "المصادقة", "عروض الأسعار", "الأصناف", "بيانات الشركة", "المستخدمون", "النسخ الاحتياطي", "التقارير")
)

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    auditLogRepository: AuditLogRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("الكل")

    val uiState: StateFlow<AuditLogUiState> = combine(
        auditLogRepository.observeAllLogs(),
        searchQuery,
        selectedCategory
    ) { logs, q, cat ->
        val clean = q.trim().lowercase()
        val filtered = logs.filter { entry ->
            val matchCat = cat == "الكل" || entry.category == cat
            val matchQ = clean.isEmpty() ||
                entry.username.lowercase().contains(clean) ||
                entry.operation.lowercase().contains(clean) ||
                entry.recordDescription.lowercase().contains(clean) ||
                entry.recordId.lowercase().contains(clean)
            matchCat && matchQ
        }
        AuditLogUiState(
            logs = filtered,
            searchQuery = q,
            selectedCategory = cat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuditLogUiState()
    )

    fun onSearchChange(value: String) {
        searchQuery.value = value
    }

    fun onCategorySelect(category: String) {
        selectedCategory.value = category
    }
}
