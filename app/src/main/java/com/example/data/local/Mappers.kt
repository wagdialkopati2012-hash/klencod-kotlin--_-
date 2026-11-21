package com.example.data.local

import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.QuotationEntity
import com.example.data.local.entity.QuotationItemEntity
import com.example.data.local.entity.QuotationWithItemsRelation
import com.example.data.local.entity.UserEntity
import com.example.domain.model.AuditLogEntry
import com.example.domain.model.CompanyInfo
import com.example.domain.model.Product
import com.example.domain.model.Quotation
import com.example.domain.model.QuotationItem
import com.example.domain.model.QuotationStatus
import com.example.domain.model.QuotationWithItems
import com.example.domain.model.User
import com.example.domain.model.UserPermissions
import kotlinx.serialization.json.Json

object Mappers {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun UserEntity.toDomain(): User {
        val perms = runCatching {
            json.decodeFromString<UserPermissions>(permissionsJson)
        }.getOrElse {
            if (isSuperAdmin) UserPermissions.superAdmin() else UserPermissions.standardEmployee()
        }
        return User(
            id = id,
            username = username,
            fullName = fullName,
            roleLabel = roleLabel,
            isActive = isActive,
            isSuperAdmin = isSuperAdmin,
            permissions = perms,
            createdAt = createdAt
        )
    }

    fun CompanyEntity.toDomain(): CompanyInfo = CompanyInfo(
        id = id,
        name = name,
        phone = phone,
        address = address,
        additionalInfo = additionalInfo,
        logoPath = logoPath,
        updatedAt = updatedAt
    )

    fun CompanyInfo.toEntity(): CompanyEntity = CompanyEntity(
        id = 1,
        name = name,
        phone = phone,
        address = address,
        additionalInfo = additionalInfo,
        logoPath = logoPath,
        updatedAt = updatedAt
    )

    fun ProductEntity.toDomain(): Product = Product(
        id = id,
        name = name,
        priceUsd = priceUsd,
        createdAt = createdAt
    )

    fun Product.toEntity(): ProductEntity = ProductEntity(
        id = id,
        name = name,
        priceUsd = priceUsd,
        createdAt = createdAt
    )

    fun QuotationEntity.toDomain(): Quotation = Quotation(
        id = id,
        quotationNumber = quotationNumber,
        customerName = customerName,
        dateMillis = dateMillis,
        notes = notes,
        totalAmountUsd = totalAmountUsd,
        status = runCatching { QuotationStatus.valueOf(status) }.getOrDefault(QuotationStatus.SAVED),
        createdByUserId = createdByUserId,
        createdByUsername = createdByUsername,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun QuotationItemEntity.toDomain(): QuotationItem = QuotationItem(
        id = id,
        quotationId = quotationId,
        productId = productId,
        productName = productName,
        unitPriceUsd = unitPriceUsd,
        quantity = quantity,
        lineTotalUsd = lineTotalUsd
    )

    fun QuotationWithItemsRelation.toDomain(): QuotationWithItems = QuotationWithItems(
        quotation = quotation.toDomain(),
        items = items.map { it.toDomain() }
    )

    fun AuditLogEntity.toDomain(): AuditLogEntry = AuditLogEntry(
        id = id,
        username = username,
        operation = operation,
        category = category,
        recordId = recordId,
        recordDescription = recordDescription,
        previousValue = previousValue,
        newValue = newValue,
        timestamp = timestamp
    )
}
