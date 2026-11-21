package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuditLogRepository
import com.example.data.repository.CompanyRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.QuotationRepository
import com.example.domain.model.AuditLogEntry
import com.example.domain.model.CompanyInfo
import com.example.domain.model.Product
import com.example.domain.model.QuotationWithItems
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import javax.inject.Inject

@Immutable
data class HomeUiState(
    val totalProducts: Int = 0,
    val totalQuotations: Int = 0,
    val totalCustomers: Int = 0,
    val totalQuotationValueFormatted: String = "$0",
    val gregorianDate: String = DateFormatter.formatGregorianFull(System.currentTimeMillis()),
    val companyInfo: CompanyInfo = CompanyInfo(),
    val recentQuotations: List<QuotationWithItems> = emptyList(),
    val recentProducts: List<Product> = emptyList(),
    val recentActivities: List<AuditLogEntry> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    productRepository: ProductRepository,
    quotationRepository: QuotationRepository,
    auditLogRepository: AuditLogRepository,
    companyRepository: CompanyRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        productRepository.allProducts,
        quotationRepository.allQuotations,
        auditLogRepository.observeAllLogs(),
        companyRepository.companyInfo
    ) { products, quotations, logs, company ->
        val totalVal = quotations.fold(BigDecimal.ZERO) { acc, q ->
            acc.add(CurrencyFormatter.parseBigDecimal(q.quotation.totalAmountUsd) ?: BigDecimal.ZERO)
        }
        val distinctCustomers = quotations
            .map { it.quotation.customerName.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .size

        HomeUiState(
            totalProducts = products.size,
            totalQuotations = quotations.size,
            totalCustomers = distinctCustomers,
            totalQuotationValueFormatted = CurrencyFormatter.formatUsd(totalVal),
            gregorianDate = DateFormatter.formatGregorianFull(System.currentTimeMillis()),
            companyInfo = company,
            recentQuotations = quotations.take(4),
            recentProducts = products.take(4),
            recentActivities = logs.take(5)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
