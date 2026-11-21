package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.User
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumTopBar
import com.example.ui.theme.LocalTenderingColors

@Composable
fun MoreScreen(
    currentUser: User,
    onOpenReports: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenAuditLog: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onLogout: () -> Unit
) {
    val perms = currentUser.effectivePermissions()
    val ext = LocalTenderingColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        PremiumTopBar(
            title = "المزيد",
            subtitle = "الأدوات الإدارية، التقارير، الصلاحيات، والإعدادات"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 115.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "user_card") {
                PremiumCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.fullName.take(1).ifEmpty { "م" },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${currentUser.username} • ${currentUser.roleLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (currentUser.isSuperAdmin) {
                            Surface(
                                color = ext.accent.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "مدير عام",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.accent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (perms.canViewOwnReports || perms.canViewAllReports) {
                item(key = "more_reports") {
                    MoreMenuRow(
                        title = "التقارير",
                        subtitle = "تقارير مالية، يومية، شهرية، سنوية، وحسب العميل والمستخدم",
                        icon = Icons.Default.Analytics,
                        tint = MaterialTheme.colorScheme.primary,
                        testTag = "more_reports_item",
                        onClick = onOpenReports
                    )
                }
            }

            if (perms.canViewUsers) {
                item(key = "more_users") {
                    MoreMenuRow(
                        title = "المستخدمون والصلاحيات",
                        subtitle = "إدارة الحسابات ومصفوفة الصلاحيات التفصيلية",
                        icon = Icons.Default.ManageAccounts,
                        tint = MaterialTheme.colorScheme.secondary,
                        testTag = "more_users_item",
                        onClick = onOpenUsers
                    )
                }
            }

            if (perms.canViewAuditLog) {
                item(key = "more_audit") {
                    MoreMenuRow(
                        title = "سجل العمليات",
                        subtitle = "توثيق كافة عمليات الدخول والتعديل والحذف والاستيراد",
                        icon = Icons.Default.History,
                        tint = ext.accent,
                        testTag = "more_audit_item",
                        onClick = onOpenAuditLog
                    )
                }
            }

            item(key = "more_theme") {
                MoreMenuRow(
                    title = "المظهر والألوان",
                    subtitle = "7 ثيمات احترافية، الوضع الداكن والفاتح، وألوان مخصصة",
                    icon = Icons.Default.ColorLens,
                    tint = Color(0xFF7C3AED),
                    testTag = "more_theme_item",
                    onClick = onOpenThemeSettings
                )
            }

            if (perms.canManageBackup) {
                item(key = "more_backup") {
                    MoreMenuRow(
                        title = "النسخ الاحتياطي والاستعادة",
                        subtitle = "تصدير واستعادة قاعدة البيانات والإعدادات والصور محلياً",
                        icon = Icons.Default.Backup,
                        tint = ext.success,
                        testTag = "more_backup_item",
                        onClick = onOpenBackupRestore
                    )
                }
            }

            item(key = "more_settings") {
                MoreMenuRow(
                    title = "الإعدادات",
                    subtitle = "إعدادات التطبيق وإدارة التخزين والبيانات",
                    icon = Icons.Default.Settings,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    testTag = "more_settings_item",
                    onClick = onOpenSettings
                )
            }

            item(key = "more_logout") {
                Spacer(modifier = Modifier.height(4.dp))
                MoreMenuRow(
                    title = "تسجيل الخروج",
                    subtitle = "إنهاء الجلسة الحالية بأمان",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    tint = MaterialTheme.colorScheme.error,
                    testTag = "more_logout_item",
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
fun MoreMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    PremiumCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
