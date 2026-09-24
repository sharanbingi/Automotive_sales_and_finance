package com.automotive.salesfinance.ui.inventory

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.Vehicle
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.model.toVehicle
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.EmptyState
import com.automotive.salesfinance.ui.components.InventoryStatusBadge
import com.automotive.salesfinance.ui.components.LoadingState
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.QuotaReachedCard
import com.automotive.salesfinance.ui.components.SearchBar
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AppSpacing
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.DeadStockMetrics
import com.automotive.salesfinance.viewmodel.DealershipFeatureLimits
import com.automotive.salesfinance.viewmodel.InventoryViewModel
import com.automotive.salesfinance.viewmodel.SortOption
import com.automotive.salesfinance.viewmodel.SubscriptionViewModel

/**
 * Premium Vehicle Inventory Screen for Automotive Sales & Finance.
 * Supports bikes and cars, adaptive phone and tablet layouts, and strict financial precision.
 */
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit = {},
    onBikeClick: (String) -> Unit = {},
    onAddBikeClick: () -> Unit = {},
    onDeadStockClick: () -> Unit = {}
) {
    val vehicles by viewModel.filteredVehicles.collectAsState()
    val deadStockMetrics by viewModel.deadStockMetrics.collectAsState()
    val selectedState by viewModel.selectedState.collectAsState()
    val selectedStore by viewModel.selectedStore.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val selectedVehicleTypeFilter by viewModel.selectedVehicleTypeFilter.collectAsState()
    val supportedVehicleTypes by viewModel.supportedVehicleTypes.collectAsState()
    val featureLimits by subscriptionViewModel.featureLimits.collectAsState()

    val availableStores by viewModel.availableStores.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    InventoryContent(
        vehicles = vehicles,
        deadStockMetrics = deadStockMetrics,
        selectedState = selectedState,
        selectedStore = selectedStore,
        searchQuery = searchQuery,
        statusFilter = statusFilter,
        selectedVehicleTypeFilter = selectedVehicleTypeFilter,
        supportedVehicleTypes = supportedVehicleTypes,
        featureLimits = featureLimits,
        availableStores = availableStores,
        isLoading = isLoading,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onStateSelect = { viewModel.setSelectedState(it) },
        onStoreSelect = { viewModel.setSelectedStore(it) },
        onStatusFilterSelect = { viewModel.setStatusFilter(it) },
        onVehicleTypeFilterSelect = { viewModel.setSelectedVehicleTypeFilter(it) },
        onSortOptionSelect = { viewModel.setSortOption(it) },
        onNavigateBack = onNavigateBack,
        onVehicleClick = onBikeClick,
        onAddVehicleClick = onAddBikeClick,
        onDeadStockClick = onDeadStockClick
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryContent(
    vehicles: List<Vehicle>,
    deadStockMetrics: DeadStockMetrics,
    selectedState: String,
    selectedStore: String,
    searchQuery: String,
    statusFilter: String,
    selectedVehicleTypeFilter: String = "ALL",
    supportedVehicleTypes: List<VehicleType> = listOf(VehicleType.BIKE, VehicleType.CAR),
    featureLimits: DealershipFeatureLimits,
    availableStores: List<Store>,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onStateSelect: (String) -> Unit,
    onStoreSelect: (String) -> Unit,
    onStatusFilterSelect: (String) -> Unit,
    onVehicleTypeFilterSelect: (String) -> Unit = {},
    onSortOptionSelect: (SortOption) -> Unit,
    onNavigateBack: () -> Unit = {},
    onVehicleClick: (String) -> Unit = {},
    onAddVehicleClick: () -> Unit = {},
    onDeadStockClick: () -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showStateMenu by remember { mutableStateOf(false) }
    var showStoreMenu by remember { mutableStateOf(false) }

    val states = listOf("ALL") + availableStores.map { it.stateCode }.distinct()
    val supportsMultipleTypes = supportedVehicleTypes.size > 1

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Vehicle Inventory",
                subtitle = "${vehicles.size} Vehicles Registered",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = onDeadStockClick,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (deadStockMetrics.totalCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text("${deadStockMetrics.totalCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Dead Stock Alert (${deadStockMetrics.totalCount} items)",
                                tint = if (deadStockMetrics.totalCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = "Sort Options"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest Inward First") },
                                onClick = {
                                    onSortOptionSelect(SortOption.NEWEST_INWARD)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest Inward First") },
                                onClick = {
                                    onSortOptionSelect(SortOption.OLDEST_INWARD)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Price: Low to High") },
                                onClick = {
                                    onSortOptionSelect(SortOption.PRICE_LOW_HIGH)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Price: High to Low") },
                                onClick = {
                                    onSortOptionSelect(SortOption.PRICE_HIGH_LOW)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (featureLimits.canAddBike) {
                FloatingActionButton(
                    onClick = onAddVehicleClick,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Inward New Vehicle"
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth >= 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.large, vertical = AppSpacing.small)
            ) {
                if (!featureLimits.canAddBike) {
                    QuotaReachedCard(
                        featureName = "Inventory Vehicles",
                        currentUsage = featureLimits.currentBikes,
                        limit = featureLimits.maxBikes,
                        onUpgradeClick = { /* Handled in settings/subscription */ },
                        modifier = Modifier.padding(bottom = AppSpacing.small)
                    )
                }

                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Search Make, Model, Variant, Chassis, Engine..."
                )

                Spacer(modifier = Modifier.height(AppSpacing.small))

                // Filter Bar Section
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)) {
                    if (supportsMultipleTypes) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedVehicleTypeFilter.equals("ALL", ignoreCase = true),
                                onClick = { onVehicleTypeFilterSelect("ALL") },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                label = { Text("All Types") }
                            )
                            FilterChip(
                                selected = selectedVehicleTypeFilter.equals("BIKE", ignoreCase = true) || selectedVehicleTypeFilter.equals("BIKES", ignoreCase = true),
                                onClick = { onVehicleTypeFilterSelect("BIKE") },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                label = { Text("Bikes") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.DirectionsBike,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                            FilterChip(
                                selected = selectedVehicleTypeFilter.equals("CAR", ignoreCase = true) || selectedVehicleTypeFilter.equals("CARS", ignoreCase = true),
                                onClick = { onVehicleTypeFilterSelect("CAR") },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                label = { Text("Cars") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.DirectionsCar,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            FilterChip(
                                selected = selectedState != "ALL",
                                onClick = { showStateMenu = true },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                label = { Text("State: $selectedState") }
                            )
                            DropdownMenu(
                                expanded = showStateMenu,
                                onDismissRequest = { showStateMenu = false }
                            ) {
                                states.forEach { state ->
                                    DropdownMenuItem(
                                        text = { Text(if (state == "ALL") "All States" else state) },
                                        onClick = {
                                            onStateSelect(state)
                                            showStateMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Box {
                            val storeLabel = if (selectedStore == "ALL") "All Stores" else {
                                availableStores.find { it.storeId == selectedStore }?.let { "${it.storeName} (${it.storeId})" } ?: selectedStore
                            }
                            FilterChip(
                                selected = selectedStore != "ALL",
                                onClick = { showStoreMenu = true },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                label = { Text(storeLabel) }
                            )
                            DropdownMenu(
                                expanded = showStoreMenu,
                                onDismissRequest = { showStoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Stores") },
                                    onClick = {
                                        onStoreSelect("ALL")
                                        showStoreMenu = false
                                    }
                                )
                                availableStores.forEach { store ->
                                    DropdownMenuItem(
                                        text = { Text("${store.storeName} (${store.storeId})") },
                                        onClick = {
                                            onStoreSelect(store.storeId)
                                            showStoreMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    val statuses = listOf("ALL", "AVAILABLE", "RESERVED", "FINANCED")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(statuses) { status ->
                            FilterChip(
                                selected = statusFilter.equals(status, ignoreCase = true),
                                onClick = { onStatusFilterSelect(status) },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                label = { Text(status) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.medium))

                val isDefaultFilter = searchQuery.isBlank() &&
                    statusFilter.equals("ALL", ignoreCase = true) &&
                    selectedVehicleTypeFilter.equals("ALL", ignoreCase = true) &&
                    selectedState == "ALL" &&
                    selectedStore == "ALL"

                if (isLoading) {
                    LoadingState(message = "Fetching vehicles registry...")
                } else if (vehicles.isEmpty()) {
                    if (isDefaultFilter) {
                        EmptyState(
                            title = "No vehicles in inventory yet",
                            subtitle = "Inward your first motorcycle or car to start tracking sales and finance.",
                            actionButton = {
                                if (featureLimits.canAddBike) {
                                    Button(
                                        onClick = onAddVehicleClick,
                                        modifier = Modifier
                                            .padding(top = AppSpacing.medium)
                                            .defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(AppSpacing.small))
                                        Text("Inward New Vehicle")
                                    }
                                }
                            }
                        )
                    } else {
                        EmptyState(
                            title = "No Vehicles Found",
                            subtitle = "No inventory vehicles match the active search or store filters."
                        )
                    }
                } else {
                    if (isTablet) {
                        // Tablet Multi-Column Responsive Grid
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val chunkedVehicles = vehicles.chunked(2)
                            items(chunkedVehicles) { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                                ) {
                                    pair.forEach { vehicle ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            VehicleCard(
                                                vehicle = vehicle,
                                                onClick = { onVehicleClick(vehicle.vehicleId) }
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        // Phone Single-Column List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(vehicles, key = { it.vehicleId }) { vehicle ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically()
                                ) {
                                    VehicleCard(
                                        vehicle = vehicle,
                                        onClick = { onVehicleClick(vehicle.vehicleId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    onClick: () -> Unit
) {
    val daysInStock = DateUtils.calculateDaysInStock(vehicle.inwardTimestamp)
    val isDeadStock = vehicle.status == BikeStatus.AVAILABLE && DateUtils.isDeadStock(vehicle.inwardTimestamp, 60)
    val isCar = vehicle.vehicleType == VehicleType.CAR

    val cardAccessibilityDesc = buildString {
        append("${if (isCar) "Car" else "Motorcycle"}: ${vehicle.make} ${vehicle.model} ${vehicle.variant} ${vehicle.year}.")
        append(" Status: ${vehicle.status.name}.")
        append(" Listed price: ${CurrencyUtils.formatCurrency(vehicle.listedPrice)}.")
        append(" Chassis: ${vehicle.chassisNumber}.")
        if (isDeadStock) append(" Alert: Dead stock exceeding $daysInStock days.")
    }

    PremiumCard(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = cardAccessibilityDesc
        },
        borderColor = if (isDeadStock) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isCar) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = if (isCar) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(end = AppSpacing.small)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isCar) Icons.Rounded.DirectionsCar else Icons.AutoMirrored.Rounded.DirectionsBike,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCar) "CAR" else "BIKE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = buildString {
                            append("${vehicle.make} ${vehicle.model}")
                            if (vehicle.variant.isNotBlank()) append(" ${vehicle.variant}")
                            append(" (${vehicle.year})")
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                InventoryStatusBadge(statusName = vehicle.status.name)
            }

            Spacer(modifier = Modifier.height(AppSpacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Chassis/VIN: ${vehicle.chassisNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Engine: ${vehicle.engineNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isCar && vehicle.fuelType.name.isNotBlank()) {
                        Text(
                            text = "Fuel: ${vehicle.fuelType.name} | Trans: ${vehicle.transmission.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    CurrencyText(
                        amount = vehicle.listedPrice,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cost: ${CurrencyUtils.formatCurrency(vehicle.costPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    Text(
                        text = "Store: ${vehicle.effectiveStoreId} (${vehicle.stateCode})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = if (vehicle.registrationNumber.isNotBlank()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (vehicle.registrationNumber.isNotBlank()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = vehicle.registrationNumber.trim().uppercase().ifBlank { "Unregistered" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isDeadStock) {
                    Text(
                        text = "Dead Stock ($daysInStock days)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "$daysInStock days in stock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BikeCard(
    bike: Bike,
    onClick: () -> Unit
) {
    VehicleCard(vehicle = bike.toVehicle(), onClick = onClick)
}

@Preview(name = "Phone Light Mode", showBackground = true)
@Preview(name = "Phone Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet Landscape", device = Devices.TABLET)
@Composable
private fun InventoryContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        InventoryContent(
            vehicles = PreviewSampleData.sampleBikes.map { it.toVehicle() },
            deadStockMetrics = DeadStockMetrics(totalCount = 1, totalCapitalTiedUp = 220000.0, maxDaysInStock = 120),
            selectedState = "ALL",
            selectedStore = "ALL",
            searchQuery = "",
            statusFilter = "ALL",
            featureLimits = DealershipFeatureLimits(canAddBike = true, currentBikes = 3, maxBikes = 100),
            availableStores = PreviewSampleData.sampleStores,
            isLoading = false,
            onSearchQueryChange = {},
            onStateSelect = {},
            onStoreSelect = {},
            onStatusFilterSelect = {},
            onSortOptionSelect = {}
        )
    }
}
