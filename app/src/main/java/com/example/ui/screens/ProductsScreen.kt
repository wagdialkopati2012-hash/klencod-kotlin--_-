package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.DuplicateImportPolicy
import com.example.domain.model.Product
import com.example.domain.model.User
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateFormatter
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumTopBar
import com.example.ui.theme.LocalTenderingColors
import com.example.ui.viewmodel.ProductSortOrder
import com.example.ui.viewmodel.ProductsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductsScreen(
    currentUser: User,
    openAddImmediately: Boolean = false,
    onConsumeOpenAdd: () -> Unit = {},
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val perms = currentUser.effectivePermissions()
    val ext = LocalTenderingColors.current

    var showAddEditDialog by remember { mutableStateOf(openAddImmediately && perms.canAddProducts) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var pendingDeleteProduct by remember { mutableStateOf<Product?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    if (openAddImmediately && perms.canAddProducts) {
        onConsumeOpenAdd()
    }

    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.handleExcelUri(uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PremiumTopBar(
                title = "الأصناف",
                subtitle = "${state.products.size} صنف مسجل",
                actions = {
                    if (state.isSelectionMode) {
                        IconButton(onClick = viewModel::selectAllVisible) {
                            Icon(Icons.Default.SelectAll, contentDescription = "تحديد الكل")
                        }
                        if (perms.canBulkDeleteProducts && state.selectedIds.isNotEmpty()) {
                            IconButton(
                                onClick = { showBulkDeleteConfirm = true },
                                modifier = Modifier.testTag("bulk_delete_products_button")
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = "حذف المحدد",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.toggleSelectionMode(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد")
                        }
                    } else {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "ترتيب")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                ProductSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.arabicLabel,
                                                fontWeight = if (state.sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.onSortOrderChange(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        if (perms.canBulkDeleteProducts && state.products.isNotEmpty()) {
                            IconButton(onClick = { viewModel.toggleSelectionMode(true) }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "تحديد متعدد")
                            }
                        }
                    }
                }
            )

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Search & Excel Import Header
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchChange,
                    placeholder = { Text("البحث عن صنف بالاسم أو الرمز...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("products_search_input")
                )

                if (perms.canImportExcel) {
                    Button(
                        onClick = {
                            excelLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/octet-stream"
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.accent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("import_excel_button")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استيراد Excel (.xlsx)", fontWeight = FontWeight.Bold)
                    }
                }

                AnimatedVisibility(visible = state.message != null) {
                    Surface(
                        color = ext.success.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.message.orEmpty(),
                                color = ext.success,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::clearBanners, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = ext.success, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = state.error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::clearBanners, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.products.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    title = "لا توجد أصناف مطابقة",
                    subtitle = "أضف أصنافاً يدوياً أو استورد قائمة الأصناف من ملف Excel (.xlsx) يحتوي على الاسم والسعر بالدولار.",
                    actionLabel = if (perms.canAddProducts) "إضافة صنف جديد" else null,
                    onAction = if (perms.canAddProducts) {
                        {
                            editingProduct = null
                            showAddEditDialog = true
                        }
                    } else null
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.products, key = { it.id }) { product ->
                        val isSelected = product.id in state.selectedIds
                        var itemMenuExpanded by remember { mutableStateOf(false) }

                        PremiumCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (state.isSelectionMode) {
                                            viewModel.toggleSelectProduct(product.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (perms.canBulkDeleteProducts) {
                                            viewModel.toggleSelectProduct(product.id)
                                        }
                                    }
                                )
                                .testTag("product_card_${product.id}"),
                            contentPadding = PaddingValues(14.dp),
                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary else ext.glassBorder
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.isSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleSelectProduct(product.id) }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = product.formattedCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "• ${DateFormatter.formatShortDate(product.createdAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = CurrencyFormatter.formatUsd(product.priceUsd),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ext.accent,
                                    fontWeight = FontWeight.Bold
                                )

                                if ((perms.canEditProducts || perms.canDeleteProducts) && !state.isSelectionMode) {
                                    Box {
                                        IconButton(onClick = { itemMenuExpanded = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "خيارات الصنف")
                                        }
                                        DropdownMenu(
                                            expanded = itemMenuExpanded,
                                            onDismissRequest = { itemMenuExpanded = false }
                                        ) {
                                            if (perms.canEditProducts) {
                                                DropdownMenuItem(
                                                    text = { Text("تعديل") },
                                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                                    onClick = {
                                                        itemMenuExpanded = false
                                                        editingProduct = product
                                                        showAddEditDialog = true
                                                    }
                                                )
                                            }
                                            if (perms.canDeleteProducts) {
                                                DropdownMenuItem(
                                                    text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    },
                                                    onClick = {
                                                        itemMenuExpanded = false
                                                        pendingDeleteProduct = product
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (perms.canAddProducts && !state.isSelectionMode) {
            ExtendedFloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة صنف") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, bottom = 96.dp)
                    .testTag("add_product_fab")
            )
        }
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            existing = editingProduct,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, price ->
                val target = editingProduct
                if (target == null) {
                    viewModel.addProduct(currentUser, name, price) {
                        showAddEditDialog = false
                    }
                } else {
                    viewModel.updateProduct(currentUser, target, name, price) {
                        showAddEditDialog = false
                    }
                }
            }
        )
    }

    // Single Delete Confirmation
    pendingDeleteProduct?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteProduct = null },
            title = { Text("تأكيد حذف الصنف") },
            text = { Text("هل أنت متأكد من حذف الصنف «${target.name}»؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(currentUser, target)
                        pendingDeleteProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteProduct = null }) { Text("إلغاء") }
            }
        )
    }

    // Bulk Delete Confirmation
    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("تأكيد الحذف الجماعي") },
            text = { Text("هل أنت متأكد من حذف ${state.selectedIds.size} صنفاً محدداً نهائياً؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showBulkDeleteConfirm = false
                        viewModel.bulkDeleteSelected(currentUser)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف المحدد") }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    // Excel Duplicate Handling Confirmation Dialog
    state.pendingExcelParse?.let { parse ->
        var policy by remember { mutableStateOf(DuplicateImportPolicy.SKIP) }
        AlertDialog(
            onDismissRequest = viewModel::dismissExcelParse,
            title = { Text("معاينة استيراد Excel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تم تحليل ${parse.validRows.size} صنفاً صالحاً من الملف.")
                    if (parse.errors.isNotEmpty()) {
                        Text(
                            text = "تنبيه: توجد ${parse.errors.size} صفوف غير صالحة سيتم تجاهلها.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (parse.duplicateNames.isNotEmpty()) {
                        Text(
                            text = "تم اكتشاف ${parse.duplicateNames.size} أصناف مكررة بالاسم. اختر إجراءً للمكرر:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DuplicateImportPolicy.entries.forEach { option ->
                                OutlinedButton(
                                    onClick = { policy = option },
                                    colors = if (policy == option) {
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    } else ButtonDefaults.outlinedButtonColors(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(option.arabicLabel, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmExcelImport(currentUser, policy) },
                    modifier = Modifier.testTag("confirm_excel_import_button")
                ) {
                    Text("بدء الاستيراد")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExcelParse) { Text("إلغاء") }
            }
        )
    }

    // Excel Import Summary Dialog
    state.importSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImportSummary,
            title = { Text("ملخص استيراد Excel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• أصناف جديدة مضافة: ${summary.insertedCount}")
                    Text("• أصناف تم تحديث سعرها: ${summary.updatedCount}")
                    Text("• أصناف مكررة تم تخطيها: ${summary.skippedCount}")
                    Text("• صفوف بأخطاء: ${summary.errorCount}")
                    if (summary.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        summary.errors.take(4).forEach { err ->
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::dismissImportSummary) { Text("تم") }
            }
        )
    }
}

@Composable
private fun AddEditProductDialog(
    existing: Product?,
    onDismiss: () -> Unit,
    onSave: (name: String, priceText: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var price by remember { mutableStateOf(existing?.priceUsd.orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "إضافة صنف جديد" else "تعديل الصنف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; validationError = null },
                    label = { Text("اسم الصنف") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it; validationError = null },
                    label = { Text("السعر بالدولار (USD)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_price_input")
                )
                if (validationError != null) {
                    Text(
                        text = validationError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val parsed = CurrencyFormatter.parsePositiveBigDecimal(price)
                    when {
                        cleanName.isEmpty() -> validationError = "يرجى إدخال اسم الصنف."
                        parsed == null -> validationError = "يرجى إدخال سعر صحيح أكبر من صفر."
                        else -> onSave(cleanName, price)
                    }
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
