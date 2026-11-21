package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.QuotationCounterEntity
import com.example.data.local.entity.QuotationEntity
import com.example.data.local.entity.QuotationItemEntity
import com.example.data.local.entity.QuotationWithItemsRelation
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY isSuperAdmin DESC, createdAt ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY isSuperAdmin DESC, createdAt ASC")
    suspend fun getAllUsersOnce(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    fun observeUserCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCountOnce(): Int

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}

@Dao
interface CompanyDao {
    @Query("SELECT * FROM company_info WHERE id = 1 LIMIT 1")
    fun observeCompany(): Flow<CompanyEntity?>

    @Query("SELECT * FROM company_info WHERE id = 1 LIMIT 1")
    suspend fun getCompanyOnce(): CompanyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompany(company: CompanyEntity)

    @Query("DELETE FROM company_info")
    suspend fun deleteAll()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC, id DESC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllProductsOnce(): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products")
    fun observeProductCount(): Flow<Int>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getByExactName(name: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM products WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}

@Dao
interface QuotationDao {
    @Transaction
    @Query("SELECT * FROM quotations ORDER BY quotationNumber DESC")
    fun observeAllQuotationsWithItems(): Flow<List<QuotationWithItemsRelation>>

    @Transaction
    @Query("SELECT * FROM quotations WHERE id = :id LIMIT 1")
    fun observeQuotationWithItems(id: Long): Flow<QuotationWithItemsRelation?>

    @Transaction
    @Query("SELECT * FROM quotations WHERE id = :id LIMIT 1")
    suspend fun getQuotationWithItemsOnce(id: Long): QuotationWithItemsRelation?

    @Query("SELECT * FROM quotations ORDER BY quotationNumber ASC")
    suspend fun getAllQuotationsOnce(): List<QuotationEntity>

    @Query("SELECT * FROM quotation_items ORDER BY quotationId ASC, id ASC")
    suspend fun getAllItemsOnce(): List<QuotationItemEntity>

    @Query("SELECT * FROM quotation_counter WHERE id = 1 LIMIT 1")
    suspend fun getCounterOnce(): QuotationCounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCounter(counter: QuotationCounterEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuotation(quotation: QuotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotationsAll(quotations: List<QuotationEntity>)

    @Update
    suspend fun updateQuotation(quotation: QuotationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<QuotationItemEntity>)

    @Query("DELETE FROM quotation_items WHERE quotationId = :quotationId")
    suspend fun deleteItemsByQuotationId(quotationId: Long)

    @Query("DELETE FROM quotations WHERE id = :quotationId")
    suspend fun deleteQuotationById(quotationId: Long)

    @Query("DELETE FROM quotation_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM quotations")
    suspend fun deleteAllQuotations()

    @Query("DELETE FROM quotation_counter")
    suspend fun deleteCounter()

    @Transaction
    suspend fun createQuotationWithAutoNumber(
        customerName: String,
        dateMillis: Long,
        notes: String,
        totalAmountUsd: String,
        status: String,
        createdByUserId: Long,
        createdByUsername: String,
        items: List<QuotationItemEntity>
    ): Pair<Long, Long> {
        val currentCounter = getCounterOnce()
        val nextNumber = (currentCounter?.lastNumber ?: 0L) + 1L
        upsertCounter(QuotationCounterEntity(id = 1, lastNumber = nextNumber))

        val now = System.currentTimeMillis()
        val quotationId = insertQuotation(
            QuotationEntity(
                quotationNumber = nextNumber,
                customerName = customerName,
                dateMillis = dateMillis,
                notes = notes,
                totalAmountUsd = totalAmountUsd,
                status = status,
                createdByUserId = createdByUserId,
                createdByUsername = createdByUsername,
                createdAt = now,
                updatedAt = now
            )
        )
        val linkedItems = items.map { it.copy(id = 0, quotationId = quotationId) }
        insertItems(linkedItems)
        return quotationId to nextNumber
    }

    @Transaction
    suspend fun updateQuotationWithItems(
        quotation: QuotationEntity,
        items: List<QuotationItemEntity>
    ) {
        updateQuotation(quotation.copy(updatedAt = System.currentTimeMillis()))
        deleteItemsByQuotationId(quotation.id)
        insertItems(items.map { it.copy(id = 0, quotationId = quotation.id) })
    }

    @Transaction
    suspend fun deleteQuotationPermanently(quotationId: Long) {
        deleteItemsByQuotationId(quotationId)
        deleteQuotationById(quotationId)
        // QuotationCounterEntity is intentionally untouched so numbers are never reused.
    }
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC, id DESC")
    fun observeAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC, id DESC")
    suspend fun getAllLogsOnce(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<AuditLogEntity>)

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAll()
}
