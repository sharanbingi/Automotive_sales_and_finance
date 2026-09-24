package com.automotive.salesfinance.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.automotive.salesfinance.AutomotiveApplication
import com.automotive.salesfinance.data.AppContainer
import com.automotive.salesfinance.ui.admin.DealershipAdminDashboardScreen
import com.automotive.salesfinance.ui.admin.DealershipDetailsScreen
import com.automotive.salesfinance.ui.admin.SuperAdminDashboardScreen
import com.automotive.salesfinance.ui.audit.AuditLogScreen
import com.automotive.salesfinance.ui.auth.LoginScreen
import com.automotive.salesfinance.ui.customer.AddCustomerScreen
import com.automotive.salesfinance.ui.customer.CustomerDetailsScreen
import com.automotive.salesfinance.ui.customer.CustomerScreen
import com.automotive.salesfinance.ui.dashboard.DashboardScreen
import com.automotive.salesfinance.ui.expense.ExpenseScreen
import com.automotive.salesfinance.ui.finance.EmiCalculatorScreen
import com.automotive.salesfinance.ui.finance.FinanceScreen
import com.automotive.salesfinance.ui.finance.LoanApplicationScreen
import com.automotive.salesfinance.ui.finance.LoanDetailsScreen
import com.automotive.salesfinance.ui.finance.PaymentScreen
import com.automotive.salesfinance.ui.finance.TransactionScreen
import com.automotive.salesfinance.ui.inventory.AddBikeScreen
import com.automotive.salesfinance.ui.inventory.BikeDetailsScreen
import com.automotive.salesfinance.ui.inventory.DeadStockScreen
import com.automotive.salesfinance.ui.inventory.InventoryScreen
import com.automotive.salesfinance.ui.reports.ReportsScreen
import com.automotive.salesfinance.ui.settings.SettingsScreen
import com.automotive.salesfinance.ui.subscription.SubscriptionScreen
import com.automotive.salesfinance.ui.support.SupportTicketScreen
import com.automotive.salesfinance.viewmodel.AuthViewModel
import com.automotive.salesfinance.viewmodel.CustomerViewModel
import com.automotive.salesfinance.viewmodel.DashboardViewModel
import com.automotive.salesfinance.viewmodel.DealershipAdminViewModel
import com.automotive.salesfinance.viewmodel.ExpenseViewModel
import com.automotive.salesfinance.viewmodel.FinanceViewModel
import com.automotive.salesfinance.viewmodel.InventoryViewModel
import com.automotive.salesfinance.viewmodel.SubscriptionViewModel
import com.automotive.salesfinance.viewmodel.SuperAdminViewModel
import com.automotive.salesfinance.viewmodel.SupportViewModel
import com.automotive.salesfinance.viewmodel.TransactionViewModel
import com.automotive.salesfinance.viewmodel.ViewModelFactory

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    appContainer: AppContainer = (LocalContext.current.applicationContext as AutomotiveApplication).appContainer,
    viewModelFactory: ViewModelFactory = ViewModelFactory(appContainer),
    authViewModel: AuthViewModel = viewModel(factory = viewModelFactory),
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val startDestination = currentUser?.let {
        authViewModel.getStartDestinationForRole(it.role)
    } ?: NavRoutes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            ) + fadeIn(animationSpec = tween(350))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            ) + fadeOut(animationSpec = tween(350))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            ) + fadeIn(animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            ) + fadeOut(animationSpec = tween(350))
        }
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { role ->
                    val destination = authViewModel.getStartDestinationForRole(role)
                    navController.navigate(destination) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // Super Admin Dashboard Route
        composable(NavRoutes.SUPER_ADMIN_DASHBOARD) {
            val superAdminViewModel: SuperAdminViewModel = viewModel(factory = viewModelFactory)
            SuperAdminDashboardScreen(
                viewModel = superAdminViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDealershipDetails = { dId -> navController.navigate(NavRoutes.dealershipDetails(dId)) },
                onNavigateToSubscriptions = { navController.navigate(NavRoutes.SUBSCRIPTION_MANAGEMENT) },
                onNavigateToAuditLogs = { navController.navigate(NavRoutes.AUDIT_LOGS) },
                onNavigateToSupportTickets = { navController.navigate(NavRoutes.SUPPORT_TICKETS) },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // Dealership Admin Dashboard Route
        composable(NavRoutes.DEALERSHIP_ADMIN_DASHBOARD) {
            val dealershipAdminViewModel: DealershipAdminViewModel = viewModel(factory = viewModelFactory)
            DealershipAdminDashboardScreen(
                viewModel = dealershipAdminViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubscription = { navController.navigate(NavRoutes.SUBSCRIPTION_MANAGEMENT) },
                onNavigateToInventory = { navController.navigate(NavRoutes.inventoryList("ALL")) },
                onNavigateToCustomers = { navController.navigate(NavRoutes.CUSTOMERS) },
                onNavigateToFinance = { navController.navigate(NavRoutes.LOAN_LIST) },
                onNavigateToReports = { navController.navigate(NavRoutes.REPORTS) },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // Expense Management Route
        composable(NavRoutes.EXPENSES) {
            val expenseViewModel: ExpenseViewModel = viewModel(factory = viewModelFactory)
            ExpenseScreen(
                expenseViewModel = expenseViewModel,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Dealership Details Route
        composable(
            route = NavRoutes.DEALERSHIP_DETAILS,
            arguments = listOf(navArgument("dealershipId") { type = NavType.StringType })
        ) { backStackEntry ->
            val dId = backStackEntry.arguments?.getString("dealershipId")
            if (dId.isNullOrBlank()) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            } else {
                val superAdminViewModel: SuperAdminViewModel = viewModel(factory = viewModelFactory)
                val dealershipAdminViewModel: DealershipAdminViewModel = viewModel(factory = viewModelFactory)
                DealershipDetailsScreen(
                    dealershipId = dId,
                    superAdminViewModel = superAdminViewModel,
                    dealershipAdminViewModel = dealershipAdminViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Subscription Management Route
        composable(NavRoutes.SUBSCRIPTION_MANAGEMENT) {
            val subscriptionViewModel: SubscriptionViewModel = viewModel(factory = viewModelFactory)
            SubscriptionScreen(
                viewModel = subscriptionViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Audit Trail Route
        composable(NavRoutes.AUDIT_LOGS) {
            val superAdminViewModel: SuperAdminViewModel = viewModel(factory = viewModelFactory)
            AuditLogScreen(
                superAdminViewModel = superAdminViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Support Tickets Route
        composable(NavRoutes.SUPPORT_TICKETS) {
            val supportViewModel: SupportViewModel = viewModel(factory = viewModelFactory)
            SupportTicketScreen(
                viewModel = supportViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Role Dashboards mapped to DashboardScreen
        listOf(
            NavRoutes.ADMIN_DASHBOARD,
            NavRoutes.STATE_MANAGER_DASHBOARD,
            NavRoutes.STORE_MANAGER_DASHBOARD,
            NavRoutes.SALES_DASHBOARD,
            NavRoutes.FINANCE_DASHBOARD,
            NavRoutes.CUSTOMER_PORTAL
        ).forEach { dashboardRoute ->
            composable(dashboardRoute) {
                val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
                DashboardScreen(
                    dashboardViewModel = dashboardViewModel,
                    authViewModel = authViewModel,
                    onNavigateToInventory = { storeId -> navController.navigate(NavRoutes.inventoryList(storeId)) },
                    onNavigateToDeadStock = { navController.navigate(NavRoutes.DEAD_STOCK_REPORT) },
                    onNavigateToLoans = { navController.navigate(NavRoutes.LOAN_LIST) },
                    onNavigateToTransactions = { navController.navigate(NavRoutes.TRANSACTIONS) },
                    onNavigateToCustomers = { navController.navigate(NavRoutes.CUSTOMERS) },
                    onNavigateToAddCustomer = { navController.navigate(NavRoutes.ADD_CUSTOMER) },
                    onNavigateToPayment = { loanId -> navController.navigate(NavRoutes.payment(loanId)) },
                    onNavigateToExpenses = { navController.navigate(NavRoutes.EXPENSES) },
                    onNavigateToReports = { navController.navigate(NavRoutes.REPORTS) },
                    onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                    onRoleSwitched = { destination ->
                        navController.navigate(destination) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Inventory Routes
        composable(
            route = NavRoutes.INVENTORY_LIST,
            arguments = listOf(navArgument("storeId") { type = NavType.StringType; defaultValue = "ALL" })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: "ALL"
            val inventoryViewModel: InventoryViewModel = viewModel(factory = viewModelFactory)
            val subscriptionViewModel: SubscriptionViewModel = viewModel(factory = viewModelFactory)
            LaunchedEffect(storeId) {
                if (storeId != "ALL") {
                    inventoryViewModel.setSelectedStore(storeId)
                }
            }
            InventoryScreen(
                viewModel = inventoryViewModel,
                subscriptionViewModel = subscriptionViewModel,
                onNavigateBack = { navController.popBackStack() },
                onBikeClick = { bikeId -> navController.navigate(NavRoutes.bikeDetail(bikeId)) },
                onAddBikeClick = { navController.navigate(NavRoutes.bikeForm("NEW")) },
                onDeadStockClick = { navController.navigate(NavRoutes.DEAD_STOCK_REPORT) }
            )
        }

        composable(
            route = NavRoutes.BIKE_DETAIL,
            arguments = listOf(navArgument("bikeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bikeId = backStackEntry.arguments?.getString("bikeId") ?: ""
            val inventoryViewModel: InventoryViewModel = viewModel(factory = viewModelFactory)
            BikeDetailsScreen(
                bikeId = bikeId,
                viewModel = inventoryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { id -> navController.navigate(NavRoutes.bikeForm(id)) },
                onFinanceClick = { id -> navController.navigate(NavRoutes.loanApplication(id)) }
            )
        }

        composable(
            route = NavRoutes.BIKE_FORM,
            arguments = listOf(navArgument("bikeId") { type = NavType.StringType; defaultValue = "NEW" })
        ) { backStackEntry ->
            val bikeId = backStackEntry.arguments?.getString("bikeId") ?: "NEW"
            val inventoryViewModel: InventoryViewModel = viewModel(factory = viewModelFactory)
            val subscriptionViewModel: SubscriptionViewModel = viewModel(factory = viewModelFactory)
            AddBikeScreen(
                bikeId = bikeId,
                viewModel = inventoryViewModel,
                subscriptionViewModel = subscriptionViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.DEAD_STOCK_REPORT) {
            val inventoryViewModel: InventoryViewModel = viewModel(factory = viewModelFactory)
            DeadStockScreen(
                viewModel = inventoryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onBikeClick = { bikeId -> navController.navigate(NavRoutes.bikeDetail(bikeId)) }
            )
        }

        // Finance Routes
        composable(NavRoutes.LOAN_LIST) {
            val financeViewModel: FinanceViewModel = viewModel(factory = viewModelFactory)
            FinanceScreen(
                viewModel = financeViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLoanClick = { loanId -> navController.navigate(NavRoutes.loanDetail(loanId)) },
                onEmiCalculatorClick = { navController.navigate(NavRoutes.EMI_CALCULATOR) },
                onNewLoanClick = { navController.navigate(NavRoutes.loanApplication("NEW")) },
                onExpensesClick = { navController.navigate(NavRoutes.EXPENSES) }
            )
        }

        composable(
            route = NavRoutes.LOAN_APPLICATION,
            arguments = listOf(navArgument("bikeId") { type = NavType.StringType; defaultValue = "NEW" })
        ) { backStackEntry ->
            val bikeId = backStackEntry.arguments?.getString("bikeId") ?: "NEW"
            val financeViewModel: FinanceViewModel = viewModel(factory = viewModelFactory)
            val inventoryViewModel: InventoryViewModel = viewModel(factory = viewModelFactory)
            LoanApplicationScreen(
                bikeId = bikeId,
                financeViewModel = financeViewModel,
                inventoryViewModel = inventoryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLoanCreated = { loanId ->
                    navController.navigate(NavRoutes.loanDetail(loanId)) {
                        popUpTo(NavRoutes.LOAN_LIST)
                    }
                }
            )
        }

        composable(
            route = NavRoutes.LOAN_DETAIL,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: ""
            val financeViewModel: FinanceViewModel = viewModel(factory = viewModelFactory)
            LoanDetailsScreen(
                loanId = loanId,
                viewModel = financeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.EMI_CALCULATOR) {
            EmiCalculatorScreen(
                onNavigateBack = { navController.popBackStack() },
                onApplyFinancing = { _, _, _, _ ->
                    navController.navigate(NavRoutes.loanApplication("NEW"))
                }
            )
        }

        // Payment Route
        composable(
            route = NavRoutes.PAYMENT,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType; defaultValue = "LOAN_001" })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getString("loanId") ?: "LOAN_001"
            val financeViewModel: FinanceViewModel = viewModel(factory = viewModelFactory)
            PaymentScreen(
                loanId = loanId,
                financeViewModel = financeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Transactions Ledger Route
        composable(NavRoutes.TRANSACTIONS) {
            val transactionViewModel: TransactionViewModel = viewModel(factory = viewModelFactory)
            TransactionScreen(
                viewModel = transactionViewModel,
                onNavigateBack = { navController.popBackStack() },
                onTransactionClick = { txn ->
                    if (txn.loanId.isNotBlank()) {
                        navController.navigate(NavRoutes.payment(txn.loanId))
                    }
                }
            )
        }

        // Customer Routes
        composable(NavRoutes.CUSTOMERS) {
            val customerViewModel: CustomerViewModel = viewModel(factory = viewModelFactory)
            val subscriptionViewModel: SubscriptionViewModel = viewModel(factory = viewModelFactory)
            CustomerScreen(
                viewModel = customerViewModel,
                subscriptionViewModel = subscriptionViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCustomerClick = { customerId -> navController.navigate(NavRoutes.customerDetail(customerId)) },
                onAddCustomerClick = { navController.navigate(NavRoutes.ADD_CUSTOMER) }
            )
        }

        composable(NavRoutes.ADD_CUSTOMER) {
            val customerViewModel: CustomerViewModel = viewModel(factory = viewModelFactory)
            val subscriptionViewModel: SubscriptionViewModel = viewModel(factory = viewModelFactory)
            AddCustomerScreen(
                viewModel = customerViewModel,
                subscriptionViewModel = subscriptionViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCustomerCreated = { created ->
                    navController.navigate(NavRoutes.customerDetail(created.customerId)) {
                        popUpTo(NavRoutes.CUSTOMERS)
                    }
                }
            )
        }

        composable(
            route = NavRoutes.CUSTOMER_DETAIL,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            val customerViewModel: CustomerViewModel = viewModel(factory = viewModelFactory)
            val financeViewModel: FinanceViewModel = viewModel(factory = viewModelFactory)
            CustomerDetailsScreen(
                customerId = customerId,
                viewModel = customerViewModel,
                financeViewModel = financeViewModel,
                onNavigateBack = { navController.popBackStack() },
                onInitiatePaymentClick = { loanId -> navController.navigate(NavRoutes.payment(loanId)) }
            )
        }

        // Reports Dashboard Route
        composable(NavRoutes.REPORTS) {
            val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
            ReportsScreen(
                dashboardViewModel = dashboardViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings Route
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                authViewModel = authViewModel,
                storeRepository = appContainer.storeRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubscription = { navController.navigate(NavRoutes.SUBSCRIPTION_MANAGEMENT) },
                onNavigateToAuditLogs = { navController.navigate(NavRoutes.AUDIT_LOGS) },
                onNavigateToSupportTickets = { navController.navigate(NavRoutes.SUPPORT_TICKETS) },
                onLogoutConfirmed = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0)
                    }
                },
                onRoleSwitched = { destination ->
                    navController.navigate(destination) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
