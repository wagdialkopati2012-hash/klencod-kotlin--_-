package com.example.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.domain.model.CompanyInfo
import com.example.domain.model.QuotationWithItems
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigDecimal

object A4PdfGenerator {

    const val A4_WIDTH = 595
    const val A4_HEIGHT = 842
    private const val MARGIN_H = 34f
    private const val MARGIN_TOP = 34f
    private const val MARGIN_BOTTOM = 42f
    private const val CONTENT_WIDTH = A4_WIDTH - MARGIN_H * 2

    fun generateQuotationPdf(
        context: Context,
        quotationWithItems: QuotationWithItems,
        company: CompanyInfo,
        primaryColorArgb: Int,
        accentColorArgb: Int,
        outputFile: File
    ): File {
        outputFile.parentFile?.mkdirs()

        val baseTypeface = ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT
        val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)

        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val logoBitmap = loadLogoBitmap(company.logoPath)

        fun startNewPage(): Canvas {
            drawFooter(canvas, company, pageNumber, baseTypeface, primaryColorArgb)
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
            page = document.startPage(pageInfo)
            return page.canvas
        }

        // Page 1 header
        var y = drawDocumentHeader(
            canvas = canvas,
            company = company,
            quotationWithItems = quotationWithItems,
            logoBitmap = logoBitmap,
            primaryColorArgb = primaryColorArgb,
            accentColorArgb = accentColorArgb,
            regularTypeface = baseTypeface,
            boldTypeface = boldTypeface
        )

        // Table geometry (RTL: right -> left)
        // Rightmost: اسم الصنف (257f), الكمية (70f), السعر (95f), الإجمالي (105f) = 527f
        val colNameW = 257f
        val colQtyW = 70f
        val colPriceW = 95f
        val colTotalW = CONTENT_WIDTH - colNameW - colQtyW - colPriceW

        val colTotalLeft = MARGIN_H
        val colPriceLeft = colTotalLeft + colTotalW
        val colQtyLeft = colPriceLeft + colPriceW
        val colNameLeft = colQtyLeft + colQtyW

        fun drawTableHeader(atY: Float): Float {
            val headerHeight = 28f
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColorArgb
                style = Paint.Style.FILL
            }
            val rect = RectF(MARGIN_H, atY, MARGIN_H + CONTENT_WIDTH, atY + headerHeight)
            canvas.drawRoundRect(rect, 4f, 4f, bgPaint)

