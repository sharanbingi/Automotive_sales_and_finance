package com.automotive.salesfinance.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.FeatureAccessManager
import com.automotive.salesfinance.data.catalog.VehicleCatalogRepository
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.model.Vehicle
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.model.toBike
import com.automotive.salesfinance.model.toVehicle
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.utils.ValidationUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption {
    NEWEST_INWARD,
    OLDEST_INWARD,
    PRICE_LOW_HIGH,
    PRICE_HIGH_LOW
}

data class InventoryUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class DeadStockMetrics(
    val totalCount: Int = 0,
    val totalCapitalTiedUp: Double = 0.0,
    val totalListedValue: Double = 0.0,
    val oldestBike: Bike? = null,
    val oldestVehicle: Vehicle? = null,
    val maxDaysInStock: Long = 0L
)

private fun safeLogD(tag: String, msg: String) {
    try {
        Log.d(tag, msg)
    } catch (_: Throwable) {
        println("[$tag] $msg")
    }
}

class InventoryViewModel(
    private val inventoryRepository: InventoryRepository,
    private val storeRepository: StoreRepository,
    val authRepository: AuthRepository,
    private val dealershipRepository: DealershipRepository,
    val vehicleCatalogRepository: VehicleCatalogRepository = VehicleCatalogRepository()
) : ViewModel() {

    val isDemoMode: StateFlow<Boolean> = authRepository.isDemoMode

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedState = MutableStateFlow("ALL")
    val selectedState: StateFlow<String> = _selectedState.asStateFlow()

    private val _selectedStore = MutableStateFlow("ALL")
    val selectedStore: StateFlow<String> = _selectedStore.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _selectedVehicleTypeFilter = MutableStateFlow("ALL")
    val selectedVehicleTypeFilter: StateFlow<String> = _selectedVehicleTypeFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST_INWARD)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()
    val errorMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow(true)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    val supportedVehicleTypes: StateFlow<List<VehicleType>> = combine(
        authRepository.currentUser,
        dealershipRepository.dealershipsFlow
    ) { user, dealerships ->
        val dId = user?.dealershipId.orEmpty().ifBlank {
            if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
        }
        val dealership = dealerships.find { it.dealershipId == dId }
            ?: if (dId == DemoData.DEMO_DEALERSHIP_ID && authRepository.isDemoMode.value) DemoData.dealerships.firstOrNull() else null
        dealership?.supportedVehicleTypes ?: listOf(VehicleType.BIKE, VehicleType.CAR)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(VehicleType.BIKE, VehicleType.CAR))

    val allVehicles: StateFlow<List<Vehicle>> = inventoryRepository.vehiclesFlow
    val allBikes: StateFlow<List<Bike>> = inventoryRepository.bikesFlow

    val filteredVehicles: StateFlow<List<Vehicle>> = combine(
        inventoryRepository.vehiclesFlow,
        authRepository.currentUser,
        _selectedState,
        _selectedStore,
        _searchQuery,
        _statusFilter,
        _selectedVehicleTypeFilter,
        _sortOption
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val vehicles = flows[0] as List<Vehicle>
        val user = flows[1] as? User
        val stateCode = flows[2] as String
        val storeId = flows[3] as String
        val query = flows[4] as String
        val status = flows[5] as String
        val vehicleTypeFilter = flows[6] as String
        val sort = flows[7] as SortOption

        val dId = user?.dealershipId.orEmpty()

        vehicles.filter { vehicle ->
            val matchesDealership = dId.isBlank() || vehicle.dealershipId == dId
            val matchesState = stateCode == "ALL" || vehicle.stateCode.equals(stateCode, ignoreCase = true)
            val matchesStore = storeId == "ALL" || vehicle.effectiveStoreId.equals(storeId, ignoreCase = true) || vehicle.storeLocation.equals(storeId, ignoreCase = true)
            val matchesStatus = when (status.uppercase()) {
                "ALL" -> true
                "AVAILABLE" -> vehicle.status == BikeStatus.AVAILABLE
                "RESERVED" -> vehicle.status == BikeStatus.RESERVED
                "FINANCED" -> vehicle.status == BikeStatus.FINANCED
                else -> true
            }
            val matchesVehicleType = when (vehicleTypeFilter.uppercase()) {
                "ALL" -> true
                "BIKE", "BIKES" -> vehicle.vehicleType == VehicleType.BIKE
                "CAR", "CARS" -> vehicle.vehicleType == VehicleType.CAR
                else -> true
            }
            val trimmedQuery = query.trim().lowercase()
            val matchesQuery = trimmedQuery.isEmpty() ||
                vehicle.chassisNumber.lowercase().contains(trimmedQuery) ||
                vehicle.engineNumber.lowercase().contains(trimmedQuery) ||
                vehicle.make.lowercase().contains(trimmedQuery) ||
                vehicle.model.lowercase().contains(trimmedQuery) ||
                vehicle.variant.lowercase().contains(trimmedQuery) ||
                vehicle.registrationNumber.lowercase().contains(trimmedQuery)

            matchesDealership && matchesState && matchesStore && matchesStatus && matchesVehicleType && matchesQuery
        }.let { list ->
            when (sort) {
                SortOption.NEWEST_INWARD -> list.sortedByDescending { it.inwardTimestamp }
                SortOption.OLDEST_INWARD -> list.sortedBy { it.inwardTimestamp }
                SortOption.PRICE_LOW_HIGH -> list.sortedBy { it.listedPrice }
                SortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.listedPrice }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredBikes: StateFlow<List<Bike>> = filteredVehicles.map { list ->
        list.map { it.toBike() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deadStockVehicles: StateFlow<List<Vehicle>> = combine(
        inventoryRepository.vehiclesFlow,
        authRepository.currentUser,
        _selectedState,
        _selectedStore
    ) { vehicles, user, stateCode, storeId ->
        val dId = user?.dealershipId.orEmpty()
        vehicles.filter { vehicle ->
            (dId.isBlank() || vehicle.dealershipId == dId) &&
                vehicle.status == BikeStatus.AVAILABLE &&
                DateUtils.isDeadStock(vehicle.inwardTimestamp, 60) &&
                (stateCode == "ALL" || vehicle.stateCode.equals(stateCode, ignoreCase = true)) &&
                (storeId == "ALL" || vehicle.effectiveStoreId.equals(storeId, ignoreCase = true) || vehicle.storeLocation.equals(storeId, ignoreCase = true))
        }.sortedBy { it.inwardTimestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deadStockBikes: StateFlow<List<Bike>> = deadStockVehicles.map { list ->
        list.map { it.toBike() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deadStockMetrics: StateFlow<DeadStockMetrics> = deadStockVehicles.map { deadStockList ->
        val totalCapital = deadStockList.sumOf { it.costPrice }
        val totalListed = deadStockList.sumOf { it.listedPrice }
        val oldest = deadStockList.minByOrNull { it.inwardTimestamp }
        val maxDays = oldest?.let { DateUtils.calculateDaysInStock(it.inwardTimestamp) } ?: 0L

        DeadStockMetrics(
            totalCount = deadStockList.size,
            totalCapitalTiedUp = totalCapital,
            totalListedValue = totalListed,
            oldestBike = oldest?.toBike(),
            oldestVehicle = oldest,
            maxDaysInStock = maxDays
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeadStockMetrics())

    fun setSelectedState(stateCode: String) {
        _selectedState.value = stateCode
        if (stateCode != "ALL" && _selectedStore.value != "ALL") {
            val store = storeRepository.getStoreById(_selectedStore.value)
            if (store != null && store.stateCode != stateCode) {
                _selectedStore.value = "ALL"
            }
        }
    }

    fun setSelectedStore(storeId: String) {
        _selectedStore.value = storeId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
    }

    fun setSelectedVehicleTypeFilter(typeFilter: String) {
        _selectedVehicleTypeFilter.value = typeFilter
    }

    val availableStores: StateFlow<List<Store>> = combine(
        storeRepository.storesFlow,
        authRepository.currentUser
    ) { stores, user ->
        val dId = user?.dealershipId.orEmpty().ifBlank {
            if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
        }
        stores.filter { store ->
            val matchesDealership = dId.isBlank() || store.dealershipId == dId
            val matchesRole = when {
                user == null || user.role == UserRole.ADMIN || user.role == UserRole.DEALERSHIP_ADMIN -> true
                user.role == UserRole.STATE_MANAGER -> store.stateCode == user.stateCode || user.stateCode == "ALL"
                else -> store.storeId == user.storeId || user.storeId == "ALL"
            }
            matchesDealership && matchesRole
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    fun getVehicleById(vehicleId: String): Vehicle? {
        return inventoryRepository.getVehicleById(vehicleId)
    }

    fun getBikeById(bikeId: String): Bike? {
        return inventoryRepository.getBikeById(bikeId)
    }

    fun getAvailableMakes(vehicleType: VehicleType, search: String = ""): List<String> =
        vehicleCatalogRepository.getMakesForVehicleType(vehicleType, search)

    fun getAvailableModels(vehicleType: VehicleType, make: String, search: String = ""): List<String> =
        vehicleCatalogRepository.getModelsForMake(vehicleType, make, search)

    fun isValidMake(vehicleType: VehicleType, make: String): Boolean =
        vehicleCatalogRepository.isValidMakeForVehicleType(vehicleType, make)

    fun isValidModel(vehicleType: VehicleType, make: String, model: String): Boolean =
        vehicleCatalogRepository.isValidModelForMake(vehicleType, make, model)

    fun validateVehicle(
        vehicle: Vehicle,
        excludeVehicleId: String? = null,
        customMake: String = "",
        customModel: String = ""
    ): String? {
        if (vehicle.chassisNumber.isBlank() || !ValidationUtils.isValidChassisNumber(vehicle.chassisNumber)) {
            return "Invalid Chassis Number (Must be 5-17 alphanumeric characters)"
        }
        if (!inventoryRepository.isChassisUnique(vehicle.chassisNumber, excludeVehicleId)) {
            return "Duplicate Chassis Number: ${vehicle.chassisNumber} already exists in inventory"
        }
        if (vehicle.engineNumber.isBlank() || !ValidationUtils.isValidEngineNumber(vehicle.engineNumber)) {
            return "Invalid Engine Number (Must be 5-20 alphanumeric characters)"
        }
        if (!inventoryRepository.isEngineUnique(vehicle.engineNumber, excludeVehicleId)) {
            return "Duplicate Engine Number: ${vehicle.engineNumber} already exists in inventory"
        }

        val effectiveMake = if (vehicle.make.trim().equals("Other", ignoreCase = true)) {
            customMake.trim()
        } else {
            vehicle.make.trim()
        }

        if (effectiveMake.isBlank()) {
            return if (vehicle.make.trim().equals("Other", ignoreCase = true)) {
                "Custom Make Name is required when 'Other' is selected"
            } else {
                "Make cannot be empty"
            }
        }

        val effectiveModel = if (vehicle.model.trim().equals("Other", ignoreCase = true)) {
            customModel.trim()
        } else {
            vehicle.model.trim()
        }

        if (effectiveModel.isBlank()) {
            return if (vehicle.model.trim().equals("Other", ignoreCase = true)) {
                "Custom Model Name is required when 'Other' is selected"
            } else {
                "Model cannot be empty"
            }
        }
        val normColor = vehicle.color.trim()
        if (normColor.isBlank()) {
            return "Color is required"
        }
        val normReg = vehicle.registrationNumber.trim().uppercase()
        if (normReg.isNotBlank()) {
            if (!inventoryRepository.isRegistrationNumberUnique(normReg, vehicle.dealershipId, excludeVehicleId)) {
                return "Registration number $normReg already exists in this dealership"
            }
        }
        if (!ValidationUtils.isValidYear(vehicle.year)) {
            return "Invalid Manufacturing Year (${vehicle.year})"
        }
        if (!ValidationUtils.isValidPrice(vehicle.costPrice)) {
            return "Cost Price must be greater than zero"
        }
        if (!ValidationUtils.isValidPrice(vehicle.listedPrice)) {
            return "Listed Price must be greater than zero"
        }
        if (vehicle.stateCode.isBlank()) {
            return "Please select or enter State Code"
        }
        if (vehicle.effectiveStoreId.isBlank() && vehicle.storeLocation.isBlank()) {
            return "Please select or enter Store Location"
        }
        if (vehicle.vehicleType == VehicleType.CAR) {
            if (vehicle.variant.isBlank()) {
                return "Variant / Trim cannot be empty"
            }
            if (vehicle.odometerKm < 0) {
                return "Odometer reading cannot be negative"
            }
        }
        return null
    }

    fun validateBike(
        chassisNumber: String,
        engineNumber: String,
        make: String,
        model: String,
        year: Int,
        costPrice: Double,
        listedPrice: Double,
        stateCode: String,
        storeLocation: String,
        excludeBikeId: String? = null,
        color: String = "Black",
        registrationNumber: String = ""
    ): String? {
        val dummyVehicle = Vehicle(
            vehicleId = excludeBikeId.orEmpty(),
            vehicleType = VehicleType.BIKE,
            chassisNumber = chassisNumber,
            engineNumber = engineNumber,
            make = make,
            model = model,
            color = color,
            registrationNumber = registrationNumber,
            year = year,
            costPrice = costPrice,
            listedPrice = listedPrice,
            stateCode = stateCode,
            storeId = storeLocation,
            storeLocation = storeLocation
        )
        return validateVehicle(dummyVehicle, excludeBikeId)
    }

    fun addOrUpdateVehicle(vehicle: Vehicle, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (_isLoading.value) return
        val user = authRepository.currentUser.value
        val activeDealershipId = if (!authRepository.isDemoMode.value) {
            user?.dealershipId.orEmpty()
        } else {
            user?.dealershipId.orEmpty().ifBlank { DemoData.DEMO_DEALERSHIP_ID }
        }

        safeLogD(
            "ASF_INVENTORY_RUNTIME",
            "TENANT SOURCE | mode = ${if (authRepository.isDemoMode.value) "DEMO" else "PRODUCTION"} | authenticatedDealershipId = '${user?.dealershipId}' | resolvedDealershipId = '$activeDealershipId'"
        )

        if (!authRepository.isDemoMode.value && activeDealershipId.isBlank()) {
            val err = "Unable to determine dealership. Please sign in again."
            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = false, errorMessage = err)
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        val dealership = dealershipRepository.dealershipsFlow.value.find { it.dealershipId == activeDealershipId }
            ?: if (activeDealershipId == DemoData.DEMO_DEALERSHIP_ID && authRepository.isDemoMode.value) DemoData.dealerships.firstOrNull() else null
        val plan = SubscriptionPlan.getPlanById(dealership?.subscriptionPlan ?: "STARTER")

        if (!FeatureAccessManager.isSubscriptionActive(dealership)) {
            val err = "Subscription inactive: account is in read-only mode"
            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = false, errorMessage = err)
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        val currentVehicleId = if (vehicle.vehicleId.isNotBlank() && vehicle.vehicleId != "NEW") vehicle.vehicleId else null

        val isNewVehicle = currentVehicleId == null || inventoryRepository.getVehicleById(vehicle.vehicleId) == null
        if (isNewVehicle) {
            val currentVehicleCount = inventoryRepository.vehiclesFlow.value.count {
                activeDealershipId.isBlank() || it.dealershipId == activeDealershipId
            }
            if (!FeatureAccessManager.canAddVehicle(currentVehicleCount, plan)) {
                val err = "Plan limit reached: your current plan allows up to ${plan.maxBikes} vehicles"
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = false, errorMessage = err)
                _operationMessage.value = err
                _isSuccess.value = false
                onError(err)
                return
            }
        }

        val normColor = vehicle.color.trim()
        val normReg = vehicle.registrationNumber.trim().uppercase()

        val vehicleToValidate = vehicle.copy(
            color = normColor,
            registrationNumber = normReg,
            dealershipId = activeDealershipId
        )

        val validationError = validateVehicle(
            vehicle = vehicleToValidate,
            excludeVehicleId = currentVehicleId
        )

        if (validationError != null) {
            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = false, errorMessage = validationError)
            _operationMessage.value = validationError
            _isSuccess.value = false
            onError(validationError)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            _isLoading.value = true
            val preparedVehicle = if (vehicleToValidate.vehicleId.isBlank() || vehicleToValidate.vehicleId == "NEW") {
                val prefix = if (vehicleToValidate.vehicleType == VehicleType.CAR) "CAR" else "BIKE"
                vehicleToValidate.copy(vehicleId = "${prefix}_${System.currentTimeMillis()}")
            } else {
                vehicleToValidate
            }
            val vehicleToSave = preparedVehicle.copy(dealershipId = activeDealershipId)

            val result = inventoryRepository.addOrUpdateVehicle(vehicleToSave)
            _isLoading.value = false

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    successMessage = "Vehicle added successfully",
                    errorMessage = null
                )
                _operationMessage.value = "Vehicle added successfully"
                _isSuccess.value = true
                safeLogD("ASF_INVENTORY_RUNTIME", "ASF_INVENTORY_RUNTIME: VIEWMODEL SAVE SUCCESS")
                onSuccess()
            } else {
                val ex = result.exceptionOrNull()
                val codeName = (ex as? FirebaseFirestoreException)?.code?.name
                val errorMsg = when {
                    codeName == "PERMISSION_DENIED" || ex?.message?.contains("Permission denied", ignoreCase = true) == true ->
                        "Permission denied: unable to save vehicle to dealership database."
                    codeName == "UNAVAILABLE" || ex?.message?.contains("Network unavailable", ignoreCase = true) == true ->
                        "Network unavailable: check your internet connection and try again."
                    else -> ex?.message ?: "Unable to save vehicle. Please try again."
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = false,
                    errorMessage = errorMsg,
                    successMessage = null
                )
                _operationMessage.value = errorMsg
                _isSuccess.value = false
                safeLogD("ASF_INVENTORY_RUNTIME", "ASF_INVENTORY_RUNTIME: VIEWMODEL SAVE FAILURE | message = $errorMsg")
                onError(errorMsg)
            }
        }
    }

    fun addOrUpdateBike(bike: Bike, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        addOrUpdateVehicle(bike.toVehicle(), onSuccess, onError)
    }

    fun deleteVehicle(vehicleId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (_isLoading.value) return
        val user = authRepository.currentUser.value
        val dId = user?.dealershipId.orEmpty().ifBlank {
            if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
        }
        val dealership = dealershipRepository.dealershipsFlow.value.find { it.dealershipId == dId }
            ?: if (dId == DemoData.DEMO_DEALERSHIP_ID && authRepository.isDemoMode.value) DemoData.dealerships.firstOrNull() else null

        if (!FeatureAccessManager.isSubscriptionActive(dealership)) {
            val err = "Subscription inactive: account is in read-only mode"
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = inventoryRepository.deleteVehicle(vehicleId)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _operationMessage.value = "Vehicle deleted successfully"
                    _isSuccess.value = true
                    onSuccess()
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to delete vehicle"
                    _operationMessage.value = msg
                    _isSuccess.value = false
                    onError(msg)
                }
            )
        }
    }

    fun deleteBike(bikeId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        deleteVehicle(bikeId, onSuccess, onError)
    }

    fun reserveVehicle(vehicleId: String, storeLocation: String, onSuccess: () -> Unit = {}) {
        if (_isLoading.value) return
        val user = authRepository.currentUser.value
        val dId = user?.dealershipId.orEmpty().ifBlank {
            if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
        }
        val dealership = dealershipRepository.dealershipsFlow.value.find { it.dealershipId == dId }
            ?: if (dId == DemoData.DEMO_DEALERSHIP_ID && authRepository.isDemoMode.value) DemoData.dealerships.firstOrNull() else null

        if (!FeatureAccessManager.isSubscriptionActive(dealership)) {
            _operationMessage.value = "Subscription inactive: account is in read-only mode"
            _isSuccess.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = inventoryRepository.reserveVehicle(storeLocation, vehicleId)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _operationMessage.value = "Vehicle marked as RESERVED"
                    _isSuccess.value = true
                    onSuccess()
                },
                onFailure = { err ->
                    _operationMessage.value = err.message ?: "Failed to reserve vehicle"
                    _isSuccess.value = false
                }
            )
        }
    }

    fun reserveBike(bikeId: String, storeLocation: String, onSuccess: () -> Unit = {}) {
        reserveVehicle(vehicleId = bikeId, storeLocation = storeLocation, onSuccess = onSuccess)
    }

    fun markVehicleFinanced(vehicleId: String, storeLocation: String, onSuccess: () -> Unit = {}) {
        if (_isLoading.value) return
        val user = authRepository.currentUser.value
        val dId = user?.dealershipId.orEmpty().ifBlank {
            if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
        }
        val dealership = dealershipRepository.dealershipsFlow.value.find { it.dealershipId == dId }
            ?: if (dId == DemoData.DEMO_DEALERSHIP_ID && authRepository.isDemoMode.value) DemoData.dealerships.firstOrNull() else null

        if (!FeatureAccessManager.isSubscriptionActive(dealership)) {
            _operationMessage.value = "Subscription inactive: account is in read-only mode"
            _isSuccess.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = inventoryRepository.markVehicleFinanced(storeLocation, vehicleId)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _operationMessage.value = "Vehicle marked as FINANCED"
                    _isSuccess.value = true
                    onSuccess()
                },
                onFailure = { err ->
                    _operationMessage.value = err.message ?: "Failed to mark vehicle as financed"
                    _isSuccess.value = false
                }
            )
        }
    }

    fun markBikeFinanced(bikeId: String, storeLocation: String, onSuccess: () -> Unit = {}) {
        markVehicleFinanced(vehicleId = bikeId, storeLocation = storeLocation, onSuccess = onSuccess)
    }
}
