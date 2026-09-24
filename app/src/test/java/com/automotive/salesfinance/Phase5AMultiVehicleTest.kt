package com.automotive.salesfinance

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.FeatureAccessManager
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.FuelType
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.TransmissionType
import com.automotive.salesfinance.model.Vehicle
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.model.toBike
import com.automotive.salesfinance.model.toVehicle
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.viewmodel.FinanceViewModel
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
class Phase5AMultiVehicleTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var customerRepository: CustomerRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        dealershipRepository = DealershipRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        loanRepository = LoanRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testVehicleModelConversionsAndBackwardCompatibility() {
        val bike = Bike(
            bikeId = "TEST_BIKE_101",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            chassisNumber = "TEST_VIN_101",
            engineNumber = "TEST_ENG_101",
            make = "Honda",
            model = "Activa",
            year = 2024,
            costPrice = 80000.0,
            listedPrice = 95000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur",
            status = BikeStatus.AVAILABLE
        )

        val vehicle = bike.toVehicle()
        assertEquals(bike.bikeId, vehicle.vehicleId)
        assertEquals(VehicleType.BIKE, vehicle.vehicleType)
        assertEquals(bike.chassisNumber, vehicle.chassisNumber)
        assertEquals(bike.engineNumber, vehicle.engineNumber)
        assertEquals(bike.make, vehicle.make)
        assertEquals(bike.model, vehicle.model)
        assertEquals(bike.costPrice, vehicle.costPrice, 0.01)
        assertEquals(bike.listedPrice, vehicle.listedPrice, 0.01)
        assertEquals("TG_Madhapur", vehicle.effectiveStoreId)

