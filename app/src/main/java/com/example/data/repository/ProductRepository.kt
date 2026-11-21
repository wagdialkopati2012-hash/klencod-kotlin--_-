package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.Mappers.toDomain
import com.example.data.local.dao.ProductDao
import com.example.data.local.entity.ProductEntity
import com.example.domain.model.DuplicateImportPolicy
import com.example.domain.model.ExcelParseResult
import com.example.domain.model.ImportSummary
import com.example.domain.model.Product
import com.example.domain.model.User
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.XlsxProductParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productDao: ProductDao,
    private val auditLogRepository: AuditLogRepository
) {
    val allProducts: Flow<List<Product>> = productDao.observeAllProducts().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun addProduct(actor: User, name: String, priceUsd: BigDecimal): Result<Long> =
        withContext(Dispatchers.IO) {
            if (!actor.effectivePermissions().canAddProducts) {
                return@withContext Result.failure(SecurityException("ليس لديك صلاحية إضافة أصناف."))
            }
            val cleanName = name.trim()
            if (cleanName.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("يرجى إدخال اسم الصنف."))
            }
            if (priceUsd <= BigDecimal.ZERO) {
                return@withContext Result.failure(IllegalArgumentException("السعر يجب أن يكون أكبر من صفر."))
            }
            val priceStr = CurrencyFormatter.toCanonicalString(priceUsd)
            val id = productDao.insertProduct(
                ProductEntity(
                    name = cleanName,
                    priceUsd = priceStr,
                    createdAt = System.currentTimeMillis()
                )
            )
            auditLogRepository.record(
                username = actor.username,
                operation = "إضافة صنف",
                category = "الأصناف",
                recordId = id.toString(),
                recordDescription = "إضافة الصنف: $cleanName",
                newValue = CurrencyFormatter.formatUsd(priceUsd)
            )
            Result.success(id)
        }

    suspend fun updateProduct(
        actor: User,
        product: Product,
        newName: String,
        newPriceUsd: BigDecimal
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canEditProducts) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية تعديل الأصناف."))
        }
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("يرجى إدخال اسم الصنف."))
        }
        if (newPriceUsd <= BigDecimal.ZERO) {
            return@withContext Result.failure(IllegalArgumentException("السعر يجب أن يكون أكبر من صفر."))
        }
        val newPriceStr = CurrencyFormatter.toCanonicalString(newPriceUsd)
        productDao.updateProduct(
            ProductEntity(
                id = product.id,
                name = cleanName,
                priceUsd = newPriceStr,
                createdAt = product.createdAt
            )
        )
        val priceChanged = product.priceUsd != newPriceStr
        auditLogRepository.record(
            username = actor.username,
            operation = if (priceChanged) "تعديل صنف وتغيير السعر" else "تعديل صنف",
            category = "الأصناف",
            recordId = product.id.toString(),
            recordDescription = "تعديل الصنف: $cleanName",
            previousValue = "${product.name} (${CurrencyFormatter.formatUsd(product.priceUsd)})",
            newValue = "$cleanName (${CurrencyFormatter.formatUsd(newPriceStr)})"
        )
        Result.success(Unit)
    }

    suspend fun deleteProduct(actor: User, product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canDeleteProducts) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية حذف الأصناف."))
        }
        productDao.deleteById(product.id)
        auditLogRepository.record(
            username = actor.username,
            operation = "حذف صنف",
            category = "الأصناف",
            recordId = product.id.toString(),
            recordDescription = "حذف الصنف: ${product.name}",
            previousValue = CurrencyFormatter.formatUsd(product.priceUsd)
        )
        Result.success(Unit)
    }

    suspend fun bulkDeleteProducts(actor: User, products: List<Product>): Result<Int> =
        withContext(Dispatchers.IO) {
            if (!actor.effectivePermissions().canBulkDeleteProducts && !actor.effectivePermissions().canDeleteProducts) {
                return@withContext Result.failure(SecurityException("ليس لديك صلاحية الحذف الجماعي للأصناف."))
            }
            if (products.isEmpty()) return@withContext Result.success(0)
            productDao.deleteByIds(products.map { it.id })
            auditLogRepository.record(
                username = actor.username,
                operation = "حذف جماعي للأصناف",
                category = "الأصناف",
                recordId = products.joinToString(",") { it.id.toString() },
                recordDescription = "حذف ${products.size} صنفاً دفعة واحدة"
            )
            Result.success(products.size)
        }

    suspend fun parseExcelFile(uri: Uri): Result<ExcelParseResult> = withContext(Dispatchers.IO) {
        runCatching {
            val existingNames = productDao.getAllProductsOnce().map { it.name }.toSet()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                XlsxProductParser.parse(stream, existingNames)
            } ?: error("تعذر فتح ملف Excel المحدد.")
        }
    }

    suspend fun commitExcelImport(
        actor: User,
        parseResult: ExcelParseResult,
        duplicatePolicy: DuplicateImportPolicy
    ): Result<ImportSummary> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canImportExcel) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية استيراد ملفات Excel."))
        }
        var inserted = 0
        var updated = 0
        var skipped = 0

        val existingMap = productDao.getAllProductsOnce()
            .associateBy { it.name.trim().lowercase() }
            .toMutableMap()

        for (row in parseResult.validRows) {
            val key = row.name.trim().lowercase()
            val priceStr = CurrencyFormatter.toCanonicalString(row.priceUsd)
            val existing = existingMap[key]

            if (existing != null) {
                when (duplicatePolicy) {
                    DuplicateImportPolicy.SKIP -> skipped++
                    DuplicateImportPolicy.UPDATE -> {
                        val updatedEntity = existing.copy(priceUsd = priceStr)
                        productDao.updateProduct(updatedEntity)
                        existingMap[key] = updatedEntity
                        updated++
                    }
                }
            } else {
                val newEntity = ProductEntity(
                    name = row.name.trim(),
                    priceUsd = priceStr,
                    createdAt = System.currentTimeMillis()
                )
                val newId = productDao.insertProduct(newEntity)
                existingMap[key] = newEntity.copy(id = newId)
                inserted++
            }
        }

        val summary = ImportSummary(
            insertedCount = inserted,
            updatedCount = updated,
            skippedCount = skipped,
            errorCount = parseResult.errors.size,
            errors = parseResult.errors
        )

        auditLogRepository.record(
            username = actor.username,
            operation = "استيراد أصناف من Excel",
            category = "الأصناف",
            recordDescription = "تمت إضافة $inserted صنف، وتحديث $updated، وتخطي $skipped، مع ${parseResult.errors.size} أخطاء."
        )

        Result.success(summary)
    }
}
