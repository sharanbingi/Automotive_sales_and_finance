package com.automotive.salesfinance.ui.dashboard

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.ui.components.AnimatedKpiCard
import com.automotive.salesfinance.ui.components.ConfirmationDialog
import com.automotive.salesfinance.ui.components.DemoRoleSwitcherDialog
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SectionHeader
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.AuthViewModel
import com.automotive.salesfinance.viewmodel.CustomerDashboardInfo
import com.automotive.salesfinance.viewmodel.DashboardMetrics
import com.automotive.salesfinance.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    authViewModel: AuthViewModel,
    onNavigateToInventory: (String) -> Unit = {},
    onNavigateToDeadStock: () -> Unit = {},
    onNavigateToLoans: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToAddCustomer: () -> Unit = {},
    onNavigateToPayment: (String) -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onRoleSwitched: (startRoute: String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isDemoMode by authViewModel.isDemoMode.collectAsState()
    val metrics by dashboardViewModel.dashboardMetrics.collectAsState()
    val customerInfo by dashboardViewModel.customerDashboardInfo.collectAsState()
    val selectedStore by dashboardViewModel.selectedStore.collectAsState()
    val availableStores by dashboardViewModel.availableStores.collectAsState()

    val recentBikes by dashboardViewModel.recentBikes.collectAsState()
    val recentCustomers by dashboardViewModel.recentCustomers.collectAsState()
    val recentTransactions by dashboardViewModel.recentTransactions.collectAsState()

    DashboardContent(
        currentUser = currentUser,
        isDemoMode = isDemoMode,
        metrics = metrics,
        customerInfo = customerInfo,
        selectedStore = selectedStore,
        availableStores = availableStores,
        recentBikes = recentBikes,
        recentCustomers = recentCustomers,
        recentTransactions = recentTransactions,
        onStoreSelected = { storeId ->
            if (storeId == "ALL") {
                dashboardViewModel.setSelectedStore("ALL")
                dashboardViewModel.setSelectedState("ALL")
            } else {
                val found = availableStores.find { it.storeId == storeId }
                found?.let {
                    dashboardViewModel.setSelectedState(it.stateCode)
                    dashboardViewModel.setSelectedStore(it.storeId)
                }
            }
        },
        onSwitchDemoRole = { role ->
            authViewModel.switchDemoRole(role) { startRoute ->
                onRoleSwitched(startRoute)
            }
        },
        onNavigateToInventory = onNavigateToInventory,
        onNavigateToDeadStock = onNavigateToDeadStock,
        onNavigateToLoans = onNavigateToLoans,
        onNavigateToTransactions = onNavigateToTransactions,
        onNavigateToCustomers = onNavigateToCustomers,
        onNavigateToAddCustomer = onNavigateToAddCustomer,
        onNavigateToPayment = onNavigateToPayment,
        onNavigateToExpenses = onNavigateToExpenses,
        onNavigateToReports = onNavigateToReports,
        onNavigateToSettings = onNavigateToSettings,
        onLogout = onLogout
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardContent(
    currentUser: User?,
    isDemoMode: Boolean = true,
    metrics: DashboardMetrics,
    customerInfo: CustomerDashboardInfo,
    selectedStore: String,
    availableStores: List<Store>,
    recentBikes: List<Bike>,
    recentCustomers: List<Customer>,
    recentTransactions: List<Transaction>,
    onStoreSelected: (String) -> Unit,
    onSwitchDemoRole: (UserRole) -> Unit = {},
    onNavigateToInventory: (String) -> Unit = {},
    onNavigateToDeadStock: () -> Unit = {},
    onNavigateToLoans: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToAddCustomer: () -> Unit = {},
    onNavigateToPayment: (String) -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val role = currentUser?.role ?: UserRole.ADMIN
    var showRoleSwitcherDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Sign out?",
            message = "Sign out of your account?",
            confirmText = "Sign out",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = {
                showLogoutDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = getDashboardTitle(role),
                subtitle = "Welcome back, ${currentUser?.name ?: "User"}",
                stores = if (role != UserRole.CUSTOMER) listOf("ALL") + availableStores.map { it.storeId } else null,
                selectedStore = selectedStore,
                onStoreSelected = onStoreSelected,
                isDemoMode = isDemoMode,
                currentRole = currentUser?.role,
                onDemoRoleClick = { showRoleSwitcherDialog = true },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer Specific Portal View
            if (role == UserRole.CUSTOMER) {
                CustomerPortalCard(
                    customerInfo = customerInfo,
                    onPayEmiClick = { loanId -> onNavigateToPayment(loanId) }
                )
            } else {
                // Alert Banners
                AnimatedVisibility(visible = metrics.overdueLoansCount > 0, enter = fadeIn() + slideInVertically()) {
                    PremiumCard(
                        onClick = onNavigateToLoans,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        borderColor = MaterialTheme.colorScheme.error
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${metrics.overdueLoansCount} Overdue Loans Require Immediate Collection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Total overdue exposure: ${CurrencyUtils.formatCurrency(metrics.overdueLoansAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = metrics.deadStockCount > 0, enter = fadeIn() + slideInVertically()) {
                    PremiumCard(
                        onClick = onNavigateToDeadStock,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        borderColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsBike,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${metrics.deadStockCount} Dead Stock Vehicles (>60 Days Aging)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Capital tied up: ${CurrencyUtils.formatCurrency(metrics.deadStockCapital)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // 1. Business Summary Section
                SectionHeader(title = "1. Business Summary", subtitle = "Fleet size, active customers, and active loans overview")

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 2
                ) {
                    AnimatedKpiCard(
                        title = "Total Vehicles",
                        value = metrics.totalBikes.toDouble(),
                        isCurrency = false,
                        subtitle = "${metrics.availableBikes} Available | ${metrics.financedBikes} Financed",
                        icon = Icons.Rounded.DirectionsBike,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToInventory(selectedStore) }
                    )

                    AnimatedKpiCard(
                        title = "Active Loans",
                        value = metrics.activeLoansCount.toDouble(),
                        isCurrency = false,
                        subtitle = "Portfolio: ${CurrencyUtils.formatCurrency(metrics.activeLoansAmount)}",
                        icon = Icons.Rounded.ReceiptLong,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToLoans
                    )
                }

                // 2. Financial Summary Section
                SectionHeader(title = "2. Financial Summary", subtitle = "Sales volume, collections, and outstanding portfolio")

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 2
                ) {
                    AnimatedKpiCard(
                        title = "Total Collected",
                        value = metrics.totalCollectionsVolume,
                        subtitle = "Today: ${CurrencyUtils.formatCurrency(metrics.todaysCollections)}",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTransactions
                    )

                    AnimatedKpiCard(
                        title = "Outstanding Balance",
                        value = metrics.activeLoansAmount,
                        subtitle = "Overdue: ${CurrencyUtils.formatCurrency(metrics.overdueLoansAmount)}",
                        icon = Icons.Rounded.Assessment,
                        accentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToLoans
                    )
                }

                // 3. Inventory Snapshot Section
                SectionHeader(title = "3. Inventory Snapshot", subtitle = "Bike & car availability and stock distribution")

                PremiumCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InventorySnapshotRow("Total Stocked Vehicles", "${metrics.totalBikes} Units")
                        InventorySnapshotRow("Available for Sale", "${metrics.availableBikes} Units", color = MaterialTheme.colorScheme.primary)
                        InventorySnapshotRow("Reserved Units", "${metrics.reservedBikes} Units", color = MaterialTheme.colorScheme.secondary)
                        InventorySnapshotRow("Financed / Sold", "${metrics.financedBikes} Units", color = MaterialTheme.colorScheme.tertiary)
                        InventorySnapshotRow("Total Valuation", CurrencyUtils.formatCurrency(metrics.totalBikesValuation), isBold = true)
                    }
                }
            }

            // 5. Quick Actions Section
            SectionHeader(title = "Quick Actions", subtitle = "Dealership operations & shortcuts")

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 3
            ) {
                QuickActionButton(
                    label = "Add Vehicle",
                    icon = Icons.Rounded.DirectionsBike,
                    onClick = { onNavigateToInventory(selectedStore) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "Add Customer",
                    icon = Icons.Rounded.PersonAdd,
                    onClick = onNavigateToAddCustomer,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "Create Loan",
                    icon = Icons.Rounded.ReceiptLong,
                    onClick = onNavigateToLoans,
                    modifier = Modifier.weight(1f)
                )
                if (role in listOf(UserRole.DEALERSHIP_ADMIN, UserRole.STORE_MANAGER, UserRole.FINANCE_USER)) {
                    QuickActionButton(
                        label = "Store Expenses",
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        onClick = onNavigateToExpenses,
                        modifier = Modifier.weight(1f)
                    )
                }
                QuickActionButton(
                    label = "Cash Payment",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    onClick = { onNavigateToPayment("LOAN_001") },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "View Reports",
                    icon = Icons.Rounded.Assessment,
                    onClick = onNavigateToReports,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "Directory",
                    icon = Icons.Rounded.People,
                    onClick = onNavigateToCustomers,
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Recent Activity Section
            SectionHeader(title = "4. Recent Activity", subtitle = "Latest showroom transactions, customers & vehicles")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Vehicles Inwarded", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToInventory(selectedStore) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    recentBikes.take(3).forEach { bike ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${bike.make} ${bike.model} (${bike.chassisNumber})", style = MaterialTheme.typography.bodyMedium)
                            Text(CurrencyUtils.formatCurrency(bike.listedPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Customers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToCustomers() }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    recentCustomers.take(3).forEach { cust ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${cust.fullName} (${cust.phone})", style = MaterialTheme.typography.bodyMedium)
                            Text("${cust.city}, ${cust.state}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Payment Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToTransactions() }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    recentTransactions.take(3).forEach { txn ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${txn.transactionId} • ${txn.paymentMethod}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                CurrencyUtils.formatCurrency(txn.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRoleSwitcherDialog) {
        DemoRoleSwitcherDialog(
            currentRole = currentUser?.role,
            onDismissRequest = { showRoleSwitcherDialog = false },
            onRoleSelected = onSwitchDemoRole,
            isDemoMode = isDemoMode
        )
    }
}

@Composable
private fun CustomerPortalCard(
    customerInfo: CustomerDashboardInfo,
    onPayEmiClick: (String) -> Unit
) {
    PremiumCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        borderColor = MaterialTheme.colorScheme.primary
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "My Vehicle & Active Loan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (customerInfo.activeLoans.isEmpty()) {
                Text(
                    text = "No active loans found on your customer profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                val primaryLoan = customerInfo.activeLoans.first()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Loan Reference", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(primaryLoan.loanId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Next Due: ${DateUtils.formatDate(primaryLoan.nextEmiDate)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Monthly EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(
                            CurrencyUtils.formatCurrency(primaryLoan.emiAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = "Pay EMI via Razorpay UPI",
                    onClick = { onPayEmiClick(primaryLoan.loanId) },
                    icon = Icons.Rounded.Payment
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumCard(
        onClick = onClick,
        modifier = modifier,
        elevation = 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InventorySnapshotRow(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}

private fun getDashboardTitle(role: UserRole): String {
    return when (role) {
        UserRole.SUPER_ADMIN -> "Super Admin Portal"
        UserRole.DEALERSHIP_ADMIN -> "Dealership Workspace"
        UserRole.ADMIN -> "Global Executive Dashboard"
        UserRole.STATE_MANAGER -> "State Operations Hub"
        UserRole.STORE_MANAGER -> "Store Showroom Dashboard"
        UserRole.SALES_USER -> "Sales Executive Workspace"
        UserRole.FINANCE_USER -> "Finance Collections Hub"
        UserRole.CUSTOMER -> "Customer Self-Service Portal"
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun DashboardContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        DashboardContent(
            currentUser = PreviewSampleData.sampleUser,
            isDemoMode = true,
            metrics = DashboardMetrics(
                totalBikes = 42,
                availableBikes = 25,
                financedBikes = 17,
                activeLoansCount = 12,
                activeLoansAmount = 1450000.0,
                overdueLoansCount = 2,
                overdueLoansAmount = 17000.0,
                todaysCollections = 25500.0,
                totalCollectionsVolume = 890000.0,
                deadStockCount = 3,
                deadStockCapital = 320000.0
            ),
            customerInfo = CustomerDashboardInfo(activeLoans = listOf(PreviewSampleData.sampleLoan)),
            selectedStore = "ALL",
            availableStores = PreviewSampleData.sampleStores,
            recentBikes = PreviewSampleData.sampleBikes,
            recentCustomers = PreviewSampleData.sampleCustomers,
            recentTransactions = PreviewSampleData.sampleTransactions,
            onStoreSelected = {}
        )
    }
}
