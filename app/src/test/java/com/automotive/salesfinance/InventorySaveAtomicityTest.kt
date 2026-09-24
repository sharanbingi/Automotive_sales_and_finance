package com.automotive.salesfinance

import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.model.Vehicle
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TestableInventoryRepository(
    authRepository: AuthRepository,
    var shouldFailFirestore: Boolean = false,
    var firestoreException: Exception? = null
) : InventoryRepository(authRepository) {

    override suspend fun performFirestoreSave(preparedVehicle: Vehicle) {
        if (shouldFailFirestore) {
            throw firestoreException ?: Exception("Simulated Firestore write error")
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class InventorySaveAtomicityTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var dealershipRepository: DealershipRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        storeRepository = StoreRepository(authRepository)
        dealershipRepository = DealershipRepository(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun setupActiveDealership(dealershipId: String = "dealership_live_001"): Dealership {
        val dealership = Dealership(
            dealershipId = dealershipId,
            name = "Live Active Dealership",
            subscriptionPlan = "BUSINESS",
            subscriptionStatus = SubscriptionStatus.ACTIVE
        )
        dealershipRepository.saveDealership(dealership)
        val user = User(
            uid = "user_live_001",
            email = "admin@dealership.com",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = dealershipId
        )
        authRepository.setCurrentUser(user)
        return dealership
    }

    private fun createTestVehicle(
        dealershipId: String = "dealership_live_001",
        chassis: String = "CHASSISATOM001",
        engine: String = "ENGATOM001",
        regNo: String = "TS09AT0001"
    ): Vehicle {
        return Vehicle(
            vehicleId = "VEH_ATOM_001",
            dealershipId = dealershipId,
            vehicleType = VehicleType.BIKE,
            make = "Honda",
            model = "Activa 6G",
            color = "Red",
            chassisNumber = chassis,
            engineNumber = engine,
            registrationNumber = regNo,
            year = 2024,
            costPrice = 80000.0,
            listedPrice = 95000.0,
            stateCode = "TG",
            storeLocation = "TG_Hyd_01",
            status = BikeStatus.AVAILABLE
        )
    }

    @Test
    fun test1_productionFirestoreWriteSuccess_updatesLocalFlowAndCallsOnSuccess() = runTest {
        authRepository.setDemoMode(false)
        setupActiveDealership("dealership_live_001")

        val repository = TestableInventoryRepository(authRepository, shouldFailFirestore = false)
        val viewModel = InventoryViewModel(repository, storeRepository, authRepository, dealershipRepository)

        val vehicle = createTestVehicle()
        var onSuccessCalled = false

        viewModel.addOrUpdateVehicle(vehicle, onSuccess = { onSuccessCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onSuccess must be called on Firestore write success", onSuccessCalled)
        assertTrue("uiState.isSuccess must be true", viewModel.uiState.value.isSuccess)
        assertEquals("Vehicle added successfully", viewModel.uiState.value.successMessage)
        assertNull("errorMessage must be null", viewModel.uiState.value.errorMessage)
        assertFalse("isLoading must be false", viewModel.uiState.value.isLoading)

        val savedVehicle = repository.vehiclesFlow.value.find { it.vehicleId == "VEH_ATOM_001" }
        assertNotNull("Vehicle must be updated in local in-memory flow", savedVehicle)
        assertEquals("TS09AT0001", savedVehicle?.registrationNumber)
    }

    @Test
    fun test2_productionFirestoreWriteFailure_doesNotUpdateLocalFlowOrCallOnSuccessAndPopulatesErrorState() = runTest {
        authRepository.setDemoMode(false)
        setupActiveDealership("dealership_live_001")

        val exception = Exception("Permission denied: unable to save vehicle to dealership database.")
        val repository = TestableInventoryRepository(authRepository, shouldFailFirestore = true, firestoreException = exception)
        val viewModel = InventoryViewModel(repository, storeRepository, authRepository, dealershipRepository)

        val vehicle = createTestVehicle()
        var onSuccessCalled = false
        var onErrorMsg: String? = null

        viewModel.addOrUpdateVehicle(vehicle, onSuccess = { onSuccessCalled = true }, onError = { onErrorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("onSuccess must NOT be called on Firestore write failure", onSuccessCalled)
        assertFalse("uiState.isSuccess must be false", viewModel.uiState.value.isSuccess)
        assertFalse("isLoading must be false", viewModel.uiState.value.isLoading)
        assertNotNull("uiState.errorMessage must be populated", viewModel.uiState.value.errorMessage)
        assertTrue(
            "ErrorMessage should explain permission denied",
            viewModel.uiState.value.errorMessage!!.contains("Permission denied")
        )
        assertEquals(viewModel.uiState.value.errorMessage, onErrorMsg)

        val savedVehicle = repository.vehiclesFlow.value.find { it.vehicleId == "VEH_ATOM_001" }
        assertNull("Vehicle must NOT be added to local flow on write failure", savedVehicle)
    }

    @Test
    fun test3_failedSave_doesNotCreateFalseDuplicateRegistrationChassisOrEngine() = runTest {
        authRepository.setDemoMode(false)
        setupActiveDealership("dealership_live_001")

        val exception = Exception("Network unavailable: check your internet connection and try again.")
        val repository = TestableInventoryRepository(authRepository, shouldFailFirestore = true, firestoreException = exception)
        val viewModel = InventoryViewModel(repository, storeRepository, authRepository, dealershipRepository)

        val vehicle = createTestVehicle(chassis = "CHASSISUNI999", engine = "ENGUNI999", regNo = "KA01AB9999")

        viewModel.addOrUpdateVehicle(vehicle)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the failed vehicle was not stored in memory
        assertTrue("Chassis should remain unique on retry", repository.isChassisUnique("CHASSISUNI999"))
        assertTrue("Engine should remain unique on retry", repository.isEngineUnique("ENGUNI999"))
        assertTrue("Registration number should remain unique on retry", repository.isRegistrationNumberUnique("KA01AB9999", "dealership_live_001"))

        // Retry validation should return null (no duplicate error)
        val retryValidationError = viewModel.validateVehicle(vehicle)
        assertNull("Form validation must pass without duplicate error for the retry vehicle", retryValidationError)
    }

    @Test
    fun test4_formDataRemainsPreservedOnFailure() = runTest {
        authRepository.setDemoMode(false)
        setupActiveDealership("dealership_live_001")

        val repository = TestableInventoryRepository(authRepository, shouldFailFirestore = true)
        val viewModel = InventoryViewModel(repository, storeRepository, authRepository, dealershipRepository)

        val vehicle = createTestVehicle()
        var onSuccessCalled = false

        viewModel.addOrUpdateVehicle(vehicle, onSuccess = { onSuccessCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        // Since onSuccess is NOT invoked, screen/form remains open and user data stays intact
        assertFalse("onSuccess callback must not be invoked on failure", onSuccessCalled)
        assertNotNull("errorMessage should contain failure info", viewModel.uiState.value.errorMessage)
        assertFalse("isLoading must be set back to false", viewModel.uiState.value.isLoading)
    }

    @Test
    fun test5_demoMode_supportsLocalVehicleAddition() = runTest {
        authRepository.setDemoMode(true)
        dealershipRepository = DealershipRepository(authRepository)

        val repository = InventoryRepository(authRepository)
        val viewModel = InventoryViewModel(repository, storeRepository, authRepository, dealershipRepository)

        val vehicle = createTestVehicle(dealershipId = "dealership_demo_001", chassis = "DEMOCH001", engine = "DEMOENG001")
        var onSuccessCalled = false

        viewModel.addOrUpdateVehicle(vehicle, onSuccess = { onSuccessCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onSuccess must be called in Demo Mode", onSuccessCalled)
        assertTrue("uiState.isSuccess must be true", viewModel.uiState.value.isSuccess)

        val addedVehicle = repository.vehiclesFlow.value.find { it.chassisNumber == "DEMOCH001" }
        assertNotNull("Demo Mode vehicle must be added to local flow", addedVehicle)
    }

    @Test
    fun test6_productionFailure_doesNotFallbackToDemoMode() = runTest {
        authRepository.setDemoMode(false)
        setupActiveDealership("dealership_live_001")

        val repository = TestableInventoryRepository(authRepository, shouldFailFirestore = true)
        val viewModel = InventoryViewModel(repository, storeRepository, authRepository, dealershipRepository)

        val vehicle = createTestVehicle()
        viewModel.addOrUpdateVehicle(vehicle)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("isDemoMode must remain false on production failure", authRepository.isDemoMode.value)
        assertFalse("viewModel.isDemoMode must remain false", viewModel.isDemoMode.value)
        assertFalse("Local flow must not fall back to DemoData vehicles", repository.vehiclesFlow.value.any { it.vehicleId == "BIKE_001" })
    }

    @Test
    fun test7_blankDealershipId_cannotWriteInventory() = runTest {
        authRepository.setDemoMode(false)
        val repository = InventoryRepository(authRepository)

        val vehicleWithBlankDealership = createTestVehicle(dealershipId = "")
        val result = repository.addOrUpdateVehicle(vehicleWithBlankDealership)

        assertTrue("addOrUpdateVehicle with blank dealershipId must fail in Live mode", result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("Exception must be IllegalArgumentException", ex is IllegalArgumentException)
        assertTrue("Message must indicate missing dealershipId", ex?.message?.contains("Missing or blank dealershipId") == true)
    }

    @Test
    fun test8_successfulVehicleSave_updatesInventoryFlowSize() = runTest {
        authRepository.setDemoMode(false)
        setupActiveDealership("dealership_live_001")

        val repository = TestableInventoryRepository(authRepository, shouldFailFirestore = false)
        val viewModel = InventoryViewModel(repository, storeRepository, authRepository, dealershipRepository)

        val initialSize = repository.vehiclesFlow.value.size

        val vehicle = createTestVehicle(chassis = "CHSIZETEST01", engine = "ENGSIZETEST01")
        viewModel.addOrUpdateVehicle(vehicle)
        testDispatcher.scheduler.advanceUntilIdle()

        val newSize = repository.vehiclesFlow.value.size
        assertEquals("Inventory flow size must increase by 1 after successful save", initialSize + 1, newSize)
    }
}
