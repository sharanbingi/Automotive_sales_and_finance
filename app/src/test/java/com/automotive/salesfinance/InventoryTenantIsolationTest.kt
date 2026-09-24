package com.automotive.salesfinance

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.model.Vehicle
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.DealershipRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryTenantIsolationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var inventoryRepository: TestableInventoryRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        inventoryRepository = TestableInventoryRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        dealershipRepository = DealershipRepository(authRepository)
        viewModel = InventoryViewModel(
            inventoryRepository = inventoryRepository,
            storeRepository = storeRepository,
            authRepository = authRepository,
            dealershipRepository = dealershipRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test1_productionDealershipAdmin_savesVehicleWithCorrectDealershipId() = runTest {
        authRepository.setDemoMode(false)
        testDispatcher.scheduler.advanceUntilIdle()

        val adminUser = User(
            uid = "user_sharan_001",
            email = "sharan@sharanmotors.com",
            dealershipId = "sharan_motors",
            role = UserRole.DEALERSHIP_ADMIN
        )
        authRepository.setCurrentUser(adminUser)

        dealershipRepository.saveDealership(
            Dealership(
                dealershipId = "sharan_motors",
                name = "Sharan Motors",
                subscriptionPlan = "STARTER",
                subscriptionStatus = SubscriptionStatus.ACTIVE
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val newVehicle = Vehicle(
            vehicleId = "NEW",
            vehicleType = VehicleType.BIKE,
            chassisNumber = "SHARANCHASSIS001",
            engineNumber = "SHARANENGINE001",
            make = "Honda",
            model = "Activa",
            color = "Black",
            registrationNumber = "TS09AB1234",
            costPrice = 80000.0,
            listedPrice = 90000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )

        var successCalled = false
        var errorMsg: String? = null
        viewModel.addOrUpdateVehicle(newVehicle, onSuccess = { successCalled = true }, onError = { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Save must succeed for valid production admin. Error: $errorMsg", successCalled)
        val saved = inventoryRepository.vehiclesFlow.value.find { it.chassisNumber == "SHARANCHASSIS001" }
        assertNotNull("Saved vehicle must exist in repository", saved)
        assertEquals("sharan_motors", saved?.dealershipId)
    }

    @Test
    fun test2_productionUser_neverReceivesDemoFallback() = runTest {
        authRepository.setDemoMode(false)
        testDispatcher.scheduler.advanceUntilIdle()

        val liveUser = User(
            uid = "user_live_999",
            email = "live@customdealership.com",
            dealershipId = "custom_dealership_123",
            role = UserRole.DEALERSHIP_ADMIN
        )
        authRepository.setCurrentUser(liveUser)

        dealershipRepository.saveDealership(
            Dealership(
                dealershipId = "custom_dealership_123",
                name = "Custom Dealership",
                subscriptionPlan = "STARTER",
                subscriptionStatus = SubscriptionStatus.ACTIVE
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val newVehicle = Vehicle(
            vehicleId = "NEW",
            dealershipId = "", // Pass empty dealershipId on input model
            vehicleType = VehicleType.CAR,
            chassisNumber = "CUSTOMVIN12345",
            engineNumber = "CUSTOMENG12345",
            make = "Hyundai",
            model = "Creta",
            variant = "SX",
            color = "White",
            registrationNumber = "TS08CD5678",
            costPrice = 1100000.0,
            listedPrice = 1350000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )

        var successCalled = false
        var errorMsg: String? = null
        viewModel.addOrUpdateVehicle(newVehicle, onSuccess = { successCalled = true }, onError = { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Save must succeed for custom dealership user. Error: $errorMsg", successCalled)
        val saved = inventoryRepository.vehiclesFlow.value.find { it.chassisNumber == "CUSTOMVIN12345" }
        assertNotNull("Saved vehicle must exist", saved)
        assertEquals("custom_dealership_123", saved?.dealershipId)
        assertFalse("Production user must never get demo_001 fallback", saved?.dealershipId == DemoData.DEMO_DEALERSHIP_ID)
    }

    @Test
    fun test3_blankProductionDealershipId_failsClosed() = runTest {
        authRepository.setDemoMode(false)
        testDispatcher.scheduler.advanceUntilIdle()

        val unassignedUser = User(
            uid = "user_no_tenant",
            email = "unassigned@test.com",
            dealershipId = "", // Blank dealershipId
            role = UserRole.DEALERSHIP_ADMIN
        )
        authRepository.setCurrentUser(unassignedUser)

        val vehicle = Vehicle(
            vehicleId = "NEW",
            chassisNumber = "UNASSIGNEDCH001",
            engineNumber = "UNASSIGNEDENG001",
            make = "TVS",
            model = "Jupiter",
            color = "Red",
            registrationNumber = "TS07EF9012",
            costPrice = 70000.0,
            listedPrice = 85000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )

        var successCalled = false
        var errorMsg: String? = null
        viewModel.addOrUpdateVehicle(vehicle, onSuccess = { successCalled = true }, onError = { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("Save must fail when production user has blank dealershipId", successCalled)
        assertEquals("Unable to determine dealership. Please sign in again.", errorMsg)
        assertEquals("Unable to determine dealership. Please sign in again.", viewModel.uiState.value.errorMessage)
        assertNull("Vehicle must not be saved when dealershipId is blank in live mode", inventoryRepository.vehiclesFlow.value.find { it.chassisNumber == "UNASSIGNEDCH001" })

        // Direct repository check
        val repoResult = inventoryRepository.addOrUpdateVehicle(vehicle.copy(dealershipId = ""))
        assertTrue("Repository must fail closed for blank dealershipId in live mode", repoResult.isFailure)
        assertTrue(repoResult.exceptionOrNull()?.message?.contains("Missing or blank dealershipId") == true)
    }

    @Test
    fun test4_explicitDemoMode_canUseDemoDealershipId() = runTest {
        authRepository.setDemoMode(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val demoUser = User(
            uid = "demo_admin",
            email = "demo@salesfinance.com",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            role = UserRole.DEALERSHIP_ADMIN
        )
        authRepository.setCurrentUser(demoUser)
        testDispatcher.scheduler.advanceUntilIdle()

        val demoVehicle = Vehicle(
            vehicleId = "NEW",
            dealershipId = "", // Blank on model, should resolve to DEMO_DEALERSHIP_ID in demo mode
            chassisNumber = "DEMOTESTCH777",
            engineNumber = "DEMOTESTENG777",
            make = "Honda",
            model = "Activa",
            color = "Blue",
            registrationNumber = "TS09DEMO777",
            costPrice = 75000.0,
            listedPrice = 88000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )

        var successCalled = false
        var errorMsg: String? = null
        viewModel.addOrUpdateVehicle(demoVehicle, onSuccess = { successCalled = true }, onError = { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Save in demo mode should succeed. Error: $errorMsg", successCalled)
        val saved = inventoryRepository.vehiclesFlow.value.find { it.chassisNumber == "DEMOTESTCH777" }
        assertNotNull("Saved vehicle must exist", saved)
        assertEquals(DemoData.DEMO_DEALERSHIP_ID, saved?.dealershipId)
    }

    @Test
    fun test5_vehicleAndBike_defaultDealershipId_isEmptyString() {
        val vehicle = Vehicle()
        val bike = Bike()

        assertEquals("", vehicle.dealershipId)
        assertEquals("", bike.dealershipId)
    }
}