        val convertedBike = vehicle.toBike()
        assertEquals(bike.bikeId, convertedBike.bikeId)
        assertEquals(bike.chassisNumber, convertedBike.chassisNumber)
        assertEquals(bike.engineNumber, convertedBike.engineNumber)
        assertEquals(bike.storeLocation, convertedBike.storeLocation)
    }

    @Test
    fun testCarFieldsHandling() {
        val car = Vehicle(
            vehicleId = "TEST_CAR_202",
            dealershipId = DemoData.DEMO_DEALERSHIP_CARS_ONLY,
            vehicleType = VehicleType.CAR,
            chassisNumber = "CAR_VIN_TEST_202",
            engineNumber = "CAR_ENG_TEST_202",
            make = "Hyundai",
            model = "Creta",
            variant = "1.5 SX Opt",
            fuelType = FuelType.PETROL,
            transmission = TransmissionType.AUTOMATIC,
            registrationNumber = "TS09AB1234",
            color = "Titan Grey",
            odometerKm = 15000,
            year = 2023,
            costPrice = 1200000.0,
            listedPrice = 1450000.0,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            storeLocation = "TG_Madhapur",
            status = BikeStatus.AVAILABLE
        )

        assertEquals(VehicleType.CAR, car.vehicleType)
        assertEquals("1.5 SX Opt", car.variant)
        assertEquals(FuelType.PETROL, car.fuelType)
        assertEquals(TransmissionType.AUTOMATIC, car.transmission)
        assertEquals("TS09AB1234", car.registrationNumber)
        assertEquals("Titan Grey", car.color)
        assertEquals(15000, car.odometerKm)
    }

    @Test
    fun testBikeOnlyDealershipBehavior() = runTest {
        val bikesOnlyDealership = dealershipRepository.dealershipsFlow.value.find {
            it.dealershipId == DemoData.DEMO_DEALERSHIP_BIKES_ONLY
        }
        assertNotNull(bikesOnlyDealership)
        assertEquals(listOf(VehicleType.BIKE), bikesOnlyDealership?.supportedVehicleTypes)

        val viewModel = InventoryViewModel(
            inventoryRepository = inventoryRepository,
            storeRepository = storeRepository,
            authRepository = authRepository,
            dealershipRepository = dealershipRepository
        )
        val vehiclesForDealership = inventoryRepository.getVehiclesForDealership(DemoData.DEMO_DEALERSHIP_BIKES_ONLY)
        assertTrue(vehiclesForDealership.all { it.vehicleType == VehicleType.BIKE })
    }

    @Test
    fun testCarOnlyDealershipBehavior() = runTest {
        val carsOnlyDealership = dealershipRepository.dealershipsFlow.value.find {
            it.dealershipId == DemoData.DEMO_DEALERSHIP_CARS_ONLY
        }
        assertNotNull(carsOnlyDealership)
        assertEquals(listOf(VehicleType.CAR), carsOnlyDealership?.supportedVehicleTypes)

        val vehiclesForDealership = inventoryRepository.getVehiclesForDealership(DemoData.DEMO_DEALERSHIP_CARS_ONLY)
        assertTrue(vehiclesForDealership.all { it.vehicleType == VehicleType.CAR })
    }

    @Test
    fun testBikeAndCarDealershipBehavior() = runTest {
        val multiVehicleDealership = dealershipRepository.dealershipsFlow.value.find {
            it.dealershipId == DemoData.DEMO_DEALERSHIP_ID
        }
        assertNotNull(multiVehicleDealership)
        assertEquals(listOf(VehicleType.BIKE, VehicleType.CAR), multiVehicleDealership?.supportedVehicleTypes)

        val vehicles = inventoryRepository.getVehiclesForDealership(DemoData.DEMO_DEALERSHIP_ID)
        val hasBikes = vehicles.any { it.vehicleType == VehicleType.BIKE }
        val hasCars = vehicles.any { it.vehicleType == VehicleType.CAR }
        assertTrue(hasBikes)
        assertTrue(hasCars)
    }

    @Test
    fun testDuplicateVinChassisAndEngineValidation() {
        val viewModel = InventoryViewModel(
            inventoryRepository = inventoryRepository,
            storeRepository = storeRepository,
            authRepository = authRepository,
            dealershipRepository = dealershipRepository
        )

        val duplicateVinCar = Vehicle(
            vehicleId = "CAR_DUP_TEST",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.CAR,
            chassisNumber = "CARVIN001", // Already exists in DemoData
            engineNumber = "ENGUNIQUE999",
            make = "Honda",
            model = "City",
            costPrice = 1000000.0,
            listedPrice = 1200000.0,
            year = 2024,
            stateCode = "TG",
            storeId = "TG_Madhapur"
        )

        val err = viewModel.validateVehicle(duplicateVinCar)
        assertNotNull(err)
        assertTrue(err?.contains("Duplicate Chassis", ignoreCase = true) == true)

        val duplicateEngineCar = Vehicle(
            vehicleId = "CAR_DUP_ENG_TEST",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.CAR,
            chassisNumber = "UNIQUEVIN9999",
            engineNumber = "ENGCAR001", // Already exists in DemoData
            make = "Honda",
            model = "City",
            costPrice = 1000000.0,
            listedPrice = 1200000.0,
            year = 2024,
            stateCode = "TG",
            storeId = "TG_Madhapur"
        )

        val errEng = viewModel.validateVehicle(duplicateEngineCar)
        assertNotNull(errEng)
        assertTrue(errEng?.contains("Duplicate Engine Number", ignoreCase = true) == true)
    }

    @Test
    fun testFinancingAtomicStatusTransitionForCarsAndBikes() = runTest {
        val car = Vehicle(
            vehicleId = "FINANCE_TEST_CAR_001",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.CAR,
            chassisNumber = "FIN_CAR_VIN_001",
            engineNumber = "FIN_CAR_ENG_001",
            make = "Tata",
            model = "Harrier",
            costPrice = 1500000.0,
            listedPrice = 1800000.0,
            year = 2024,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            status = BikeStatus.AVAILABLE
        )

        val addRes = inventoryRepository.addOrUpdateVehicle(car)
        assertTrue(addRes.isSuccess)

        val res = inventoryRepository.markVehicleFinanced("TG_Madhapur", "FINANCE_TEST_CAR_001")
        assertTrue(res.isSuccess)

        val updatedCar = inventoryRepository.getVehicleById("FINANCE_TEST_CAR_001")
        assertNotNull(updatedCar)
        assertEquals(BikeStatus.FINANCED, updatedCar?.status)

        // Check backward compatibility
        val updatedBikeRef = inventoryRepository.getBikeById("FINANCE_TEST_CAR_001")
        assertNotNull(updatedBikeRef)
        assertEquals(BikeStatus.FINANCED, updatedBikeRef?.status)
    }

    @Test
    fun testTenantIsolationPreservation() = runTest {
        val bikesDemo = inventoryRepository.getVehiclesForDealership(DemoData.DEMO_DEALERSHIP_ID)
        val bikesApex = inventoryRepository.getVehiclesForDealership(DemoData.DEMO_DEALERSHIP_BIKES_ONLY)
        val carsRoyal = inventoryRepository.getVehiclesForDealership(DemoData.DEMO_DEALERSHIP_CARS_ONLY)

        assertTrue(bikesDemo.all { it.dealershipId == DemoData.DEMO_DEALERSHIP_ID })
        assertTrue(bikesApex.all { it.dealershipId == DemoData.DEMO_DEALERSHIP_BIKES_ONLY })
        assertTrue(carsRoyal.all { it.dealershipId == DemoData.DEMO_DEALERSHIP_CARS_ONLY })

        val apexHasCar = bikesApex.any { it.vehicleType == VehicleType.CAR }
        assertFalse(apexHasCar)

        val royalHasBike = carsRoyal.any { it.vehicleType == VehicleType.BIKE }
        assertFalse(royalHasBike)
    }
}
