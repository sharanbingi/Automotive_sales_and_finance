package com.automotive.salesfinance.ui.admin

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.ui.components.AnimatedKpiCard
import com.automotive.salesfinance.ui.components.ConfirmationDialog
import com.automotive.salesfinance.ui.components.EmptyState
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTextField
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SecondaryButton
import com.automotive.salesfinance.ui.components.SectionHeader
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AppSpacing
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.viewmodel.DealershipAdminMetrics
import com.automotive.salesfinance.viewmodel.DealershipAdminViewModel

/**
 * Premium Dealership Admin Dashboard Screen.
 * Provides executive overview, inventory & sales KPI metrics, store outlet management,
 * team member access control, and trial subscription status.
 */
@Composable
fun DealershipAdminDashboardScreen(
    viewModel: DealershipAdminViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val currentDealership by viewModel.currentDealership.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val users by viewModel.users.collectAsState()
    val trialDaysRemaining by viewModel.trialDaysRemaining.collectAsState()
    val isTrialActive by viewModel.isTrialActive.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    DealershipAdminDashboardContent(
        currentDealership = currentDealership,
        metrics = metrics,
        stores = stores,
        users = users,
        trialDaysRemaining = trialDaysRemaining,
        isTrialActive = isTrialActive,
        snackbarHostState = snackbarHostState,
        onToggleUserActiveStatus = { viewModel.toggleUserActiveStatus(it) },
        onSaveStore = { store, onComplete -> viewModel.saveStore(store, onComplete) },
        onSaveUser = { user, onComplete -> viewModel.saveUser(user, onComplete) },
        onUpdateSupportedVehicleTypes = { types -> viewModel.updateSupportedVehicleTypes(types) },
        onNavigateBack = onNavigateBack,
        onNavigateToSubscription = onNavigateToSubscription,
        onNavigateToInventory = onNavigateToInventory,
        onNavigateToCustomers = onNavigateToCustomers,
        onNavigateToFinance = onNavigateToFinance,
        onNavigateToReports = onNavigateToReports,
        onLogout = onLogout
    )
}

@Composable
fun DealershipAdminDashboardContent(
    currentDealership: Dealership?,
    metrics: DealershipAdminMetrics,
    stores: List<Store>,
    users: List<User>,
    trialDaysRemaining: Int,
    isTrialActive: Boolean,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onToggleUserActiveStatus: (String) -> Unit,
    onSaveStore: (Store, () -> Unit) -> Unit,
    onSaveUser: (User, () -> Unit) -> Unit,
    onUpdateSupportedVehicleTypes: (List<VehicleType>) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview Metrics", "Store Outlets (${stores.size})", "Team Staff (${users.size})")

    var showAddStoreDialog by remember { mutableStateOf(false) }
    var storeToEdit by remember { mutableStateOf<Store?>(null) }

    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Sign Out Confirmation",
            message = "Are you sure you want to sign out of your dealership workspace?",
            confirmText = "Sign Out",
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PremiumTopBar(
                title = currentDealership?.name ?: "Loading Dealership...",
                subtitle = if (currentDealership != null) {
                    "Plan: ${currentDealership.subscriptionPlan} • Status: ${currentDealership.subscriptionStatus.name}"
                } else {
                    "Loading workspace details..."
                },
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = { storeToEdit = null; showAddStoreDialog = true },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add Store Outlet")
                }
            } else if (selectedTabIndex == 2) {
                FloatingActionButton(
                    onClick = { userToEdit = null; showAddUserDialog = true },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Rounded.PersonAdd, contentDescription = "Invite Team Member")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TrialExpirationBanner(
                dealership = currentDealership,
                trialDaysRemaining = trialDaysRemaining,
                isTrialActive = isTrialActive,
                onUpgradeClick = onNavigateToSubscription
            )

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> ScopedMetricsGridSection(
                    currentDealership = currentDealership,
                    metrics = metrics,
                    onUpdateSupportedVehicleTypes = onUpdateSupportedVehicleTypes,
                    onNavigateToInventory = onNavigateToInventory,
                    onNavigateToCustomers = onNavigateToCustomers,
                    onNavigateToFinance = onNavigateToFinance,
                    onNavigateToReports = onNavigateToReports
                )
                1 -> StoresManagementSection(
                    stores = stores,
                    onEditStore = { s ->
                        storeToEdit = s
                        showAddStoreDialog = true
                    }
                )
                2 -> UsersManagementSection(
                    users = users,
                    onToggleActive = onToggleUserActiveStatus,
                    onEditUser = { u ->
                        userToEdit = u
                        showAddUserDialog = true
                    }
                )
            }
        }
    }

    if (showAddStoreDialog) {
        StoreDialog(
            existingStore = storeToEdit,
            onDismiss = { showAddStoreDialog = false; storeToEdit = null },
            onSave = { store ->
                onSaveStore(store) {
                    showAddStoreDialog = false
                    storeToEdit = null
                }
            }
        )
    }

    if (showAddUserDialog) {
        UserDialog(
            existingUser = userToEdit,
            stores = stores,
            onDismiss = { showAddUserDialog = false; userToEdit = null },
            onSave = { user ->
                onSaveUser(user) {
                    showAddUserDialog = false
                    userToEdit = null
                }
            }
        )
    }
}

