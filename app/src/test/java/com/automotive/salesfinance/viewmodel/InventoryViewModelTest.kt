package com.automotive.salesfinance.viewmodel

import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.StoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.automotive.salesfinance.repository.DealershipRepository

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        inventoryRepository = InventoryRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        val dealershipRepository = DealershipRepository(authRepository)
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
    fun validateBike_detectsDuplicateChassisAndEngine() {
        // "TEST001" and "ENG001" exist in DemoData
        val errChassis = viewModel.validateBike(
            chassisNumber = "TEST001",
            engineNumber = "UNIQUEENG100",
            make = "Honda",
            model = "Activa",
            year = 2024,
            costPrice = 80000.0,
            listedPrice = 90000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )
        assertNotNull(errChassis)
        assertTrue(errChassis!!.contains("Duplicate Chassis Number"))

        val errEngine = viewModel.validateBike(
            chassisNumber = "UNIQUECH100",
            engineNumber = "ENG001",
            make = "Honda",
            model = "Activa",
            year = 2024,
            costPrice = 80000.0,
            listedPrice = 90000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )
        assertNotNull(errEngine)
        assertTrue(errEngine!!.contains("Duplicate Engine Number"))
    }

    @Test
    fun validateBike_passesForValidData() {
        val err = viewModel.validateBike(
            chassisNumber = "UNIQUECH999",
            engineNumber = "UNIQUEENG999",
            make = "Royal Enfield",
            model = "Hunter 350",
            year = 2024,
            costPrice = 150000.0,
            listedPrice = 175000.0,
            stateCode = "KA",
            storeLocation = "KA_Bangalore"
        )
        assertNull(err)
    }

    @Test
    fun addOrUpdateBike_addsNewBikeSuccessfully() = runTest {
        val newBike = Bike(
            bikeId = "NEW",
            chassisNumber = "NEWCHASSIS101",
            engineNumber = "NEWENG101",
            make = "TVS",
            model = "Apache RTR 160",
            color = "Black",
            year = 2024,
            costPrice = 110000.0,
            listedPrice = 130000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur",
            status = BikeStatus.AVAILABLE
        )

        var successCalled = false
        viewModel.addOrUpdateBike(newBike, onSuccess = { successCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        val added = inventoryRepository.bikesFlow.value.find { it.chassisNumber == "NEWCHASSIS101" }
        assertNotNull(added)
        assertEquals("TVS", added?.make)
    }

    @Test
    fun deleteBike_removesBikeFromInventory() = runTest {
        var success = false
        viewModel.deleteBike("BIKE_001", onSuccess = { success = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(inventoryRepository.getBikeById("BIKE_001"))
    }

    @Test
    fun reserveBike_updatesStatusToReserved() = runTest {
        var success = false
        viewModel.reserveBike("BIKE_002", "TG_Madhapur", onSuccess = { success = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        val bike = inventoryRepository.getBikeById("BIKE_002")
        assertEquals(BikeStatus.RESERVED, bike?.status)
    }
}
