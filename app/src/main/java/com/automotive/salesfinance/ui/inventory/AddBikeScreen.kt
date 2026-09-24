package com.automotive.salesfinance.ui.inventory

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.automotive.salesfinance.data.catalog.VehicleCatalogRepository
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.FuelType
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.TransmissionType
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.model.Vehicle
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.model.toBike
import com.automotive.salesfinance.model.toVehicle
import com.automotive.salesfinance.ui.components.CurrencyText
import com.automotive.salesfinance.ui.components.GlassCard
import com.automotive.salesfinance.ui.components.PremiumCard
import com.automotive.salesfinance.ui.components.PremiumTextField
import com.automotive.salesfinance.ui.components.PremiumTopBar
import com.automotive.salesfinance.ui.components.PrimaryButton
import com.automotive.salesfinance.ui.components.QuotaReachedCard
import com.automotive.salesfinance.ui.components.SearchableDropdownField
import com.automotive.salesfinance.ui.preview.PreviewSampleData
import com.automotive.salesfinance.ui.theme.AutomotiveSalesAndFinanceTheme
import com.automotive.salesfinance.viewmodel.DealershipFeatureLimits
import com.automotive.salesfinance.viewmodel.InventoryViewModel
import com.automotive.salesfinance.viewmodel.SubscriptionViewModel
import java.util.Calendar
import java.util.Locale

