package com.automotive.salesfinance.ui.inventory

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Vehicle
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.model.toBike
import com.automotive.salesfinance.model.toVehicle
import com.automotive.salesfinance.ui.components.ConfirmationDialog
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.EmptyState
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.InventoryStatusBadge
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.SecondaryButton
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.viewmodel.InventoryViewModel
import java.util.Locale

@Composable
fun BikeDetailsScreen(
    bikeId: String,
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onFinanceClick: (String) -> Unit = {}
) {
    val vehicle = viewModel.getVehicleById(bikeId)

    VehicleDetailsContent(
        vehicle = vehicle,
        onReserveVehicle = { id, store -> viewModel.reserveVehicle(id, store) },
        onDeleteVehicle = { id -> viewModel.deleteVehicle(id, onSuccess = onNavigateBack) },
        onNavigateBack = onNavigateBack,
        onEditClick = onEditClick,
        onFinanceClick = onFinanceClick
    )
}

@Composable
fun BikeDetailsContent(
    bike: Bike?,
    onReserveBike: (String, String) -> Unit = { _, _ -> },
    onDeleteBike: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onFinanceClick: (String) -> Unit = {}
) {
    VehicleDetailsContent(
        vehicle = bike?.toVehicle(),
        onReserveVehicle = onReserveBike,
        onDeleteVehicle = onDeleteBike,
        onNavigateBack = onNavigateBack,
        onEditClick = onEditClick,
        onFinanceClick = onFinanceClick
    )
}

@Composable
fun VehicleDetailsContent(
    vehicle: Vehicle?,
    onReserveVehicle: (String, String) -> Unit = { _, _ -> },
    onDeleteVehicle: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onFinanceClick: (String) -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = vehicle?.let { buildString { append("${it.make} ${it.model}"); if (it.variant.isNotBlank()) append(" ${it.variant}") } } ?: "Vehicle Specs",
                subtitle = vehicle?.let { "VIN/Chassis: ${it.chassisNumber}" },
                onNavigateBack = onNavigateBack,
                actions = {
                    if (vehicle != null) {
                        IconButton(onClick = { onEditClick(vehicle.vehicleId) }) {
                            Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Edit Vehicle")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete Vehicle", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (vehicle == null) {
            EmptyState(
                title = "Vehicle Not Found",
                subtitle = "The requested vehicle record is missing or deleted.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            val daysInStock = DateUtils.calculateDaysInStock(vehicle.inwardTimestamp)
            val isDeadStock = vehicle.status == BikeStatus.AVAILABLE && daysInStock > 60
            val isCar = vehicle.vehicleType == VehicleType.CAR

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Header Banner Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCar) Icons.Rounded.DirectionsCar else Icons.AutoMirrored.Rounded.DirectionsBike,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
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
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            InventoryStatusBadge(statusName = vehicle.status.name)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Model Year: ${vehicle.year}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CurrencyText(
                            amount = vehicle.listedPrice,
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (isDeadStock) {
                    PremiumCard(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        borderColor = MaterialTheme.colorScheme.error
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                            Text(
                                text = "DEAD STOCK ALERT: Vehicle in inventory for $daysInStock days (>60 days threshold). High capital risk.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Specifications Card
                PremiumCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isCar) "Car Specifications & Registration" else "Vehicle Specifications & Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        SpecRow("Vehicle Type", if (isCar) "Four Wheeler (Car)" else "Two Wheeler (Bike)")
                        SpecRow("Make & Model", "${vehicle.make} ${vehicle.model}")
                        if (vehicle.variant.isNotBlank()) SpecRow("Variant / Trim", vehicle.variant)
                        SpecRow("Color", vehicle.color.ifBlank { "Not Specified" })
                        SpecRow("Registration No", vehicle.registrationNumber.ifBlank { "Not Registered" })
                        if (isCar) {
                            SpecRow("Fuel Type", vehicle.fuelType.name)
                            SpecRow("Transmission", vehicle.transmission.name)
                            if (vehicle.odometerKm > 0) SpecRow("Odometer Reading", "${vehicle.odometerKm} Km")
                        }
                        SpecRow("Chassis Number (VIN)", vehicle.chassisNumber)
                        SpecRow("Engine Number", vehicle.engineNumber)
                        SpecRow("State Code", vehicle.stateCode)
                        SpecRow("Store Location", vehicle.effectiveStoreId)
                        SpecRow("Inward Date", DateUtils.formatDate(vehicle.inwardTimestamp))
                        SpecRow("Stock Duration", "$daysInStock days")
                    }
                }

                // Financial Breakdown Card
                PremiumCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Financial Margin Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        SpecRow("Inward Cost Price", CurrencyUtils.formatCurrency(vehicle.costPrice))
                        SpecRow("Listed Selling Price", CurrencyUtils.formatCurrency(vehicle.listedPrice))

                        val margin = vehicle.listedPrice - vehicle.costPrice
                        val marginPct = if (vehicle.costPrice > 0) (margin / vehicle.costPrice) * 100 else 0.0
                        SpecRow(
                            "Estimated Profit Margin",
                            "${CurrencyUtils.formatCurrency(margin)} (${String.format(Locale.getDefault(), "%.1f", marginPct)}%)"
                        )
                    }
                }

                // Action Buttons Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (vehicle.status == BikeStatus.AVAILABLE) {
                        SecondaryButton(
                            text = "Reserve Vehicle",
                            onClick = { onReserveVehicle(vehicle.vehicleId, vehicle.effectiveStoreId) },
                            icon = Icons.Rounded.Bookmark
                        )

                        PrimaryButton(
                            text = "Finance Vehicle (Create Loan)",
                            onClick = { onFinanceClick(vehicle.vehicleId) },
                            icon = Icons.Rounded.MonetizationOn
                        )
                    } else if (vehicle.status == BikeStatus.RESERVED) {
                        PrimaryButton(
                            text = "Convert Reservation to Financing",
                            onClick = { onFinanceClick(vehicle.vehicleId) },
                            icon = Icons.Rounded.MonetizationOn
                        )
                    }

                    SecondaryButton(
                        text = "Edit Specs",
                        onClick = { onEditClick(vehicle.vehicleId) },
                        icon = Icons.Rounded.Edit
                    )
                }
            }
        }
    }

    if (showDeleteDialog && vehicle != null) {
        ConfirmationDialog(
            title = "Delete Vehicle Record",
            message = "Are you sure you want to delete ${vehicle.make} ${vehicle.model} (VIN: ${vehicle.chassisNumber})? This operation cannot be reversed.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                onDeleteVehicle(vehicle.vehicleId)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun BikeDetailsContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        BikeDetailsContent(
            bike = PreviewSampleData.sampleBike
        )
    }
}
