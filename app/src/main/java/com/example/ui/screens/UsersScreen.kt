package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.User
import com.example.domain.model.UserPermissions
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumTopBar
import com.example.ui.theme.LocalTenderingColors
import com.example.ui.viewmodel.UsersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    currentUser: User,
    onBack: () -> Unit,
    viewModel: UsersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val perms = currentUser.effectivePermissions()
    val ext = LocalTenderingColors.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<User?>(null) }
    var passwordTarget by remember { mutableStateOf<User?>(null) }
    var permissionsTarget by remember { mutableStateOf<User?>(null) }
    var deleteTarget by remember { mutableStateOf<User?>(null) }

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PremiumTopBar(
                title = "المستخدمون والصلاحيات",
                subtitle = "${state.users.size} مستخدم مسجل",
                onBack = onBack
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchChange,
                    placeholder = { Text("البحث عن مستخدم...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = state.message != null) {
                    Surface(
                        color = ext.success.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.message.orEmpty(),
                                color = ext.success,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::clearBanners, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = ext.success)
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = state.error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::clearBanners, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.users, key = { it.id }) { user ->
                    PremiumCard(modifier = Modifier.testTag("user_card_${user.username}")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.fullName.take(1).ifEmpty { "م" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.fullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "@${user.username} • ${user.roleLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (user.isActive) ext.success.copy(alpha = 0.14f) else MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (user.isActive) "نشط" else "معلق",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (user.isActive) ext.success else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (perms.canModifyPermissions && !user.isSuperAdmin) {
                                IconButton(
                                    onClick = { permissionsTarget = user },
                                    modifier = Modifier.testTag("permissions_user_${user.username}")
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = "الصلاحيات", tint = ext.accent)
                                }
                            }
                            if (perms.canEditUsers) {
                                IconButton(onClick = { editingUser = user }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل")
                                }
                                IconButton(onClick = { passwordTarget = user }) {
                                    Icon(Icons.Default.Key, contentDescription = "تغيير كلمة المرور")
                                }
                            }
                            if (perms.canSuspendUsers && user.id != currentUser.id) {
                                IconButton(onClick = { viewModel.toggleUserActive(currentUser, user) }) {
                                    Icon(
                                        imageVector = if (user.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                                        contentDescription = if (user.isActive) "تعليق" else "تفعيل",
                                        tint = if (user.isActive) ext.warning else ext.success
                                    )
                                }
                            }
                            if (perms.canDeleteUsers && user.id != currentUser.id) {
                                IconButton(onClick = { deleteTarget = user }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (perms.canAddUsers) {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة مستخدم") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, bottom = 28.dp)
                    .testTag("add_user_fab")
            )
        }
    }

    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onSave = { username, fullName, password, roleLabel, isSuperAdmin ->
                viewModel.addUser(
                    actor = currentUser,
                    username = username,
                    fullName = fullName,
                    password = password,
                    roleLabel = roleLabel,
                    isSuperAdmin = isSuperAdmin,
                    permissions = if (isSuperAdmin) UserPermissions.superAdmin() else UserPermissions.standardEmployee()
                ) {
                    showAddDialog = false
                }
            }
        )
    }

    editingUser?.let { target ->
        EditUserDialog(
            user = target,
            onDismiss = { editingUser = null },
            onSave = { fullName, roleLabel, isSuperAdmin ->
                viewModel.updateUserDetails(currentUser, target.id, fullName, roleLabel, isSuperAdmin) {
                    editingUser = null
                }
            }
        )
    }

    passwordTarget?.let { target ->
        ChangePasswordDialog(
            user = target,
            onDismiss = { passwordTarget = null },
            onSave = { newPass ->
                viewModel.changePassword(currentUser, target.id, newPass) {
                    passwordTarget = null
                }
            }
        )
    }

    permissionsTarget?.let { target ->
        PermissionMatrixSheet(
            user = target,
            onDismiss = { permissionsTarget = null },
            onSave = { updatedPerms ->
                viewModel.updatePermissions(currentUser, target.id, updatedPerms) {
                    permissionsTarget = null
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("تأكيد حذف المستخدم") },
            text = { Text("هل أنت متأكد من حذف حساب «${target.fullName}» نهائياً؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(currentUser, target)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun AddUserDialog(
    onDismiss: () -> Unit,
    onSave: (username: String, fullName: String, password: String, roleLabel: String, isSuperAdmin: Boolean) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var roleLabel by remember { mutableStateOf("موظف مبيعات") }
    var isSuperAdmin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مستخدم جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roleLabel,
                    onValueChange = { roleLabel = it },
                    label = { Text("المسمى الوظيفي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("صلاحيات مدير عام (Super Admin)")
                    Switch(checked = isSuperAdmin, onCheckedChange = { isSuperAdmin = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(username, fullName, password, roleLabel, isSuperAdmin) }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun EditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (fullName: String, roleLabel: String, isSuperAdmin: Boolean) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var roleLabel by remember { mutableStateOf(user.roleLabel) }
    var isSuperAdmin by remember { mutableStateOf(user.isSuperAdmin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات ${user.username}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roleLabel,
                    onValueChange = { roleLabel = it },
                    label = { Text("المسمى الوظيفي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("مدير عام (Super Admin)")
                    Switch(checked = isSuperAdmin, onCheckedChange = { isSuperAdmin = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(fullName, roleLabel, isSuperAdmin) }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newPass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغيير كلمة المرور — ${user.username}") },
        text = {
            OutlinedTextField(
                value = newPass,
                onValueChange = { newPass = it },
                label = { Text("كلمة المرور الجديدة") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSave(newPass) }) { Text("تحديث") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionMatrixSheet(
    user: User,
    onDismiss: () -> Unit,
    onSave: (UserPermissions) -> Unit
) {
    var p by remember(user.id) { mutableStateOf(user.permissions) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "مصفوفة الصلاحيات — ${user.fullName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            PermGroupTitle("الشركة")
            PermToggle("عرض بيانات الشركة", p.canViewCompany) { p = p.copy(canViewCompany = it) }
            PermToggle("تعديل بيانات الشركة", p.canEditCompany) { p = p.copy(canEditCompany = it) }
            PermToggle("تغيير الشعار", p.canChangeCompanyLogo) { p = p.copy(canChangeCompanyLogo = it) }
            PermToggle("حذف بيانات/شعار الشركة", p.canDeleteCompany) { p = p.copy(canDeleteCompany = it) }

            HorizontalDivider()
            PermGroupTitle("الأصناف")
            PermToggle("عرض الأصناف", p.canViewProducts) { p = p.copy(canViewProducts = it) }
            PermToggle("إضافة أصناف", p.canAddProducts) { p = p.copy(canAddProducts = it) }
            PermToggle("استيراد Excel", p.canImportExcel) { p = p.copy(canImportExcel = it) }
            PermToggle("تعديل الأصناف", p.canEditProducts) { p = p.copy(canEditProducts = it) }
            PermToggle("حذف صنف", p.canDeleteProducts) { p = p.copy(canDeleteProducts = it) }
            PermToggle("حذف جماعي للأصناف", p.canBulkDeleteProducts) { p = p.copy(canBulkDeleteProducts = it) }

            HorizontalDivider()
            PermGroupTitle("عروض الأسعار")
            PermToggle("إنشاء عرض سعر", p.canCreateQuotations) { p = p.copy(canCreateQuotations = it) }
            PermToggle("عرض عروض الأسعار", p.canViewQuotations) { p = p.copy(canViewQuotations = it) }
            PermToggle("معاينة A4", p.canPreviewQuotations) { p = p.copy(canPreviewQuotations = it) }
            PermToggle("تعديل عروضه فقط", p.canEditOwnQuotations) { p = p.copy(canEditOwnQuotations = it) }
            PermToggle("تعديل جميع العروض", p.canEditAllQuotations) { p = p.copy(canEditAllQuotations = it) }
            PermToggle("حذف عروضه فقط", p.canDeleteOwnQuotations) { p = p.copy(canDeleteOwnQuotations = it) }
            PermToggle("حذف جميع العروض", p.canDeleteAllQuotations) { p = p.copy(canDeleteAllQuotations = it) }
            PermToggle("تصدير PDF", p.canExportPdf) { p = p.copy(canExportPdf = it) }
            PermToggle("طباعة", p.canPrintQuotations) { p = p.copy(canPrintQuotations = it) }
            PermToggle("مشاركة", p.canShareQuotations) { p = p.copy(canShareQuotations = it) }

            HorizontalDivider()
            PermGroupTitle("التقارير")
            PermToggle("عرض تقاريره فقط", p.canViewOwnReports) { p = p.copy(canViewOwnReports = it) }
            PermToggle("عرض جميع التقارير", p.canViewAllReports) { p = p.copy(canViewAllReports = it) }
            PermToggle("عرض الملخصات المالية", p.canViewFinancialSummaries) { p = p.copy(canViewFinancialSummaries = it) }
            PermToggle("تعديل السجلات المالية", p.canEditFinancialRecords) { p = p.copy(canEditFinancialRecords = it) }
            PermToggle("حذف السجلات المالية", p.canDeleteFinancialRecords) { p = p.copy(canDeleteFinancialRecords = it) }
            PermToggle("تصدير التقارير", p.canExportReports) { p = p.copy(canExportReports = it) }

            HorizontalDivider()
            PermGroupTitle("المستخدمون والنظام")
            PermToggle("عرض المستخدمين", p.canViewUsers) { p = p.copy(canViewUsers = it) }
            PermToggle("إضافة مستخدم", p.canAddUsers) { p = p.copy(canAddUsers = it) }
            PermToggle("تعديل مستخدم", p.canEditUsers) { p = p.copy(canEditUsers = it) }
            PermToggle("تعليق مستخدم", p.canSuspendUsers) { p = p.copy(canSuspendUsers = it) }
            PermToggle("حذف مستخدم", p.canDeleteUsers) { p = p.copy(canDeleteUsers = it) }
            PermToggle("تعديل الصلاحيات", p.canModifyPermissions) { p = p.copy(canModifyPermissions = it) }
            PermToggle("عرض سجل العمليات", p.canViewAuditLog) { p = p.copy(canViewAuditLog = it) }
            PermToggle("النسخ الاحتياطي والاستعادة", p.canManageBackup) { p = p.copy(canManageBackup = it) }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onSave(p) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("حفظ الصلاحيات", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PermGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun PermToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}
