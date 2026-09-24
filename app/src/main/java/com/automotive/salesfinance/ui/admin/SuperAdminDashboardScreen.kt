package com.automotive.salesfinance.ui.admin

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.automotive.salesfinance.model.AuditLog
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.SupportTicket
import com.automotive.salesfinance.model.TicketStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.model.VehicleType
import androidx.compose.material.icons.automirrored.filled.Logout
import com.automotive.salesfinance.ui.components.AnimatedKpiCard
import com.automotive.salesfinance.ui.components.ConfirmationDialog
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.EmptyState
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTextField
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SearchBar
import com.automotive.salesfinance.ui.components.SecondaryButton
import com.automotive.salesfinance.ui.components.SectionHeader
import com.automotive.salesfinance.ui.components.SubscriptionStatusBadge
import com.automotive.salesfinance.ui.components.TicketStatusBadge
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.viewmodel.PlatformStats
import com.automotive.salesfinance.viewmodel.SuperAdminViewModel

@Composable
fun SuperAdminDashboardScreen(
    viewModel: SuperAdminViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToDealershipDetails: (String) -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToAuditLogs: () -> Unit = {},
    onNavigateToSupportTickets: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsState()

    if (currentUser != null && currentUser?.role != UserRole.SUPER_ADMIN) {
        AccessDeniedScreen(onNavigateBack = onNavigateBack)
        return
    }

    val stats by viewModel.platformStats.collectAsState()
    val dealerships by viewModel.filteredDealerships.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val plans by viewModel.plans.collectAsState()

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

    SuperAdminDashboardContent(
        currentUser = currentUser,
        stats = stats,
        dealerships = dealerships,
        searchQuery = searchQuery,
        statusFilter = statusFilter,
        supportTickets = supportTickets,
        auditLogs = auditLogs,
        plans = plans,
        snackbarHostState = snackbarHostState,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onStatusFilterChange = { viewModel.setStatusFilter(it) },
        onActivateDealership = { viewModel.activateDealership(it) },
        onSuspendDealership = { viewModel.suspendDealership(it) },
        onExtendTrial = { viewModel.extendTrial(it) },
        onUpdateDealershipPlan = { dealershipId, planId -> viewModel.updateDealershipPlan(dealershipId, planId) },
        onSaveStore = { dealershipId, storeName, stateCode, city, address, onComplete ->
            viewModel.saveStore(dealershipId, storeName, stateCode, city, address, onSuccess = onComplete)
        },
        onUpdatePlan = { plan -> viewModel.updatePlan(plan) },
        onUpdateSupportedVehicleTypes = { id, types -> viewModel.updateSupportedVehicleTypes(id, types) },
        onPostAdminResponse = { ticketId, text, status -> viewModel.postAdminResponse(ticketId, text, status) },
        onCreateDealership = { name, legalName, email, phone, address, city, state, supportedVehicleTypes, subscriptionPlan, trialDays, storeName, storeCity, storeState, storeAddress, adminName, adminEmail, adminPhone, onComplete ->
            viewModel.createDealership(
                name = name,
                legalName = legalName,
                email = email,
                phone = phone,
                address = address,
                city = city,
                state = state,
                supportedVehicleTypes = supportedVehicleTypes,
                subscriptionPlan = subscriptionPlan,
                trialDays = trialDays,
                storeName = storeName,
                storeCity = storeCity,
                storeState = storeState,
                storeAddress = storeAddress,
                adminName = adminName,
                adminEmail = adminEmail,
                adminPhone = adminPhone,
                onSuccess = onComplete
            )
        },
        onNavigateBack = onNavigateBack,
        onNavigateToDealershipDetails = onNavigateToDealershipDetails,
        onNavigateToSubscriptions = onNavigateToSubscriptions,
        onNavigateToAuditLogs = onNavigateToAuditLogs,
        onNavigateToSupportTickets = onNavigateToSupportTickets,
        onLogout = onLogout
    )
}

