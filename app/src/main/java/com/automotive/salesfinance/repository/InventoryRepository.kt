package com.automotive.salesfinance.repository

import android.util.Log
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Vehicle
import com.automotive.salesfinance.model.toBike
import com.automotive.salesfinance.model.toVehicle
import com.automotive.salesfinance.utils.DateUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun safeLogD(tag: String, msg: String) {
    try {
        Log.d(tag, msg)
    } catch (_: Throwable) {
        println("[$tag] $msg")
    }
}

open class InventoryRepository(private val authRepository: AuthRepository) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _vehiclesFlow = MutableStateFlow<List<Vehicle>>(
        if (authRepository.isDemoMode.value) DemoData.vehicles else emptyList()
    )
    val vehiclesFlow: StateFlow<List<Vehicle>> = _vehiclesFlow.asStateFlow()

    private val _bikesFlow = MutableStateFlow<List<Bike>>(
        if (authRepository.isDemoMode.value) DemoData.vehicles.map { it.toBike() } else emptyList()
    )
    val bikesFlow: StateFlow<List<Bike>> = _bikesFlow.asStateFlow()

    private fun syncBikesFromVehicles() {
        _bikesFlow.value = _vehiclesFlow.value.map { it.toBike() }
    }

    fun clearInMemoryState() {
        _vehiclesFlow.value = emptyList()
        _bikesFlow.value = emptyList()
    }

    init {
        authRepository.registerClearListener {
            clearInMemoryState()
        }
        CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch {
            authRepository.isDemoMode.collect { isDemo ->
                if (!isDemo) {
                    _vehiclesFlow.value = _vehiclesFlow.value.filterNot { v ->
                        DemoData.vehicles.any { it.vehicleId == v.vehicleId }
                    }
                    syncBikesFromVehicles()
                } else if (_vehiclesFlow.value.isEmpty()) {
                    _vehiclesFlow.value = DemoData.vehicles
                    syncBikesFromVehicles()
                }
            }
        }
    }

    // Vehicle Methods
    fun getAllVehicles(): List<Vehicle> = _vehiclesFlow.value

    fun getVehicles(dealershipId: String = ""): List<Vehicle> = getVehiclesForDealership(dealershipId)

    fun getVehiclesForDealership(dealershipId: String): List<Vehicle> {
        if (dealershipId.isBlank()) {
            return if (authRepository.isDemoMode.value) {
                _vehiclesFlow.value.filter { it.dealershipId == DemoData.DEMO_DEALERSHIP_ID }
            } else {
                emptyList()
            }
        }
        return _vehiclesFlow.value.filter { it.dealershipId == dealershipId }
    }

    fun getVehiclesForStore(dealershipId: String, storeId: String): List<Vehicle> {
        val vehicles = getVehiclesForDealership(dealershipId)
        if (storeId == "ALL") return vehicles
        return vehicles.filter { it.effectiveStoreId == storeId || it.storeLocation == storeId }
    }

    fun getVehiclesForState(dealershipId: String, stateCode: String): List<Vehicle> {
        val vehicles = getVehiclesForDealership(dealershipId)
        if (stateCode == "ALL") return vehicles
        return vehicles.filter { it.stateCode.equals(stateCode, ignoreCase = true) }
    }

    fun getVehicleById(vehicleId: String): Vehicle? {
        return _vehiclesFlow.value.find { it.vehicleId == vehicleId }
    }

    fun getStoreInventory(dealershipId: String = "", storeId: String = "ALL"): List<Vehicle> {
        val dId = dealershipId.ifBlank { if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else "" }
        return getVehiclesForStore(dId, storeId)
    }

    suspend fun fetchVehiclesFromFirestore(dealershipId: String = ""): List<Vehicle> {
        if (authRepository.isDemoMode.value) {
            return if (dealershipId.isBlank()) _vehiclesFlow.value else getVehiclesForDealership(dealershipId)
        }
        if (firestore == null) {
            _vehiclesFlow.value = _vehiclesFlow.value.filterNot { v -> DemoData.vehicles.any { it.vehicleId == v.vehicleId } }
            syncBikesFromVehicles()
            return if (dealershipId.isBlank()) _vehiclesFlow.value else getVehiclesForDealership(dealershipId)
        }
        return try {
            val allFetched = mutableListOf<Vehicle>()
            if (dealershipId.isBlank()) {
                val snapshot = firestore?.collectionGroup("vehicles")?.limit(200)?.get()?.await()
                snapshot?.documents?.forEach { doc ->
                    try {
                        val v = doc.toObject(Vehicle::class.java)
                        if (v != null) allFetched.add(v)
                    } catch (_: Exception) {}
                }
            } else {
                // Read directly from /dealerships/{dealershipId}/vehicles
                val directVehiclesSnap = firestore?.collection("dealerships")
                    ?.document(dealershipId)
                    ?.collection("vehicles")
                    ?.limit(200)
                    ?.get()
                    ?.await()

                directVehiclesSnap?.documents?.forEach { doc ->
                    try {
                        val v = doc.toObject(Vehicle::class.java)
                        if (v != null && allFetched.none { it.vehicleId == v.vehicleId }) {
                            allFetched.add(v)
                        }
                    } catch (_: Exception) {}
                }

                // Read legacy /dealerships/{dealershipId}/bikes if present
                val directBikesSnap = firestore?.collection("dealerships")
                    ?.document(dealershipId)
                    ?.collection("bikes")
                    ?.limit(200)
                    ?.get()
                    ?.await()

                directBikesSnap?.documents?.forEach { doc ->
                    try {
                        val b = doc.toObject(Bike::class.java)
                        if (b != null && allFetched.none { it.vehicleId == b.bikeId }) {
                            allFetched.add(b.toVehicle())
                        }
                    } catch (_: Exception) {}
                }

                val storesSnapshot = firestore?.collection("dealerships")
                    ?.document(dealershipId)
                    ?.collection("stores")
                    ?.limit(50)
                    ?.get()
                    ?.await()

                storesSnapshot?.documents?.forEach { storeDoc ->
                    val vehiclesSnap = storeDoc.reference.collection("vehicles").limit(100).get().await()
                    vehiclesSnap.documents.forEach { doc ->
                        try {
                            val v = doc.toObject(Vehicle::class.java)
                            if (v != null && allFetched.none { it.vehicleId == v.vehicleId }) {
                                allFetched.add(v)
                            }
                        } catch (_: Exception) {}
                    }
                    // Legacy bikes subcollection merge
                    val bikesSnap = storeDoc.reference.collection("bikes").limit(100).get().await()
                    bikesSnap.documents.forEach { doc ->
                        try {
                            val b = doc.toObject(Bike::class.java)
                            if (b != null && allFetched.none { it.vehicleId == b.bikeId }) {
                                allFetched.add(b.toVehicle())
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            if (dealershipId.isBlank()) {
                _vehiclesFlow.value = allFetched
            } else {
                val current = _vehiclesFlow.value.filterNot { it.dealershipId == dealershipId || DemoData.vehicles.any { demo -> demo.vehicleId == it.vehicleId } }.toMutableList()
                current.addAll(allFetched)
                _vehiclesFlow.value = current
            }
            syncBikesFromVehicles()
            allFetched
        } catch (e: Exception) {
            val liveOnly = _vehiclesFlow.value.filterNot { v -> DemoData.vehicles.any { it.vehicleId == v.vehicleId } }
            _vehiclesFlow.value = liveOnly
            syncBikesFromVehicles()
            if (dealershipId.isBlank()) liveOnly else liveOnly.filter { it.dealershipId == dealershipId }
        }
    }

    fun isChassisUnique(chassis: String, excludeVehicleId: String? = null, excludeBikeId: String? = null): Boolean {
        val targetExcludeId = excludeVehicleId ?: excludeBikeId
        val trimmed = chassis.trim().uppercase()
        if (trimmed.isBlank()) return true
        return _vehiclesFlow.value.none {
            it.chassisNumber.equals(trimmed, ignoreCase = true) && it.vehicleId != targetExcludeId
        }
    }

    fun isEngineUnique(engine: String, excludeVehicleId: String? = null, excludeBikeId: String? = null): Boolean {
        val targetExcludeId = excludeVehicleId ?: excludeBikeId
        val trimmed = engine.trim().uppercase()
        if (trimmed.isBlank()) return true
        return _vehiclesFlow.value.none {
            it.engineNumber.equals(trimmed, ignoreCase = true) && it.vehicleId != targetExcludeId
        }
    }

    fun isRegistrationNumberUnique(
        registrationNumber: String,
        dealershipId: String,
        currentVehicleId: String? = null
    ): Boolean {
        if (registrationNumber.isBlank()) return true
        val normReg = registrationNumber.trim().uppercase()
        val vehicles = getVehicles(dealershipId)
        return vehicles.none {
            it.registrationNumber.trim().uppercase() == normReg && it.vehicleId != currentVehicleId
        }
    }

    fun getDeadStockVehicles(dealershipId: String = "", storeId: String? = null, daysThreshold: Int = 60): List<Vehicle> {
        val baseList = if (storeId != null && storeId != "ALL") {
            getVehiclesForStore(dealershipId, storeId)
        } else {
            getVehiclesForDealership(dealershipId)
        }
        return baseList.filter {
            it.status == BikeStatus.AVAILABLE && DateUtils.calculateDaysInStock(it.inwardTimestamp) > daysThreshold
        }
    }

    suspend fun reserveVehicle(storeId: String, vehicleId: String): Result<Unit> {
        return updateVehicleStatus(storeId, vehicleId, BikeStatus.RESERVED)
    }

    suspend fun markVehicleFinanced(storeId: String, vehicleId: String): Result<Unit> {
        return updateVehicleStatus(storeId, vehicleId, BikeStatus.FINANCED)
    }

    suspend fun updateVehicleStatus(storeId: String, vehicleId: String, newStatus: BikeStatus): Result<Unit> {
        val current = _vehiclesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.vehicleId == vehicleId }
        if (index >= 0) {
            val updated = current[index].copy(status = newStatus, updatedAt = System.currentTimeMillis())
            if (updated.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
                return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
            }
            current[index] = updated
            _vehiclesFlow.value = current
            syncBikesFromVehicles()

            if (!authRepository.isDemoMode.value && firestore != null) {
                try {
                    // Direct dealership vehicle path: /dealerships/{dealershipId}/vehicles/{vehicleId}
                    firestore?.collection("dealerships")
                        ?.document(updated.dealershipId)
                        ?.collection("vehicles")
                        ?.document(vehicleId)
                        ?.set(updated)
                        ?.await()

                    val targetStore = if (storeId.isNotBlank() && storeId != "ALL") storeId else updated.effectiveStoreId
                    if (targetStore.isNotBlank()) {
                        firestore?.collection("dealerships")
                            ?.document(updated.dealershipId)
                            ?.collection("stores")
                            ?.document(targetStore)
                            ?.collection("vehicles")
                            ?.document(vehicleId)
                            ?.set(updated)
                            ?.await()
                    }
                } catch (_: Exception) {}
            }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Vehicle not found with ID: $vehicleId"))
    }

    fun requireNonBlankDealershipId(dealershipId: String) {
        require(dealershipId.isNotBlank()) { "Missing or blank dealershipId" }
    }

    suspend fun addVehicle(vehicle: Vehicle): Result<Unit> = addOrUpdateVehicle(vehicle)

    suspend fun updateVehicle(vehicle: Vehicle): Result<Unit> = addOrUpdateVehicle(vehicle)

    suspend fun addOrUpdateVehicle(vehicle: Vehicle): Result<Unit> {
        val preparedVehicle = if (vehicle.dealershipId.isBlank()) {
            if (authRepository.isDemoMode.value) {
                vehicle.copy(dealershipId = DemoData.DEMO_DEALERSHIP_ID)
            } else {
                vehicle
            }
        } else {
            vehicle
        }

        try {
            requireNonBlankDealershipId(preparedVehicle.dealershipId)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        if (!isChassisUnique(preparedVehicle.chassisNumber, excludeVehicleId = preparedVehicle.vehicleId)) {
            return Result.failure(Exception("Duplicate chassis/VIN number: ${preparedVehicle.chassisNumber}"))
        }
        if (!isEngineUnique(preparedVehicle.engineNumber, excludeVehicleId = preparedVehicle.vehicleId)) {
            return Result.failure(Exception("Duplicate engine number: ${preparedVehicle.engineNumber}"))
        }

        if (authRepository.isDemoMode.value) {
            val current = _vehiclesFlow.value.toMutableList()
            val index = current.indexOfFirst { it.vehicleId == preparedVehicle.vehicleId }
            if (index >= 0) {
                current[index] = preparedVehicle
            } else {
                current.add(preparedVehicle)
            }
            _vehiclesFlow.value = current
            syncBikesFromVehicles()
            return Result.success(Unit)
        }

        safeLogD(
            "ASF_INVENTORY_RUNTIME",
            "ASF_INVENTORY_RUNTIME: SAVE REQUEST | dealershipId = ${preparedVehicle.dealershipId}, vehicleType = ${preparedVehicle.vehicleType}, vehicleId = ${preparedVehicle.vehicleId}, regNo = ${preparedVehicle.registrationNumber}"
        )
        safeLogD(
            "ASF_INVENTORY_RUNTIME",
            "ASF_INVENTORY_RUNTIME: WRITE START | path = dealerships/${preparedVehicle.dealershipId}/vehicles/${preparedVehicle.vehicleId}"
        )

        return try {
            performFirestoreSave(preparedVehicle)

            safeLogD("ASF_INVENTORY_RUNTIME", "ASF_INVENTORY_RUNTIME: WRITE SUCCESS")

            val current = _vehiclesFlow.value.toMutableList()
            val index = current.indexOfFirst { it.vehicleId == preparedVehicle.vehicleId }
            if (index >= 0) {
                current[index] = preparedVehicle
            } else {
                current.add(preparedVehicle)
            }
            _vehiclesFlow.value = current
            syncBikesFromVehicles()

            safeLogD(
                "ASF_INVENTORY_RUNTIME",
                "ASF_INVENTORY_RUNTIME: LOCAL FLOW UPDATE AFTER WRITE SUCCESS | size = ${_vehiclesFlow.value.size}"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            safeLogD(
                "ASF_INVENTORY_RUNTIME",
                "ASF_INVENTORY_RUNTIME: WRITE FAILED | exception = ${e.javaClass.simpleName} | code = ${(e as? FirebaseFirestoreException)?.code} | message = ${e.message}"
            )
            Result.failure(e)
        }
    }

    protected open suspend fun performFirestoreSave(preparedVehicle: Vehicle) {
        val db = firestore ?: throw IllegalStateException("Firestore database is unavailable")
        db.collection("dealerships")
            .document(preparedVehicle.dealershipId)
            .collection("vehicles")
            .document(preparedVehicle.vehicleId)
            .set(preparedVehicle)
            .await()

        if (preparedVehicle.effectiveStoreId.isNotBlank()) {
            db.collection("dealerships")
                .document(preparedVehicle.dealershipId)
                .collection("stores")
                .document(preparedVehicle.effectiveStoreId)
                .collection("vehicles")
                .document(preparedVehicle.vehicleId)
                .set(preparedVehicle)
                .await()
        }
    }

    suspend fun deleteVehicle(vehicleId: String): Result<Unit> {
        val current = _vehiclesFlow.value.toMutableList()
        val vehicle = current.find { it.vehicleId == vehicleId }
        if (vehicle != null && vehicle.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }
        val removed = current.removeIf { it.vehicleId == vehicleId }
        if (removed) {
            _vehiclesFlow.value = current
            syncBikesFromVehicles()

            if (!authRepository.isDemoMode.value && firestore != null && vehicle != null) {
                try {
                    // Direct dealership vehicle path deletion: /dealerships/{dealershipId}/vehicles/{vehicleId}
                    firestore?.collection("dealerships")
                        ?.document(vehicle.dealershipId)
                        ?.collection("vehicles")
                        ?.document(vehicleId)
                        ?.delete()
                        ?.await()

                    if (vehicle.effectiveStoreId.isNotBlank()) {
                        firestore?.collection("dealerships")
                            ?.document(vehicle.dealershipId)
                            ?.collection("stores")
                            ?.document(vehicle.effectiveStoreId)
                            ?.collection("vehicles")
                            ?.document(vehicleId)
                            ?.delete()
                            ?.await()
                    }
                } catch (_: Exception) {}
            }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Vehicle not found with ID: $vehicleId"))
    }

    // Backward Compatibility Bike Methods Routing to Vehicle Implementation
    fun getAllBikes(): List<Bike> = _bikesFlow.value

    fun getBikesForDealership(dealershipId: String): List<Bike> {
        return getVehiclesForDealership(dealershipId).map { it.toBike() }
    }

    fun getBikesForStore(dealershipId: String, storeId: String): List<Bike> {
        return getVehiclesForStore(dealershipId, storeId).map { it.toBike() }
    }

    fun getBikesForStore(storeId: String): List<Bike> {
        return getBikesForStore(if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else "", storeId)
    }

    fun getBikesForState(dealershipId: String, stateCode: String): List<Bike> {
        return getVehiclesForState(dealershipId, stateCode).map { it.toBike() }
    }

    fun getBikesForState(stateCode: String): List<Bike> {
        return getBikesForState(if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else "", stateCode)
    }

    fun getBikesForDealershipAndStore(dealershipId: String, storeId: String): List<Bike> {
        return getBikesForStore(dealershipId, storeId)
    }

    fun getBikeById(bikeId: String): Bike? {
        return getVehicleById(bikeId)?.toBike()
    }

    suspend fun fetchBikesFromFirestore(dealershipId: String): List<Bike> {
        return fetchVehiclesFromFirestore(dealershipId).map { it.toBike() }
    }

    fun getDeadStockBikes(dealershipId: String = "", storeId: String? = null, daysThreshold: Int = 60): List<Bike> {
        return getDeadStockVehicles(dealershipId, storeId, daysThreshold).map { it.toBike() }
    }

    suspend fun addBike(bike: Bike): Result<Unit> = addOrUpdateVehicle(bike.toVehicle())

    suspend fun addOrUpdateBike(bike: Bike): Result<Unit> = addOrUpdateVehicle(bike.toVehicle())

    suspend fun deleteBike(bikeId: String): Result<Unit> = deleteVehicle(bikeId)

    suspend fun reserveBike(storeId: String, bikeId: String): Result<Unit> = reserveVehicle(storeId, bikeId)

    suspend fun markBikeFinanced(storeId: String, bikeId: String): Result<Unit> = markVehicleFinanced(storeId, bikeId)

    suspend fun updateBikeStatus(storeId: String, bikeId: String, newStatus: BikeStatus): Result<Unit> {
        return updateVehicleStatus(storeId, bikeId, newStatus)
    }
}
