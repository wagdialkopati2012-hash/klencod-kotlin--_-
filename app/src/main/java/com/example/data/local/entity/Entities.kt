package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val passwordHash: String,
    val roleLabel: String,
    val isActive: Boolean = true,
    val isSuperAdmin: Boolean = false,
    val permissionsJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "company_info")
data class CompanyEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val phone: String,
    val address: String,
    val additionalInfo: String,
    val logoPath: String?,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["name"])]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val priceUsd: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quotation_counter")
@Serializable
data class QuotationCounterEntity(
    @PrimaryKey val id: Int = 1,
    val lastNumber: Long = 0
)

@Entity(
    tableName = "quotations",
    indices = [
        Index(value = ["quotationNumber"], unique = true),
        Index(value = ["customerName"]),
        Index(value = ["dateMillis"])
    ]
)
data class QuotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

@Entity(
    tableName = "quotation_items",
    foreignKeys = [
        ForeignKey(
            entity = QuotationEntity::class,
            parentColumns = ["id"],
            childColumns = ["quotationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["quotationId"])]
)
data class QuotationItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quotationId: Long,
    val productId: Long?,
    val productName: String,
    val unitPriceUsd: String,
    val quantity: String,
    val lineTotalUsd: String
)

data class QuotationWithItemsRelation(
    @Embedded val quotation: QuotationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "quotationId"
    )
    val items: List<QuotationItemEntity>
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["timestamp"]), Index(value = ["category"])]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val operation: String,
    val category: String,
    val recordId: String,
    val recordDescription: String,
    val previousValue: String,
    val newValue: String,
    val timestamp: Long = System.currentTimeMillis()
)
