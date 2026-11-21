package com.example.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Immutable
@Serializable
data class User(
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val roleLabel: String = "مستخدم",
    val isActive: Boolean = true,
    val isSuperAdmin: Boolean = false,
    val permissions: UserPermissions = UserPermissions.standardEmployee(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun effectivePermissions(): UserPermissions =
        if (isSuperAdmin) UserPermissions.superAdmin() else permissions
}

@Immutable
@Serializable
data class CompanyInfo(
    val id: Int = 1,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val additionalInfo: String = "",
    val logoPath: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
@Serializable
data class Product(
    val id: Long = 0,
    val name: String,
    val priceUsd: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedCode: String
        get() = "P" + id.toString().padStart(3, '0')

    val priceDecimal: BigDecimal
        get() = priceUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO
}

@Serializable
enum class QuotationStatus(val arabicLabel: String) {
    DRAFT("مسودة"),
    SENT("مرسل"),
    APPROVED("معتمد"),
    SAVED("محفوظ")
}

@Immutable
@Serializable
data class QuotationItem(
    val id: Long = 0,
    val quotationId: Long = 0,
    val productId: Long?,
    val productName: String,
    val unitPriceUsd: String,
    val quantity: String,
    val lineTotalUsd: String
)

@Immutable
@Serializable
data class Quotation(
    val id: Long = 0,
    val quotationNumber: Long,
    val customerName: String,
    val dateMillis: Long,
    val notes: String = "",
    val totalAmountUsd: String,
    val status: QuotationStatus = QuotationStatus.SAVED,
    val createdByUserId: Long,
    val createdByUsername: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
@Serializable
data class QuotationWithItems(
    val quotation: Quotation,
    val items: List<QuotationItem>
)

@Immutable
@Serializable
data class AuditLogEntry(
    val id: Long = 0,
    val username: String,
    val operation: String,
    val category: String,
    val recordId: String,
    val recordDescription: String,
    val previousValue: String = "",
    val newValue: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class ExcelImportRow(
    val rowNumber: Int,
    val name: String,
    val priceUsd: BigDecimal
)

@Immutable
data class ExcelParseResult(
    val validRows: List<ExcelImportRow>,
    val errors: List<String>,
    val duplicateNames: Set<String>
)

@Immutable
data class ImportSummary(
    val insertedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val errors: List<String> = emptyList()
)

enum class DuplicateImportPolicy(val arabicLabel: String) {
    SKIP("تخطي المكرر"),
    UPDATE("تحديث السعر للمكرر")
}
