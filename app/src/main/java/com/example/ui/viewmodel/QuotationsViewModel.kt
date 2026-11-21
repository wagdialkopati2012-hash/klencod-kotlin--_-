package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.QuotationRepository
import com.example.domain.model.Quotation
import com.example.domain.model.QuotationItem
import com.example.domain.model.QuotationStatus
import com.example.domain.model.QuotationWithItems
import com.example.domain.model.User
import com.example.domain.util.CurrencyFormatter
import com.example.ui.navigation.SelectedProductPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import javax.inject.Inject

enum class QuotationSortOrder(val arabicLabel: String) {
    NUMBER_DESC("الأحدث (رقم العرض)"),
    NUMBER_ASC("الأقدم (رقم العرض)"),
    AMOUNT_DESC("الأعلى قيمة"),
    DATE_DESC("الأحدث تاريخاً")
}

@Immutable
data class QuotationsListUiState(
    val quotations: List<QuotationWithItems> = emptyList(),
    val searchQuery: String = "",
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val sortOrder: QuotationSortOrder = QuotationSortOrder.NUMBER_DESC,
    val pendingDelete: Quotation? = null,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class QuotationsListViewModel @Inject constructor(
    private val quotationRepository: QuotationRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val startDateMillis = MutableStateFlow<Long?>(null)
    private val endDateMillis = MutableStateFlow<Long?>(null)
    private val sortOrder = MutableStateFlow(QuotationSortOrder.NUMBER_DESC)
    private val pendingDelete = MutableStateFlow<Quotation?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    private val filterFlow = combine(searchQuery, startDateMillis, endDateMillis, sortOrder) { q, start, end, sort ->
        FilterParams(q, start, end, sort)
    }

    val uiState: StateFlow<QuotationsListUiState> = combine(
        quotationRepository.allQuotations,
        filterFlow,
        pendingDelete,
        message,
        error
    ) { all, filter, deleteTarget, msg, err ->
        val q = filter.query.trim().lowercase()
        val filtered = all.filter { item ->
            val quot = item.quotation
            val matchesQuery = q.isEmpty() ||
                quot.customerName.lowercase().contains(q) ||
                quot.quotationNumber.toString().contains(q) ||
                quot.createdByUsername.lowercase().contains(q)
            val matchesStart = filter.start == null || quot.dateMillis >= filter.start
            val matchesEnd = filter.end == null || quot.dateMillis <= (filter.end + 86_399_999L)
            matchesQuery && matchesStart && matchesEnd
        }
        val sorted = when (filter.sort) {
            QuotationSortOrder.NUMBER_DESC -> filtered.sortedByDescending { it.quotation.quotationNumber }
            QuotationSortOrder.NUMBER_ASC -> filtered.sortedBy { it.quotation.quotationNumber }
            QuotationSortOrder.AMOUNT_DESC -> filtered.sortedByDescending {
                CurrencyFormatter.parseBigDecimal(it.quotation.totalAmountUsd) ?: BigDecimal.ZERO
            }
            QuotationSortOrder.DATE_DESC -> filtered.sortedByDescending { it.quotation.dateMillis }
        }
        QuotationsListUiState(
            quotations = sorted,
            searchQuery = filter.query,
            startDateMillis = filter.start,
            endDateMillis = filter.end,
            sortOrder = filter.sort,
            pendingDelete = deleteTarget,
            message = msg,
            error = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuotationsListUiState()
    )

    fun onSearchChange(value: String) {
        searchQuery.value = value
    }

    fun onSortOrderChange(order: QuotationSortOrder) {
        sortOrder.value = order
    }

    fun setDateRange(start: Long?, end: Long?) {
        startDateMillis.value = start
        endDateMillis.value = end
    }

    fun requestDelete(quotation: Quotation) {
        pendingDelete.value = quotation
    }

    fun dismissDelete() {
        pendingDelete.value = null
    }

    fun confirmPermanentDelete(actor: User) {
        val target = pendingDelete.value ?: return
        viewModelScope.launch {
            pendingDelete.value = null
            quotationRepository.deleteQuotationPermanently(actor, target).fold(
                onSuccess = {
                    message.value = "تم حذف عرض السعر رقم #${target.quotationNumber} نهائياً."
                },
                onFailure = { err ->
                    error.value = err.message ?: "تعذر حذف عرض السعر."
                }
            )
        }
    }

    fun clearBanners() {
        message.value = null
        error.value = null
    }

    private data class FilterParams(
        val query: String,
        val start: Long?,
        val end: Long?,
        val sort: QuotationSortOrder
    )
}

@Immutable
data class DraftQuotationLine(
    val localKey: String,
    val productId: Long?,
    val productName: String,
    val unitPriceUsd: String,
    val quantityInput: String = "", // CRITICAL: Always starts EMPTY on product selection!
    val lineTotalUsd: BigDecimal? = null,
    val quantityError: String? = null
)

@Immutable
data class CreateEditQuotationUiState(
    val editingQuotationId: Long = 0L,
    val displayQuotationNumber: Long = 1L,
    val isEditing: Boolean = false,
    val originalQuotation: Quotation? = null,
    val customerName: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val status: QuotationStatus = QuotationStatus.SAVED,
    val lines: List<DraftQuotationLine> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val grandTotal: BigDecimal
        get() = lines.fold(BigDecimal.ZERO) { acc, line ->
            acc.add(line.lineTotalUsd ?: BigDecimal.ZERO)
        }
}

@HiltViewModel
class CreateEditQuotationViewModel @Inject constructor(
    private val quotationRepository: QuotationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditQuotationUiState())
    val uiState: StateFlow<CreateEditQuotationUiState> = _uiState.asStateFlow()
    private var loadedId: Long? = null

    fun initialize(quotationId: Long) {
        if (loadedId == quotationId) return
        loadedId = quotationId

        viewModelScope.launch {
            if (quotationId <= 0L) {
                val nextNum = quotationRepository.getNextExpectedQuotationNumber()
                _uiState.value = CreateEditQuotationUiState(
                    editingQuotationId = 0L,
                    displayQuotationNumber = nextNum,
                    isEditing = false,
                    dateMillis = System.currentTimeMillis()
                )
            } else {
                val existing = quotationRepository.getQuotationOnce(quotationId)
                if (existing != null) {
                    val q = existing.quotation
                    _uiState.value = CreateEditQuotationUiState(
                        editingQuotationId = q.id,
                        displayQuotationNumber = q.quotationNumber,
                        isEditing = true,
                        originalQuotation = q,
                        customerName = q.customerName,
                        dateMillis = q.dateMillis,
                        notes = q.notes,
                        status = q.status,
                        lines = existing.items.mapIndexed { idx, item ->
                            val qtyDec = CurrencyFormatter.parsePositiveBigDecimal(item.quantity)
                            val unitDec = CurrencyFormatter.parseBigDecimal(item.unitPriceUsd) ?: BigDecimal.ZERO
                            val lineTotal = if (qtyDec != null) {
                                CurrencyFormatter.calculateLineTotal(unitDec, qtyDec)
                            } else {
                                CurrencyFormatter.parseBigDecimal(item.lineTotalUsd)
                            }
                            DraftQuotationLine(
                                localKey = "${item.id}_${idx}_${System.nanoTime()}",
                                productId = item.productId,
                                productName = item.productName,
                                unitPriceUsd = item.unitPriceUsd,
                                quantityInput = CurrencyFormatter.formatQuantity(item.quantity).replace(",", ""),
                                lineTotalUsd = lineTotal,
                                quantityError = null
                            )
                        }
                    )
                } else {
                    _uiState.update { it.copy(error = "تعذر العثور على عرض السعر المطلوب.") }
                }
            }
        }
    }

    fun onCustomerNameChange(value: String) {
        _uiState.update { it.copy(customerName = value, error = null) }
    }

    fun onDateChange(millis: Long) {
        _uiState.update { it.copy(dateMillis = millis, error = null) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value, error = null) }
    }

    fun onStatusChange(status: QuotationStatus) {
        _uiState.update { it.copy(status = status) }
    }

    fun getExistingQuantitiesJson(): String {
        val map = mutableMapOf<Long, BigDecimal>()
        for (line in _uiState.value.lines) {
            val pid = line.productId ?: continue
            val qty = CurrencyFormatter.parsePositiveBigDecimal(line.quantityInput) ?: continue
            map[pid] = (map[pid] ?: BigDecimal.ZERO).add(qty)
        }
        if (map.isEmpty()) return ""
        val stringMap = map.mapValues { (_, qty) -> qty.stripTrailingZeros().toPlainString() }
        return runCatching { Json.encodeToString(stringMap) }.getOrDefault("")
    }

    /**
     * Adds all selected products simultaneously and merges quantities intelligently
     * if any selected product already exists in the current quotation.
     */
    fun addSelectedProducts(selected: List<SelectedProductPayload>) {
        if (selected.isEmpty()) return
        _uiState.update { current ->
            current.copy(
                lines = mergeSelectedProductsIntoLines(current.lines, selected),
                error = null
            )
        }
    }

    fun onLineQuantityChange(localKey: String, input: String) {
        val normalized = CurrencyFormatter.normalizeDigits(input)
        _uiState.update { current ->
            val updatedLines = current.lines.map { line ->
                if (line.localKey != localKey) {
                    line
                } else {
                    val trimmed = normalized.trim()
                    if (trimmed.isEmpty()) {
                        line.copy(
                            quantityInput = normalized,
                            lineTotalUsd = null,
                            quantityError = "الكمية مطلوبة"
                        )
                    } else {
                        val qty = CurrencyFormatter.parseBigDecimal(trimmed)
                        if (qty == null) {
                            line.copy(
                                quantityInput = normalized,
                                lineTotalUsd = null,
                                quantityError = "قيمة رقمية غير صالحة"
                            )
                        } else if (qty <= BigDecimal.ZERO) {
                            line.copy(
                                quantityInput = normalized,
                                lineTotalUsd = null,
                                quantityError = "يجب أن تكون الكمية أكبر من صفر"
                            )
                        } else {
                            val unit = CurrencyFormatter.parseBigDecimal(line.unitPriceUsd) ?: BigDecimal.ZERO
                            val total = CurrencyFormatter.calculateLineTotal(unit, qty)
                            line.copy(
                                quantityInput = normalized,
                                lineTotalUsd = total,
                                quantityError = null
                            )
                        }
                    }
                }
            }
            current.copy(lines = updatedLines, error = null)
        }
    }

    fun removeLine(localKey: String) {
        _uiState.update { current ->
            current.copy(lines = current.lines.filterNot { it.localKey == localKey })
        }
    }

    fun saveQuotation(
        actor: User,
        onSaved: (savedQuotationId: Long) -> Unit
    ) {
        val s = _uiState.value
        if (s.customerName.trim().isEmpty()) {
            _uiState.update { it.copy(error = "يرجى إدخال اسم العميل.") }
            return
        }
        if (s.lines.isEmpty()) {
            _uiState.update { it.copy(error = "يرجى اختيار صنف واحد على الأقل لإضافته إلى عرض السعر.") }
            return
        }

        // Re-validate every line quantity strictly
        val validatedLines = s.lines.map { line ->
            val qty = CurrencyFormatter.parsePositiveBigDecimal(line.quantityInput)
            if (qty == null) {
                line.copy(
                    lineTotalUsd = null,
                    quantityError = if (line.quantityInput.isBlank()) {
                        "يرجى إدخال الكمية يدوياً"
                    } else {
                        "الكمية يجب أن تكون أكبر من صفر"
                    }
                )
            } else {
                line
            }
        }
        if (validatedLines.any { it.quantityError != null || it.lineTotalUsd == null }) {
            _uiState.update {
                it.copy(
                    lines = validatedLines,
                    error = "توجد أصناف بكميات غير مكتملة أو غير صالحة. يرجى إدخال كمية صحيحة لكل صنف."
                )
            }
            return
        }

        val quotationItems = validatedLines.map { line ->
            val qty = CurrencyFormatter.parsePositiveBigDecimal(line.quantityInput)!!
            val unit = CurrencyFormatter.parseBigDecimal(line.unitPriceUsd) ?: BigDecimal.ZERO
            val lineTotal = CurrencyFormatter.calculateLineTotal(unit, qty)
            QuotationItem(
                productId = line.productId,
                productName = line.productName,
                unitPriceUsd = line.unitPriceUsd,
                quantity = qty.stripTrailingZeros().toPlainString(),
                lineTotalUsd = CurrencyFormatter.toCanonicalString(lineTotal)
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val original = s.originalQuotation
            if (s.isEditing && original != null) {
                quotationRepository.updateQuotation(
                    actor = actor,
                    original = original,
                    customerName = s.customerName,
                    dateMillis = s.dateMillis,
                    notes = s.notes,
                    status = s.status,
                    items = quotationItems
                ).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isSaving = false) }
                        onSaved(original.id)
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(isSaving = false, error = err.message ?: "تعذر حفظ التعديلات.")
                        }
                    }
                )
            } else {
                quotationRepository.createQuotation(
                    actor = actor,
                    customerName = s.customerName,
                    dateMillis = s.dateMillis,
                    notes = s.notes,
                    status = s.status,
                    items = quotationItems
                ).fold(
                    onSuccess = { (newId, _) ->
                        _uiState.update { it.copy(isSaving = false) }
                        onSaved(newId)
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(isSaving = false, error = err.message ?: "تعذر إنشاء عرض السعر.")
                        }
                    }
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun mergeSelectedProductsIntoLines(
            existingLines: List<DraftQuotationLine>,
            selected: List<SelectedProductPayload>
        ): List<DraftQuotationLine> {
            val mutableLines = existingLines.toMutableList()
            for (payload in selected) {
                val incomingQty = CurrencyFormatter.parsePositiveBigDecimal(payload.quantity) ?: continue
                val existingIndex = mutableLines.indexOfFirst { it.productId == payload.productId }
                if (existingIndex >= 0) {
                    val existingLine = mutableLines[existingIndex]
                    val currentQty = CurrencyFormatter.parsePositiveBigDecimal(existingLine.quantityInput) ?: BigDecimal.ZERO
                    val combinedQty = currentQty.add(incomingQty)
                    val unitDec = CurrencyFormatter.parseBigDecimal(existingLine.unitPriceUsd) ?: BigDecimal.ZERO
                    val newLineTotal = CurrencyFormatter.calculateLineTotal(unitDec, combinedQty)
                    mutableLines[existingIndex] = existingLine.copy(
                        quantityInput = combinedQty.stripTrailingZeros().toPlainString(),
                        lineTotalUsd = newLineTotal,
                        quantityError = null
                    )
                } else {
                    val unitDec = CurrencyFormatter.parseBigDecimal(payload.unitPriceUsd) ?: BigDecimal.ZERO
                    val newLineTotal = CurrencyFormatter.calculateLineTotal(unitDec, incomingQty)
                    mutableLines.add(
                        DraftQuotationLine(
                            localKey = "prod_${payload.productId}_${System.nanoTime()}_${mutableLines.size}",
                            productId = payload.productId,
                            productName = payload.productName,
                            unitPriceUsd = payload.unitPriceUsd,
                            quantityInput = incomingQty.stripTrailingZeros().toPlainString(),
                            lineTotalUsd = newLineTotal,
                            quantityError = null
                        )
                    )
                }
            }
            return mutableLines
        }
    }
}

@HiltViewModel
class QuotationPreviewViewModel @Inject constructor(
    private val quotationRepository: QuotationRepository
) : ViewModel() {

    private val _quotation = MutableStateFlow<QuotationWithItems?>(null)
    val quotation: StateFlow<QuotationWithItems?> = _quotation.asStateFlow()

    fun load(quotationId: Long) {
        viewModelScope.launch {
            quotationRepository.observeQuotation(quotationId).collect {
                _quotation.value = it
            }
        }
    }
}
