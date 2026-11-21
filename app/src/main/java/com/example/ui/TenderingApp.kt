package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.domain.model.CompanyInfo
import com.example.domain.model.User
import com.example.ui.components.CapsuleNavItem
import com.example.ui.components.FloatingCapsuleNavigationBar
import com.example.ui.navigation.AuditLogRoute
import com.example.ui.navigation.BackupRestoreRoute
import com.example.ui.navigation.CompanyRoute
import com.example.ui.navigation.CreateEditQuotationRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.MoreRoute
import com.example.ui.navigation.ProductsRoute
import com.example.ui.navigation.QuotationPreviewRoute
import com.example.ui.navigation.QuotationsRoute
import com.example.ui.navigation.ReportsRoute
import com.example.ui.navigation.SelectProductsRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.ThemeSettingsRoute
import com.example.ui.navigation.UsersRoute
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CompanyScreen
import com.example.ui.screens.CreateEditQuotationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.ProductSelectionScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.QuotationPreviewScreen
import com.example.ui.screens.QuotationsListScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemeSettingsScreen
import com.example.ui.screens.UsersScreen
import com.example.ui.viewmodel.SessionState
import kotlinx.serialization.json.Json

private const val KEY_SELECTED_PRODUCTS_RESULT = "selected_products_result_json"

