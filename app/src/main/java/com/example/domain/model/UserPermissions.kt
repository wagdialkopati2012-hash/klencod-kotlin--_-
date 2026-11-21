package com.example.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class UserPermissions(
    // Company
    val canViewCompany: Boolean = true,
    val canEditCompany: Boolean = false,
    val canChangeCompanyLogo: Boolean = false,
    val canDeleteCompany: Boolean = false,

    // Products
    val canViewProducts: Boolean = true,
    val canAddProducts: Boolean = false,
    val canImportExcel: Boolean = false,
    val canEditProducts: Boolean = false,
    val canDeleteProducts: Boolean = false,
    val canBulkDeleteProducts: Boolean = false,

    // Quotations
    val canCreateQuotations: Boolean = true,
    val canViewQuotations: Boolean = true,
    val canPreviewQuotations: Boolean = true,
    val canEditOwnQuotations: Boolean = true,
    val canEditAllQuotations: Boolean = false,
    val canDeleteOwnQuotations: Boolean = false,
    val canDeleteAllQuotations: Boolean = false,
    val canExportPdf: Boolean = true,
    val canPrintQuotations: Boolean = true,
    val canShareQuotations: Boolean = true,

    // Reports
    val canViewOwnReports: Boolean = true,
    val canViewAllReports: Boolean = false,
    val canViewFinancialSummaries: Boolean = false,
    val canEditFinancialRecords: Boolean = false,
    val canDeleteFinancialRecords: Boolean = false,
    val canExportReports: Boolean = false,

    // Users
    val canViewUsers: Boolean = false,
    val canAddUsers: Boolean = false,
    val canEditUsers: Boolean = false,
    val canSuspendUsers: Boolean = false,
    val canDeleteUsers: Boolean = false,
    val canModifyPermissions: Boolean = false,

    // Audit & Settings
    val canViewAuditLog: Boolean = false,
    val canManageBackup: Boolean = false,
    val canManageTheme: Boolean = true
) {
    companion object {
        fun superAdmin(): UserPermissions = UserPermissions(
            canViewCompany = true,
            canEditCompany = true,
            canChangeCompanyLogo = true,
            canDeleteCompany = true,
            canViewProducts = true,
            canAddProducts = true,
            canImportExcel = true,
            canEditProducts = true,
            canDeleteProducts = true,
            canBulkDeleteProducts = true,
            canCreateQuotations = true,
            canViewQuotations = true,
            canPreviewQuotations = true,
            canEditOwnQuotations = true,
            canEditAllQuotations = true,
            canDeleteOwnQuotations = true,
            canDeleteAllQuotations = true,
            canExportPdf = true,
            canPrintQuotations = true,
            canShareQuotations = true,
            canViewOwnReports = true,
            canViewAllReports = true,
            canViewFinancialSummaries = true,
            canEditFinancialRecords = true,
            canDeleteFinancialRecords = true,
            canExportReports = true,
            canViewUsers = true,
            canAddUsers = true,
            canEditUsers = true,
            canSuspendUsers = true,
            canDeleteUsers = true,
            canModifyPermissions = true,
            canViewAuditLog = true,
            canManageBackup = true,
            canManageTheme = true
        )

        fun standardEmployee(): UserPermissions = UserPermissions()
    }
}
