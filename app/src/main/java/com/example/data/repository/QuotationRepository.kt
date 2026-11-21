package com.example.data.repository

import com.example.data.local.Mappers.toDomain
import com.example.data.local.dao.QuotationDao
import com.example.data.local.entity.QuotationEntity
import com.example.data.local.entity.QuotationItemEntity
import com.example.domain.model.Quotation
import com.example.domain.model.QuotationItem
import com.example.domain.model.QuotationStatus
import com.example.domain.model.QuotationWithItems
import com.example.domain.model.User
import com.example.domain.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuotationRepository @Inject constructor(
    private val quotationDao: QuotationDao,
    private val auditLogRepository: AuditLogRepository
) {
    val allQuotations: Flow<List<QuotationWithItems>> =
        quotationDao.observeAllQuotationsWithItems().map { list ->
            list.map { it.toDomain() }
        }

    fun observeQuotation(id: Long): Flow<QuotationWithItems?> =
        quotationDao.observeQuotationWithItems(id).map { it?.toDomain() }

    suspend fun getQuotationOnce(id: Long): QuotationWithItems? = withContext(Dispatchers.IO) {
        quotationDao.getQuotationWithItemsOnce(id)?.toDomain()
    }

    suspend fun getNextExpectedQuotationNumber(): Long = withContext(Dispatchers.IO) {
        (quotationDao.getCounterOnce()?.lastNumber ?: 0L) + 1L
    }

    suspend fun createQuotation(
        actor: User,
        customerName: String,
        dateMillis: Long,
        notes: String,
        status: QuotationStatus,
        items: List<QuotationItem>
    ): Result<Pair<Long, Long>> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canCreateQuotations) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية إنشاء عروض الأسعار."))
        }
        val cleanCustomer = customerName.trim()
        if (cleanCustomer.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("يرجى إدخال اسم العميل."))
        }
        if (items.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("يجب إضافة صنف واحد على الأقل."))
        }

        val total = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(CurrencyFormatter.parseBigDecimal(item.lineTotalUsd) ?: BigDecimal.ZERO)
        }

        val entityItems = items.map { item ->
            QuotationItemEntity(
                quotationId = 0,
                productId = item.productId,
                productName = item.productName,
                unitPriceUsd = item.unitPriceUsd,
                quantity = item.quantity,
                lineTotalUsd = item.lineTotalUsd
            )
        }

        val totalStr = CurrencyFormatter.toCanonicalString(total)
        val (quotationId, quotationNumber) = quotationDao.createQuotationWithAutoNumber(
            customerName = cleanCustomer,
            dateMillis = dateMillis,
            notes = notes.trim(),
            totalAmountUsd = totalStr,
            status = status.name,
            createdByUserId = actor.id,
            createdByUsername = actor.username,
            items = entityItems
        )

        auditLogRepository.record(
            username = actor.username,
            operation = "إنشاء عرض سعر",
            category = "عروض الأسعار",
            recordId = quotationNumber.toString(),
            recordDescription = "إنشاء عرض سعر رقم #$quotationNumber للعميل $cleanCustomer",
            newValue = CurrencyFormatter.formatUsd(total)
        )

        Result.success(quotationId to quotationNumber)
    }

    suspend fun updateQuotation(
        actor: User,
        original: Quotation,
        customerName: String,
        dateMillis: Long,
        notes: String,
        status: QuotationStatus,
        items: List<QuotationItem>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val perms = actor.effectivePermissions()
        val allowed = perms.canEditAllQuotations ||
            (perms.canEditOwnQuotations && original.createdByUserId == actor.id)
        if (!allowed) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية تعديل عرض السعر هذا."))
        }
        val cleanCustomer = customerName.trim()
        if (cleanCustomer.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("يرجى إدخال اسم العميل."))
        }
        if (items.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("يجب إضافة صنف واحد على الأقل."))
        }

        val total = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(CurrencyFormatter.parseBigDecimal(item.lineTotalUsd) ?: BigDecimal.ZERO)
        }
        val totalStr = CurrencyFormatter.toCanonicalString(total)

        val updatedEntity = QuotationEntity(
            id = original.id,
            quotationNumber = original.quotationNumber, // Preserved permanently
            customerName = cleanCustomer,
            dateMillis = dateMillis,
            notes = notes.trim(),
            totalAmountUsd = totalStr,
            status = status.name,
            createdByUserId = original.createdByUserId,
            createdByUsername = original.createdByUsername,
            createdAt = original.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        val entityItems = items.map { item ->
            QuotationItemEntity(
                quotationId = original.id,
                productId = item.productId,
                productName = item.productName,
                unitPriceUsd = item.unitPriceUsd,
                quantity = item.quantity,
                lineTotalUsd = item.lineTotalUsd
            )
        }

        quotationDao.updateQuotationWithItems(updatedEntity, entityItems)

        auditLogRepository.record(
            username = actor.username,
            operation = "تعديل عرض سعر",
            category = "عروض الأسعار",
            recordId = original.quotationNumber.toString(),
            recordDescription = "تعديل عرض السعر رقم #${original.quotationNumber} للعميل $cleanCustomer",
            previousValue = "${original.customerName} (${CurrencyFormatter.formatUsd(original.totalAmountUsd)})",
            newValue = "$cleanCustomer (${CurrencyFormatter.formatUsd(total)})"
        )

        Result.success(Unit)
    }

    suspend fun deleteQuotationPermanently(
        actor: User,
        quotation: Quotation
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val perms = actor.effectivePermissions()
        val allowed = perms.canDeleteAllQuotations ||
            (perms.canDeleteOwnQuotations && quotation.createdByUserId == actor.id)
        if (!allowed) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية حذف عرض السعر هذا."))
        }

        quotationDao.deleteQuotationPermanently(quotation.id)

        auditLogRepository.record(
            username = actor.username,
            operation = "حذف عرض سعر نهائياً",
            category = "عروض الأسعار",
            recordId = quotation.quotationNumber.toString(),
            recordDescription = "حذف نهائي لعرض السعر رقم #${quotation.quotationNumber} للعميل ${quotation.customerName}",
            previousValue = CurrencyFormatter.formatUsd(quotation.totalAmountUsd),
            newValue = "محذوف نهائياً"
        )

        Result.success(Unit)
    }
}