@Composable
fun TenderingAppRoot(
    sessionState: SessionState,
    companyInfo: CompanyInfo,
    onLogout: () -> Unit
) {
    when (sessionState) {
        is SessionState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SessionState.Unauthenticated -> {
            LoginScreen(
                needsInitialOwnerSetup = sessionState.needsInitialOwnerSetup,
                companyInfo = companyInfo
            )
        }
        is SessionState.Authenticated -> {
            AuthenticatedAppShell(
                currentUser = sessionState.user,
                companyInfo = companyInfo,
                onLogout = onLogout
            )
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    currentUser: User,
    companyInfo: CompanyInfo,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val perms = currentUser.effectivePermissions()

    var openAddProductImmediately by remember { mutableStateOf(false) }

    val capsuleItems = remember(perms) {
        buildList {
            add(CapsuleNavItem("home", "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"))
            if (perms.canViewCompany) {
                add(CapsuleNavItem("company", "بيانات الشركة", Icons.Filled.Business, Icons.Outlined.Business, "nav_company"))
            }
            if (perms.canViewProducts) {
                add(CapsuleNavItem("products", "الأصناف", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_products"))
            }
            if (perms.canViewQuotations) {
                add(CapsuleNavItem("quotations", "عروض الأسعار", Icons.Filled.RequestQuote, Icons.Outlined.RequestQuote, "nav_quotations"))
            }
            add(CapsuleNavItem("more", "المزيد", Icons.Filled.Menu, Icons.Outlined.Menu, "nav_more"))
        }
    }

    val currentTabId = when {
        destination?.hasRoute<HomeRoute>() == true -> "home"
        destination?.hasRoute<CompanyRoute>() == true -> "company"
        destination?.hasRoute<ProductsRoute>() == true -> "products"
        destination?.hasRoute<QuotationsRoute>() == true -> "quotations"
        destination?.hasRoute<MoreRoute>() == true -> "more"
        else -> null
    }

    fun navigateToTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                enterTransition = {
                    slideInHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetX = { it / 6 }
                    ) + fadeIn()
                },
                exitTransition = { fadeOut() },
                popEnterTransition = {
                    slideInHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetX = { -it / 6 }
                    ) + fadeIn()
                },
                popExitTransition = { fadeOut() }
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        currentUser = currentUser,
                        onCreateQuotation = {
                            if (perms.canCreateQuotations) {
                                navController.navigate(CreateEditQuotationRoute(0L))
                            }
                        },
                        onAddProduct = {
                            if (perms.canAddProducts) {
                                openAddProductImmediately = true
                                navigateToTopLevel(ProductsRoute)
                            }
                        },
                        onViewQuotations = {
                            if (perms.canViewQuotations) {
                                navigateToTopLevel(QuotationsRoute)
                            }
                        },
                        onViewReports = {
                            if (perms.canViewOwnReports || perms.canViewAllReports) {
                                navController.navigate(ReportsRoute)
                            }
                        },
                        onOpenAuditLog = {
                            if (perms.canViewAuditLog) {
                                navController.navigate(AuditLogRoute)
                            }
                        },
                        onPreviewQuotation = { id ->
                            if (perms.canPreviewQuotations) {
                                navController.navigate(QuotationPreviewRoute(id))
                            }
                        }
                    )
                }

                composable<CompanyRoute> {
                    CompanyScreen(
                        currentUser = currentUser,
                        companyInfo = companyInfo
                    )
                }

                composable<ProductsRoute> {
                    ProductsScreen(
                        currentUser = currentUser,
                        openAddImmediately = openAddProductImmediately,
                        onConsumeOpenAdd = { openAddProductImmediately = false }
                    )
                }

                composable<QuotationsRoute> {
                    QuotationsListScreen(
                        currentUser = currentUser,
                        companyInfo = companyInfo,
                        onCreateQuotation = {
                            if (perms.canCreateQuotations) {
                                navController.navigate(CreateEditQuotationRoute(0L))
                            }
                        },
                        onEditQuotation = { id ->
                            navController.navigate(CreateEditQuotationRoute(id))
                        },
                        onPreviewQuotation = { id ->
                            if (perms.canPreviewQuotations) {
                                navController.navigate(QuotationPreviewRoute(id))
                            }
                        }
                    )
                }

                composable<MoreRoute> {
                    MoreScreen(
                        currentUser = currentUser,
                        onOpenReports = {
                            if (perms.canViewOwnReports || perms.canViewAllReports) {
                                navController.navigate(ReportsRoute)
                            }
                        },
                        onOpenUsers = {
                            if (perms.canViewUsers) {
                                navController.navigate(UsersRoute)
                            }
                        },
                        onOpenAuditLog = {
                            if (perms.canViewAuditLog) {
                                navController.navigate(AuditLogRoute)
                            }
                        },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                        onOpenThemeSettings = { navController.navigate(ThemeSettingsRoute) },
                        onOpenBackupRestore = {
                            if (perms.canManageBackup) {
                                navController.navigate(BackupRestoreRoute)
                            }
                        },
                        onLogout = onLogout
                    )
                }

                composable<CreateEditQuotationRoute> { entry ->
                    val args = entry.toRoute<CreateEditQuotationRoute>()
                    val pendingSelectedJson by entry.savedStateHandle
                        .getStateFlow<String?>(KEY_SELECTED_PRODUCTS_RESULT, null)
                        .collectAsStateWithLifecycle()
                    CreateEditQuotationScreen(
                        quotationId = args.quotationId,
                        currentUser = currentUser,
                        pendingSelectedProductsJson = pendingSelectedJson,
                        onConsumeSelectedProducts = {
                            entry.savedStateHandle[KEY_SELECTED_PRODUCTS_RESULT] = null
                        },
                        onOpenProductSelection = { existingJson ->
                            navController.navigate(SelectProductsRoute(existingJson))
                        },
                        onBack = { navController.popBackStack() },
                        onSaved = { savedId, openPreview ->
                            if (openPreview && perms.canPreviewQuotations) {
                                navController.navigate(QuotationPreviewRoute(savedId)) {
                                    popUpTo<CreateEditQuotationRoute> { inclusive = true }
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    )
                }

                composable<SelectProductsRoute> { entry ->
                    val args = entry.toRoute<SelectProductsRoute>()
                    ProductSelectionScreen(
                        existingQuantitiesJson = args.existingQuantitiesJson,
                        onBack = { navController.popBackStack() },
                        onConfirmSelection = { selectedList ->
                            val encoded = Json.encodeToString(selectedList)
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(KEY_SELECTED_PRODUCTS_RESULT, encoded)
                            navController.popBackStack()
                        }
                    )
                }

                composable<QuotationPreviewRoute> { entry ->
                    val args = entry.toRoute<QuotationPreviewRoute>()
                    QuotationPreviewScreen(
                        quotationId = args.quotationId,
                        currentUser = currentUser,
                        companyInfo = companyInfo,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<ReportsRoute> {
                    ReportsScreen(
                        currentUser = currentUser,
                        onBack = { navController.popBackStack() },
                        onPreviewQuotation = { id ->
                            if (perms.canPreviewQuotations) {
                                navController.navigate(QuotationPreviewRoute(id))
                            }
                        }
                    )
                }

                composable<UsersRoute> {
                    UsersScreen(
                        currentUser = currentUser,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<AuditLogRoute> {
                    AuditLogScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<SettingsRoute> {
                    SettingsScreen(
                        currentUser = currentUser,
                        onBack = { navController.popBackStack() },
                        onOpenThemeSettings = { navController.navigate(ThemeSettingsRoute) },
                        onOpenCompany = {
                            if (perms.canViewCompany) {
                                navigateToTopLevel(CompanyRoute)
                            }
                        },
                        onOpenBackupRestore = {
                            if (perms.canManageBackup) {
                                navController.navigate(BackupRestoreRoute)
                            }
                        },
                        onLogout = onLogout
                    )
                }

                composable<ThemeSettingsRoute> {
                    ThemeSettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<BackupRestoreRoute> {
                    BackupRestoreScreen(
                        currentUser = currentUser,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Floating Capsule Navigation Bar shown only on top-level tabs
            AnimatedVisibility(
                visible = currentTabId != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingCapsuleNavigationBar(
                    items = capsuleItems,
                    selectedId = currentTabId.orEmpty(),
                    onSelect = { item ->
                        when (item.id) {
                            "home" -> navigateToTopLevel(HomeRoute)
                            "company" -> navigateToTopLevel(CompanyRoute)
                            "products" -> navigateToTopLevel(ProductsRoute)
                            "quotations" -> navigateToTopLevel(QuotationsRoute)
                            "more" -> navigateToTopLevel(MoreRoute)
                        }
                    }
                )
            }
        }
    }
}