            val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10.5f
                typeface = boldTypeface
            }

            drawCellText(canvas, "اسم الصنف", colNameLeft, atY, colNameW, headerHeight, headerPaint, Layout.Alignment.ALIGN_NORMAL)
            drawCellText(canvas, "الكمية", colQtyLeft, atY, colQtyW, headerHeight, headerPaint, Layout.Alignment.ALIGN_CENTER)
            drawCellText(canvas, "السعر", colPriceLeft, atY, colPriceW, headerHeight, headerPaint, Layout.Alignment.ALIGN_CENTER)
            drawCellText(canvas, "الإجمالي", colTotalLeft, atY, colTotalW, headerHeight, headerPaint, Layout.Alignment.ALIGN_CENTER)

            return atY + headerHeight
        }

        y = drawTableHeader(y)

        val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            typeface = baseTypeface
        }
        val boldCellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            typeface = boldTypeface
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.7f
            style = Paint.Style.STROKE
        }
        val altRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }

        val maxContentBottom = A4_HEIGHT - MARGIN_BOTTOM - 26f

        quotationWithItems.items.forEachIndexed { index, item ->
            val nameLayout = createRtlLayout(item.productName, cellPaint, (colNameW - 12f).toInt().coerceAtLeast(40), Layout.Alignment.ALIGN_NORMAL)
            val rowHeight = maxOf(26f, nameLayout.height + 12f)

            if (y + rowHeight > maxContentBottom) {
                canvas = startNewPage()
                y = MARGIN_TOP + 8f
                y = drawTableHeader(y)
            }

            val rowRect = RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + rowHeight)
            if (index % 2 == 1) {
                canvas.drawRect(rowRect, altRowPaint)
            }
            canvas.drawRect(rowRect, borderPaint)

            // Vertical column dividers
            canvas.drawLine(colQtyLeft, y, colQtyLeft, y + rowHeight, borderPaint)
            canvas.drawLine(colPriceLeft, y, colPriceLeft, y + rowHeight, borderPaint)
            canvas.drawLine(colTotalLeft, y, colTotalLeft, y + rowHeight, borderPaint)

            // Draw RTL cells
            drawStaticLayoutCenteredY(canvas, nameLayout, colNameLeft + 6f, y, rowHeight)
            drawCellText(
                canvas,
                CurrencyFormatter.formatQuantity(item.quantity),
                colQtyLeft,
                y,
                colQtyW,
                rowHeight,
                cellPaint,
                Layout.Alignment.ALIGN_CENTER
            )
            drawCellText(
                canvas,
                CurrencyFormatter.formatUsd(item.unitPriceUsd),
                colPriceLeft,
                y,
                colPriceW,
                rowHeight,
                cellPaint,
                Layout.Alignment.ALIGN_CENTER
            )
            drawCellText(
                canvas,
                CurrencyFormatter.formatUsd(item.lineTotalUsd),
                colTotalLeft,
                y,
                colTotalW,
                rowHeight,
                boldCellPaint,
                Layout.Alignment.ALIGN_CENTER
            )

            y += rowHeight
        }

        // Ensure enough room for Total box + Amount in words + Notes + Signature
        val totalDecimal = CurrencyFormatter.parseBigDecimal(quotationWithItems.quotation.totalAmountUsd)
            ?: BigDecimal.ZERO
        val wordsText = ArabicNumberToWords.convertUsdToArabicWords(totalDecimal)
        val notesText = quotationWithItems.quotation.notes.trim()
        val summaryEstimatedHeight = 145f + if (notesText.isNotEmpty()) 48f else 0f

        if (y + summaryEstimatedHeight > maxContentBottom) {
            canvas = startNewPage()
            y = MARGIN_TOP + 12f
        }

        y += 14f

        // Highlighted Grand Total Box ("الإجمالي الكلي")
        val totalBoxHeight = 38f
        val totalBoxRect = RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + totalBoxHeight)
        val totalBoxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blendWithWhite(primaryColorArgb, 0.90f)
            style = Paint.Style.FILL
        }
        val totalBoxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColorArgb
            strokeWidth = 1.4f
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(totalBoxRect, 6f, 6f, totalBoxFill)
        canvas.drawRoundRect(totalBoxRect, 6f, 6f, totalBoxStroke)

        val totalLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColorArgb
            textSize = 12f
            typeface = boldTypeface
        }
        val totalValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColorArgb
            textSize = 13.5f
            typeface = boldTypeface
        }

        drawCellText(canvas, "الإجمالي الكلي:", MARGIN_H + CONTENT_WIDTH - 140f, y, 128f, totalBoxHeight, totalLabelPaint, Layout.Alignment.ALIGN_NORMAL)
        drawCellText(canvas, CurrencyFormatter.formatUsd(totalDecimal), MARGIN_H + 14f, y, 200f, totalBoxHeight, totalValuePaint, Layout.Alignment.ALIGN_OPPOSITE)

        y += totalBoxHeight + 10f

        // Amount in words
        val wordsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 10f
            typeface = boldTypeface
        }
        val wordsLayout = createRtlLayout("المبلغ كتابةً: $wordsText", wordsPaint, CONTENT_WIDTH.toInt(), Layout.Alignment.ALIGN_NORMAL)
        canvas.save()
        canvas.translate(MARGIN_H, y)
        wordsLayout.draw(canvas)
        canvas.restore()
        y += wordsLayout.height + 12f

        // Notes section
        if (notesText.isNotEmpty()) {
            val notesTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColorArgb
                textSize = 10.5f
                typeface = boldTypeface
            }
            val notesBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 9.5f
                typeface = baseTypeface
            }
            val notesLayout = createRtlLayout(notesText, notesBodyPaint, (CONTENT_WIDTH - 20f).toInt(), Layout.Alignment.ALIGN_NORMAL)
            val boxH = notesLayout.height + 30f
            val notesRect = RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + boxH)
            canvas.drawRoundRect(notesRect, 5f, 5f, Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            })
            canvas.drawRoundRect(notesRect, 5f, 5f, borderPaint)

            val titleLayout = createRtlLayout("ملاحظات:", notesTitlePaint, (CONTENT_WIDTH - 20f).toInt(), Layout.Alignment.ALIGN_NORMAL)
            canvas.save()
            canvas.translate(MARGIN_H + 10f, y + 6f)
            titleLayout.draw(canvas)
            canvas.translate(0f, titleLayout.height + 4f)
            notesLayout.draw(canvas)
            canvas.restore()
            y += boxH + 16f
        } else {
            y += 12f
        }

        // Authorized Signature
        val sigPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            typeface = boldTypeface
        }
        val sigSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
            typeface = baseTypeface
        }
        val sigX = MARGIN_H + 12f
        val sigWidth = 175f
        val sigLayout1 = createRtlLayout("مع خالص التقدير والاحترام،", sigPaint, sigWidth.toInt(), Layout.Alignment.ALIGN_CENTER)
        val sigLayout2 = createRtlLayout("التوقيع المعتمد / إدارة الشركة", sigSubPaint, sigWidth.toInt(), Layout.Alignment.ALIGN_CENTER)
        canvas.save()
        canvas.translate(sigX, y)
        sigLayout1.draw(canvas)
        canvas.translate(0f, sigLayout1.height + 4f)
        sigLayout2.draw(canvas)
        canvas.restore()

        val lineY = y + sigLayout1.height + sigLayout2.height + 24f
        canvas.drawLine(sigX + 18f, lineY, sigX + sigWidth - 18f, lineY, Paint().apply {
            color = accentColorArgb
            strokeWidth = 1.2f
        })

        drawFooter(canvas, company, pageNumber, baseTypeface, primaryColorArgb)
        document.finishPage(page)

        FileOutputStream(outputFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()
        logoBitmap?.recycle()

        return outputFile
    }

    private fun drawDocumentHeader(
        canvas: Canvas,
        company: CompanyInfo,
        quotationWithItems: QuotationWithItems,
        logoBitmap: Bitmap?,
        primaryColorArgb: Int,
        accentColorArgb: Int,
        regularTypeface: Typeface,
        boldTypeface: Typeface
    ): Float {
        var y = MARGIN_TOP

        // Top accent bar
        canvas.drawRect(
            RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + 5f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColorArgb }
        )
        canvas.drawRect(
            RectF(MARGIN_H, y, MARGIN_H + 120f, y + 5f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColorArgb }
        )
        y += 14f

        val companyNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColorArgb
            textSize = 15f
            typeface = boldTypeface
        }
        val companyDetailPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 9.5f
            typeface = regularTypeface
        }

        // Logo on left side of RTL header (or right? In Arabic corporate letterhead, company info is on right, logo on left or vice versa)
        val logoSize = 54f
        if (logoBitmap != null) {
            val destRect = RectF(MARGIN_H, y, MARGIN_H + logoSize, y + logoSize)
            canvas.drawBitmap(logoBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        val infoWidth = (CONTENT_WIDTH - (if (logoBitmap != null) logoSize + 14f else 0f)).toInt()
        val infoStartX = MARGIN_H + (if (logoBitmap != null) logoSize + 14f else 0f)

        val compName = company.name.ifBlank { "عرض سعر" }
        val nameLayout = createRtlLayout(compName, companyNamePaint, infoWidth, Layout.Alignment.ALIGN_NORMAL)
        canvas.save()
        canvas.translate(infoStartX, y)
        nameLayout.draw(canvas)
        var textY = nameLayout.height + 3f

        val details = buildList {
            if (company.phone.isNotBlank()) add("هاتف: ${company.phone}")
            if (company.address.isNotBlank()) add("العنوان: ${company.address}")
            if (company.additionalInfo.isNotBlank()) add(company.additionalInfo)
        }.joinToString("  |  ")

        if (details.isNotBlank()) {
            val detailLayout = createRtlLayout(details, companyDetailPaint, infoWidth, Layout.Alignment.ALIGN_NORMAL)
            canvas.translate(0f, textY)
            detailLayout.draw(canvas)
            textY += detailLayout.height
        }
        canvas.restore()

        y += maxOf(logoSize, textY) + 14f

        // Title Banner: عرض سعر
        val titleHeight = 32f
        val titleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blendWithWhite(primaryColorArgb, 0.92f)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + titleHeight), 6f, 6f, titleBg)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColorArgb
            textSize = 15f
            typeface = boldTypeface
        }
        drawCellText(canvas, "عرض سعر", MARGIN_H, y, CONTENT_WIDTH, titleHeight, titlePaint, Layout.Alignment.ALIGN_CENTER)
        y += titleHeight + 12f

        // Metadata box: Quotation Number, Customer Name, Date
        val q = quotationWithItems.quotation
        val metaHeight = 52f
        val metaRect = RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + metaHeight)
        canvas.drawRoundRect(metaRect, 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        })
        canvas.drawRoundRect(metaRect, 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        })

        val metaLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10.5f
            typeface = boldTypeface
        }
        val metaRightW = (CONTENT_WIDTH * 0.56f).toInt()
        val metaLeftW = (CONTENT_WIDTH * 0.40f).toInt()

        val customerLayout = createRtlLayout("اسم العميل: ${q.customerName}", metaLabelPaint, metaRightW, Layout.Alignment.ALIGN_NORMAL)
        val numberLayout = createRtlLayout("رقم عرض السعر: #${q.quotationNumber}", metaLabelPaint, metaLeftW, Layout.Alignment.ALIGN_OPPOSITE)
        val dateLayout = createRtlLayout("التاريخ: ${DateFormatter.formatShortDate(q.dateMillis)}", metaLabelPaint, metaLeftW, Layout.Alignment.ALIGN_OPPOSITE)

        canvas.save()
        canvas.translate(MARGIN_H + CONTENT_WIDTH - metaRightW - 10f, y + 10f)
        customerLayout.draw(canvas)
        canvas.restore()

        canvas.save()
        canvas.translate(MARGIN_H + 10f, y + 7f)
        numberLayout.draw(canvas)
        canvas.translate(0f, numberLayout.height + 4f)
        dateLayout.draw(canvas)
        canvas.restore()

        return y + metaHeight + 14f
    }

    private fun drawFooter(
        canvas: Canvas,
        company: CompanyInfo,
        pageNumber: Int,
        regularTypeface: Typeface,
        primaryColorArgb: Int
    ) {
        val footerY = A4_HEIGHT - MARGIN_BOTTOM + 10f
        canvas.drawLine(
            MARGIN_H,
            footerY,
            MARGIN_H + CONTENT_WIDTH,
            footerY,
            Paint().apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 0.7f
            }
        )
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 8.5f
            typeface = regularTypeface
        }
        val compLabel = company.name.ifBlank { "عرض سعر" }
        drawCellText(canvas, compLabel, MARGIN_H + CONTENT_WIDTH - 240f, footerY + 4f, 240f, 18f, footerPaint, Layout.Alignment.ALIGN_NORMAL)
        drawCellText(canvas, "صفحة $pageNumber", MARGIN_H, footerY + 4f, 100f, 18f, footerPaint, Layout.Alignment.ALIGN_OPPOSITE)
    }

    private fun createRtlLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment
    ): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(10))
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, 1.12f)
            .setIncludePad(false)
            .build()
    }

    private fun drawCellText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        paint: TextPaint,
        alignment: Layout.Alignment
    ) {
        val layout = createRtlLayout(text, paint, (width - 8f).toInt().coerceAtLeast(10), alignment)
        drawStaticLayoutCenteredY(canvas, layout, x + 4f, y, height)
    }

    private fun drawStaticLayoutCenteredY(
        canvas: Canvas,
        layout: StaticLayout,
        x: Float,
        topY: Float,
        boxHeight: Float
    ) {
        val offsetY = ((boxHeight - layout.height) / 2f).coerceAtLeast(2f)
        canvas.save()
        canvas.translate(x, topY + offsetY)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun blendWithWhite(color: Int, whiteRatio: Float): Int {
        val r = ((Color.red(color) * (1f - whiteRatio)) + (255f * whiteRatio)).toInt().coerceIn(0, 255)
        val g = ((Color.green(color) * (1f - whiteRatio)) + (255f * whiteRatio)).toInt().coerceIn(0, 255)
        val b = ((Color.blue(color) * (1f - whiteRatio)) + (255f * whiteRatio)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun loadLogoBitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        )
    }
}
