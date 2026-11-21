package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.util.CurrencyFormatter
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PremiumTopBar
import com.example.ui.navigation.SelectedProductPayload
import com.example.ui.theme.LocalTenderingColors
import com.example.ui.viewmodel.ProductSelectionUiState
import com.example.ui.viewmodel.ProductSelectionViewModel
import com.example.ui.viewmodel.SelectableProductRow
import com.example.ui.viewmodel.SelectedProductSummaryItem
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSelectionScreen(
    existingQuantitiesJson: String,
    onBack: () -> Unit,
    onConfirmSelection: (List<SelectedProductPayload>) -> Unit,
    viewModel: ProductSelectionViewModel = hiltViewModel()
) {
    LaunchedEffect(existingQuantitiesJson) {
        viewModel.setExistingQuantitiesJson(existingQuantitiesJson)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    var showReviewSheet by remember { mutableStateOf(false) }

    BackHandler {
        keyboardController?.hide()
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
    ) {
        // 1. Header
        PremiumTopBar(
            title = "اختيار الأصناف",
            subtitle = if (state.totalCatalogCount > 0) {
                "${state.totalCatalogCount} صنف متاح • أدخل الكميات مباشرة"
            } else {
                "إضافة عدة أصناف لعرض السعر دفعة واحدة"
            },
            onBack = {
                keyboardController?.hide()
                onBack()
            },
            actions = {
                AnimatedVisibility(visible = state.selectedCount > 0) {
                    TextButton(
                        onClick = { viewModel.clearAllSelections() },
                        modifier = Modifier.testTag("clear_all_selected_products_button")
                    ) {
                        Text(
                            text = "مسح المحدد",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        )

        // 2. Search Bar & Quick Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text("ابحث باسم الصنف أو الكود...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = viewModel::clearSearch,
                            modifier = Modifier.testTag("clear_product_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح البحث"
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_selection_search_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !state.showSelectedOnly,
                        onClick = { viewModel.toggleShowSelectedOnly(false) },
                        label = {
                            Text("جميع الأصناف (${state.totalCatalogCount})")
                        },
                        modifier = Modifier.testTag("filter_all_products_chip")
                    )
                    FilterChip(
                        selected = state.showSelectedOnly,
                        onClick = { viewModel.toggleShowSelectedOnly(!state.showSelectedOnly) },
                        label = {
                            Text("المحددة (${state.selectedCount})")
                        },
                        leadingIcon = if (state.selectedCount > 0) {
                            {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("filter_selected_products_chip")
                    )
                }

                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    modifier = Modifier.testTag("dismiss_keyboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardHide,
                        contentDescription = "إخفاء لوحة المفاتيح",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Scrollable Product List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (!state.isLoading && state.rows.isEmpty()) {
                val isFilteredBySelected = state.showSelectedOnly
                val hasSearch = searchQuery.isNotBlank()
                EmptyStateView(
                    icon = if (hasSearch) Icons.Default.Search else Icons.Default.Inventory2,
                    title = when {
                        isFilteredBySelected -> "لا توجد أصناف محددة بعد"
                        hasSearch -> "لم يتم العثور على أصناف"
                        else -> "قائمة الأصناف فارغة"
                    },
                    subtitle = when {
                        isFilteredBySelected -> "أدخل الكمية بجانب أي صنف لإضافته تلقائياً إلى القائمة المحددة."
                        hasSearch -> "جرّب البحث باسم مختلف أو بكود الصنف (مثل P001)."
                        else -> "أضف أصنافاً من قسم الأصناف أولاً لتتمكن من إضافتها لعروض الأسعار."
                    },
                    actionLabel = when {
                        isFilteredBySelected -> "عرض جميع الأصناف"
                        hasSearch -> "مسح البحث"
                        else -> null
                    },
                    onAction = when {
                        isFilteredBySelected -> { { viewModel.toggleShowSelectedOnly(false) } }
                        hasSearch -> { { viewModel.clearSearch() } }
                        else -> null
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("product_selection_empty_state")
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                        .align(Alignment.TopCenter)
                        .testTag("product_selection_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.rows,
                        key = { it.product.id },
                        contentType = { "selectable_product_card" }
                    ) { row ->
                        ProductSelectionCard(
                            row = row,
                            onQuantityChange = { value ->
                                viewModel.onQuantityChange(row.product.id, value)
                            },
                            onClearQuantity = {
                                viewModel.clearProductQuantity(row.product.id)
                            },
                            onMoveNext = {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        )
                    }
                }
            }
        }

        // 4. Fixed Bottom Selection Summary Bar (Always visible above keyboard & nav bar)
        FixedSelectionSummaryBar(
            state = state,
            errorMessage = errorMessage,
            onReviewSelected = {
                focusManager.clearFocus()
                keyboardController?.hide()
                showReviewSheet = true
            },
            onConfirm = {
                focusManager.clearFocus()
                keyboardController?.hide()
                val payload = viewModel.confirmSelection()
                if (payload != null) {
                    onConfirmSelection(payload)
                }
            }
        )
    }

    // Optional Review Selected Products Bottom Sheet
    if (showReviewSheet) {
        ReviewSelectedProductsBottomSheet(
            selectedItems = state.selectedItems,
            totalAmount = state.totalAmount,
            onDismiss = { showReviewSheet = false },
            onQuantityChange = viewModel::onQuantityChange,
            onRemoveItem = viewModel::clearProductQuantity,
            onConfirm = {
                showReviewSheet = false
                val payload = viewModel.confirmSelection()
                if (payload != null) {
                    onConfirmSelection(payload)
                }
            }
        )
    }
}

@Composable
private fun ProductSelectionCard(
    row: SelectableProductRow,
    onQuantityChange: (String) -> Unit,
    onClearQuantity: () -> Unit,
    onMoveNext: () -> Unit
) {
    val ext = LocalTenderingColors.current

    val containerColor by animateColorAsState(
        targetValue = if (row.isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (ext.isDark) 0.28f else 0.36f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "productCardBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            row.quantityError != null -> MaterialTheme.colorScheme.error
            row.isSelected -> MaterialTheme.colorScheme.primary
            else -> ext.glassBorder
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "productCardBorder"
    )

    val elevation by animateDpAsState(
        targetValue = if (row.isSelected) 4.dp else 1.5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "productCardElevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selectable_product_card_${row.product.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(if (row.isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // RTL Row: Start (Right) = Product Details, End (Left) = Quantity Input Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (row.isSelected) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Outlined.RadioButtonUnchecked
                            },
                            contentDescription = if (row.isSelected) "صنف محدد" else "غير محدد",
                            tint = if (row.isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = row.product.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = row.product.formattedCode,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "السعر: ${CurrencyFormatter.formatUsd(row.product.priceUsd)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (row.existingQuotationQty != null) {
                        Surface(
                            color = ext.accent.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "موجود في العرض: ${CurrencyFormatter.formatQuantity(row.existingQuotationQty)} (سيُضاف عليها)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.accent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // End (Left in RTL): Large, mobile-friendly empty-by-default Quantity Field
                OutlinedTextField(
                    value = row.quantityInput,
                    onValueChange = onQuantityChange,
                    label = { Text("الكمية") },
                    placeholder = {
                        Text(
                            text = "أدخل",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    isError = row.quantityError != null,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    trailingIcon = if (row.quantityInput.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = onClearQuantity,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("clear_qty_${row.product.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح الكمية",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { onMoveNext() }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .width(128.dp)
                        .testTag("select_qty_input_${row.product.id}")
                )
            }

            // Live Subtotal / Inline Validation Row
            AnimatedVisibility(
                visible = row.subtotal != null || row.quantityError != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (row.quantityError != null) {
                    Text(
                        text = row.quantityError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else if (row.subtotal != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الإجمالي الفرعي",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatUsd(row.subtotal),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("product_subtotal_${row.product.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FixedSelectionSummaryBar(
    state: ProductSelectionUiState,
    errorMessage: String?,
    onReviewSelected: () -> Unit,
    onConfirm: () -> Unit
) {
    val ext = LocalTenderingColors.current
    val hasSelection = state.selectedCount > 0

    Surface(
        shadowElevation = 14.dp,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.glassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selection_summary_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selected Count + Review Pill
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedContent(
                            targetState = state.selectedCount,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "selectedCountAnim"
                        ) { count ->
                            Text(
                                text = if (count == 0) {
                                    "0 أصناف محددة"
                                } else {
                                    "الأصناف المحددة: $count"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("summary_selected_count_text")
                            )
                        }

                        AnimatedVisibility(visible = hasSelection) {
                            Surface(
                                onClick = onReviewSelected,
                                shape = RoundedCornerShape(10.dp),
                                color = ext.accent.copy(alpha = 0.14f),
                                modifier = Modifier.testTag("review_selected_products_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ListAlt,
                                        contentDescription = null,
                                        tint = ext.accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "مراجعة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.accent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (hasSelection) {
                        Text(
                            text = "إجمالي الكمية: ${CurrencyFormatter.formatQuantity(state.totalQuantity)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Calculated Total Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الإجمالي",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedContent(
                        targetState = if (hasSelection) {
                            CurrencyFormatter.formatUsd(state.totalAmount)
                        } else {
                            "$0.00"
                        },
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "summaryTotalAnim"
                    ) { formattedTotal ->
                        Text(
                            text = formattedTotal,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (hasSelection) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.testTag("summary_total_amount_text")
                        )
                    }
                }
            }

            // Confirm & Add Products Button
            val buttonColor by animateColorAsState(
                targetValue = if (hasSelection) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "confirmButtonColor"
            )

            Button(
                onClick = onConfirm,
                enabled = hasSelection,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("confirm_add_products_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddTask,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasSelection) {
                        "تأكيد وإضافة الأصناف (${state.selectedCount})"
                    } else {
                        "تأكيد وإضافة الأصناف"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSelectedProductsBottomSheet(
    selectedItems: List<SelectedProductSummaryItem>,
    totalAmount: BigDecimal,
    onDismiss: () -> Unit,
    onQuantityChange: (Long, String) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مراجعة الأصناف المحددة (${selectedItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = CurrencyFormatter.formatUsd(totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            if (selectedItems.isEmpty()) {
                Text(
                    text = "لم يتم تحديد أي أصناف بعد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedItems, key = { it.productId }) { item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.productCode} • ${CurrencyFormatter.formatUsd(item.unitPriceUsd)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "الإجمالي الفرعي: ${CurrencyFormatter.formatUsd(item.subtotalDecimal)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedTextField(
                                    value = item.quantityInput,
                                    onValueChange = { onQuantityChange(item.productId, it) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(92.dp)
                                )

                                IconButton(onClick = { onRemoveItem(item.productId) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "إزالة الصنف",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onConfirm,
                enabled = selectedItems.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 50.dp)
            ) {
                Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تأكيد وإضافة إلى عرض السعر", fontWeight = FontWeight.Bold)
            }
        }
    }
}
