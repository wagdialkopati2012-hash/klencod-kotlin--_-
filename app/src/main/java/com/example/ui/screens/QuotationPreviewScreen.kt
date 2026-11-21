package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.domain.model.CompanyInfo
import com.example.domain.model.QuotationItem
import com.example.domain.model.QuotationWithItems
import com.example.domain.model.User
import com.example.domain.util.A4PdfGenerator
import com.example.domain.util.ArabicNumberToWords
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateFormatter
import com.example.ui.components.PremiumTopBar
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.LocalTenderingColors
import com.example.ui.theme.blendWith
import com.example.ui.viewmodel.QuotationPreviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal

@Composable
fun QuotationPreviewScreen(
    quotationId: Long,
    currentUser: User,
    companyInfo: CompanyInfo,
    onBack: () -> Unit,
    viewModel: QuotationPreviewViewModel = hiltViewModel()
) {
    LaunchedEffect(quotationId) {
        viewModel.load(quotationId)
    }

    val quotationWithItems by viewModel.quotation.collectAsStateWithLifecycle()
    val perms = currentUser.effectivePermissions()
    val ext = LocalTenderingColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var zoomScale by remember { mutableFloatStateOf(1f) }

    BackHandler { onBack() }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val data = quotationWithItems ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val primaryArgb = ext.heroStart.toArgb()
            val accentArgb = ext.accent.toArgb()
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val tempFile = File(context.cacheDir, "pdfs/export_q_${data.quotation.quotationNumber}.pdf")
                        A4PdfGenerator.generateQuotationPdf(
                            context = context,
                            quotationWithItems = data,
                            company = companyInfo,
                            primaryColorArgb = primaryArgb,
                            accentColorArgb = accentArgb,
                            outputFile = tempFile
                        )
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempFile.inputStream().use { it.copyTo(out) }
                        } ?: error("تعذر الحفظ إلى الوجهة المحددة.")
                    }
                }
                result.fold(
                    onSuccess = {
                        Toast.makeText(context, "تم حفظ ملف PDF بنجاح.", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(context, err.message ?: "تعذر حفظ الملف.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    fun buildPdfFile(data: QuotationWithItems, onReady: (File) -> Unit) {
        val primaryArgb = ext.heroStart.toArgb()
        val accentArgb = ext.accent.toArgb()
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                val outFile = File(context.cacheDir, "pdfs/quotation_${data.quotation.quotationNumber}.pdf")
                A4PdfGenerator.generateQuotationPdf(
                    context = context,
                    quotationWithItems = data,
                    company = companyInfo,
                    primaryColorArgb = primaryArgb,
                    accentColorArgb = accentArgb,
                    outputFile = outFile
                )
            }
            onReady(file)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        PremiumTopBar(
            title = "معاينة عرض السعر",
            subtitle = quotationWithItems?.let { "نموذج A4 — عرض رقم #${it.quotation.quotationNumber}" },
            onBack = onBack,
            actions = {
                IconButton(onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.75f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "تصغير")
                }
                IconButton(onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(2.2f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "تكبير")
                }
            }
        )

        val data = quotationWithItems
        if (data == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Split items into true A4 pages (12 items per page 1, 18 items per continuation page)
            val pages = remember(data.items) { paginateItemsForA4(data.items) }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.75f, 2.2f)
                        }
                    }
            ) {
                val basePaperWidth = (maxWidth - 24.dp).coerceAtMost(560.dp)
                val scaledWidth = basePaperWidth * zoomScale

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    pages.forEachIndexed { pageIndex, pageItems ->
                        A4PaperSheet(
                            data = data,
                            companyInfo = companyInfo,
                            pageItems = pageItems,
                            pageNumber = pageIndex + 1,
                            totalPages = pages.size,
                            isFirstPage = pageIndex == 0,
                            isLastPage = pageIndex == pages.lastIndex,
                            primaryColor = ext.heroStart,
                            accentColor = ext.accent,
                            modifier = Modifier
                                .width(scaledWidth)
                                .aspectRatio(595f / 842f)
                                .graphicsLayer()
                        )
                    }
                }
            }

            // Bottom PDF / Print / Share Action Bar
            Surface(
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (perms.canExportPdf) {
                        Button(
                            onClick = {
                                buildPdfFile(data) { file ->
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching { context.startActivity(viewIntent) }
                                        .onFailure {
                                            Toast.makeText(context, "تم إنشاء ملف PDF في ${file.name}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preview_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("معاينة PDF", fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                savePdfLauncher.launch("عرض_سعر_${data.quotation.quotationNumber}.pdf")
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_pdf_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ PDF", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    if (perms.canShareQuotations) {
                        OutlinedButton(
                            onClick = {
                                buildPdfFile(data) { file ->
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "عرض سعر #${data.quotation.quotationNumber}")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة عرض السعر"))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_pdf_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    if (perms.canPrintQuotations) {
                        OutlinedButton(
                            onClick = {
                                buildPdfFile(data) { file ->
                                    A4PdfGenerator.printPdfFile(context, file, "عرض سعر #${data.quotation.quotationNumber}")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.accent),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("print_pdf_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun A4PaperSheet(
    data: QuotationWithItems,
    companyInfo: CompanyInfo,
    pageItems: List<QuotationItem>,
    pageNumber: Int,
    totalPages: Int,
    isFirstPage: Boolean,
    isLastPage: Boolean,
    primaryColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val q = data.quotation
    val paperTextPrimary = Color(0xFF0F172A)
    val paperTextSecondary = Color(0xFF475569)
    val borderColor = Color(0xFFCBD5E1)
    val softTint = primaryColor.blendWith(Color.White, 0.92f)

    Surface(
        modifier = modifier.shadow(14.dp, RoundedCornerShape(4.dp)),
        shape = RoundedCornerShape(4.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Top Accent Stripe
            Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                Box(modifier = Modifier.weight(0.28f).height(4.dp).background(accentColor))
                Box(modifier = Modifier.weight(0.72f).height(4.dp).background(primaryColor))
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (isFirstPage) {
                // Company Letterhead
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = companyInfo.name.ifBlank { "عرض سعر" },
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryColor
                        )
                        val infoLine = buildList {
                            if (companyInfo.phone.isNotBlank()) add("هاتف: ${companyInfo.phone}")
                            if (companyInfo.address.isNotBlank()) add(companyInfo.address)
                        }.joinToString(" | ")
                        if (infoLine.isNotBlank()) {
                            Text(
                                text = infoLine,
                                fontFamily = CairoFontFamily,
                                fontSize = 8.5.sp,
                                color = paperTextSecondary
                            )
                        }
                        if (companyInfo.additionalInfo.isNotBlank()) {
                            Text(
                                text = companyInfo.additionalInfo,
                                fontFamily = CairoFontFamily,
                                fontSize = 8.sp,
                                color = paperTextSecondary
                            )
                        }
                    }

                    val logoFile = companyInfo.logoPath?.let { File(it) }?.takeIf { it.exists() }
                    if (logoFile != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Image(
                            painter = rememberAsyncImagePainter(logoFile),
                            contentDescription = "شعار الشركة",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title Box: عرض سعر
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(softTint, RoundedCornerShape(4.dp))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "عرض سعر",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata Row (Quotation Number, Date, Customer Name)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(0.7.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اسم العميل: ${q.customerName}",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            color = paperTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "رقم عرض السعر: #${q.quotationNumber}",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                color = primaryColor
                            )
                            Text(
                                text = "التاريخ: ${DateFormatter.formatShortDate(q.dateMillis)}",
                                fontFamily = CairoFontFamily,
                                fontSize = 8.5.sp,
                                color = paperTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // RTL Products Table: اسم الصنف | الكمية | السعر | الإجمالي
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.7.dp, borderColor, RoundedCornerShape(4.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryColor)
                        .padding(vertical = 5.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اسم الصنف",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        color = Color.White,
                        modifier = Modifier.weight(0.46f)
                    )
                    Text(
                        text = "الكمية",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.14f)
                    )
                    Text(
                        text = "السعر",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.19f)
                    )
                    Text(
                        text = "الإجمالي",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.21f)
                    )
                }

                pageItems.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (idx % 2 == 1) Color(0xFFF8FAFC) else Color.White)
                            .padding(vertical = 5.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.productName,
                            fontFamily = CairoFontFamily,
                            fontSize = 8.2.sp,
                            color = paperTextPrimary,
                            modifier = Modifier.weight(0.46f)
                        )
                        Text(
                            text = CurrencyFormatter.formatQuantity(item.quantity),
                            fontFamily = CairoFontFamily,
                            fontSize = 8.2.sp,
                            color = paperTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.14f)
                        )
                        Text(
                            text = CurrencyFormatter.formatUsd(item.unitPriceUsd),
                            fontFamily = CairoFontFamily,
                            fontSize = 8.2.sp,
                            color = paperTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.19f)
                        )
                        Text(
                            text = CurrencyFormatter.formatUsd(item.lineTotalUsd),
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.2.sp,
                            color = paperTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.21f)
                        )
                    }
                    if (idx < pageItems.lastIndex) {
                        HorizontalDivider(thickness = 0.5.dp, color = borderColor)
                    }
                }
            }

            if (isLastPage) {
                Spacer(modifier = Modifier.height(10.dp))

                // Grand Total Highlighted Colored Box
                val totalDec = CurrencyFormatter.parseBigDecimal(q.totalAmountUsd) ?: BigDecimal.ZERO
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = softTint,
                    border = BorderStroke(1.2.dp, primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الإجمالي الكلي",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = primaryColor
                        )
                        Text(
                            text = CurrencyFormatter.formatUsd(totalDec),
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Amount in words
                Text(
                    text = "المبلغ كتابةً: ${ArabicNumberToWords.convertUsdToArabicWords(totalDec)}",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.5.sp,
                    color = paperTextPrimary
                )

                if (q.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(7.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(0.6.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(7.dp)) {
                            Text(
                                text = "ملاحظات:",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.2.sp,
                                color = primaryColor
                            )
                            Text(
                                text = q.notes,
                                fontFamily = CairoFontFamily,
                                fontSize = 8.sp,
                                color = paperTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Authorized Signature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "مع خالص التقدير والاحترام،",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = paperTextPrimary
                        )
                        Text(
                            text = "التوقيع المعتمد / إدارة الشركة",
                            fontFamily = CairoFontFamily,
                            fontSize = 8.sp,
                            color = paperTextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .width(105.dp)
                                .height(1.dp)
                                .background(accentColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            HorizontalDivider(thickness = 0.6.dp, color = borderColor)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = companyInfo.name.ifBlank { "عرض سعر" },
                    fontFamily = CairoFontFamily,
                    fontSize = 7.5.sp,
                    color = paperTextSecondary
                )
                Text(
                    text = "صفحة $pageNumber من $totalPages",
                    fontFamily = CairoFontFamily,
                    fontSize = 7.5.sp,
                    color = paperTextSecondary
                )
            }
        }
    }
}

private fun paginateItemsForA4(items: List<QuotationItem>): List<List<QuotationItem>> {
    if (items.isEmpty()) return listOf(emptyList())
    val firstPageCap = 12
    val nextPageCap = 18
    if (items.size <= firstPageCap) return listOf(items)

    val result = mutableListOf<List<QuotationItem>>()
    result.add(items.take(firstPageCap))
    var idx = firstPageCap
    while (idx < items.size) {
        result.add(items.subList(idx, (idx + nextPageCap).coerceAtMost(items.size)))
        idx += nextPageCap
    }
    return result
}
