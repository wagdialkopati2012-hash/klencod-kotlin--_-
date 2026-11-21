package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuditLogRepository
import com.example.data.repository.QuotationRepository
import com.example.domain.model.QuotationWithItems
import com.example.domain.model.User
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class ReportTab(val arabicLabel: String) {
    FINANCIAL("مالية"),
    DAILY("يومي"),
    MONTHLY("شهري"),
    ANNUAL("سنوي"),
    BY_CUSTOMER("حسب العميل"),
    BY_USER("حسب المستخدم")
}

@Immutable
data class ReportGroupRow(
    val label: String,
    val count: Int,
    val totalValue: BigDecimal,
    val totalFormatted: String,
    val proportion: Float
)

@Immutable
data class ReportsUiState(
    val activeTab: ReportTab = ReportTab.FINANCIAL,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val customerFilter: String = "",
    val userFilter: String = "",
    val quotationNumberFilter: String = "",
    val totalQuotations: Int = 0,
    val totalValueFormatted: String = "$0",
    val averageValueFormatted: String = "$0",
    val highestValueFormatted: String = "$0",
    val groupedRows: List<ReportGroupRow> = emptyList(),
    val filteredQuotations: List<QuotationWithItems> = emptyList(),
    val availableCustomers: List<String> = emptyList(),
    val availableUsers: List<String> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val quotationRepository: QuotationRepository,
    private val auditLogRepository: AuditLogRepository
) : ViewModel() {

    private val activeTab = MutableStateFlow(ReportTab.FINANCIAL)
    private val startDate = MutableStateFlow<Long?>(null)
    private val endDate = MutableStateFlow<Long?>(null)
    private val customerFilter = MutableStateFlow("")
    private val userFilter = MutableStateFlow("")
    private val numberFilter = MutableStateFlow("")
    private val currentActor = MutableStateFlow<User?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    private val filterFlow = combine(
        activeTab, startDate, endDate, customerFilter, userFilter
    ) { tab, s, e, cust, usr ->
        FilterA(tab, s, e, cust, usr)
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        quotationRepository.allQuotations,
        filterFlow,
        numberFilter,
        currentActor,
        combine(message, error) { m, e -> m to e }
    ) { all, f, numFilter, actor, banners ->
        val perms = actor?.effectivePermissions()
        val scopedList = when {
            actor == null -> emptyList()
            perms?.canViewAllReports == true -> all
            perms?.canViewOwnReports == true -> all.filter { it.quotation.createdByUserId == actor.id }
            else -> emptyList()
        }

        val numClean = numFilter.trim()
        val filtered = scopedList.filter { item ->
            val q = item.quotation
            val matchStart = f.start == null || q.dateMillis >= f.start
            val matchEnd = f.end == null || q.dateMillis <= (f.end + 86_399_999L)
            val matchCust = f.customer.isBlank() ||
                q.customerName.contains(f.customer.trim(), ignoreCase = true)
            val matchUser = f.user.isBlank() ||
                q.createdByUsername.contains(f.user.trim(), ignoreCase = true)
            val matchNum = numClean.isEmpty() || q.quotationNumber.toString().contains(numClean)
            matchStart && matchEnd && matchCust && matchUser && matchNum
        }

        val totals = filtered.map {
            CurrencyFormatter.parseBigDecimal(it.quotation.totalAmountUsd) ?: BigDecimal.ZERO
        }
        val sum = totals.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
        val avg = if (totals.isNotEmpty()) {
            sum.divide(BigDecimal(totals.size), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        val highest = totals.maxOrNull() ?: BigDecimal.ZERO

        val grouped = buildGroupedRows(filtered, f.tab)

        ReportsUiState(
            activeTab = f.tab,
            startDateMillis = f.start,
            endDateMillis = f.end,
            customerFilter = f.customer,
            userFilter = f.user,
            quotationNumberFilter = numFilter,
            totalQuotations = filtered.size,
            totalValueFormatted = CurrencyFormatter.formatUsd(sum),
            averageValueFormatted = CurrencyFormatter.formatUsd(avg),
            highestValueFormatted = CurrencyFormatter.formatUsd(highest),
            groupedRows = grouped,
            filteredQuotations = filtered,
            availableCustomers = scopedList.map { it.quotation.customerName }.distinct().sorted(),
            availableUsers = scopedList.map { it.quotation.createdByUsername }.distinct().sorted(),
            message = banners.first,
            error = banners.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    fun setActor(user: User?) {
        currentActor.value = user
    }

    fun onTabChange(tab: ReportTab) {
        activeTab.value = tab
    }

    fun setDateRange(start: Long?, end: Long?) {
        startDate.value = start
        endDate.value = end
    }

    fun onCustomerFilterChange(value: String) {
        customerFilter.value = value
    }

    fun onUserFilterChange(value: String) {
        userFilter.value = value
    }

    fun onNumberFilterChange(value: String) {
        numberFilter.value = value
    }

    fun clearFilters() {
        startDate.value = null
        endDate.value = null
        customerFilter.value = ""
        userFilter.value = ""
        numberFilter.value = ""
    }

    fun exportCsvReport(actor: User, uri: Uri) {
        if (!actor.effectivePermissions().canExportReports) {
            error.value = "ليس لديك صلاحية تصدير التقارير."
            return
        }
        val rows = uiState.value.filteredQuotations
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val sb = StringBuilder()
                    sb.append("\uFEFF") // UTF-8 BOM for Arabic Excel compatibility
                    sb.append("رقم العرض,العميل,التاريخ,المستخدم,الحالة,الإجمالي (USD)\n")
                    rows.forEach { item ->
                        val q = item.quotation
                        val safeCustomer = q.customerName.replace("\"", "\"\"")
                        sb.append("${q.quotationNumber},\"$safeCustomer\",${DateFormatter.formatShortDate(q.dateMillis)},${q.createdByUsername},${q.status.arabicLabel},${q.totalAmountUsd}\n")
                    }
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(sb.toString().toByteArray(Charsets.UTF_8))
                    } ?: error("تعذر حفظ ملف التقرير.")
                    auditLogRepository.record(
                        username = actor.username,
                        operation = "تصدير تقرير",
                        category = "التقارير",
                        recordDescription = "تصدير تقرير CSV يحتوي على ${rows.size} عرض سعر"
                    )
                }.fold(
                    onSuccess = { message.value = "تم تصدير التقرير بنجاح (${rows.size} سجل)." },
                    onFailure = { err -> error.value = err.message ?: "تعذر تصدير التقرير." }
                )
            }
        }
    }

    fun clearBanners() {
        message.value = null
        error.value = null
    }

    private fun buildGroupedRows(list: List<QuotationWithItems>, tab: ReportTab): List<ReportGroupRow> {
        if (list.isEmpty()) return emptyList()

        val dayFmt = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        val monthFmt = SimpleDateFormat("yyyy/MM", Locale.US)
        val yearFmt = SimpleDateFormat("yyyy", Locale.US)

        val groups: Map<String, List<QuotationWithItems>> = when (tab) {
            ReportTab.FINANCIAL, ReportTab.MONTHLY -> list.groupBy { monthFmt.format(Date(it.quotation.dateMillis)) }
            ReportTab.DAILY -> list.groupBy { dayFmt.format(Date(it.quotation.dateMillis)) }
            ReportTab.ANNUAL -> list.groupBy { yearFmt.format(Date(it.quotation.dateMillis)) }
            ReportTab.BY_CUSTOMER -> list.groupBy { it.quotation.customerName.ifBlank { "غير محدد" } }
            ReportTab.BY_USER -> list.groupBy { it.quotation.createdByUsername }
        }

        val computed = groups.map { (key, items) ->
            val sum = items.fold(BigDecimal.ZERO) { acc, item ->
                acc.add(CurrencyFormatter.parseBigDecimal(item.quotation.totalAmountUsd) ?: BigDecimal.ZERO)
            }
            Triple(key, items.size, sum)
        }.sortedByDescending { it.third }

        val maxVal = computed.maxOfOrNull { it.third }?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE

        return computed.map { (label, count, sum) ->
            val ratio = sum.divide(maxVal, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0.05f, 1f)
            ReportGroupRow(
                label = label,
                count = count,
                totalValue = sum,
                totalFormatted = CurrencyFormatter.formatUsd(sum),
                proportion = ratio
            )
        }
    }

    private data class FilterA(
        val tab: ReportTab,
        val start: Long?,
        val end: Long?,
        val customer: String,
        val user: String
    )
}
