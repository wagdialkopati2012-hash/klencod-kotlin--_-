package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.CompanyDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.QuotationDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.QuotationCounterEntity
import com.example.data.local.entity.QuotationEntity
import com.example.data.local.entity.QuotationItemEntity
import com.example.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CompanyEntity::class,
        ProductEntity::class,
        QuotationCounterEntity::class,
        QuotationEntity::class,
        QuotationItemEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun companyDao(): CompanyDao
    abstract fun productDao(): ProductDao
    abstract fun quotationDao(): QuotationDao
    abstract fun auditLogDao(): AuditLogDao
}