@Composable
private fun TrialExpirationBanner(
    dealership: Dealership?,
    trialDaysRemaining: Int,
    isTrialActive: Boolean,
    onUpgradeClick: () -> Unit
) {
    if (dealership == null) return

    val isExpired = (dealership.subscriptionStatus == SubscriptionStatus.EXPIRED) ||
            (dealership.subscriptionStatus == SubscriptionStatus.SUSPENDED)

    if (isTrialActive || isExpired) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.medium),
            backgroundColor = if (isExpired) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isExpired) "Subscription Expired" else "14-Day Free Trial Active",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = if (isExpired) "Account in read-only mode." else "$trialDaysRemaining days remaining in trial period.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isExpired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                PrimaryButton(
                    text = "Upgrade Plan",
                    onClick = onUpgradeClick,
                    modifier = Modifier.width(135.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScopedMetricsGridSection(
    currentDealership: Dealership?,
    metrics: DealershipAdminMetrics,
    onUpdateSupportedVehicleTypes: (List<VehicleType>) -> Unit = {},
    onNavigateToInventory: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToReports: () -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            // Section 1: Overview & Reports Entry Point
            SectionHeader(
                title = "Overview & Business Analytics",
                subtitle = "Executive indicators & reporting hub"
            )

            // Reports & Analytics Card
            PremiumCard(
                onClick = onNavigateToReports,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                borderColor = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Assessment,
                            contentDescription = "Analytics Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.medium))
                        Column {
                            Text(
                                text = "Business Analytics & Reports Suite",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "View sales volume, stock valuation, EMI risk & daily collections",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Section 2: Supported Vehicle Categories Config
            currentDealership?.let { d ->
                var bikesChecked by remember(d.supportedVehicleTypes) {
                    mutableStateOf(d.supportedVehicleTypes.contains(VehicleType.BIKE))
                }
                var carsChecked by remember(d.supportedVehicleTypes) {
                    mutableStateOf(d.supportedVehicleTypes.contains(VehicleType.CAR))
                }

                PremiumCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                    ) {
                        Text(
                            text = "Supported Vehicle Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Configure inventory types enabled for this dealership showroom:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Checkbox(
                                    checked = bikesChecked,
                                    onCheckedChange = { checked ->
                                        if (checked || carsChecked) {
                                            bikesChecked = checked
                                            val types = mutableListOf<VehicleType>()
                                            if (bikesChecked) types.add(VehicleType.BIKE)
                                            if (carsChecked) types.add(VehicleType.CAR)
                                            onUpdateSupportedVehicleTypes(types)
                                        }
                                    }
                                )
                                Text(
                                    text = "Bikes / Two Wheelers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Checkbox(
                                    checked = carsChecked,
                                    onCheckedChange = { checked ->
                                        if (checked || bikesChecked) {
                                            carsChecked = checked
                                            val types = mutableListOf<VehicleType>()
                                            if (bikesChecked) types.add(VehicleType.BIKE)
                                            if (carsChecked) types.add(VehicleType.CAR)
                                            onUpdateSupportedVehicleTypes(types)
                                        }
                                    }
                                )
                                Text(
                                    text = "Cars / Four Wheelers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Inventory & Customer Indicators
            SectionHeader(
                title = "Inventory & Customer Indicators",
                subtitle = "Stock availability & active customer directory"
            )

            if (isTablet) {
                // Tablet Multi-Column KPI Grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    maxItemsInEachRow = 3
                ) {
                    AnimatedKpiCard(
                        title = "Vehicles Inventory",
                        value = metrics.totalBikes.toDouble(),
                        isCurrency = false,
                        subtitle = "${metrics.availableBikes} Avail | ${metrics.financedBikes} Sold",
                        icon = Icons.Rounded.TwoWheeler,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToInventory
                    )

                    AnimatedKpiCard(
                        title = "Customer Profiles",
                        value = metrics.totalCustomers.toDouble(),
                        isCurrency = false,
                        subtitle = "Active Directory",
                        icon = Icons.Rounded.People,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCustomers
                    )

                    AnimatedKpiCard(
                        title = "Showroom Outlets",
                        value = metrics.totalStoresCount.toDouble(),
                        isCurrency = false,
                        subtitle = "Active Outlets",
                        icon = Icons.Rounded.Storefront,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Phone 2-Column Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedKpiCard(
                        title = "Vehicles Inventory",
                        value = metrics.totalBikes.toDouble(),
                        isCurrency = false,
                        subtitle = "${metrics.availableBikes} Avail | ${metrics.financedBikes} Sold",
                        icon = Icons.Rounded.TwoWheeler,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToInventory
                    )

                    AnimatedKpiCard(
                        title = "Customer Profiles",
                        value = metrics.totalCustomers.toDouble(),
                        isCurrency = false,
                        subtitle = "Active Directory",
                        icon = Icons.Rounded.People,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCustomers
                    )
                }
            }

            // Section 4: Finance & Collections Indicators
            SectionHeader(
                title = "Finance & Collections Indicators",
                subtitle = "Active loan contracts & today's processed collections"
            )

            if (isTablet) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    maxItemsInEachRow = 3
                ) {
                    AnimatedKpiCard(
                        title = "Active Portfolio",
                        value = metrics.activeLoansCount.toDouble(),
                        isCurrency = false,
                        subtitle = "${metrics.overdueLoansCount} Overdue Contracts",
                        icon = Icons.Rounded.MonetizationOn,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToFinance
                    )

                    AnimatedKpiCard(
                        title = "Today's Collections",
                        value = metrics.todaysCollections,
                        subtitle = "Processed Payments Today",
                        icon = Icons.Rounded.Receipt,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToFinance
                    )

                    AnimatedKpiCard(
                        title = "Team Members",
                        value = metrics.totalUsersCount.toDouble(),
                        isCurrency = false,
                        subtitle = "Staff Accounts",
                        icon = Icons.Rounded.Group,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedKpiCard(
                        title = "Active Portfolio",
                        value = metrics.activeLoansCount.toDouble(),
                        isCurrency = false,
                        subtitle = "${metrics.overdueLoansCount} Overdue Contracts",
                        icon = Icons.Rounded.MonetizationOn,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToFinance
                    )

                    AnimatedKpiCard(
                        title = "Today's Collections",
                        value = metrics.todaysCollections,
                        subtitle = "Processed Payments Today",
                        icon = Icons.Rounded.Receipt,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToFinance
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedKpiCard(
                        title = "Showroom Outlets",
                        value = metrics.totalStoresCount.toDouble(),
                        isCurrency = false,
                        subtitle = "Active Outlets",
                        icon = Icons.Rounded.Storefront,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )

                    AnimatedKpiCard(
                        title = "Team Members",
                        value = metrics.totalUsersCount.toDouble(),
                        isCurrency = false,
                        subtitle = "Staff Accounts",
                        icon = Icons.Rounded.Group,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 5: Dead Stock Warning
            if (metrics.deadStockCount > 0) {
                PremiumCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    borderColor = MaterialTheme.colorScheme.error
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics {
                            contentDescription = "Dead stock alert: ${metrics.deadStockCount} vehicles exceeding 60 days in stock. Tied capital: ${CurrencyUtils.formatCurrency(metrics.deadStockCapital)}."
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.medium))
                        Column {
                            Text(
                                text = "Dead Stock Alert: ${metrics.deadStockCount} Vehicles (>60 Days Aging)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Tied Capital: ${CurrencyUtils.formatCurrency(metrics.deadStockCapital)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoresManagementSection(
    stores: List<Store>,
    onEditStore: (Store) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.medium)
    ) {
        SectionHeader(title = "Showroom Store Outlets")
        Spacer(modifier = Modifier.height(AppSpacing.small))

        if (stores.isEmpty()) {
            EmptyState(title = "No Store Outlets Found", subtitle = "Click '+' below to register a new store showroom.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                items(stores, key = { it.storeId }) { store ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                        PremiumCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(store.storeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("ID: ${store.storeId} • State: ${store.stateCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = { onEditStore(store) },
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    ) {
                                        Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Edit Store ${store.storeName}")
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.small), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Text("Address: ${store.address}, ${store.city}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersManagementSection(
    users: List<User>,
    onToggleActive: (String) -> Unit,
    onEditUser: (User) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.medium)
    ) {
        SectionHeader(title = "Dealership Staff & User Accounts")
        Spacer(modifier = Modifier.height(AppSpacing.small))

        if (users.isEmpty()) {
            EmptyState(title = "No Staff Accounts Found", subtitle = "Click '+' below to invite team members.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                items(users, key = { it.uid }) { user ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                        PremiumCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("${user.email} • Role: ${user.role.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = user.active,
                                            onCheckedChange = { onToggleActive(user.uid) },
                                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        IconButton(
                                            onClick = { onEditUser(user) },
                                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        ) {
                                            Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Edit User ${user.name}")
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.small), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Text("Assigned Store: ${user.storeId} • State: ${user.stateCode}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreDialog(
    existingStore: Store?,
    onDismiss: () -> Unit,
    onSave: (Store) -> Unit
) {
    var storeName by remember { mutableStateOf(existingStore?.storeName ?: "") }
    var stateCode by remember { mutableStateOf(existingStore?.stateCode ?: "TG") }
    var city by remember { mutableStateOf(existingStore?.city ?: "") }
    var address by remember { mutableStateOf(existingStore?.address ?: "") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingStore == null) "Add New Store Outlet" else "Edit Store Outlet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                if (localError != null) {
                    Text(
                        text = localError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                PremiumTextField(value = storeName, onValueChange = { storeName = it; localError = null }, label = "Store Name *")
                PremiumTextField(value = stateCode, onValueChange = { stateCode = it; localError = null }, label = "State Code (e.g., TG/KA) *")
                PremiumTextField(value = city, onValueChange = { city = it; localError = null }, label = "City *")
                PremiumTextField(value = address, onValueChange = { address = it; localError = null }, label = "Street Address *", singleLine = false)
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Save Store",
                onClick = {
                    if (storeName.isBlank() || stateCode.isBlank() || city.isBlank() || address.isBlank()) {
                        localError = "Please fill in all store details"
                        return@PrimaryButton
                    }
                    val store = Store(
                        storeId = existingStore?.storeId ?: "",
                        storeName = storeName.trim(),
                        stateCode = stateCode.trim().uppercase(),
                        city = city.trim(),
                        address = address.trim(),
                        active = existingStore?.active ?: true
                    )
                    onSave(store)
                }
            )
        },
        dismissButton = { SecondaryButton(text = "Cancel", onClick = onDismiss) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDialog(
    existingUser: User?,
    stores: List<Store>,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var name by remember { mutableStateOf(existingUser?.name ?: "") }
    var email by remember { mutableStateOf(existingUser?.email ?: "") }
    var selectedRole by remember { mutableStateOf(existingUser?.role ?: UserRole.SALES_USER) }
    var selectedStoreId by remember { mutableStateOf(existingUser?.storeId ?: stores.firstOrNull()?.storeId ?: "ALL") }

    var roleExpanded by remember { mutableStateOf(false) }
    var storeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingUser == null) "Invite Team Member" else "Edit User Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                PremiumTextField(value = name, onValueChange = { name = it }, label = "Full Name *")
                PremiumTextField(value = email, onValueChange = { email = it }, label = "Email Address *")

                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    PremiumTextField(
                        value = selectedRole.name,
                        onValueChange = {},
                        readOnly = true,
                        label = "User Role *",
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = {
                                    selectedRole = role
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = storeExpanded,
                    onExpandedChange = { storeExpanded = !storeExpanded }
                ) {
                    PremiumTextField(
                        value = selectedStoreId,
                        onValueChange = {},
                        readOnly = true,
                        label = "Assigned Store *",
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = storeExpanded,
                        onDismissRequest = { storeExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("ALL Stores") },
                            onClick = {
                                selectedStoreId = "ALL"
                                storeExpanded = false
                            }
                        )
                        stores.forEach { store ->
                            DropdownMenuItem(
                                text = { Text("${store.storeName} (${store.storeId})") },
                                onClick = {
                                    selectedStoreId = store.storeId
                                    storeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Save User",
                onClick = {
                    val user = User(
                        uid = existingUser?.uid ?: "",
                        name = name,
                        email = email,
                        role = selectedRole,
                        storeId = selectedStoreId,
                        stateCode = stores.find { it.storeId == selectedStoreId }?.stateCode ?: "ALL",
                        active = existingUser?.active ?: true
                    )
                    onSave(user)
                },
                enabled = name.isNotBlank() && email.isNotBlank()
            )
        },
        dismissButton = { SecondaryButton(text = "Cancel", onClick = onDismiss) }
    )
}

@Preview(name = "Phone Light Mode", showBackground = true)
@Preview(name = "Phone Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet Landscape", device = Devices.TABLET)
@Composable
private fun DealershipAdminDashboardContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        DealershipAdminDashboardContent(
            currentDealership = PreviewSampleData.sampleDealership,
            metrics = DealershipAdminMetrics(
                totalBikes = 42,
                availableBikes = 25,
                financedBikes = 17,
                totalCustomers = 30,
                activeLoansCount = 12,
                overdueLoansCount = 2,
                todaysCollections = 25500.0,
                deadStockCount = 3,
                deadStockCapital = 320000.0,
                totalStoresCount = 2,
                totalUsersCount = 5
            ),
            stores = PreviewSampleData.sampleStores,
            users = PreviewSampleData.sampleUsers,
            trialDaysRemaining = 10,
            isTrialActive = true,
            onToggleUserActiveStatus = {},
            onSaveStore = { _, _ -> },
            onSaveUser = { _, _ -> }
        )
    }
}