@Composable
fun SuperAdminDashboardContent(
    currentUser: User?,
    stats: PlatformStats,
    dealerships: List<Dealership>,
    searchQuery: String,
    statusFilter: String,
    supportTickets: List<SupportTicket>,
    auditLogs: List<AuditLog>,
    plans: List<SubscriptionPlan>,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onActivateDealership: (String) -> Unit,
    onSuspendDealership: (String) -> Unit,
    onExtendTrial: (String) -> Unit,
    onUpdateDealershipPlan: (String, String) -> Unit,
    onSaveStore: (dealershipId: String, storeName: String, stateCode: String, city: String, address: String, onComplete: () -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onUpdatePlan: (SubscriptionPlan) -> Unit,
    onUpdateSupportedVehicleTypes: (String, List<VehicleType>) -> Unit = { _, _ -> },
    onPostAdminResponse: (String, String, TicketStatus?) -> Unit,
    onCreateDealership: (
        name: String,
        legalName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        state: String,
        supportedVehicleTypes: List<VehicleType>,
        subscriptionPlan: String,
        trialDays: Int,
        storeName: String,
        storeCity: String,
        storeState: String,
        storeAddress: String,
        adminName: String,
        adminEmail: String,
        adminPhone: String,
        onComplete: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onNavigateBack: () -> Unit = {},
    onNavigateToDealershipDetails: (String) -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToAuditLogs: () -> Unit = {},
    onNavigateToSupportTickets: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    if (currentUser != null && currentUser.role != UserRole.SUPER_ADMIN) {
        AccessDeniedScreen(onNavigateBack = onNavigateBack)
        return
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Dealerships", "SaaS Plans", "Audit Logs", "Support Desk")

    var selectedDealershipForPlan by remember { mutableStateOf<Dealership?>(null) }
    var selectedDealershipForStore by remember { mutableStateOf<Dealership?>(null) }
    var selectedDealershipForVehicleTypes by remember { mutableStateOf<Dealership?>(null) }
    var selectedPlanForEdit by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var selectedTicketForResponse by remember { mutableStateOf<SupportTicket?>(null) }
    var showCreateDealershipDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Sign Out",
            message = "Sign out of your account?",
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
        floatingActionButton = {
            if (selectedTabIndex == 0 && currentUser?.role == UserRole.SUPER_ADMIN) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDealershipDialog = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = "Add Dealership") },
                    text = { Text("Add Dealership") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        topBar = {
            PremiumTopBar(
                title = "Super Admin Control Center",
                subtitle = "Multi-Tenant Platform SaaS Command Center",
                onNavigateBack = onNavigateBack,
                actions = {
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
        ) {
            PlatformMetricsHeader(stats = stats)

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
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
                0 -> DealershipsTabContent(
                    dealerships = dealerships,
                    searchQuery = searchQuery,
                    statusFilter = statusFilter,
                    showAddButton = currentUser?.role == UserRole.SUPER_ADMIN,
                    onAddDealershipClick = { showCreateDealershipDialog = true },
                    onSearchQueryChange = onSearchQueryChange,
                    onStatusFilterChange = onStatusFilterChange,
                    onActivate = onActivateDealership,
                    onSuspend = onSuspendDealership,
                    onExtendTrial = onExtendTrial,
                    onChangePlanClick = { d -> selectedDealershipForPlan = d },
                    onAddStoreClick = { d -> selectedDealershipForStore = d },
                    onEditVehicleTypesClick = { d -> selectedDealershipForVehicleTypes = d },
                    onDealershipClick = { id -> onNavigateToDealershipDetails(id) }
                )
                1 -> SubscriptionPlansTabContent(
                    plans = plans,
                    onEditPlan = { plan -> selectedPlanForEdit = plan },
                    onManageSubscriptions = onNavigateToSubscriptions
                )
                2 -> AuditLogsTabSummary(
                    logsCount = auditLogs.size,
                    recentLogs = auditLogs.take(5),
                    onViewFullLogs = onNavigateToAuditLogs
                )
                3 -> SupportTicketsTabSummary(
                    ticketsCount = supportTickets.size,
                    recentTickets = supportTickets,
                    onRespondClick = { ticket -> selectedTicketForResponse = ticket },
                    onViewAllTickets = onNavigateToSupportTickets
                )
            }
        }
    }

    selectedDealershipForPlan?.let { dealership ->
        AlertDialog(
            onDismissRequest = { selectedDealershipForPlan = null },
            title = { Text("Update Subscription Tier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Assign new plan to ${dealership.name}:")
                    plans.forEach { plan ->
                        SecondaryButton(
                            text = "${plan.name} - ${CurrencyUtils.formatCurrency(plan.monthlyPrice)}/mo",
                            onClick = {
                                onUpdateDealershipPlan(dealership.dealershipId, plan.planId)
                                selectedDealershipForPlan = null
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedDealershipForPlan = null }) { Text("Cancel") }
            }
        )
    }

    selectedDealershipForStore?.let { dealership ->
        AddStoreDialog(
            dealershipName = dealership.name,
            onDismiss = { selectedDealershipForStore = null },
            onSave = { storeName, stateCode, city, address ->
                onSaveStore(dealership.dealershipId, storeName, stateCode, city, address) {
                    selectedDealershipForStore = null
                }
            }
        )
    }

    selectedDealershipForVehicleTypes?.let { dealership ->
        var bikesChecked by remember { mutableStateOf(dealership.supportedVehicleTypes.contains(VehicleType.BIKE)) }
        var carsChecked by remember { mutableStateOf(dealership.supportedVehicleTypes.contains(VehicleType.CAR)) }

        AlertDialog(
            onDismissRequest = { selectedDealershipForVehicleTypes = null },
            title = { Text("Supported Vehicle Types (${dealership.name})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select vehicle categories supported by this dealership:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = bikesChecked, onCheckedChange = { bikesChecked = it })
                        Text("Two Wheelers (Bikes)")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = carsChecked, onCheckedChange = { carsChecked = it })
                        Text("Four Wheelers (Cars)")
                    }
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = "Save Vehicle Types",
                    onClick = {
                        val types = mutableListOf<VehicleType>()
                        if (bikesChecked) types.add(VehicleType.BIKE)
                        if (carsChecked) types.add(VehicleType.CAR)
                        onUpdateSupportedVehicleTypes(dealership.dealershipId, types)
                        selectedDealershipForVehicleTypes = null
                    },
                    enabled = bikesChecked || carsChecked
                )
            },
            dismissButton = {
                SecondaryButton(text = "Cancel", onClick = { selectedDealershipForVehicleTypes = null })
            }
        )
    }

    selectedPlanForEdit?.let { plan ->
        EditPlanDialog(
            plan = plan,
            onDismiss = { selectedPlanForEdit = null },
            onSave = { updatedPlan ->
                onUpdatePlan(updatedPlan)
                selectedPlanForEdit = null
            }
        )
    }

    selectedTicketForResponse?.let { ticket ->
        RespondTicketDialog(
            ticket = ticket,
            onDismiss = { selectedTicketForResponse = null },
            onSendResponse = { ticketId, responseText, newStatus ->
                onPostAdminResponse(ticketId, responseText, newStatus)
                selectedTicketForResponse = null
            }
        )
    }

    if (showCreateDealershipDialog) {
        CreateDealershipDialog(
            plans = plans,
            onDismiss = { showCreateDealershipDialog = false },
            onCreate = { name, legalName, email, phone, address, city, state, vehicleTypes, plan, trialDays, storeName, storeCity, storeState, storeAddress, adminName, adminEmail, adminPhone ->
                onCreateDealership(
                    name, legalName, email, phone, address, city, state, vehicleTypes, plan, trialDays, storeName, storeCity, storeState, storeAddress, adminName, adminEmail, adminPhone
                ) {
                    showCreateDealershipDialog = false
                }
            }
        )
    }
}

@Composable
private fun AccessDeniedScreen(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        PremiumCard(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            borderColor = MaterialTheme.colorScheme.error
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Access Restricted",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Super Admin privileges required to access global platform tenant administration.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                PrimaryButton(text = "Return Back", onClick = onNavigateBack)
            }
        }
    }
}

