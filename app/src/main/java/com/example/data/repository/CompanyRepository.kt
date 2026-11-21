package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.Mappers.toDomain
import com.example.data.local.Mappers.toEntity
import com.example.data.local.dao.CompanyDao
import com.example.domain.model.CompanyInfo
import com.example.domain.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val companyDao: CompanyDao,
    private val auditLogRepository: AuditLogRepository
) {
    val companyInfo: Flow<CompanyInfo> = companyDao.observeCompany().map {
        it?.toDomain() ?: CompanyInfo()
    }

    suspend fun saveCompanyInfo(
        actor: User,
        name: String,
        phone: String,
        address: String,
        additionalInfo: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canEditCompany) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية تعديل بيانات الشركة."))
        }
        val existing = companyDao.getCompanyOnce()?.toDomain() ?: CompanyInfo()
        val updated = existing.copy(
            name = name.trim(),
            phone = phone.trim(),
            address = address.trim(),
            additionalInfo = additionalInfo.trim(),
            updatedAt = System.currentTimeMillis()
        )
        companyDao.upsertCompany(updated.toEntity())
        auditLogRepository.record(
            username = actor.username,
            operation = "تحديث بيانات الشركة",
            category = "بيانات الشركة",
            recordId = "1",
            recordDescription = "تحديث بيانات الشركة: ${updated.name.ifBlank { "بدون اسم" }}",
            previousValue = existing.name,
            newValue = updated.name
        )
        Result.success(Unit)
    }

    suspend fun saveCompanyLogo(actor: User, sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canChangeCompanyLogo) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية تغيير شعار الشركة."))
        }
        val dir = File(context.filesDir, "company").apply { mkdirs() }
        val targetFile = File(dir, "company_logo_${System.currentTimeMillis()}.png")

        val existing = companyDao.getCompanyOnce()?.toDomain() ?: CompanyInfo()
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة ملف الصورة المحدد."))

        // Delete old logo file if present
        existing.logoPath?.let { oldPath ->
            runCatching {
                val oldFile = File(oldPath)
                if (oldFile.exists() && oldFile.absolutePath != targetFile.absolutePath) {
                    oldFile.delete()
                }
            }
        }

        val updated = existing.copy(
            logoPath = targetFile.absolutePath,
            updatedAt = System.currentTimeMillis()
        )
        companyDao.upsertCompany(updated.toEntity())
        auditLogRepository.record(
            username = actor.username,
            operation = "تغيير شعار الشركة",
            category = "بيانات الشركة",
            recordId = "1",
            recordDescription = "تم رفع وتحديث شعار الشركة"
        )
        Result.success(targetFile.absolutePath)
    }

    suspend fun deleteCompanyLogo(actor: User): Result<Unit> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canDeleteCompany && !actor.effectivePermissions().canChangeCompanyLogo) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية حذف شعار الشركة."))
        }
        val existing = companyDao.getCompanyOnce()?.toDomain() ?: CompanyInfo()
        existing.logoPath?.let { path ->
            runCatching { File(path).delete() }
        }
        val updated = existing.copy(logoPath = null, updatedAt = System.currentTimeMillis())
        companyDao.upsertCompany(updated.toEntity())
        auditLogRepository.record(
            username = actor.username,
            operation = "حذف شعار الشركة",
            category = "بيانات الشركة",
            recordId = "1",
            recordDescription = "تم حذف شعار الشركة"
        )
        Result.success(Unit)
    }
}
