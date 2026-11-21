package com.example.domain.util

import android.util.Xml
import com.example.domain.model.ExcelImportRow
import com.example.domain.model.ExcelParseResult
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.zip.ZipInputStream

object XlsxProductParser {

    fun parse(inputStream: InputStream, existingProductNames: Set<String>): ExcelParseResult {
        val zipEntries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    zipEntries[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (zipEntries.isEmpty()) {
            return ExcelParseResult(
                validRows = emptyList(),
                errors = listOf("الملف المحدد فارغ أو ليس ملف Excel (.xlsx) صالحاً."),
                duplicateNames = emptySet()
            )
        }

        val sharedStrings = zipEntries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()

        val firstSheetPath = resolveFirstSheetPath(zipEntries)
        val sheetBytes = firstSheetPath?.let { zipEntries[it] }
            ?: zipEntries.entries.firstOrNull {
                it.key.startsWith("xl/worksheets/sheet") && it.key.endsWith(".xml")
            }?.value

        if (sheetBytes == null) {
            return ExcelParseResult(
                validRows = emptyList(),
                errors = listOf("لم يتم العثور على ورقة عمل داخل ملف Excel."),
                duplicateNames = emptySet()
            )
        }

        val rawRows = parseWorksheetRows(sheetBytes, sharedStrings)
        if (rawRows.isEmpty()) {
            return ExcelParseResult(
                validRows = emptyList(),
                errors = listOf("ورقة العمل فارغة ولا تحتوي على صفوف بيانات."),
                duplicateNames = emptySet()
            )
        }

        val validRows = mutableListOf<ExcelImportRow>()
        val errors = mutableListOf<String>()
        val duplicates = mutableSetOf<String>()
        val normalizedExisting = existingProductNames.map { it.trim().lowercase() }.toSet()
        val seenInFile = mutableSetOf<String>()

        rawRows.forEachIndexed { index, (rowNum, cols) ->
            val name = cols[0]?.trim().orEmpty()
            val rawPrice = cols[1]?.trim().orEmpty()

            if (name.isEmpty() && rawPrice.isEmpty()) {
                return@forEachIndexed
            }

            val parsedPrice = CurrencyFormatter.parseBigDecimal(rawPrice)

            // Skip header row automatically (e.g., "اسم الصنف" | "السعر" in first row)
            if (index == 0 && parsedPrice == null && name.isNotEmpty()) {
                return@forEachIndexed
            }

            if (name.isEmpty()) {
                errors.add("الصف $rowNum: اسم الصنف فارغ.")
                return@forEachIndexed
            }

            if (parsedPrice == null || parsedPrice <= BigDecimal.ZERO) {
                errors.add("الصف $rowNum ($name): السعر غير صالح أو يجب أن يكون أكبر من صفر.")
                return@forEachIndexed
            }

            val normalizedName = name.lowercase()
            if (normalizedName in normalizedExisting || !seenInFile.add(normalizedName)) {
                duplicates.add(name)
            }

            validRows.add(
                ExcelImportRow(
                    rowNumber = rowNum,
                    name = name,
                    priceUsd = parsedPrice.setScale(2, RoundingMode.HALF_UP)
                )
            )
        }

        return ExcelParseResult(
            validRows = validRows,
            errors = errors,
            duplicateNames = duplicates
        )
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var inSi = false
        var inT = false
        val currentText = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            inSi = true
                            currentText.clear()
                        }
                        "t" -> if (inSi) inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inSi && inT) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> inT = false
                        "si" -> {
                            strings.add(currentText.toString())
                            inSi = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun resolveFirstSheetPath(entries: Map<String, ByteArray>): String? {
        val workbookBytes = entries["xl/workbook.xml"] ?: return null
        val relsBytes = entries["xl/_rels/workbook.xml.rels"] ?: return null

        var firstRelId: String? = null
        val wbParser = Xml.newPullParser()
        wbParser.setInput(ByteArrayInputStream(workbookBytes), "UTF-8")
        var event = wbParser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && wbParser.name == "sheet") {
                for (i in 0 until wbParser.attributeCount) {
                    val attrName = wbParser.getAttributeName(i)
                    if (attrName == "id" || attrName.endsWith(":id")) {
                        firstRelId = wbParser.getAttributeValue(i)
                        break
                    }
                }
                if (firstRelId != null) break
            }
            event = wbParser.next()
        }

        val targetId = firstRelId ?: return null
        val relParser = Xml.newPullParser()
        relParser.setInput(ByteArrayInputStream(relsBytes), "UTF-8")
        event = relParser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && relParser.name == "Relationship") {
                val id = relParser.getAttributeValue(null, "Id")
                val target = relParser.getAttributeValue(null, "Target")
                if (id == targetId && target != null) {
                    return if (target.startsWith("/")) target.drop(1)
                    else if (target.startsWith("xl/")) target
                    else "xl/$target"
                }
            }
            event = relParser.next()
        }
        return null
    }

    private fun parseWorksheetRows(
        bytes: ByteArray,
        sharedStrings: List<String>
    ): List<Pair<Int, Map<Int, String>>> {
        val rows = mutableListOf<Pair<Int, Map<Int, String>>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var currentRowNum = 0
        var currentRowCols = mutableMapOf<Int, String>()
        var currentColIdx = -1
        var currentCellType = ""
        var inValue = false
        var inInlineT = false
        val valueBuilder = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRowNum = parser.getAttributeValue(null, "r")?.toIntOrNull()
                                ?: (rows.size + 1)
                            currentRowCols = mutableMapOf()
                        }
                        "c" -> {
                            val cellRef = parser.getAttributeValue(null, "r")
                            currentColIdx = if (cellRef != null) columnIndexFromRef(cellRef) else (currentRowCols.size)
                            currentCellType = parser.getAttributeValue(null, "t").orEmpty()
                            valueBuilder.clear()
                        }
                        "v" -> {
                            inValue = true
                            valueBuilder.clear()
                        }
                        "t" -> {
                            if (currentCellType == "inlineStr") {
                                inInlineT = true
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue || inInlineT) {
                        valueBuilder.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inValue = false
                        "t" -> inInlineT = false
                        "c" -> {
                            val raw = valueBuilder.toString()
                            val resolved = when (currentCellType) {
                                "s" -> {
                                    val idx = raw.trim().toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                                }
                                else -> raw
                            }
                            if (currentColIdx in 0..1) {
                                currentRowCols[currentColIdx] = resolved
                            }
                            currentColIdx = -1
                            currentCellType = ""
                        }
                        "row" -> {
                            if (currentRowCols.isNotEmpty()) {
                                rows.add(currentRowNum to currentRowCols)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun columnIndexFromRef(ref: String): Int {
        var col = 0
        for (ch in ref) {
            if (ch.isLetter()) {
                col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }
}
