package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.User
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateFormatter
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumTopBar
import com.example.ui.theme.LocalTenderingColors
import com.example.ui.viewmodel.ReportTab
import com.example.ui.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    currentUser: User,
    onBack: () -> Unit,
    onPreviewQuotation: (Long) -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    LaunchedEffect(currentUser.id) {
        viewModel.setActor(currentUser)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val perms = currentUser.effectivePermissions()
    val ext = LocalTenderingColors.current

    var showDatePicker by remember { mutableStateOf(false) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsvReport(currentUser, uri)
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        PremiumTopBar(
            title = "التقارير",
            subtitle = if (perms.canViewAllReports) "تقارير شاملة لجميع المستخدمين" else "تقاريري الشخصية",
            onBack = onBack,
            actions = {
                if (perms.canExportReports) {
                    IconButton(
                        onClick = {
                            exportCsvLauncher.launch("تقرير_عروض_الأسعار_${System.currentTimeMillis()}.csv")
                        },
                        modifier = Modifier.testTag("export_report_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "تصدير التقرير")
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "banners") {
                AnimatedVisibility(visible = state.message != null) {
                    Surface(
                        color = ext.success.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.message.orEmpty(),
                                color = ext.success,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::clearBanners, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = ext.success)
                            }
                        }
                    }
                }
            }

            // Report Tabs
            item(key = "tabs") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReportTab.entries, key = { it.name }) { tab ->
                        FilterChip(
                            selected = state.activeTab == tab,
                            onClick = { viewModel.onTabChange(tab) },
                            label = { Text(tab.arabicLabel) }
                        )
                    }
                }
            }

            // Filters Card
            item(key = "filters") {
                PremiumCard(contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تصفية التقرير",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = viewModel::clearFilters) {
                            Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مسح الفلاتر")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.customerFilter,
                            onValueChange = viewModel::onCustomerFilterChange,
                            label = { Text("العميل") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.quotationNumberFilter,
                            onValueChange = viewModel::onNumberFilterChange,
                            label = { Text("رقم العرض") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (perms.canViewAllReports) {
                            OutlinedTextField(
                                value = state.userFilter,
                                onValueChange = viewModel::onUserFilterChange,
                                label = { Text("المستخدم") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val dateLabel = if (state.startDateMillis != null) {
                                "${DateFormatter.formatShortDate(state.startDateMillis!!)} - ${
                                    state.endDateMillis?.let { DateFormatter.formatShortDate(it) } ?: "الآن"
                                }"
                            } else "تاريخ البداية والنهاية"
                            Text(dateLabel, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Financial Statistics Cards
            item(key = "stats_grid") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportMetricCard(
                            title = "إجمالي عروض الأسعار",
                            value = state.totalQuotations.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        if (perms.canViewFinancialSummaries) {
                            ReportMetricCard(
                                title = "إجمالي القيمة",
                                value = state.totalValueFormatted,
                                highlight = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (perms.canViewFinancialSummaries) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ReportMetricCard(
                                title = "متوسط قيمة العرض",
                                value = state.averageValueFormatted,
                                modifier = Modifier.weight(1f)
                            )
                            ReportMetricCard(
                                title = "أعلى عرض سعر",
                                value = state.highestValueFormatted,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Grouped Visual Bars & Breakdown
            if (state.groupedRows.isEmpty()) {
                item(key = "empty_reports") {
                    EmptyStateView(
                        icon = Icons.Default.Analytics,
                        title = "لا توجد بيانات للتقرير",
                        subtitle = "جرب تعديل الفلاتر أو إضافة عروض أسعار لعرض التحليلات المالية."
                    )
                }
            } else {
                item(key = "breakdown_header") {
                    Text(
                        text = "التحليل (${state.activeTab.arabicLabel})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(state.groupedRows, key = { it.label }) { row ->
                    val animatedRatio by animateFloatAsState(
                        targetValue = row.proportion,
                        animationSpec = spring(),
                        label = "barRatio"
                    )
                    PremiumCard(contentPadding = PaddingValues(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = row.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${row.count} عرض" + if (perms.canViewFinancialSummaries) "  •  ${row.totalFormatted}" else "",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedRatio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }

                // Matching quotations summary list
                item(key = "matching_list_header") {
                    Text(
                        text = "العروض المشمولة (${state.filteredQuotations.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.filteredQuotations.take(20), key = { "rep_q_${it.quotation.id}" }) { item ->
                    val q = item.quotation
                    PremiumCard(
                        onClick = if (perms.canPreviewQuotations) {
                            { onPreviewQuotation(q.id) }
                        } else null,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "#${q.quotationNumber} — ${q.customerName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${DateFormatter.formatShortDate(q.dateMillis)} • ${q.createdByUsername}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (perms.canViewFinancialSummaries) {
                                Text(
                                    text = CurrencyFormatter.formatUsd(q.totalAmountUsd),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ext.accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val rangeState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = state.startDateMillis,
            initialSelectedEndDateMillis = state.endDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setDateRange(rangeState.selectedStartDateMillis, rangeState.selectedEndDateMillis)
                        showDatePicker = false
                    }
                ) { Text("تطبيق") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
            }
        ) {
            DateRangePicker(state = rangeState, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    PremiumCard(
        modifier = modifier,
        containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
