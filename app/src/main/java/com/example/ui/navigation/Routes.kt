package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object HomeRoute

@Serializable
object CompanyRoute

@Serializable
object ProductsRoute

@Serializable
object QuotationsRoute

@Serializable
object MoreRoute

@Serializable
data class CreateEditQuotationRoute(val quotationId: Long = 0L)

@Serializable
data class SelectProductsRoute(val existingQuantitiesJson: String = "")

@Serializable
data class SelectedProductPayload(
    val productId: Long,
    val productName: String,
    val productCode: String,
    val unitPriceUsd: String,
    val quantity: String
)

@Serializable
data class QuotationPreviewRoute(val quotationId: Long)

@Serializable
object ReportsRoute

@Serializable
object UsersRoute

@Serializable
object AuditLogRoute

@Serializable
object SettingsRoute

@Serializable
object ThemeSettingsRoute

@Serializable
object BackupRestoreRoute
