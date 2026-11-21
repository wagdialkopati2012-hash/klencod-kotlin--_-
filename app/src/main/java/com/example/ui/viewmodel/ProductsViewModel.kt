package com.example.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ProductRepository
import com.example.domain.model.DuplicateImportPolicy
import com.example.domain.model.ExcelParseResult
import com.example.domain.model.ImportSummary
import com.example.domain.model.Product
import com.example.domain.model.User
import com.example.domain.util.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProductSortOrder(val arabicLabel: String) {
    NEWEST("الأحدث"),
    NAME_ASC("الاسم (أ - ي)"),
    PRICE_DESC("الأعلى سعراً"),
    PRICE_ASC("الأقل سعراً")
}

@Immutable
data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: ProductSortOrder = ProductSortOrder.NEWEST,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val pendingExcelParse: ExcelParseResult? = null,
    val importSummary: ImportSummary? = null,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(ProductSortOrder.NEWEST)
    private val isSelectionMode = MutableStateFlow(false)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val isLoading = MutableStateFlow(false)
    private val pendingExcelParse = MutableStateFlow<ExcelParseResult?>(null)
    private val importSummary = MutableStateFlow<ImportSummary?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    private val filterFlow = combine(searchQuery, sortOrder, isSelectionMode, selectedIds) { q, sort, selMode, selIds ->
        FilterParams(q, sort, selMode, selIds)
    }

    private val statusFlow = combine(isLoading, pendingExcelParse, importSummary, message, error) { loading, parse, summary, msg, err ->
        StatusParams(loading, parse, summary, msg, err)
    }

    val uiState: StateFlow<ProductsUiState> = combine(
        productRepository.allProducts,
        filterFlow,
        statusFlow
    ) { allProducts, filter, status ->
        val query = filter.query.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter {
                it.name.lowercase().contains(query) || it.formattedCode.lowercase().contains(query)
            }
        }
        val sorted = when (filter.sort) {
            ProductSortOrder.NEWEST -> filtered.sortedByDescending { it.createdAt }
            ProductSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            ProductSortOrder.PRICE_DESC -> filtered.sortedByDescending { it.priceDecimal }
            ProductSortOrder.PRICE_ASC -> filtered.sortedBy { it.priceDecimal }
        }
        ProductsUiState(
            products = sorted,
            searchQuery = filter.query,
            sortOrder = filter.sort,
            isSelectionMode = filter.selectionMode,
            selectedIds = filter.selectedIds,
            isLoading = status.loading,
            pendingExcelParse = status.parse,
            importSummary = status.summary,
            message = status.message,
            error = status.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductsUiState()
    )

    fun onSearchChange(value: String) {
        searchQuery.value = value
    }

    fun onSortOrderChange(order: ProductSortOrder) {
        sortOrder.value = order
    }

    fun toggleSelectionMode(enable: Boolean? = null) {
        val next = enable ?: !isSelectionMode.value
        isSelectionMode.value = next
        if (!next) selectedIds.value = emptySet()
    }

    fun toggleSelectProduct(id: Long) {
        if (!isSelectionMode.value) isSelectionMode.value = true
        selectedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun selectAllVisible() {
        val visibleIds = uiState.value.products.map { it.id }.toSet()
        isSelectionMode.value = true
        selectedIds.value = if (selectedIds.value.size == visibleIds.size && visibleIds.isNotEmpty()) {
            emptySet()
        } else {
            visibleIds
        }
    }

    fun addProduct(actor: User, name: String, priceText: String, onDone: () -> Unit) {
        val price = CurrencyFormatter.parsePositiveBigDecimal(priceText)
        if (price == null) {
            error.value = "يرجى إدخال سعر صحيح أكبر من صفر."
            return
        }
        viewModelScope.launch {
            isLoading.value = true
            productRepository.addProduct(actor, name, price).fold(
                onSuccess = {
                    isLoading.value = false
                    message.value = "تمت إضافة الصنف بنجاح."
                    onDone()
                },
                onFailure = { err ->
                    isLoading.value = false
                    error.value = err.message ?: "تعذر إضافة الصنف."
                }
            )
        }
    }

    fun updateProduct(actor: User, product: Product, name: String, priceText: String, onDone: () -> Unit) {
        val price = CurrencyFormatter.parsePositiveBigDecimal(priceText)
        if (price == null) {
            error.value = "يرجى إدخال سعر صحيح أكبر من صفر."
            return
        }
        viewModelScope.launch {
            isLoading.value = true
            productRepository.updateProduct(actor, product, name, price).fold(
                onSuccess = {
                    isLoading.value = false
                    message.value = "تم تحديث الصنف بنجاح."
                    onDone()
                },
                onFailure = { err ->
                    isLoading.value = false
                    error.value = err.message ?: "تعذر تحديث الصنف."
                }
            )
        }
    }

    fun deleteProduct(actor: User, product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(actor, product).fold(
                onSuccess = {
                    selectedIds.update { it - product.id }
                    message.value = "تم حذف الصنف «${product.name}»."
                },
                onFailure = { err ->
                    error.value = err.message ?: "تعذر حذف الصنف."
                }
            )
        }
    }

    fun bulkDeleteSelected(actor: User) {
        val ids = selectedIds.value
        val targets = uiState.value.products.filter { it.id in ids }
        if (targets.isEmpty()) return
        viewModelScope.launch {
            productRepository.bulkDeleteProducts(actor, targets).fold(
                onSuccess = { count ->
                    selectedIds.value = emptySet()
                    isSelectionMode.value = false
                    message.value = "تم حذف $count صنفاً بنجاح."
                },
                onFailure = { err ->
                    error.value = err.message ?: "تعذر الحذف الجماعي."
                }
            )
        }
    }

    fun handleExcelUri(uri: Uri) {
        viewModelScope.launch {
            isLoading.value = true
            productRepository.parseExcelFile(uri).fold(
                onSuccess = { parseResult ->
                    isLoading.value = false
                    if (parseResult.validRows.isEmpty() && parseResult.errors.isNotEmpty()) {
                        error.value = parseResult.errors.first()
                    } else if (parseResult.validRows.isEmpty()) {
                        error.value = "لم يتم العثور على أصناف صالحة في ملف Excel."
                    } else {
                        pendingExcelParse.value = parseResult
                    }
                },
                onFailure = { err ->
                    isLoading.value = false
                    error.value = err.message ?: "تعذر قراءة ملف Excel."
                }
            )
        }
    }

    fun confirmExcelImport(actor: User, policy: DuplicateImportPolicy) {
        val parse = pendingExcelParse.value ?: return
        viewModelScope.launch {
            isLoading.value = true
            productRepository.commitExcelImport(actor, parse, policy).fold(
                onSuccess = { summary ->
                    isLoading.value = false
                    pendingExcelParse.value = null
                    importSummary.value = summary
                },
                onFailure = { err ->
                    isLoading.value = false
                    error.value = err.message ?: "تعذر استيراد البيانات."
                }
            )
        }
    }

    fun dismissExcelParse() {
        pendingExcelParse.value = null
    }

    fun dismissImportSummary() {
        importSummary.value = null
    }

    fun clearBanners() {
        message.value = null
        error.value = null
    }

    private data class FilterParams(
        val query: String,
        val sort: ProductSortOrder,
        val selectionMode: Boolean,
        val selectedIds: Set<Long>
    )

    private data class StatusParams(
        val loading: Boolean,
        val parse: ExcelParseResult?,
        val summary: ImportSummary?,
        val message: String?,
        val error: String?
    )
}
