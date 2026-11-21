package com.example.data.repository

import com.example.data.local.Mappers
import com.example.data.local.Mappers.toDomain
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserEntity
import com.example.data.preferences.PreferencesManager
import com.example.domain.model.User
import com.example.domain.model.UserPermissions
import com.example.domain.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class Error(val message: String) : AuthResult
}

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val preferencesManager: PreferencesManager,
    private val auditLogRepository: AuditLogRepository
) {
    val hasAnyUser: Flow<Boolean> = userDao.observeUserCount().map { it > 0 }

    val allUsers: Flow<List<User>> = userDao.observeAllUsers().map { list -> list.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<User?> = preferencesManager.activeUserIdFlow.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(null)
        } else {
            userDao.observeById(userId).map { entity ->
                val domain = entity?.toDomain()
                if (domain != null && domain.isActive) domain else null
            }
        }
    }

    val rememberedUsername: Flow<Pair<Boolean, String>> = preferencesManager.rememberedUsernameFlow

    suspend fun createInitialOwner(
        fullName: String,
        username: String,
        password: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val cleanName = fullName.trim()
        val cleanUsername = username.trim()
        if (cleanName.isEmpty()) return@withContext AuthResult.Error("يرجى إدخال الاسم الكامل للمدير.")
        if (cleanUsername.length < 3) return@withContext AuthResult.Error("اسم المستخدم يجب أن يكون 3 أحرف على الأقل.")
        if (password.length < 4) return@withContext AuthResult.Error("كلمة المرور يجب أن تكون 4 أحرف على الأقل.")
        if (userDao.getUserCountOnce() > 0) {
            return@withContext AuthResult.Error("تم إنشاء حساب مدير النظام مسبقاً.")
        }

        val hash = PasswordHasher.hashPassword(password)
        val permsJson = Mappers.json.encodeToString(UserPermissions.superAdmin())
        val userId = userDao.insertUser(
            UserEntity(
                username = cleanUsername,
                fullName = cleanName,
                passwordHash = hash,
                roleLabel = "مدير النظام",
                isActive = true,
                isSuperAdmin = true,
                permissionsJson = permsJson
            )
        )
        preferencesManager.saveSession(userId, cleanUsername, rememberUsername = true)
        auditLogRepository.record(
            username = cleanUsername,
            operation = "إنشاء حساب مدير النظام",
            category = "المستخدمون",
            recordId = userId.toString(),
            recordDescription = "تهيئة حساب المالك الأول: $cleanName ($cleanUsername)"
        )
        val created = userDao.getById(userId)!!.toDomain()
        AuthResult.Success(created)
    }

    suspend fun login(
        username: String,
        password: String,
        rememberUsername: Boolean
    ): AuthResult = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim()
        if (cleanUsername.isEmpty() || password.isEmpty()) {
            return@withContext AuthResult.Error("يرجى إدخال اسم المستخدم وكلمة المرور.")
        }

        val entity = userDao.getByUsername(cleanUsername)
            ?: return@withContext AuthResult.Error("اسم المستخدم أو كلمة المرور غير صحيحة.")

        if (!PasswordHasher.verifyPassword(password, entity.passwordHash)) {
            return@withContext AuthResult.Error("اسم المستخدم أو كلمة المرور غير صحيحة.")
        }

        if (!entity.isActive) {
            return@withContext AuthResult.Error("تم تعليق هذا الحساب. يرجى مراجعة مدير النظام.")
        }

        preferencesManager.saveSession(entity.id, entity.username, rememberUsername)
        auditLogRepository.record(
            username = entity.username,
            operation = "تسجيل الدخول",
            category = "المصادقة",
            recordId = entity.id.toString(),
            recordDescription = "دخول المستخدم ${entity.fullName} إلى النظام"
        )
        AuthResult.Success(entity.toDomain())
    }

    suspend fun logout(activeUser: User?) = withContext(Dispatchers.IO) {
        preferencesManager.clearActiveSession()
        if (activeUser != null) {
            auditLogRepository.record(
                username = activeUser.username,
                operation = "تسجيل الخروج",
                category = "المصادقة",
                recordId = activeUser.id.toString(),
                recordDescription = "خروج المستخدم ${activeUser.fullName}"
            )
        }
    }

    suspend fun addUser(
        actor: User,
        username: String,
        fullName: String,
        password: String,
        roleLabel: String,
        isSuperAdmin: Boolean,
        permissions: UserPermissions
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canAddUsers) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية إضافة مستخدمين."))
        }
        val cleanUsername = username.trim()
        val cleanFullName = fullName.trim()
        if (cleanUsername.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("اسم المستخدم يجب أن يكون 3 أحرف على الأقل."))
        }
        if (cleanFullName.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("يرجى إدخال الاسم الكامل."))
        }
        if (password.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("كلمة المرور يجب أن تكون 4 أحرف على الأقل."))
        }
        if (userDao.getByUsername(cleanUsername) != null) {
            return@withContext Result.failure(IllegalArgumentException("اسم المستخدم مسجل مسبقاً."))
        }

        val effectivePerms = if (isSuperAdmin) UserPermissions.superAdmin() else permissions
        val entity = UserEntity(
            username = cleanUsername,
            fullName = cleanFullName,
            passwordHash = PasswordHasher.hashPassword(password),
            roleLabel = roleLabel.ifBlank { if (isSuperAdmin) "مدير النظام" else "مستخدم" },
            isActive = true,
            isSuperAdmin = isSuperAdmin,
            permissionsJson = Mappers.json.encodeToString(effectivePerms)
        )
        val id = userDao.insertUser(entity)
        auditLogRepository.record(
            username = actor.username,
            operation = "إضافة مستخدم",
            category = "المستخدمون",
            recordId = id.toString(),
            recordDescription = "إضافة المستخدم $cleanFullName ($cleanUsername)",
            newValue = entity.roleLabel
        )
        Result.success(id)
    }

    suspend fun updateUserDetails(
        actor: User,
        targetUserId: Long,
        fullName: String,
        roleLabel: String,
        isSuperAdmin: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canEditUsers) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية تعديل المستخدمين."))
        }
        val existing = userDao.getById(targetUserId)
            ?: return@withContext Result.failure(IllegalArgumentException("المستخدم غير موجود."))
        val updated = existing.copy(
            fullName = fullName.trim().ifEmpty { existing.fullName },
            roleLabel = roleLabel.trim().ifEmpty { existing.roleLabel },
            isSuperAdmin = isSuperAdmin
        )
        userDao.updateUser(updated)
        auditLogRepository.record(
            username = actor.username,
            operation = "تعديل بيانات مستخدم",
            category = "المستخدمون",
            recordId = targetUserId.toString(),
            recordDescription = "تعديل بيانات ${existing.username}",
            previousValue = "${existing.fullName} - ${existing.roleLabel}",
            newValue = "${updated.fullName} - ${updated.roleLabel}"
        )
        Result.success(Unit)
    }

    suspend fun changeUserPassword(
        actor: User,
        targetUserId: Long,
        newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canEditUsers && actor.id != targetUserId) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية تغيير كلمة المرور."))
        }
        if (newPassword.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("كلمة المرور يجب أن تكون 4 أحرف على الأقل."))
        }
        val existing = userDao.getById(targetUserId)
            ?: return@withContext Result.failure(IllegalArgumentException("المستخدم غير موجود."))
        userDao.updateUser(existing.copy(passwordHash = PasswordHasher.hashPassword(newPassword)))
        auditLogRepository.record(
            username = actor.username,
            operation = "تغيير كلمة المرور",
            category = "المستخدمون",
            recordId = targetUserId.toString(),
            recordDescription = "تغيير كلمة المرور للمستخدم ${existing.username}"
        )
        Result.success(Unit)
    }

    suspend fun setUserActiveStatus(
        actor: User,
        targetUserId: Long,
        active: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canSuspendUsers) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية تفعيل أو تعليق المستخدمين."))
        }
        if (actor.id == targetUserId && !active) {
            return@withContext Result.failure(IllegalArgumentException("لا يمكنك تعليق حسابك الحالي."))
        }
        val existing = userDao.getById(targetUserId)
            ?: return@withContext Result.failure(IllegalArgumentException("المستخدم غير موجود."))
        userDao.updateUser(existing.copy(isActive = active))
        auditLogRepository.record(
            username = actor.username,
            operation = if (active) "تفعيل مستخدم" else "تعليق مستخدم",
            category = "المستخدمون",
            recordId = targetUserId.toString(),
            recordDescription = "${if (active) "تفعيل" else "تعليق"} حساب ${existing.username}",
            previousValue = if (existing.isActive) "نشط" else "معلق",
            newValue = if (active) "نشط" else "معلق"
        )
        Result.success(Unit)
    }

    suspend fun updateUserPermissions(
        actor: User,
        targetUserId: Long,
        newPermissions: UserPermissions
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canModifyPermissions) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية تعديل الصلاحيات."))
        }
        val existing = userDao.getById(targetUserId)
            ?: return@withContext Result.failure(IllegalArgumentException("المستخدم غير موجود."))
        val updatedJson = Mappers.json.encodeToString(newPermissions)
        userDao.updateUser(existing.copy(permissionsJson = updatedJson))
        auditLogRepository.record(
            username = actor.username,
            operation = "تعديل الصلاحيات",
            category = "المستخدمون",
            recordId = targetUserId.toString(),
            recordDescription = "تحديث مصفوفة الصلاحيات للمستخدم ${existing.username}"
        )
        Result.success(Unit)
    }

    suspend fun deleteUser(
        actor: User,
        targetUserId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!actor.effectivePermissions().canDeleteUsers) {
            return@withContext Result.failure(SecurityException("ليس لديك صلاحية حذف المستخدمين."))
        }
        if (actor.id == targetUserId) {
            return@withContext Result.failure(IllegalArgumentException("لا يمكنك حذف حسابك الحالي أثناء تسجيل الدخول."))
        }
        val existing = userDao.getById(targetUserId)
            ?: return@withContext Result.failure(IllegalArgumentException("المستخدم غير موجود."))
        if (existing.isSuperAdmin) {
            val superAdmins = userDao.getAllUsersOnce().count { it.isSuperAdmin }
            if (superAdmins <= 1) {
                return@withContext Result.failure(IllegalArgumentException("لا يمكن حذف آخر مدير نظام."))
            }
        }
        userDao.deleteUserById(targetUserId)
        auditLogRepository.record(
            username = actor.username,
            operation = "حذف مستخدم",
            category = "المستخدمون",
            recordId = targetUserId.toString(),
            recordDescription = "حذف المستخدم ${existing.fullName} (${existing.username})"
        )
        Result.success(Unit)
    }
}