@Composable
private fun PlatformMetricsHeader(stats: PlatformStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                AnimatedKpiCard(
                    title = "Monthly MRR",
                    value = stats.totalMRR,
                    icon = Icons.Rounded.Receipt,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(160.dp)
                )
            }
            item {
                AnimatedKpiCard(
                    title = "Total Tenants",
                    value = stats.totalDealerships.toDouble(),
                    isCurrency = false,
                    icon = Icons.Rounded.Business,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(160.dp)
                )
            }
            item {
                AnimatedKpiCard(
                    title = "Active Subscriptions",
                    value = stats.activeDealerships.toDouble(),
                    isCurrency = false,
                    icon = Icons.Rounded.CheckCircle,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.width(160.dp)
                )
            }
            item {
                AnimatedKpiCard(
                    title = "Free Trial Tenants",
                    value = stats.trialDealerships.toDouble(),
                    isCurrency = false,
                    icon = Icons.Rounded.HourglassTop,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(160.dp)
                )
            }
            item {
                AnimatedKpiCard(
                    title = "Total Stores",
                    value = stats.totalStores.toDouble(),
                    isCurrency = false,
                    icon = Icons.Rounded.Storefront,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(160.dp)
                )
            }
            item {
                AnimatedKpiCard(
                    title = "System Users",
                    value = stats.totalUsers.toDouble(),
                    isCurrency = false,
                    icon = Icons.Rounded.People,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.width(160.dp)
                )
            }
        }
    }
}

