package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ProductRepository
import com.example.domain.model.Product
import com.example.domain.util.CurrencyFormatter
import com.example.ui.navigation.SelectedProductPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import javax.inject.Inject

@Immutable
data class SelectableProductRow(
    val product: Product,
    val quantityInput: String = "",
    val existingQuotationQty: BigDecimal? = null,
    val subtotal: BigDecimal? = null,
    val quantityError: String? = null,
    val isSelected: Boolean = false
)

@Immutable
data class SelectedProductSummaryItem(
    val productId: Long,
    val productName: String,
    val productCode: String,
    val unitPriceUsd: String,
    val unitPriceDecimal: BigDecimal,
    val quantityInput: String,
    val quantityDecimal: BigDecimal,
    val subtotalDecimal: BigDecimal
)

@Immutable
data class ProductSelectionUiState(
    val showSelectedOnly: Boolean = false,
    val rows: List<SelectableProductRow> = emptyList(),
    val selectedItems: List<SelectedProductSummaryItem> = emptyList(),
    val selectedCount: Int = 0,
    val totalQuantity: BigDecimal = BigDecimal.ZERO,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val totalCatalogCount: Int = 0,
    val isLoading: Boolean = true
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ProductSelectionViewModel @Inject constructor(
    productRepository: ProductRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showSelectedOnly = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _existingQuantities = MutableStateFlow<Map<Long, BigDecimal>>(emptyMap())

    // Restores automatically across configuration changes / process recreation via SavedStateHandle
    private val _enteredQuantities = MutableStateFlow<Map<Long, String>>(
        restoreQuantitiesFromSavedState(savedStateHandle.get<String>(KEY_SAVED_QUANTITIES))
    )

    private var lastLoadedExistingJson: String? = null

    private val debouncedQueryFlow = _searchQuery
        .debounce { query -> if (query.isBlank()) 0L else 60L }
        .distinctUntilChanged()

    val uiState: StateFlow<ProductSelectionUiState> = combine(
        productRepository.allProducts,
        debouncedQueryFlow,
        _enteredQuantities,
        _showSelectedOnly,
        _existingQuantities
    ) { products, query, quantitiesMap, selectedOnly, existingMap ->
        computeSelectionState(
            products = products,
            query = query,
            quantitiesMap = quantitiesMap,
            showSelectedOnly = selectedOnly,
            existingMap = existingMap
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProductSelectionUiState()
        )

    fun setExistingQuantitiesJson(json: String) {
        if (lastLoadedExistingJson == json) return
        lastLoadedExistingJson = json
        if (json.isBlank()) {
            _existingQuantities.value = emptyMap()
            return
        }
        val parsed = runCatching {
            val rawMap = Json.decodeFromString<Map<Long, String>>(json)
            rawMap.mapNotNull { (id, qtyStr) ->
                CurrencyFormatter.parsePositiveBigDecimal(qtyStr)?.let { id to it }
            }.toMap()
        }.getOrDefault(emptyMap())
        _existingQuantities.value = parsed
    }

    fun onSearchQueryChange(value: String) {
        _searchQuery.value = value
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun toggleShowSelectedOnly(selectedOnly: Boolean) {
        _showSelectedOnly.value = selectedOnly
    }

    fun onQuantityChange(productId: Long, rawInput: String) {
        val sanitized = sanitizeQuantityInput(rawInput)
        _enteredQuantities.update { current ->
            val updated = if (sanitized.isEmpty()) {
                current - productId
            } else {
                current + (productId to sanitized)
            }
            persistQuantitiesToSavedState(updated)
            updated
        }
        _errorMessage.value = null
    }

    fun clearProductQuantity(productId: Long) {
        _enteredQuantities.update { current ->
            val updated = current - productId
            persistQuantitiesToSavedState(updated)
            updated
        }
    }

    fun clearAllSelections() {
        _enteredQuantities.value = emptyMap()
        persistQuantitiesToSavedState(emptyMap())
        _showSelectedOnly.value = false
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Validates entered quantities, ignores empty and <= 0 rows,
     * and returns all selected products with quantities > 0.
     */
    fun confirmSelection(): List<SelectedProductPayload>? {
        val selected = uiState.value.selectedItems
        if (selected.isEmpty()) {
            _errorMessage.value = "يرجى إدخال كمية صالحة أكبر من صفر لصنف واحد على الأقل."
            return null
        }
        return selected.map { item ->
            SelectedProductPayload(
                productId = item.productId,
                productName = item.productName,
                productCode = item.productCode,
                unitPriceUsd = item.unitPriceUsd,
                quantity = item.quantityDecimal.stripTrailingZeros().toPlainString()
            )
        }
    }

    private fun persistQuantitiesToSavedState(map: Map<Long, String>) {
        val encoded = runCatching { Json.encodeToString(map) }.getOrNull()
        savedStateHandle[KEY_SAVED_QUANTITIES] = encoded
    }

    companion object {
        private const val KEY_SAVED_QUANTITIES = "saved_product_selection_quantities"

        private fun restoreQuantitiesFromSavedState(encoded: String?): Map<Long, String> {
            if (encoded.isNullOrBlank()) return emptyMap()
            return runCatching { Json.decodeFromString<Map<Long, String>>(encoded) }
                .getOrDefault(emptyMap())
        }

        /**
         * Normalizes Arabic/Persian numerals, strips negative signs and invalid characters,
         * and allows only non-negative decimal numeric strings (at most one decimal point).
         */
        fun sanitizeQuantityInput(raw: String): String {
            val normalized = CurrencyFormatter.normalizeDigits(raw)
            val sb = StringBuilder(normalized.length)
            var hasDot = false
            for (ch in normalized) {
                when {
                    ch in '0'..'9' -> sb.append(ch)
                    ch == '.' && !hasDot -> {
                        sb.append('.')
                        hasDot = true
                    }
                }
            }
            return sb.toString()
        }

        /**
         * Pure function to compute filtered product rows, individual subtotals,
         * and full cross-search selection summary.
         */
        fun computeSelectionState(
            products: List<Product>,
            query: String,
            quantitiesMap: Map<Long, String>,
            showSelectedOnly: Boolean,
            existingMap: Map<Long, BigDecimal>
        ): ProductSelectionUiState {
            val selectedItems = ArrayList<SelectedProductSummaryItem>()
            var totalQuantity = BigDecimal.ZERO
            var totalAmount = BigDecimal.ZERO

            val cleanQuery = CurrencyFormatter.normalizeDigits(query).trim().lowercase()
            val filteredRows = ArrayList<SelectableProductRow>(products.size)

            for (product in products) {
                val qtyStr = quantitiesMap[product.id].orEmpty()
                val parsedQty = CurrencyFormatter.parseBigDecimal(qtyStr)
                val isPositive = parsedQty != null && parsedQty > BigDecimal.ZERO
                val isZeroEntered = qtyStr.isNotEmpty() && parsedQty != null && parsedQty.compareTo(BigDecimal.ZERO) == 0

                val subtotal = if (isPositive) {
                    CurrencyFormatter.calculateLineTotal(product.priceDecimal, parsedQty!!)
                } else {
                    null
                }

                val qtyError = when {
                    isZeroEntered -> "الكمية يجب أن تكون أكبر من 0"
                    qtyStr.isNotEmpty() && qtyStr != "." && parsedQty == null -> "قيمة غير صالحة"
                    else -> null
                }

                if (isPositive && subtotal != null) {
                    selectedItems.add(
                        SelectedProductSummaryItem(
                            productId = product.id,
                            productName = product.name,
                            productCode = product.formattedCode,
                            unitPriceUsd = product.priceUsd,
                            unitPriceDecimal = product.priceDecimal,
                            quantityInput = qtyStr,
                            quantityDecimal = parsedQty!!,
                            subtotalDecimal = subtotal
                        )
                    )
                    totalQuantity = totalQuantity.add(parsedQty)
                    totalAmount = totalAmount.add(subtotal)
                }

                val matchesFilter = if (showSelectedOnly) {
                    isPositive
                } else if (cleanQuery.isEmpty()) {
                    true
                } else {
                    product.name.lowercase().contains(cleanQuery) ||
                        product.formattedCode.lowercase().contains(cleanQuery) ||
                        product.id.toString() == cleanQuery
                }

                if (matchesFilter) {
                    filteredRows.add(
                        SelectableProductRow(
                            product = product,
                            quantityInput = qtyStr,
                            existingQuotationQty = existingMap[product.id],
                            subtotal = subtotal,
                            quantityError = qtyError,
                            isSelected = isPositive
                        )
                    )
                }
            }

            return ProductSelectionUiState(
                showSelectedOnly = showSelectedOnly,
                rows = filteredRows,
                selectedItems = selectedItems,
                selectedCount = selectedItems.size,
                totalQuantity = totalQuantity,
                totalAmount = totalAmount,
                totalCatalogCount = products.size,
                isLoading = false
            )
        }
    }
}