@Composable
fun AddBikeScreen(
    bikeId: String = "NEW",
    viewModel: InventoryViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val existingVehicle = remember(bikeId) {
        if (bikeId != "NEW" && bikeId.isNotBlank()) {
            viewModel.getVehicleById(bikeId)
        } else null
    }

    val featureLimits by subscriptionViewModel.featureLimits.collectAsState()
    val supportedVehicleTypes by viewModel.supportedVehicleTypes.collectAsState()
    val availableStores by viewModel.availableStores.collectAsState()
    val currentUser by viewModel.authRepository.currentUser.collectAsState()
    val errorMessage by viewModel.operationMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val isStoreRestricted = currentUser?.role == UserRole.STORE_MANAGER || currentUser?.role == UserRole.SALES_USER
    val userStoreId = currentUser?.storeId.orEmpty()

    LaunchedEffect(Unit) {
        viewModel.clearOperationMessage()
    }

    AddVehicleContent(
        vehicleId = bikeId,
        existingVehicle = existingVehicle,
        currentUser = currentUser,
        supportedVehicleTypes = supportedVehicleTypes,
        featureLimits = featureLimits,
        availableStores = availableStores,
        isStoreRestricted = isStoreRestricted,
        userStoreId = userStoreId,
        errorMessage = errorMessage,
        isLoading = isLoading,
        onValidateVehicle = { vehicle ->
            viewModel.validateVehicle(vehicle, if (bikeId != "NEW") bikeId else null)
        },
        onSaveVehicle = { vehicle ->
            viewModel.addOrUpdateVehicle(vehicle, onSuccess = onNavigateBack)
        },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBikeContent(
    bikeId: String,
    existingBike: Bike?,
    currentUser: User? = null,
    featureLimits: DealershipFeatureLimits,
    availableStores: List<Store>,
    isStoreRestricted: Boolean,
    userStoreId: String,
    errorMessage: String?,
    onValidateBike: (String, String, String, String, Int, Double, Double, String, String) -> String?,
    onSaveBike: (Bike) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    AddVehicleContent(
        vehicleId = bikeId,
        existingVehicle = existingBike?.toVehicle(),
        currentUser = currentUser,
        supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR),
        featureLimits = featureLimits,
        availableStores = availableStores,
        isStoreRestricted = isStoreRestricted,
        userStoreId = userStoreId,
        errorMessage = errorMessage,
        onValidateVehicle = { v ->
            onValidateBike(
                v.chassisNumber,
                v.engineNumber,
                v.make,
                v.model,
                v.year,
                v.costPrice,
                v.listedPrice,
                v.stateCode,
                v.storeLocation
            )
        },
        onSaveVehicle = { v -> onSaveBike(v.toBike()) },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleContent(
    vehicleId: String,
    existingVehicle: Vehicle?,
    currentUser: User? = null,
    supportedVehicleTypes: List<VehicleType> = listOf(VehicleType.BIKE, VehicleType.CAR),
    featureLimits: DealershipFeatureLimits,
    availableStores: List<Store>,
    isStoreRestricted: Boolean,
    userStoreId: String,
    errorMessage: String?,
    isLoading: Boolean = false,
    onValidateVehicle: (Vehicle) -> String?,
    onSaveVehicle: (Vehicle) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val catalogRepo = remember { VehicleCatalogRepository() }

    val initialType = existingVehicle?.vehicleType
        ?: if (supportedVehicleTypes.contains(VehicleType.BIKE)) VehicleType.BIKE else supportedVehicleTypes.firstOrNull() ?: VehicleType.BIKE

    var selectedVehicleType by remember { mutableStateOf(initialType) }

    val initialMakeIsCatalog = remember(existingVehicle, selectedVehicleType) {
        existingVehicle != null && catalogRepo.isValidMakeForVehicleType(selectedVehicleType, existingVehicle.make)
    }

    var make by remember(existingVehicle) {
        mutableStateOf(
            if (existingVehicle == null) ""
            else if (initialMakeIsCatalog) existingVehicle.make
            else "Other"
        )
    }

    var customMake by remember(existingVehicle) {
        mutableStateOf(
            if (existingVehicle == null) ""
            else if (!initialMakeIsCatalog || existingVehicle.make == "Other") existingVehicle.make
            else ""
        )
    }

    val initialModelIsCatalog = remember(existingVehicle, selectedVehicleType, make) {
        existingVehicle != null && make != "Other" && catalogRepo.isValidModelForMake(selectedVehicleType, make, existingVehicle.model)
    }

    var model by remember(existingVehicle) {
        mutableStateOf(
            if (existingVehicle == null) ""
            else if (initialModelIsCatalog) existingVehicle.model
            else "Other"
        )
    }

    var customModel by remember(existingVehicle) {
        mutableStateOf(
            if (existingVehicle == null) ""
            else if (!initialModelIsCatalog || existingVehicle.model == "Other") existingVehicle.model
            else ""
        )
    }

    val makeOptions = remember(selectedVehicleType) {
        catalogRepo.getMakesForVehicleType(selectedVehicleType)
    }

    val modelOptions = remember(selectedVehicleType, make) {
        if (make.isNotBlank() && make != "Other") {
            catalogRepo.getModelsForMake(selectedVehicleType, make)
        } else {
            emptyList()
        }
    }

    var chassisNumber by remember { mutableStateOf(existingVehicle?.chassisNumber ?: "") }
    var engineNumber by remember { mutableStateOf(existingVehicle?.engineNumber ?: "") }
    var variant by remember { mutableStateOf(existingVehicle?.variant ?: "") }
    var selectedFuelType by remember { mutableStateOf(existingVehicle?.fuelType ?: FuelType.PETROL) }
    var selectedTransmission by remember { mutableStateOf(existingVehicle?.transmission ?: TransmissionType.MANUAL) }
    var registrationNumber by remember { mutableStateOf(existingVehicle?.registrationNumber ?: "") }
    var color by remember { mutableStateOf(existingVehicle?.color ?: "") }
    var odometerKmStr by remember { mutableStateOf(existingVehicle?.odometerKm?.takeIf { it > 0 }?.toString() ?: "") }

    var yearStr by remember { mutableStateOf((existingVehicle?.year ?: Calendar.getInstance().get(Calendar.YEAR)).toString()) }
    var costPriceStr by remember { mutableStateOf(existingVehicle?.costPrice?.takeIf { it > 0 }?.toLong()?.toString() ?: "") }
    var listedPriceStr by remember { mutableStateOf(existingVehicle?.listedPrice?.takeIf { it > 0 }?.toLong()?.toString() ?: "") }
    var stateCode by remember { mutableStateOf(existingVehicle?.stateCode ?: "TG") }
    var storeLocation by remember { mutableStateOf(existingVehicle?.storeLocation ?: (if (isStoreRestricted && userStoreId.isNotBlank() && userStoreId != "ALL") userStoreId else "TG_Madhapur")) }

    val states = availableStores.map { it.stateCode }.distinct().ifEmpty { listOf("TG", "KA") }

    LaunchedEffect(availableStores) {
        if (isStoreRestricted && userStoreId.isNotBlank() && userStoreId != "ALL") {
            storeLocation = userStoreId
            val storeObj = availableStores.find { it.storeId == userStoreId }
            if (storeObj != null) {
                stateCode = storeObj.stateCode
            }
        } else if (existingVehicle == null && availableStores.isNotEmpty()) {
            val matched = availableStores.find { it.storeId == storeLocation } ?: availableStores.first()
            storeLocation = matched.storeId
            stateCode = matched.stateCode
        }
    }

    val year = yearStr.toIntOrNull() ?: 2026
    val costPrice = costPriceStr.toDoubleOrNull() ?: 0.0
    val listedPrice = listedPriceStr.toDoubleOrNull() ?: 0.0
    val odometerKm = odometerKmStr.toIntOrNull() ?: 0

    val normReg = registrationNumber.trim().uppercase()
    val normColor = color.trim()

    val effectiveMake = if (make == "Other") customMake.trim() else make.trim()
    val effectiveModel = if (model == "Other") customModel.trim() else model.trim()

    val activeDealershipId = currentUser?.dealershipId.orEmpty()

    val draftVehicle = Vehicle(
        vehicleId = if (vehicleId != "NEW") vehicleId else "",
        dealershipId = activeDealershipId,
        vehicleType = selectedVehicleType,
        chassisNumber = chassisNumber,
        engineNumber = engineNumber,
        make = effectiveMake,
        model = effectiveModel,
        variant = variant,
        fuelType = selectedFuelType,
        transmission = selectedTransmission,
        registrationNumber = normReg,
        color = normColor,
        odometerKm = odometerKm,
        year = year,
        costPrice = costPrice,
        listedPrice = listedPrice,
        stateCode = stateCode,
        storeId = storeLocation,
        storeLocation = storeLocation,
        status = existingVehicle?.status ?: BikeStatus.AVAILABLE,
        inwardTimestamp = existingVehicle?.inwardTimestamp ?: System.currentTimeMillis()
    )

    val realTimeError = remember(chassisNumber, engineNumber, make, customMake, model, customModel, color, registrationNumber, yearStr, costPriceStr, listedPriceStr, stateCode, storeLocation, selectedVehicleType) {
        onValidateVehicle(draftVehicle)
    }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = if (vehicleId == "NEW") "Inward New Vehicle" else "Edit Vehicle Specs",
                subtitle = "Stock Registry Specification Form",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subscription Quota Warning
            if (vehicleId == "NEW" && !featureLimits.canAddBike) {
                QuotaReachedCard(
                    featureName = "Inventory Vehicles",
                    currentUsage = featureLimits.currentBikes,
                    limit = featureLimits.maxBikes,
                    onUpgradeClick = { /* Handled in settings */ }
                )
            }

            // Vehicle Type Selector FilterChips if dealership supports multiple types
            if (supportedVehicleTypes.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = selectedVehicleType == VehicleType.BIKE,
                        onClick = {
                            if (selectedVehicleType != VehicleType.BIKE) {
                                selectedVehicleType = VehicleType.BIKE
                                make = ""
                                model = ""
                                customMake = ""
                                customModel = ""
                            }
                        },
                        label = { Text("Two Wheeler (Bike)", fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.DirectionsBike,
                                contentDescription = null
                            )
                        }
                    )
                    FilterChip(
                        selected = selectedVehicleType == VehicleType.CAR,
                        onClick = {
                            if (selectedVehicleType != VehicleType.CAR) {
                                selectedVehicleType = VehicleType.CAR
                                make = ""
                                model = ""
                                customMake = ""
                                customModel = ""
                            }
                        },
                        label = { Text("Four Wheeler (Car)", fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsCar,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            // Profit Margin Live Preview Card
            if (costPrice > 0 && listedPrice > 0) {
                val margin = listedPrice - costPrice
                val marginPercent = if (costPrice > 0) (margin / costPrice) * 100 else 0.0
                val isProfitable = margin >= 0

                AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (isProfitable) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Estimated Profit Margin",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isProfitable) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                CurrencyText(
                                    amount = margin,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (isProfitable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%% Margin", marginPercent),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isProfitable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Vehicle Identification Card
            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "1. Vehicle Identification (${if (selectedVehicleType == VehicleType.CAR) "Car" else "Bike"})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    PremiumTextField(
                        value = chassisNumber,
                        onValueChange = { chassisNumber = it.uppercase().trim() },
                        label = if (selectedVehicleType == VehicleType.CAR) "VIN / Chassis Number" else "Chassis Number",
                        placeholder = if (selectedVehicleType == VehicleType.CAR) "e.g. MA3E1234567890123" else "e.g. ME4KC23128912345",
                        leadingIcon = Icons.Rounded.QrCode,
                        isError = chassisNumber.isNotEmpty() && !chassisNumber.matches(Regex("^[A-Z0-9]{5,17}$")),
                        errorMessage = if (chassisNumber.isNotEmpty() && !chassisNumber.matches(Regex("^[A-Z0-9]{5,17}$"))) "Must be 5-17 alphanumeric characters" else null
                    )

                    PremiumTextField(
                        value = engineNumber,
                        onValueChange = { engineNumber = it.uppercase().trim() },
                        label = "Engine Number",
                        placeholder = "e.g. ENG12345678",
                        leadingIcon = if (selectedVehicleType == VehicleType.CAR) Icons.Rounded.DirectionsCar else Icons.AutoMirrored.Rounded.DirectionsBike,
                        isError = engineNumber.isNotEmpty() && !engineNumber.matches(Regex("^[A-Z0-9]{5,20}$")),
                        errorMessage = if (engineNumber.isNotEmpty() && !engineNumber.matches(Regex("^[A-Z0-9]{5,20}$"))) "Must be 5-20 alphanumeric characters" else null
                    )

                    SearchableDropdownField(
                        label = "Make / Brand",
                        selectedOption = make,
                        options = makeOptions,
                        onOptionSelected = { selected ->
                            if (make != selected) {
                                make = selected
                                model = ""
                                customModel = ""
                                if (selected != "Other") {
                                    customMake = ""
                                }
                            }
                        },
                        placeholder = if (selectedVehicleType == VehicleType.CAR) "e.g. Hyundai" else "e.g. Honda",
                        leadingIcon = {
                            Icon(
                                imageVector = if (selectedVehicleType == VehicleType.CAR) Icons.Rounded.DirectionsCar else Icons.AutoMirrored.Rounded.DirectionsBike,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    SearchableDropdownField(
                        label = "Model Name",
                        selectedOption = model,
                        options = modelOptions,
                        onOptionSelected = { selected ->
                            if (model != selected) {
                                model = selected
                                if (selected != "Other") {
                                    customModel = ""
                                }
                            }
                        },
                        placeholder = if (selectedVehicleType == VehicleType.CAR) "e.g. Creta" else "e.g. Activa 6G",
                        enabled = make.isNotBlank(),
                        leadingIcon = {
                            Icon(
                                imageVector = if (selectedVehicleType == VehicleType.CAR) Icons.Rounded.DirectionsCar else Icons.AutoMirrored.Rounded.DirectionsBike,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (make == "Other") {
                        PremiumTextField(
                            value = customMake,
                            onValueChange = { customMake = it },
                            label = "Custom Make Name",
                            placeholder = "e.g. Custom Brand",
                            isError = customMake.isBlank(),
                            errorMessage = if (customMake.isBlank()) "Custom Make Name is required" else null
                        )
                    }

                    if (model == "Other") {
                        PremiumTextField(
                            value = customModel,
                            onValueChange = { customModel = it },
                            label = "Custom Model Name",
                            placeholder = "e.g. Custom Model",
                            isError = customModel.isBlank(),
                            errorMessage = if (customModel.isBlank()) "Custom Model Name is required" else null
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PremiumTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = "Color",
                            placeholder = if (selectedVehicleType == VehicleType.CAR) "e.g. Polar White" else "e.g. Black",
                            modifier = Modifier.weight(1f)
                        )

                        PremiumTextField(
                            value = registrationNumber,
                            onValueChange = { registrationNumber = it.uppercase() },
                            label = "Registration Number",
                            placeholder = if (selectedVehicleType == VehicleType.CAR) "e.g. TS09FA1234" else "e.g. TS09AB1234",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Car-Specific Fields: Variant, Fuel Type, Transmission, Odometer
                    if (selectedVehicleType == VehicleType.CAR) {
                        PremiumTextField(
                            value = variant,
                            onValueChange = { variant = it },
                            label = "Variant / Trim",
                            placeholder = "e.g. 1.5 Petrol SX / EV Empowered"
                        )

                        SearchableDropdownField(
                            label = "Fuel Type",
                            selectedOption = selectedFuelType.name,
                            options = FuelType.entries.map { it.name },
                            onOptionSelected = { selectedName ->
                                FuelType.entries.find { it.name == selectedName }?.let {
                                    selectedFuelType = it
                                }
                            },
                            allowCustomInput = false,
                            modifier = Modifier.fillMaxWidth()
                        )

                        SearchableDropdownField(
                            label = "Transmission",
                            selectedOption = selectedTransmission.name,
                            options = TransmissionType.entries.map { it.name },
                            onOptionSelected = { selectedName ->
                                TransmissionType.entries.find { it.name == selectedName }?.let {
                                    selectedTransmission = it
                                }
                            },
                            allowCustomInput = false,
                            modifier = Modifier.fillMaxWidth()
                        )

                        PremiumTextField(
                            value = odometerKmStr,
                            onValueChange = { odometerKmStr = it },
                            label = "Odometer Reading (Km)",
                            placeholder = "e.g. 12000",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // Pricing & Store Card
            PremiumCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "2. Pricing & Showroom Assignment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PremiumTextField(
                            value = yearStr,
                            onValueChange = { yearStr = it },
                            label = "Manufacturing Year",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        PremiumTextField(
                            value = costPriceStr,
                            onValueChange = { costPriceStr = it },
                            label = "Cost Price (₹)",
                            placeholder = if (selectedVehicleType == VehicleType.CAR) "1100000" else "85000",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PremiumTextField(
                        value = listedPriceStr,
                        onValueChange = { listedPriceStr = it },
                        label = "Listed Showroom Selling Price (₹)",
                        placeholder = if (selectedVehicleType == VehicleType.CAR) "1350000" else "98000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    SearchableDropdownField(
                        label = "State Code",
                        selectedOption = stateCode,
                        options = states,
                        onOptionSelected = { st ->
                            stateCode = st
                        },
                        allowCustomInput = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val isLocked = isStoreRestricted && userStoreId.isNotBlank() && userStoreId != "ALL"
                    if (isLocked) {
                        PremiumTextField(
                            value = storeLocation,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = "Store Location (Locked)",
                            leadingIcon = Icons.Rounded.Lock,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val storeOptions = availableStores.map { store -> store.storeId }
                        SearchableDropdownField(
                            label = "Store Location",
                            selectedOption = storeLocation,
                            options = storeOptions,
                            onOptionSelected = { selectedStoreId ->
                                storeLocation = selectedStoreId
                                availableStores.find { it.storeId == selectedStoreId }?.let { storeObj ->
                                    stateCode = storeObj.stateCode
                                }
                            },
                            allowCustomInput = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            val displayError = realTimeError ?: errorMessage
            if (displayError != null) {
                Text(
                    text = displayError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            PrimaryButton(
                text = if (vehicleId == "NEW") "Save & Inward Vehicle" else "Update Specs",
                onClick = { onSaveVehicle(draftVehicle) },
                enabled = !isLoading && realTimeError == null && (vehicleId != "NEW" || featureLimits.canAddBike),
                icon = Icons.Rounded.Save
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet", device = Devices.TABLET)
@Composable
private fun AddVehicleContentPreview() {
    AutomotiveSalesAndFinanceTheme {
        AddVehicleContent(
            vehicleId = "NEW",
            existingVehicle = null,
            supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR),
            featureLimits = DealershipFeatureLimits(canAddBike = true, currentBikes = 5, maxBikes = 100),
            availableStores = PreviewSampleData.sampleStores,
            isStoreRestricted = false,
            userStoreId = "ALL",
            errorMessage = null,
            onValidateVehicle = { null },
            onSaveVehicle = {}
        )
    }
}