@Composable
private fun DealershipsTabContent(
    dealerships: List<Dealership>,
    searchQuery: String,
    statusFilter: String,
    showAddButton: Boolean = false,
    onAddDealershipClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onActivate: (String) -> Unit,
    onSuspend: (String) -> Unit,
    onExtendTrial: (String) -> Unit,
    onChangePlanClick: (Dealership) -> Unit,
    onAddStoreClick: (Dealership) -> Unit,
    onEditVehicleTypesClick: (Dealership) -> Unit,
    onDealershipClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tenant Dealerships (${dealerships.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (showAddButton) {
                PrimaryButton(
                    text = "+ Add Dealership",
                    onClick = onAddDealershipClick,
                    modifier = Modifier.height(36.dp)
                )
            }
        }

        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Search Dealership by Name, City, ID..."
        )

        Spacer(modifier = Modifier.height(8.dp))

        val filterOptions = listOf("ALL", "ACTIVE", "TRIAL", "EXPIRED", "SUSPENDED")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filterOptions) { filter ->
                FilterChip(
                    selected = statusFilter.equals(filter, ignoreCase = true),
                    onClick = { onStatusFilterChange(filter) },
                    label = { Text(filter) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (dealerships.isEmpty()) {
            EmptyState(title = "No Dealerships Found", subtitle = "No tenant accounts match the filter criteria.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dealerships, key = { it.dealershipId }) { d ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                        DealershipCardItem(
                            dealership = d,
                            onActivate = { onActivate(d.dealershipId) },
                            onSuspend = { onSuspend(d.dealershipId) },
                            onExtendTrial = { onExtendTrial(d.dealershipId) },
                            onChangePlan = { onChangePlanClick(d) },
                            onAddStore = { onAddStoreClick(d) },
                            onEditVehicleTypes = { onEditVehicleTypesClick(d) },
                            onClick = { onDealershipClick(d.dealershipId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DealershipCardItem(
    dealership: Dealership,
    onActivate: () -> Unit,
    onSuspend: () -> Unit,
    onExtendTrial: () -> Unit,
    onChangePlan: () -> Unit,
    onAddStore: () -> Unit,
    onEditVehicleTypes: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    PremiumCard(onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dealership.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${dealership.city}, ${dealership.state} • ID: ${dealership.dealershipId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SubscriptionStatusBadge(statusName = dealership.subscriptionStatus.name)

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = "Actions")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Full Details") },
                            onClick = { showMenu = false; onClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("Supported Vehicle Types") },
                            onClick = { showMenu = false; onEditVehicleTypes() }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Store Outlet") },
                            onClick = { showMenu = false; onAddStore() }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Plan Tier") },
                            onClick = { showMenu = false; onChangePlan() }
                        )
                        if (dealership.subscriptionStatus == SubscriptionStatus.TRIAL) {
                            DropdownMenuItem(
                                text = { Text("Extend Trial (+14 Days)") },
                                onClick = { showMenu = false; onExtendTrial() }
                            )
                        }
                        if (!dealership.active || dealership.subscriptionStatus != SubscriptionStatus.ACTIVE) {
                            DropdownMenuItem(
                                text = { Text("Activate Account") },
                                onClick = { showMenu = false; onActivate() }
                            )
                        }
                        if (dealership.active && dealership.subscriptionStatus != SubscriptionStatus.SUSPENDED) {
                            DropdownMenuItem(
                                text = { Text("Suspend Account", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onSuspend() }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Plan: ${dealership.subscriptionPlan} • Types: ${dealership.supportedVehicleTypes.joinToString { it.name }}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Contact: ${dealership.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SubscriptionPlansTabContent(
    plans: List<SubscriptionPlan>,
    onEditPlan: (SubscriptionPlan) -> Unit,
    onManageSubscriptions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        SectionHeader(
            title = "SaaS Subscription Tier Configurations",
            actionText = "Manage Billings",
            onActionClick = onManageSubscriptions
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(plans, key = { it.planId }) { plan ->
                PlanCardItem(plan = plan, onEditPlan = { onEditPlan(plan) })
            }
        }
    }
}

@Composable
private fun PlanCardItem(
    plan: SubscriptionPlan,
    onEditPlan: () -> Unit
) {
    PremiumCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    CurrencyText(amount = plan.monthlyPrice, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEditPlan) {
                    Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Edit Plan")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Text("Plan Quotas & Limits:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Stores: ${if (plan.maxStores == Int.MAX_VALUE) "Unlimited" else plan.maxStores}", style = MaterialTheme.typography.bodySmall)
                Text("Users: ${if (plan.maxUsers == Int.MAX_VALUE) "Unlimited" else plan.maxUsers}", style = MaterialTheme.typography.bodySmall)
                Text("Bikes: ${if (plan.maxBikes == Int.MAX_VALUE) "Unlimited" else plan.maxBikes}", style = MaterialTheme.typography.bodySmall)
                Text("Customers: ${if (plan.maxCustomers == Int.MAX_VALUE) "Unlimited" else plan.maxCustomers}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EditPlanDialog(
    plan: SubscriptionPlan,
    onDismiss: () -> Unit,
    onSave: (SubscriptionPlan) -> Unit
) {
    var monthlyPriceText by remember { mutableStateOf(plan.monthlyPrice.toLong().toString()) }
    var yearlyPriceText by remember { mutableStateOf(plan.yearlyPrice.toLong().toString()) }
    var maxStoresText by remember { mutableStateOf(if (plan.maxStores == Int.MAX_VALUE) "-1" else plan.maxStores.toString()) }
    var maxUsersText by remember { mutableStateOf(if (plan.maxUsers == Int.MAX_VALUE) "-1" else plan.maxUsers.toString()) }
    var maxBikesText by remember { mutableStateOf(if (plan.maxBikes == Int.MAX_VALUE) "-1" else plan.maxBikes.toString()) }
    var maxCustomersText by remember { mutableStateOf(if (plan.maxCustomers == Int.MAX_VALUE) "-1" else plan.maxCustomers.toString()) }

    var reportsEnabled by remember { mutableStateOf(plan.reportsEnabled) }
    var advancedReportsEnabled by remember { mutableStateOf(plan.advancedReportsEnabled) }
    var multiStoreEnabled by remember { mutableStateOf(plan.multiStoreEnabled) }
    var financeEnabled by remember { mutableStateOf(plan.financeEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Plan: ${plan.name}") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PremiumTextField(
                    value = monthlyPriceText,
                    onValueChange = { monthlyPriceText = it },
                    label = "Monthly Price (₹)"
                )
                PremiumTextField(
                    value = yearlyPriceText,
                    onValueChange = { yearlyPriceText = it },
                    label = "Yearly Price (₹)"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumTextField(
                        value = maxStoresText,
                        onValueChange = { maxStoresText = it },
                        label = "Max Stores (-1 Unlimited)",
                        modifier = Modifier.weight(1f)
                    )
                    PremiumTextField(
                        value = maxUsersText,
                        onValueChange = { maxUsersText = it },
                        label = "Max Users (-1 Unlimited)",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumTextField(
                        value = maxBikesText,
                        onValueChange = { maxBikesText = it },
                        label = "Max Bikes (-1 Unlimited)",
                        modifier = Modifier.weight(1f)
                    )
                    PremiumTextField(
                        value = maxCustomersText,
                        onValueChange = { maxCustomersText = it },
                        label = "Max Customers (-1 Unlimited)",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Reports Module", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = reportsEnabled, onCheckedChange = { reportsEnabled = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Advanced Analytics", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = advancedReportsEnabled, onCheckedChange = { advancedReportsEnabled = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Multi-Store", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = multiStoreEnabled, onCheckedChange = { multiStoreEnabled = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Finance Module", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = financeEnabled, onCheckedChange = { financeEnabled = it })
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Save Plan",
                onClick = {
                    fun parseLimit(text: String, defaultVal: Int): Int {
                        val parsed = text.toIntOrNull() ?: defaultVal
                        return if (parsed < 0) Int.MAX_VALUE else parsed
                    }

                    val updated = plan.copy(
                        monthlyPrice = monthlyPriceText.toDoubleOrNull() ?: plan.monthlyPrice,
                        yearlyPrice = yearlyPriceText.toDoubleOrNull() ?: plan.yearlyPrice,
                        maxStores = parseLimit(maxStoresText, plan.maxStores),
                        maxUsers = parseLimit(maxUsersText, plan.maxUsers),
                        maxBikes = parseLimit(maxBikesText, plan.maxBikes),
                        maxCustomers = parseLimit(maxCustomersText, plan.maxCustomers),
                        reportsEnabled = reportsEnabled,
                        advancedReportsEnabled = advancedReportsEnabled,
                        multiStoreEnabled = multiStoreEnabled,
                        financeEnabled = financeEnabled
                    )
                    onSave(updated)
                }
            )
        },
        dismissButton = {
            SecondaryButton(text = "Cancel", onClick = onDismiss)
        }
    )
}

@Composable
private fun AuditLogsTabSummary(
    logsCount: Int,
    recentLogs: List<AuditLog>,
    onViewFullLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        SectionHeader(
            title = "Audit Trail Logs ($logsCount)",
            actionText = "View Full Audit Trail",
            onActionClick = onViewFullLogs
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recentLogs) { log ->
                PremiumCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(log.action, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(log.entityType, style = MaterialTheme.typography.labelSmall)
                        }
                        Text("User: ${log.userId} • ID: ${log.entityId}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportTicketsTabSummary(
    ticketsCount: Int,
    recentTickets: List<SupportTicket>,
    onRespondClick: (SupportTicket) -> Unit,
    onViewAllTickets: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        SectionHeader(
            title = "Support Desk ($ticketsCount)",
            actionText = "Support Desk",
            onActionClick = onViewAllTickets
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(recentTickets, key = { it.ticketId }) { ticket ->
                PremiumCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ticket.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TicketStatusBadge(statusName = ticket.status.name)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ticket.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SecondaryButton(
                            text = "Respond / Resolve",
                            onClick = { onRespondClick(ticket) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RespondTicketDialog(
    ticket: SupportTicket,
    onDismiss: () -> Unit,
    onSendResponse: (ticketId: String, responseText: String, newStatus: TicketStatus?) -> Unit
) {
    var responseText by remember { mutableStateOf(ticket.adminResponse) }
    var selectedStatus by remember { mutableStateOf(ticket.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Respond to Ticket: ${ticket.subject}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Description: ${ticket.description}", style = MaterialTheme.typography.bodySmall)

                PremiumTextField(
                    value = responseText,
                    onValueChange = { responseText = it },
                    label = "Admin Response",
                    singleLine = false
                )

                Text("Update Status:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TicketStatus.entries.toTypedArray()) { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Post Response",
                onClick = { onSendResponse(ticket.ticketId, responseText, selectedStatus) },
                enabled = responseText.isNotBlank()
            )
        },
        dismissButton = {
            SecondaryButton(text = "Cancel", onClick = onDismiss)
        }
    )
}

@Composable
private fun AddStoreDialog(
    dealershipName: String,
    onDismiss: () -> Unit,
    onSave: (storeName: String, stateCode: String, city: String, address: String) -> Unit
) {
    var storeName by remember { mutableStateOf("") }
    var stateCode by remember { mutableStateOf("TG") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Store Outlet ($dealershipName)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (localError != null) {
                    Text(
                        text = localError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                PremiumTextField(value = storeName, onValueChange = { storeName = it; localError = null }, label = "Store Name")
                PremiumTextField(value = stateCode, onValueChange = { stateCode = it; localError = null }, label = "State Code (TG/KA)")
                PremiumTextField(value = city, onValueChange = { city = it; localError = null }, label = "City")
                PremiumTextField(value = address, onValueChange = { address = it; localError = null }, label = "Street Address", singleLine = false)
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
                    onSave(storeName.trim(), stateCode.trim().uppercase(), city.trim(), address.trim())
                }
            )
        },
        dismissButton = { SecondaryButton(text = "Cancel", onClick = onDismiss) }
    )
}

@Composable
private fun CreateDealershipDialog(
    plans: List<SubscriptionPlan>,
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        legalName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        state: String,
        supportedVehicleTypes: List<VehicleType>,
        subscriptionPlan: String,
        trialDays: Int,
        storeName: String,
        storeCity: String,
        storeState: String,
        storeAddress: String,
        adminName: String,
        adminEmail: String,
        adminPhone: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var legalName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }

    var bikesChecked by remember { mutableStateOf(true) }
    var carsChecked by remember { mutableStateOf(true) }

    var selectedPlanId by remember { mutableStateOf("STARTER") }
    var planDropdownExpanded by remember { mutableStateOf(false) }
    var trialDaysText by remember { mutableStateOf("14") }

    var storeName by remember { mutableStateOf("") }
    var storeCity by remember { mutableStateOf("") }
    var storeState by remember { mutableStateOf("") }
    var storeAddress by remember { mutableStateOf("") }

    var adminName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }
    var adminPhone by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val normalizedId = remember(name) {
        val trimmed = name.trim().lowercase()
            .replace("[^a-z0-9\\s_]".toRegex(), "")
            .trim()
            .replace("\\s+".toRegex(), "_")
        if (trimmed.isNotBlank()) trimmed else "apex_motors"
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Onboard New Dealership", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (localError != null) {
                    Text(
                        text = localError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Section 1: Dealership Details
                Text("1. Dealership Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                PremiumTextField(value = name, onValueChange = { name = it; localError = null }, label = "Dealership Name *")
                Text("Preview ID: $normalizedId", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PremiumTextField(value = legalName, onValueChange = { legalName = it; localError = null }, label = "Legal Entity Name *")
                PremiumTextField(value = email, onValueChange = { email = it; localError = null }, label = "Contact Email *")
                PremiumTextField(value = phone, onValueChange = { phone = it; localError = null }, label = "Phone Number *")
                PremiumTextField(value = address, onValueChange = { address = it; localError = null }, label = "Street Address *", singleLine = false)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumTextField(value = city, onValueChange = { city = it; if (storeCity.isBlank()) storeCity = it; localError = null }, label = "City *", modifier = Modifier.weight(1f))
                    PremiumTextField(value = state, onValueChange = { state = it; if (storeState.isBlank()) storeState = it; localError = null }, label = "State *", modifier = Modifier.weight(1f))
                }

                HorizontalDivider()

                // Section 2: Business Vehicle Types
                Text("2. Business Vehicle Types", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = bikesChecked, onCheckedChange = { bikesChecked = it; localError = null })
                    Text("Two Wheelers / Bikes")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = carsChecked, onCheckedChange = { carsChecked = it; localError = null })
                    Text("Four Wheelers / Cars")
                }

                HorizontalDivider()

                // Section 3: SaaS Plan & Trial
                Text("3. SaaS Plan & Trial", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Box {
                    SecondaryButton(
                        text = "Plan: ${SubscriptionPlan.getPlanById(selectedPlanId).name}",
                        onClick = { planDropdownExpanded = true }
                    )
                    DropdownMenu(expanded = planDropdownExpanded, onDismissRequest = { planDropdownExpanded = false }) {
                        plans.forEach { plan ->
                            DropdownMenuItem(
                                text = { Text("${plan.name} (${CurrencyUtils.formatCurrency(plan.monthlyPrice)}/mo)") },
                                onClick = { selectedPlanId = plan.planId; planDropdownExpanded = false }
                            )
                        }
                    }
                }
                PremiumTextField(value = trialDaysText, onValueChange = { trialDaysText = it; localError = null }, label = "Trial Period (Days)")

                HorizontalDivider()

                // Section 4: Primary Store Outlet
                Text("4. Primary Store Outlet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                PremiumTextField(value = storeName, onValueChange = { storeName = it; localError = null }, label = "Store Outlet Name *")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumTextField(value = storeCity, onValueChange = { storeCity = it; localError = null }, label = "Store City *", modifier = Modifier.weight(1f))
                    PremiumTextField(value = storeState, onValueChange = { storeState = it; localError = null }, label = "Store State *", modifier = Modifier.weight(1f))
                }
                PremiumTextField(value = storeAddress, onValueChange = { storeAddress = it; localError = null }, label = "Store Address", singleLine = false)

                HorizontalDivider()

                // Section 5: Dealership Admin Setup
                Text("5. Dealership Admin Setup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                PremiumTextField(value = adminName, onValueChange = { adminName = it; localError = null }, label = "Admin Full Name *")
                PremiumTextField(value = adminEmail, onValueChange = { adminEmail = it; localError = null }, label = "Admin Contact Email *")
                PremiumTextField(value = adminPhone, onValueChange = { adminPhone = it; localError = null }, label = "Admin Phone")

                Text(
                    text = "Notice: Setup Pending Invitation. Primary admin user account created with status 'Pending Invitation' (inactive = false).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (isSubmitting) "Creating..." else "Create Dealership",
                onClick = {
                    if (name.isBlank() || legalName.isBlank() || email.isBlank() || phone.isBlank() || address.isBlank() || city.isBlank() || state.isBlank()) {
                        localError = "Please fill in all Dealership details."
                        return@PrimaryButton
                    }
                    if (!bikesChecked && !carsChecked) {
                        localError = "Select at least one supported vehicle type."
                        return@PrimaryButton
                    }
                    if (storeName.isBlank() || storeCity.isBlank() || storeState.isBlank()) {
                        localError = "Please fill in Primary Store Outlet details."
                        return@PrimaryButton
                    }
                    if (adminName.isBlank() || adminEmail.isBlank()) {
                        localError = "Please fill in Admin Name and Email."
                        return@PrimaryButton
                    }

                    val vehicleTypes = mutableListOf<VehicleType>()
                    if (bikesChecked) vehicleTypes.add(VehicleType.BIKE)
                    if (carsChecked) vehicleTypes.add(VehicleType.CAR)

                    val trialDays = trialDaysText.toIntOrNull() ?: 14

                    isSubmitting = true
                    onCreate(
                        name.trim(),
                        legalName.trim(),
                        email.trim(),
                        phone.trim(),
                        address.trim(),
                        city.trim(),
                        state.trim(),
                        vehicleTypes,
                        selectedPlanId,
                        trialDays,
                        storeName.trim(),
                        storeCity.trim(),
                        storeState.trim().uppercase(),
                        storeAddress.ifBlank { address }.trim(),
                        adminName.trim(),
                        adminEmail.trim(),
                        adminPhone.trim()
                    )
                },
                enabled = !isSubmitting
            )
        },
        dismissButton = {
            SecondaryButton(text = "Cancel", onClick = onDismiss, enabled = !isSubmitting)
        }
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun SuperAdminDashboardContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        SuperAdminDashboardContent(
            currentUser = PreviewSampleData.sampleUser.copy(role = UserRole.SUPER_ADMIN),
            stats = PlatformStats(
                totalDealerships = 5,
                activeDealerships = 4,
                trialDealerships = 1,
                totalStores = 12,
                totalUsers = 35,
                totalMRR = 34995.0
            ),
            dealerships = listOf(PreviewSampleData.sampleDealership),
            searchQuery = "",
            statusFilter = "ALL",
            supportTickets = PreviewSampleData.sampleSupportTickets,
            auditLogs = PreviewSampleData.sampleAuditLogs,
            plans = SubscriptionPlan.ALL_PLANS,
            onSearchQueryChange = {},
            onStatusFilterChange = {},
            onActivateDealership = {},
            onSuspendDealership = {},
            onExtendTrial = {},
            onUpdateDealershipPlan = { _, _ -> },
            onUpdatePlan = {},
            onPostAdminResponse = { _, _, _ -> }
        )
    }
}
