package com.automotive.salesfinance

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.FuelType
import com.automotive.salesfinance.model.TransmissionType
import com.automotive.salesfinance.model.Vehicle
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.model.toBike
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Phase5AVehicleColorAndRegistrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        inventoryRepository = InventoryRepository(authRepository)
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
    fun test1_bikeSavesColorAndRegistrationNumber() = runTest {
        val bike = Bike(
            bikeId = "TEST_BIKE_01",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            chassisNumber = "TESTBIKECH001",
            engineNumber = "TESTBIKEENG001",
            make = "Royal Enfield",
            model = "Hunter 350",
            year = 2024,
            color = "Matte Black",
            registrationNumber = "TS09BK1234",
            costPrice = 140000.0,
            listedPrice = 175000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur",
            status = BikeStatus.AVAILABLE
        )

        var success = false
        viewModel.addOrUpdateBike(bike, onSuccess = { success = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        val savedVehicle = inventoryRepository.getVehicleById("TEST_BIKE_01")
        assertNotNull(savedVehicle)
        assertEquals("Matte Black", savedVehicle?.color)
        assertEquals("TS09BK1234", savedVehicle?.registrationNumber)

        val savedBike = savedVehicle?.toBike()
        assertNotNull(savedBike)
        assertEquals("Matte Black", savedBike?.color)
        assertEquals("TS09BK1234", savedBike?.registrationNumber)
    }

    @Test
    fun test2_carSavesColorAndRegistrationNumber() = runTest {
        val car = Vehicle(
            vehicleId = "TEST_CAR_01",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.CAR,
            chassisNumber = "TESTCARCH001",
            engineNumber = "TESTCARENG001",
            make = "Hyundai",
            model = "Creta",
            variant = "SX Petrol",
            fuelType = FuelType.PETROL,
            transmission = TransmissionType.MANUAL,
            registrationNumber = "KA01CR5678",
            color = "Pearl White",
            odometerKm = 5000,
            year = 2024,
            costPrice = 1100000.0,
            listedPrice = 1350000.0,
            stateCode = "KA",
            storeId = "KA_Bangalore",
            storeLocation = "KA_Bangalore",
            status = BikeStatus.AVAILABLE
        )

        var success = false
        viewModel.addOrUpdateVehicle(car, onSuccess = { success = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        val savedVehicle = inventoryRepository.getVehicleById("TEST_CAR_01")
        assertNotNull(savedVehicle)
        assertEquals("Pearl White", savedVehicle?.color)
        assertEquals("KA01CR5678", savedVehicle?.registrationNumber)
    }

    @Test
    fun test3_registrationNumberNormalization() = runTest {
        val vehicle = Vehicle(
            vehicleId = "TEST_NORM_01",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.BIKE,
            chassisNumber = "TESTNORMCH001",
            engineNumber = "TESTNORMENG001",
            make = "Honda",
            model = "Activa",
            year = 2024,
            color = "Red",
            registrationNumber = "  ts09norm9999  ",
            costPrice = 80000.0,
            listedPrice = 90000.0,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            storeLocation = "TG_Madhapur",
            status = BikeStatus.AVAILABLE
        )

        var success = false
        viewModel.addOrUpdateVehicle(vehicle, onSuccess = { success = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        val saved = inventoryRepository.getVehicleById("TEST_NORM_01")
        assertNotNull(saved)
        assertEquals("TS09NORM9999", saved?.registrationNumber)

        val isUnique = inventoryRepository.isRegistrationNumberUnique("ts09norm9999", DemoData.DEMO_DEALERSHIP_ID)
        assertFalse(isUnique)
    }

    @Test
    fun test4_blankRegistrationNumberAllowedForUnregisteredVehicles() = runTest {
        val unregVehicle = Vehicle(
            vehicleId = "TEST_UNREG_01",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.BIKE,
            chassisNumber = "TESTUNREGCH01",
            engineNumber = "TESTUNREGENG01",
            make = "TVS",
            model = "Jupiter",
            year = 2024,
            color = "Blue",
            registrationNumber = "   ",
            costPrice = 75000.0,
            listedPrice = 85000.0,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            storeLocation = "TG_Madhapur",
            status = BikeStatus.AVAILABLE
        )

        var success = false
        viewModel.addOrUpdateVehicle(unregVehicle, onSuccess = { success = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertTrue(inventoryRepository.isRegistrationNumberUnique("", DemoData.DEMO_DEALERSHIP_ID))
    }

    @Test
    fun test5_duplicateNonBlankRegistrationNumberInSameDealershipRejected() = runTest {
        val v1 = Vehicle(
            vehicleId = "TEST_DUP_01",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.CAR,
            chassisNumber = "TESTDUPCH001",
            engineNumber = "TESTDUPENG001",
            make = "Maruti",
            model = "Swift",
            variant = "VXI",
            color = "Red",
            registrationNumber = "TS09DUP111",
            costPrice = 600000.0,
            listedPrice = 700000.0,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            storeLocation = "TG_Madhapur"
        )
        viewModel.addOrUpdateVehicle(v1)
        testDispatcher.scheduler.advanceUntilIdle()

        val v2 = Vehicle(
            vehicleId = "TEST_DUP_02",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.CAR,
            chassisNumber = "TESTDUPCH002",
            engineNumber = "TESTDUPENG002",
            make = "Tata",
            model = "Nexon",
            variant = "XZ",
            color = "Blue",
            registrationNumber = "ts09dup111",
            costPrice = 800000.0,
            listedPrice = 900000.0,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            storeLocation = "TG_Madhapur"
        )

        var errorMsg: String? = null
        viewModel.addOrUpdateVehicle(v2, onError = { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(errorMsg)
        assertTrue(errorMsg!!.contains("Registration number TS09DUP111 already exists in this dealership"))
    }

    @Test
    fun test6_editingVehicleDoesNotDetectItsOwnRegistrationNumberAsDuplicate() = runTest {
        val v1 = Vehicle(
            vehicleId = "VEH_EDIT_01",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.BIKE,
            chassisNumber = "TESTEDITCH001",
            engineNumber = "TESTEDITENG001",
            make = "Yamaha",
            model = "FZ",
            color = "Black",
            registrationNumber = "TS09OWN123",
            costPrice = 100000.0,
            listedPrice = 120000.0,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            storeLocation = "TG_Madhapur"
        )
        viewModel.addOrUpdateVehicle(v1)
        testDispatcher.scheduler.advanceUntilIdle()

        val isUniqueForSelf = inventoryRepository.isRegistrationNumberUnique("TS09OWN123", DemoData.DEMO_DEALERSHIP_ID, "VEH_EDIT_01")
        assertTrue(isUniqueForSelf)

        val updatedV1 = v1.copy(color = "Silver")
        var success = false
        viewModel.addOrUpdateVehicle(updatedV1, onSuccess = { success = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertEquals("Silver", inventoryRepository.getVehicleById("VEH_EDIT_01")?.color)
    }

    @Test
    fun test7_registrationNumberSearchMatching() = runTest {
        backgroundScope.launch { viewModel.filteredVehicles.collect {} }

        val v = Vehicle(
            vehicleId = "TEST_SEARCH_01",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.CAR,
            chassisNumber = "TESTSRCHCH01",
            engineNumber = "TESTSRCHENG01",
            make = "Honda",
            model = "City",
            variant = "ZX",
            color = "White",
            registrationNumber = "TS09SEARCH99",
            costPrice = 1000000.0,
            listedPrice = 1200000.0,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            storeLocation = "TG_Madhapur"
        )
        viewModel.addOrUpdateVehicle(v)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setSearchQuery("SEARCH99")
        testDispatcher.scheduler.advanceUntilIdle()

        val results = viewModel.filteredVehicles.value
        assertTrue(results.any { it.registrationNumber == "TS09SEARCH99" })

        viewModel.setSearchQuery("ts09search")
        testDispatcher.scheduler.advanceUntilIdle()

        val resultsCaseInsensitive = viewModel.filteredVehicles.value
        assertTrue(resultsCaseInsensitive.any { it.registrationNumber == "TS09SEARCH99" })
    }

    @Test
    fun test8_bikeOnlyCarOnlyAndBikeAndCarDealershipSupport() = runTest {
        backgroundScope.launch { viewModel.filteredVehicles.collect {} }

        // Test filtering by vehicle type filter in ViewModel
        viewModel.setSelectedVehicleTypeFilter("BIKE")
        testDispatcher.scheduler.advanceUntilIdle()
        val bikesOnly = viewModel.filteredVehicles.value
        assertTrue(bikesOnly.isNotEmpty())
        assertTrue(bikesOnly.all { it.vehicleType == VehicleType.BIKE })

        viewModel.setSelectedVehicleTypeFilter("CAR")
        testDispatcher.scheduler.advanceUntilIdle()
        val carsOnly = viewModel.filteredVehicles.value
        assertTrue(carsOnly.isNotEmpty())
        assertTrue(carsOnly.all { it.vehicleType == VehicleType.CAR })

        viewModel.setSelectedVehicleTypeFilter("ALL")
        testDispatcher.scheduler.advanceUntilIdle()
        val allTypes = viewModel.filteredVehicles.value
        assertTrue(allTypes.any { it.vehicleType == VehicleType.BIKE })
        assertTrue(allTypes.any { it.vehicleType == VehicleType.CAR })
    }
}
