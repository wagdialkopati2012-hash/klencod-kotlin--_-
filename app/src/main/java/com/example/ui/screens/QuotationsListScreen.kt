package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CompanyInfo
import com.example.domain.model.QuotationWithItems
import com.example.domain.model.User
import com.example.domain.util.A4PdfGenerator
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateFormatter
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumTopBar
import com.example.ui.theme.LocalTenderingColors
import com.example.ui.viewmodel.QuotationSortOrder
import com.example.ui.viewmodel.QuotationsListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationsListScreen(
    currentUser: User,
    companyInfo: CompanyInfo,
    onCreateQuotation: () -> Unit,
    onEditQuotation: (Long) -> Unit,
    onPreviewQuotation: (Long) -> Unit,
    viewModel: QuotationsListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val perms = currentUser.effectivePermissions()
    val ext = LocalTenderingColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSortMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    fun generateTempPdf(item: QuotationWithItems, onReady: (File) -> Unit) {
        val primaryArgb = ext.heroStart.toArgb()
        val accentArgb = ext.accent.toArgb()
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                val outFile = File(context.cacheDir, "pdfs/quotation_${item.quotation.quotationNumber}.pdf")
                A4PdfGenerator.generateQuotationPdf(
                    context = context,
                    quotationWithItems = item,
                    company = companyInfo,
                    primaryColorArgb = primaryArgb,
                    accentColorArgb = accentArgb,
                    outputFile = outFile
                )
            }
            onReady(file)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PremiumTopBar(
                title = "عروض الأسعار",
                subtitle = "${state.quotations.size} عرض سعر محفوظ",
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "تصفية بالتاريخ",
                            tint = if (state.startDateMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "ترتيب")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            QuotationSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            order.arabicLabel,
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
                }
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchChange,
                    placeholder = { Text("بحث باسم العميل أو رقم عرض السعر...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quotations_search_input")
                )

                if (state.startDateMillis != null || state.endDateMillis != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.setDateRange(null, null) },
                            label = {
                                val s = state.startDateMillis?.let { DateFormatter.formatShortDate(it) } ?: "—"
                                val e = state.endDateMillis?.let { DateFormatter.formatShortDate(it) } ?: "—"
                                Text("الفترة: $s إلى $e")
                            },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "إزالة الفلتر", modifier = Modifier.size(16.dp))
                            }
                        )
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
                            verticalAlignment = Alignment.CenterVertically
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.quotations.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.RequestQuote,
                    title = "لا توجد عروض أسعار",
                    subtitle = "أنشئ عرض سعر جديداً وسيتم ترقيمه تلقائياً بدءاً من الرقم 1 مع حساب دقيق وتصدير PDF A4 احترافي.",
                    actionLabel = if (perms.canCreateQuotations) "إنشاء عرض سعر" else null,
                    onAction = if (perms.canCreateQuotations) onCreateQuotation else null
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.quotations, key = { it.quotation.id }) { item ->
                        val q = item.quotation
                        val canEdit = perms.canEditAllQuotations ||
                            (perms.canEditOwnQuotations && q.createdByUserId == currentUser.id)
                        val canDelete = perms.canDeleteAllQuotations ||
                            (perms.canDeleteOwnQuotations && q.createdByUserId == currentUser.id)

                        PremiumCard(
                            onClick = if (perms.canPreviewQuotations) {
                                { onPreviewQuotation(q.id) }
                            } else null,
                            modifier = Modifier.testTag("quotation_card_${q.quotationNumber}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "عرض #${q.quotationNumber}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        color = ext.accent.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = q.status.arabicLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.accent,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = CurrencyFormatter.formatUsd(q.totalAmountUsd),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = q.customerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "التاريخ: ${DateFormatter.formatShortDate(q.dateMillis)}  •  بواسطة: ${q.createdByUsername}  •  ${item.items.size} أصناف",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (perms.canPreviewQuotations) {
                                        IconButton(
                                            onClick = { onPreviewQuotation(q.id) },
                                            modifier = Modifier.testTag("preview_quotation_${q.quotationNumber}")
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = "معاينة", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    if (canEdit) {
                                        IconButton(
                                            onClick = { onEditQuotation(q.id) },
                                            modifier = Modifier.testTag("edit_quotation_${q.quotationNumber}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                    if (perms.canExportPdf) {
                                        IconButton(onClick = {
                                            generateTempPdf(item) { file ->
                                                Toast.makeText(context, "تم تجهيز PDF: ${file.name}", Toast.LENGTH_SHORT).show()
                                                onPreviewQuotation(q.id)
                                            }
                                        }) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = "تصدير PDF", tint = ext.accent)
                                        }
                                    }
                                    if (perms.canPrintQuotations) {
                                        IconButton(onClick = {
                                            generateTempPdf(item) { file ->
                                                A4PdfGenerator.printPdfFile(context, file, "عرض سعر #${q.quotationNumber}")
                                            }
                                        }) {
                                            Icon(Icons.Default.Print, contentDescription = "طباعة", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (perms.canShareQuotations) {
                                        IconButton(onClick = {
                                            generateTempPdf(item) { file ->
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/pdf"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    putExtra(Intent.EXTRA_SUBJECT, "عرض سعر #${q.quotationNumber} - ${q.customerName}")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة عرض السعر"))
                                            }
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                if (canDelete) {
                                    IconButton(
                                        onClick = { viewModel.requestDelete(q) },
                                        modifier = Modifier.testTag("delete_quotation_${q.quotationNumber}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "حذف عرض السعر",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (perms.canCreateQuotations) {
            ExtendedFloatingActionButton(
                onClick = onCreateQuotation,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("عرض سعر جديد") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, bottom = 96.dp)
                    .testTag("create_quotation_fab")
            )
        }
    }

    // Exact Required Permanent Deletion Confirmation Dialog
    state.pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = {
                Text(
                    text = "تأكيد حذف عرض السعر",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف عرض السعر رقم ${target.quotationNumber}؟\nسيتم حذف عرض السعر وجميع تفاصيله نهائياً، ولا يمكن التراجع عن هذه العملية."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmPermanentDelete(currentUser) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_permanent_delete_button")
                ) {
                    Text("حذف نهائياً")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissDelete,
                    modifier = Modifier.testTag("cancel_permanent_delete_button")
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showDatePicker) {
        val dateRangeState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = state.startDateMillis,
            initialSelectedEndDateMillis = state.endDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setDateRange(
                            dateRangeState.selectedStartDateMillis,
                            dateRangeState.selectedEndDateMillis
                        )
                        showDatePicker = false
                    }
                ) { Text("تطبيق") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
            }
        ) {
            DateRangePicker(state = dateRangeState, modifier = Modifier.weight(1f))
        }
    }
}
