package com.example.data.repository

import com.example.data.local.Mappers.toDomain
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.entity.AuditLogEntity
import com.example.domain.model.AuditLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogRepository @Inject constructor(
    private val auditLogDao: AuditLogDao
) {
    fun observeAllLogs(): Flow<List<AuditLogEntry>> =
        auditLogDao.observeAllLogs().map { list -> list.map { it.toDomain() } }

    suspend fun record(
        username: String,
        operation: String,
        category: String,
        recordId: String = "",
        recordDescription: String,
        previousValue: String = "",
        newValue: String = ""
    ) {
        auditLogDao.insertLog(
            AuditLogEntity(
                username = username,
                operation = operation,
                category = category,
                recordId = recordId,
                recordDescription = recordDescription,
                previousValue = previousValue,
                newValue = newValue,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
