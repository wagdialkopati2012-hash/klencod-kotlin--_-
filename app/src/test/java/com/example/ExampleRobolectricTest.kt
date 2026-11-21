package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.QuotationItemEntity
import com.example.domain.model.Product
import com.example.domain.util.ArabicNumberToWords
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.PasswordHasher
import com.example.domain.util.XlsxProductParser
import com.example.ui.navigation.SelectedProductPayload
import com.example.ui.viewmodel.CreateEditQuotationViewModel
import com.example.ui.viewmodel.DraftQuotationLine
import com.example.ui.viewmodel.ProductSelectionViewModel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `app name matches Arabic quotation app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("عرض سعر", appName)
    }

    @Test
    fun `password hashing verifies valid and rejects invalid passwords`() {
        val hash = PasswordHasher.hashPassword("Tender@2026")
        assertTrue(PasswordHasher.verifyPassword("Tender@2026", hash))
        assertFalse(PasswordHasher.verifyPassword("WrongPass", hash))
    }

    @Test
    fun `big decimal currency calculations and formatting`() {
        val unit = CurrencyFormatter.parsePositiveBigDecimal("25")!!
        val qty = CurrencyFormatter.parsePositiveBigDecimal("٣")!! // Arabic digit 3
        val total = CurrencyFormatter.calculateLineTotal(unit, qty)
        assertEquals(BigDecimal("75.00"), total)
        assertEquals("$75", CurrencyFormatter.formatUsd(total))
        assertEquals("$2,850", CurrencyFormatter.formatUsd("2850.00"))
        assertNull(CurrencyFormatter.parsePositiveBigDecimal("0"))
        assertNull(CurrencyFormatter.parsePositiveBigDecimal("-5"))
    }

    @Test
    fun `arabic number to words formats usd correctly`() {
        val words = ArabicNumberToWords.convertUsdToArabicWords(BigDecimal("2850.00"))
        assertTrue(words.contains("ألفان"))
        assertTrue(words.contains("دولار"))
    }

    @Test
    fun `quotation numbering starts at 1 and never reuses deleted numbers`() = runTest {
        val dao = database.quotationDao()
        val item = QuotationItemEntity(
            quotationId = 0,
            productId = 1,
            productName = "لابتوب",
            unitPriceUsd = "1200.00",
            quantity = "2",
            lineTotalUsd = "2400.00"
        )

        val (id1, num1) = dao.createQuotationWithAutoNumber(
            customerName = "شركة أ", dateMillis = 1000L, notes = "",
            totalAmountUsd = "2400.00", status = "SAVED",
            createdByUserId = 1L, createdByUsername = "admin", items = listOf(item)
        )
        val (id2, num2) = dao.createQuotationWithAutoNumber(
            customerName = "شركة ب", dateMillis = 1000L, notes = "",
            totalAmountUsd = "2400.00", status = "SAVED",
            createdByUserId = 1L, createdByUsername = "admin", items = listOf(item)
        )
        val (id3, num3) = dao.createQuotationWithAutoNumber(
            customerName = "شركة ج", dateMillis = 1000L, notes = "",
            totalAmountUsd = "2400.00", status = "SAVED",
            createdByUserId = 1L, createdByUsername = "admin", items = listOf(item)
        )

        assertEquals(1L, num1)
        assertEquals(2L, num2)
        assertEquals(3L, num3)

        // Permanently delete quotation #2
        dao.deleteQuotationPermanently(id2)
        assertNull(dao.getQuotationWithItemsOnce(id2))

        val remainingNumbers = dao.getAllQuotationsOnce().map { it.quotationNumber }
        assertEquals(listOf(1L, 3L), remainingNumbers)

        // Next quotation must receive #4 (never reuse #2)
        val (_, num4) = dao.createQuotationWithAutoNumber(
            customerName = "شركة د", dateMillis = 1000L, notes = "",
            totalAmountUsd = "2400.00", status = "SAVED",
            createdByUserId = 1L, createdByUsername = "admin", items = listOf(item)
        )
        assertEquals(4L, num4)
    }

    @Test
    fun `xlsx parser reads valid rows skips header and detects duplicates`() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zos.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <si><t>اسم الصنف</t></si>
                    <si><t>السعر</t></si>
                    <si><t>شاشة عرض</t></si>
                    <si><t>طابعة ليزر</t></si>
                </sst>
                """.trimIndent().toByteArray()
            )
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <sheetData>
                        <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row>
                        <row r="2"><c r="A2" t="s"><v>2</v></c><c r="B2"><v>600</v></c></row>
                        <row r="3"><c r="A3" t="s"><v>3</v></c><c r="B3"><v>450.5</v></c></row>
                    </sheetData>
                </worksheet>
                """.trimIndent().toByteArray()
            )
            zos.closeEntry()
        }

        val result = XlsxProductParser.parse(
            ByteArrayInputStream(baos.toByteArray()),
            existingProductNames = setOf("شاشة عرض")
        )
        assertEquals(2, result.validRows.size)
        assertEquals("شاشة عرض", result.validRows[0].name)
        assertEquals(BigDecimal("600.00"), result.validRows[0].priceUsd)
        assertEquals("طابعة ليزر", result.validRows[1].name)
        assertEquals(BigDecimal("450.50"), result.validRows[1].priceUsd)
        assertTrue("شاشة عرض" in result.duplicateNames)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `product selection calculates subtotals ignores zero and preserves across search`() {
        val catalog = listOf(
            Product(id = 274, name = "Europercolor Effervescent Granules", priceUsd = "0.82"),
            Product(id = 12, name = "شاشة عرض", priceUsd = "600.00"),
            Product(id = 15, name = "طابعة ليزر", priceUsd = "450.50")
        )

        // Default state: all quantity fields empty, 0 selected, $0 total
        val initial = ProductSelectionViewModel.computeSelectionState(
            products = catalog,
            query = "",
            quantitiesMap = emptyMap(),
            showSelectedOnly = false,
            existingMap = emptyMap()
        )
        assertEquals(0, initial.selectedCount)
        assertEquals(BigDecimal.ZERO, initial.totalAmount)
        assertTrue(initial.rows.all { it.quantityInput.isEmpty() && !it.isSelected && it.subtotal == null })

        // Negative & invalid chars sanitized
        assertEquals("10.5", ProductSelectionViewModel.sanitizeQuantityInput("-10.5abc"))
        assertEquals("3", ProductSelectionViewModel.sanitizeQuantityInput("٣"))

        // User enters qty 10 for P274, qty 0 for #12, qty 2 for #15, while searching for "P274"
        val quantities = mapOf(
            274L to "10",
            12L to "0",
            15L to "2"
        )
        val filteredByCode = ProductSelectionViewModel.computeSelectionState(
            products = catalog,
            query = "P274",
            quantitiesMap = quantities,
            showSelectedOnly = false,
            existingMap = emptyMap()
        )
        assertEquals(1, filteredByCode.rows.size)
        assertEquals(BigDecimal("8.20"), filteredByCode.rows[0].subtotal)
        // Selected summary still retains BOTH valid products (#274 and #15) and ignores #12 (qty 0)
        assertEquals(2, filteredByCode.selectedCount)
        assertEquals(BigDecimal("12"), filteredByCode.totalQuantity)
        assertEquals(BigDecimal("909.20"), filteredByCode.totalAmount)
    }

    @Test
    fun `merging selected products into quotation combines duplicates and appends new lines`() {
        val existing = listOf(
            DraftQuotationLine(
                localKey = "existing_274",
                productId = 274L,
                productName = "Europercolor Effervescent Granules",
                unitPriceUsd = "0.80", // Historical unit price preserved
                quantityInput = "5",
                lineTotalUsd = BigDecimal("4.00")
            )
        )
        val incoming = listOf(
            SelectedProductPayload(
                productId = 274L,
                productName = "Europercolor Effervescent Granules",
                productCode = "P274",
                unitPriceUsd = "0.82",
                quantity = "10"
            ),
            SelectedProductPayload(
                productId = 15L,
                productName = "طابعة ليزر",
                productCode = "P015",
                unitPriceUsd = "450.50",
                quantity = "2"
            )
        )

        val merged = CreateEditQuotationViewModel.mergeSelectedProductsIntoLines(existing, incoming)
        assertEquals(2, merged.size)
        // Product 274 combined 5 + 10 = 15 at historical price 0.80 -> 12.00
        assertEquals("15", merged[0].quantityInput)
        assertEquals(BigDecimal("12.00"), merged[0].lineTotalUsd)
        // Product 15 added cleanly
        assertEquals("2", merged[1].quantityInput)
        assertEquals(BigDecimal("901.00"), merged[1].lineTotalUsd)
    }
}
