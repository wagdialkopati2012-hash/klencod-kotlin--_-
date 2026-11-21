package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.Mappers
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.QuotationCounterEntity
import com.example.data.local.entity.QuotationEntity
import com.example.data.local.entity.QuotationItemEntity
import com.example.data.local.entity.UserEntity
import com.example.data.preferences.PreferencesManager
import com.example.domain.model.ThemeSettings
import com.example.domain.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupUserRecord(
    val id: Long,
    val username: String,
    val fullName: String,
    val passwordHash: String,
    val roleLabel: String,
    val isActive: Boolean,
    val isSuperAdmin: Boolean,
    val permissionsJson: String,
    val createdAt: Long
)

@Serializable
data class BackupCompanyRecord(
    val name: String,
    val phone: String,
    val address: String,
    val additionalInfo: String,
    val logoBase64: String? = null,
    val updatedAt: Long
)

@Serializable
data class BackupProductRecord(
    val id: Long,
    val name: String,
    val priceUsd: String,
    val createdAt: Long
)

@Serializable
data class BackupQuotationRecord(
    val id: Long,
    val quotationNumber: Long,
    val customerName: String,
    val dateMillis: Long,
    val notes: String,
    val totalAmountUsd: String,
    val status: String,
    val createdByUserId: Long,
    val createdByUsername: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupQuotationItemRecord(
    val id: Long,
    val quotationId: Long,
    val productId: Long?,
    val productName: String,
    val unitPriceUsd: String,
    val quantity: String,
    val lineTotalUsd: String
)

@Serializable
data class BackupAuditRecord(
    val id: Long,
    val username: String,
    val operation: String,
    val category: String,
    val recordId: String,
    val recordDescription: String,
    val previousValue: String,
    val newValue: String,
    val timestamp: Long
)

@Serializable
data class BackupPayload(
    val formatVersion: Int = 1,
    val appName: String = "عرض سعر",
    val exportedAt: Long = System.currentTimeMillis(),
    val quotationCounter: Long = 0L,
    val customers: List<String> = emptyList(),
    val users: List<BackupUserRecord> = emptyList(),
    val company: BackupCompanyRecord? = null,
    val products: List<BackupProductRecord> = emptyList(),
    val quotations: List<BackupQuotationRecord> = emptyList(),
    val quotationItems: List<BackupQuotationItemRecord> = emptyList(),
    val auditLogs: List<BackupAuditRecord> = emptyList(),
    val themeSettings: ThemeSettings = ThemeSettings(),
    val rememberedUsername: String = ""
)

data class BackupValidationSummary(
    val payload: BackupPayload,
    val usersCount: Int,
    val productsCount: Int,
    val quotationsCount: Int,
    val customersCount: Int,
    val auditLogsCount: Int,
    val quotationCounter: Long,
    val exportedAt: Long
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val preferencesManager: PreferencesManager,
    private val auditLogRepository: AuditLogRepository
) {

    suspend fun exportBackupToUri(actor: User, destinationUri: Uri): Result<BackupValidationSummary> =
        withContext(Dispatchers.IO) {
            if (!actor.effectivePermissions().canManageBackup) {
                return@withContext Result.failure(SecurityException("ليس لديك صلاحية إنشاء نسخة احتياطية."))
            }
            runCatching {
                val users = database.userDao().getAllUsersOnce()
                val company = database.companyDao().getCompanyOnce()
                val products = database.productDao().getAllProductsOnce()
                val quotations = database.quotationDao().getAllQuotationsOnce()
                val items = database.quotationDao().getAllItemsOnce()
                val counter = database.quotationDao().getCounterOnce()?.lastNumber
                    ?: (quotations.maxOfOrNull { it.quotationNumber } ?: 0L)
                val logs = database.auditLogDao().getAllLogsOnce()
                val theme = preferencesManager.themeSettingsFlow.first()
                val (_, rememberedUser) = preferencesManager.rememberedUsernameFlow.first()

                val logoBase64 = company?.logoPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        runCatching {
                            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                        }.getOrNull()
                    } else null
                }

                val distinctCustomers = quotations.map { it.customerName.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()

                val payload = BackupPayload(
                    formatVersion = 1,
                    exportedAt = System.currentTimeMillis(),
                    quotationCounter = counter,
                    customers = distinctCustomers,
                    users = users.map {
                        BackupUserRecord(
                            id = it.id,
                            username = it.username,
                            fullName = it.fullName,
                            passwordHash = it.passwordHash,
                            roleLabel = it.roleLabel,
                            isActive = it.isActive,
                            isSuperAdmin = it.isSuperAdmin,
                            permissionsJson = it.permissionsJson,
                            createdAt = it.createdAt
                        )
                    },
                    company = company?.let {
                        BackupCompanyRecord(
                            name = it.name,
                            phone = it.phone,
                            address = it.address,
                            additionalInfo = it.additionalInfo,
                            logoBase64 = logoBase64,
                            updatedAt = it.updatedAt
                        )
                    },
                    products = products.map {
                        BackupProductRecord(
                            id = it.id,
                            name = it.name,
                            priceUsd = it.priceUsd,
                            createdAt = it.createdAt
                        )
                    },
                    quotations = quotations.map {
                        BackupQuotationRecord(
                            id = it.id,
                            quotationNumber = it.quotationNumber,
                            customerName = it.customerName,
                            dateMillis = it.dateMillis,
                            notes = it.notes,
                            totalAmountUsd = it.totalAmountUsd,
                            status = it.status,
                            createdByUserId = it.createdByUserId,
                            createdByUsername = it.createdByUsername,
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt
                        )
                    },
                    quotationItems = items.map {
                        BackupQuotationItemRecord(
                            id = it.id,
                            quotationId = it.quotationId,
                            productId = it.productId,
                            productName = it.productName,
                            unitPriceUsd = it.unitPriceUsd,
                            quantity = it.quantity,
                            lineTotalUsd = it.lineTotalUsd
                        )
                    },
                    auditLogs = logs.map {
                        BackupAuditRecord(
                            id = it.id,
                            username = it.username,
                            operation = it.operation,
                            category = it.category,
                            recordId = it.recordId,
                            recordDescription = it.recordDescription,
                            previousValue = it.previousValue,
                            newValue = it.newValue,
                            timestamp = it.timestamp
                        )
                    },
                    themeSettings = theme,
                    rememberedUsername = rememberedUser
                )

                val jsonString = Mappers.json.encodeToString(payload)
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    out.write(jsonString.toByteArray(Charsets.UTF_8))
                } ?: error("تعذر الكتابة إلى الملف المحدد.")

                auditLogRepository.record(
                    username = actor.username,
                    operation = "إنشاء نسخة احتياطية",
                    category = "النسخ الاحتياطي",
                    recordDescription = "تصدير نسخة احتياطية شاملة (${products.size} صنف، ${quotations.size} عرض سعر)"
                )

                BackupValidationSummary(
                    payload = payload,
                    usersCount = payload.users.size,
                    productsCount = payload.products.size,
                    quotationsCount = payload.quotations.size,
                    customersCount = distinctCustomers.size,
                    auditLogsCount = payload.auditLogs.size,
                    quotationCounter = counter,
                    exportedAt = payload.exportedAt
                )
            }
        }

    suspend fun validateBackupFromUri(sourceUri: Uri): Result<BackupValidationSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val content = context.contentResolver.openInputStream(sourceUri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: error("تعذر قراءة ملف النسخة الاحتياطية.")

                val payload = Mappers.json.decodeFromString<BackupPayload>(content)
                require(payload.formatVersion == 1) {
                    "إصدار النسخة الاحتياطية غير مدعوم (${payload.formatVersion})."
                }
                require(payload.users.isNotEmpty()) {
                    "ملف النسخة الاحتياطية لا يحتوي على أي مستخدمين."
                }
                val distinctNums = payload.quotations.map { it.quotationNumber }.toSet()
                require(distinctNums.size == payload.quotations.size) {
                    "توجد أرقام عروض أسعار مكررة داخل النسخة الاحتياطية."
                }

                BackupValidationSummary(
                    payload = payload,
                    usersCount = payload.users.size,
                    productsCount = payload.products.size,
                    quotationsCount = payload.quotations.size,
                    customersCount = payload.customers.size.coerceAtLeast(
                        payload.quotations.map { it.customerName.trim() }.filter { it.isNotEmpty() }.distinct().size
                    ),
                    auditLogsCount = payload.auditLogs.size,
                    quotationCounter = maxOf(
                        payload.quotationCounter,
                        payload.quotations.maxOfOrNull { it.quotationNumber } ?: 0L
                    ),
                    exportedAt = payload.exportedAt
                )
            }
        }

    suspend fun restoreValidatedBackup(
        actor: User,
        summary: BackupValidationSummary
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canManageBackup) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية استعادة النسخة الاحتياطية."))
        }
        runCatching {
            val payload = summary.payload

            // Restore company logo image if bundled in backup
            val restoredLogoPath = payload.company?.logoBase64?.let { base64 ->
                runCatching {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val dir = File(context.filesDir, "company").apply { mkdirs() }
                    val file = File(dir, "company_logo_restored_${System.currentTimeMillis()}.png")
                    file.writeBytes(bytes)
                    file.absolutePath
                }.getOrNull()
            }

            database.withTransaction {
                database.quotationDao().deleteAllItems()
                database.quotationDao().deleteAllQuotations()
                database.quotationDao().deleteCounter()
                database.productDao().deleteAll()
                database.companyDao().deleteAll()
                database.auditLogDao().deleteAll()
                database.userDao().deleteAll()

                database.userDao().insertAll(
                    payload.users.map {
                        UserEntity(
                            id = it.id,
                            username = it.username,
                            fullName = it.fullName,
                            passwordHash = it.passwordHash,
                            roleLabel = it.roleLabel,
                            isActive = it.isActive,
                            isSuperAdmin = it.isSuperAdmin,
                            permissionsJson = it.permissionsJson,
                            createdAt = it.createdAt
                        )
                    }
                )

                payload.company?.let { comp ->
                    database.companyDao().upsertCompany(
                        CompanyEntity(
                            id = 1,
                            name = comp.name,
                            phone = comp.phone,
                            address = comp.address,
                            additionalInfo = comp.additionalInfo,
                            logoPath = restoredLogoPath,
                            updatedAt = comp.updatedAt
                        )
                    )
                }

                database.productDao().insertAll(
                    payload.products.map {
                        ProductEntity(
                            id = it.id,
                            name = it.name,
                            priceUsd = it.priceUsd,
                            createdAt = it.createdAt
                        )
                    }
                )

                database.quotationDao().insertQuotationsAll(
                    payload.quotations.map {
                        QuotationEntity(
                            id = it.id,
                            quotationNumber = it.quotationNumber,
                            customerName = it.customerName,
                            dateMillis = it.dateMillis,
                            notes = it.notes,
                            totalAmountUsd = it.totalAmountUsd,
                            status = it.status,
                            createdByUserId = it.createdByUserId,
                            createdByUsername = it.createdByUsername,
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt
                        )
                    }
                )

                database.quotationDao().insertItems(
                    payload.quotationItems.map {
                        QuotationItemEntity(
                            id = it.id,
                            quotationId = it.quotationId,
                            productId = it.productId,
                            productName = it.productName,
                            unitPriceUsd = it.unitPriceUsd,
                            quantity = it.quantity,
                            lineTotalUsd = it.lineTotalUsd
                        )
                    }
                )

                val safeCounter = maxOf(
                    payload.quotationCounter,
                    payload.quotations.maxOfOrNull { it.quotationNumber } ?: 0L
                )
                database.quotationDao().upsertCounter(
                    QuotationCounterEntity(id = 1, lastNumber = safeCounter)
                )

                database.auditLogDao().insertAll(
                    payload.auditLogs.map {
                        AuditLogEntity(
                            id = it.id,
                            username = it.username,
                            operation = it.operation,
                            category = it.category,
                            recordId = it.recordId,
                            recordDescription = it.recordDescription,
                            previousValue = it.previousValue,
                            newValue = it.newValue,
                            timestamp = it.timestamp
                        )
                    }
                )
            }

            preferencesManager.applyThemeSettings(payload.themeSettings)
            val restoredActiveUser = payload.users.firstOrNull {
                it.username.equals(actor.username, ignoreCase = true) && it.isActive
            } ?: payload.users.firstOrNull { it.isSuperAdmin && it.isActive }
                ?: payload.users.first { it.isActive }

            preferencesManager.saveSession(
                userId = restoredActiveUser.id,
                username = restoredActiveUser.username,
                rememberUsername = payload.rememberedUsername.isNotEmpty()
            )

            auditLogRepository.record(
                username = restoredActiveUser.username,
                operation = "استعادة نسخة احتياطية",
                category = "النسخ الاحتياطي",
                recordDescription = "تمت استعادة البيانات كاملة (${payload.products.size} صنف، ${payload.quotations.size} عرض سعر)"
            )
        }
    }

    suspend fun getDatabaseStorageBytes(): Long = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath("tendering_system.db")
        val dbWal = context.getDatabasePath("tendering_system.db-wal")
        val dbShm = context.getDatabasePath("tendering_system.db-shm")
        val logoDir = File(context.filesDir, "company")
        val pdfCacheDir = File(context.cacheDir, "pdfs")
        val mediaSize = logoDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } +
            pdfCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        (if (dbFile.exists()) dbFile.length() else 0L) +
            (if (dbWal.exists()) dbWal.length() else 0L) +
            (if (dbShm.exists()) dbShm.length() else 0L) +
            mediaSize
    }

    suspend fun clearPdfCache(): Long = withContext(Dispatchers.IO) {
        val pdfCacheDir = File(context.cacheDir, "pdfs")
        var freed = 0L
        if (pdfCacheDir.exists()) {
            pdfCacheDir.listFiles()?.forEach { f ->
                freed += f.length()
                f.delete()
            }
        }
        freed
    }
}
