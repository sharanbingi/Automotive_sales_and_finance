package com.automotive.salesfinance

import com.automotive.salesfinance.data.catalog.VehicleCatalogRepository
import com.automotive.salesfinance.model.FuelType
import com.automotive.salesfinance.model.TransmissionType
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class Phase5AVehicleCatalogTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var catalogRepository: VehicleCatalogRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        catalogRepository = VehicleCatalogRepository()
        authRepository = AuthRepository()
        dealershipRepository = DealershipRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        viewModel = InventoryViewModel(
            inventoryRepository = inventoryRepository,
            storeRepository = storeRepository,
            authRepository = authRepository,
            dealershipRepository = dealershipRepository,
            vehicleCatalogRepository = catalogRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test1_bikeMakesCatalog_containsIndiaFocusedBrandsAndOther() {
        val makes = catalogRepository.getMakesForVehicleType(VehicleType.BIKE)
        assertTrue("Makes should contain Honda", makes.contains("Honda"))
        assertTrue("Makes should contain Hero", makes.contains("Hero"))
        assertTrue("Makes should contain TVS", makes.contains("TVS"))
        assertTrue("Makes should contain Bajaj", makes.contains("Bajaj"))
        assertTrue("Makes should contain Royal Enfield", makes.contains("Royal Enfield"))
        assertTrue("Makes should contain Yamaha", makes.contains("Yamaha"))
        assertTrue("Makes should contain Suzuki", makes.contains("Suzuki"))
        assertTrue("Makes should contain KTM", makes.contains("KTM"))
        assertTrue("Makes should contain Ather", makes.contains("Ather"))
        assertTrue("Makes should contain Ola Electric", makes.contains("Ola Electric"))
        assertTrue("Makes should contain Other", makes.contains("Other"))
    }

    @Test
    fun test2_carMakesCatalog_containsIndiaFocusedBrandsAndOther() {
        val makes = catalogRepository.getMakesForVehicleType(VehicleType.CAR)
        assertTrue("Makes should contain Maruti Suzuki", makes.contains("Maruti Suzuki"))
        assertTrue("Makes should contain Hyundai", makes.contains("Hyundai"))
        assertTrue("Makes should contain Tata Motors", makes.contains("Tata Motors"))
        assertTrue("Makes should contain Mahindra", makes.contains("Mahindra"))
        assertTrue("Makes should contain Toyota", makes.contains("Toyota"))
        assertTrue("Makes should contain Kia", makes.contains("Kia"))
        assertTrue("Makes should contain Honda", makes.contains("Honda"))
        assertTrue("Makes should contain Volkswagen", makes.contains("Volkswagen"))
        assertTrue("Makes should contain Other", makes.contains("Other"))
    }

    @Test
    fun test3_getModelsForMake_returnsHondaBikeModelsAndOther() {
        val models = catalogRepository.getModelsForMake(VehicleType.BIKE, "Honda")
        assertTrue("Honda models should contain Activa 6G", models.contains("Activa 6G"))
        assertTrue("Honda models should contain Activa 125", models.contains("Activa 125"))
        assertTrue("Honda models should contain Shine 125", models.contains("Shine 125"))
        assertTrue("Honda models should contain SP 125", models.contains("SP 125"))
        assertTrue("Honda models should contain Unicorn", models.contains("Unicorn"))
        assertTrue("Honda models should contain Other", models.contains("Other"))
    }

    @Test
    fun test4_getModelsForMake_returnsHyundaiCarModelsAndOther() {
        val models = catalogRepository.getModelsForMake(VehicleType.CAR, "Hyundai")
        assertTrue("Hyundai models should contain Creta", models.contains("Creta"))
        assertTrue("Hyundai models should contain Venue", models.contains("Venue"))
        assertTrue("Hyundai models should contain i20", models.contains("i20"))
        assertTrue("Hyundai models should contain Verna", models.contains("Verna"))
        assertTrue("Hyundai models should contain Exter", models.contains("Exter"))
        assertTrue("Hyundai models should contain Other", models.contains("Other"))
    }

    @Test
    fun test5_getMakesForVehicleType_searchFilter_returnsMatchingMakes() {
        val filtered = catalogRepository.getMakesForVehicleType(VehicleType.BIKE, "Hon")
        assertEquals(1, filtered.size)
        assertEquals("Honda", filtered.first())
    }

    @Test
    fun test6_getMakesForVehicleType_caseInsensitiveSearch() {
        val filtered = catalogRepository.getMakesForVehicleType(VehicleType.CAR, "maruti")
        assertEquals(1, filtered.size)
        assertEquals("Maruti Suzuki", filtered.first())
    }

    @Test
    fun test7_getModelsForMake_searchFilter_returnsMatchingModels() {
        val filtered = catalogRepository.getModelsForMake(VehicleType.BIKE, "Honda", "Act")
        assertTrue("Filtered models should contain Activa 6G", filtered.contains("Activa 6G"))
        assertTrue("Filtered models should contain Activa 125", filtered.contains("Activa 125"))
        assertFalse("Filtered models should not contain Shine 125", filtered.contains("Shine 125"))
    }

    @Test
    fun test8_getModelsForMake_returnsOtherWhenMakeIsOther() {
        val models = catalogRepository.getModelsForMake(VehicleType.BIKE, "Other")
        assertEquals(listOf("Other"), models)
    }

    @Test
    fun test9_getModelsForMake_returnsOtherWhenMakeIsCustomOrUnknown() {
        val models = catalogRepository.getModelsForMake(VehicleType.BIKE, "CustomBrand123")
        assertEquals(listOf("Other"), models)
    }

    @Test
    fun test10_isValidMakeForVehicleType_returnsTrueForValidBikeMake() {
        assertTrue("TVS should be valid make for BIKE", catalogRepository.isValidMakeForVehicleType(VehicleType.BIKE, "TVS"))
    }

    @Test
    fun test11_isValidMakeForVehicleType_returnsFalseForCarMakeInBikeType() {
        assertFalse("Maruti Suzuki should be invalid make for BIKE", catalogRepository.isValidMakeForVehicleType(VehicleType.BIKE, "Maruti Suzuki"))
    }

    @Test
    fun test12_isValidMakeForVehicleType_returnsTrueForOther() {
        assertTrue("Other should be valid for BIKE", catalogRepository.isValidMakeForVehicleType(VehicleType.BIKE, "Other"))
        assertTrue("Other should be valid for CAR", catalogRepository.isValidMakeForVehicleType(VehicleType.CAR, "Other"))
    }

    @Test
    fun test13_isValidMakeForVehicleType_returnsFalseForBlankOrUnknown() {
        assertFalse("Blank make should be invalid", catalogRepository.isValidMakeForVehicleType(VehicleType.BIKE, ""))
        assertFalse("Spaces make should be invalid", catalogRepository.isValidMakeForVehicleType(VehicleType.BIKE, "   "))
    }

    @Test
    fun test14_isValidModelForMake_returnsTrueForValidModelOfMake() {
        assertTrue("Classic 350 should be valid model for Royal Enfield BIKE", catalogRepository.isValidModelForMake(VehicleType.BIKE, "Royal Enfield", "Classic 350"))
    }

    @Test
    fun test15_isValidModelForMake_returnsFalseForMismatchedModel() {
        assertFalse("Nexon should be invalid model for Maruti Suzuki CAR", catalogRepository.isValidModelForMake(VehicleType.CAR, "Maruti Suzuki", "Nexon"))
    }

    @Test
    fun test16_isValidModelForMake_returnsTrueWhenModelIsOther() {
        assertTrue("Other model should be valid for Honda BIKE", catalogRepository.isValidModelForMake(VehicleType.BIKE, "Honda", "Other"))
    }

    @Test
    fun test17_isValidModelForMake_returnsTrueWhenMakeIsOtherAndModelNonBlank() {
        assertTrue("Custom model should be valid when make is Other", catalogRepository.isValidModelForMake(VehicleType.BIKE, "Other", "CustomModel123"))
    }

    @Test
    fun test18_isValidModelForMake_returnsFalseWhenModelIsBlank() {
        assertFalse("Blank model should be invalid", catalogRepository.isValidModelForMake(VehicleType.BIKE, "Honda", ""))
    }

    @Test
    fun test19_inventoryViewModel_getAvailableMakes_delegatesToRepository() {
        val makes = viewModel.getAvailableMakes(VehicleType.BIKE)
        assertTrue(makes.contains("Honda"))
        assertTrue(makes.contains("TVS"))
        assertTrue(makes.contains("Other"))
    }

    @Test
    fun test20_inventoryViewModel_getAvailableModels_delegatesToRepository() {
        val models = viewModel.getAvailableModels(VehicleType.CAR, "Tata Motors")
        assertTrue(models.contains("Nexon"))
        assertTrue(models.contains("Punch"))
        assertTrue(models.contains("Other"))
    }

    @Test
    fun test21_inventoryViewModel_isValidMake_and_isValidModel() {
        assertTrue(viewModel.isValidMake(VehicleType.CAR, "Mahindra"))
        assertTrue(viewModel.isValidModel(VehicleType.CAR, "Mahindra", "Thar"))
        assertFalse(viewModel.isValidModel(VehicleType.CAR, "Mahindra", "Activa 6G"))
    }

    @Test
    fun test22_inventoryViewModel_validation_returnsErrorForBlankMakeOrModel() {
        val invalidVehicleBlankMake = Vehicle(
            chassisNumber = "CHASSIS123456",
            engineNumber = "ENGINE123456",
            make = "",
            model = "Activa 6G",
            color = "Red",
            year = 2024,
            costPrice = 80000.0,
            listedPrice = 95000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )
        val err1 = viewModel.validateVehicle(invalidVehicleBlankMake)
        assertNotNull(err1)
        assertTrue("Error should mention Make", err1!!.contains("Make cannot be empty"))

        val invalidVehicleBlankModel = Vehicle(
            chassisNumber = "CHASSIS123456",
            engineNumber = "ENGINE123456",
            make = "Honda",
            model = "",
            color = "Red",
            year = 2024,
            costPrice = 80000.0,
            listedPrice = 95000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )
        val err2 = viewModel.validateVehicle(invalidVehicleBlankModel)
        assertNotNull(err2)
        assertTrue("Error should mention Model", err2!!.contains("Model cannot be empty"))
    }

    @Test
    fun test23_inventoryViewModel_validation_returnsErrorForOtherWithBlankCustomValue() {
        val vehicleMakeOther = Vehicle(
            chassisNumber = "CHASSIS123456",
            engineNumber = "ENGINE123456",
            make = "Other",
            model = "CustomModel",
            color = "Red",
            year = 2024,
            costPrice = 80000.0,
            listedPrice = 95000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )
        val err1 = viewModel.validateVehicle(vehicleMakeOther, customMake = "")
        assertNotNull(err1)
        assertTrue("Error should ask for Custom Make Name", err1!!.contains("Custom Make Name is required"))

        val vehicleModelOther = Vehicle(
            chassisNumber = "CHASSIS123456",
            engineNumber = "ENGINE123456",
            make = "Honda",
            model = "Other",
            color = "Red",
            year = 2024,
            costPrice = 80000.0,
            listedPrice = 95000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )
        val err2 = viewModel.validateVehicle(vehicleModelOther, customModel = "")
        assertNotNull(err2)
        assertTrue("Error should ask for Custom Model Name", err2!!.contains("Custom Model Name is required"))
    }

    @Test
    fun test24_inventoryViewModel_validation_succeedsForOtherWithValidCustomValues() {
        val vehicleCustomBoth = Vehicle(
            chassisNumber = "CHASSIS123456",
            engineNumber = "ENGINE123456",
            make = "Other",
            model = "Other",
            color = "Red",
            year = 2024,
            costPrice = 80000.0,
            listedPrice = 95000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )
        val err = viewModel.validateVehicle(
            vehicle = vehicleCustomBoth,
            customMake = "Tesla",
            customModel = "Model S"
        )
        assertNull("Validation should succeed with non-blank custom make & model", err)
    }

    @Test
    fun test25_sourceArchitecture_dropdownsAndAddBikeScreen_noProhibitedComponents() {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        var searchableDropdownFile = File(userDir, "app/src/main/java/com/automotive/salesfinance/ui/components/SearchableDropdown.kt")
        if (!searchableDropdownFile.exists()) {
            searchableDropdownFile = File(userDir, "src/main/java/com/automotive/salesfinance/ui/components/SearchableDropdown.kt")
        }

        var addBikeScreenFile = File(userDir, "app/src/main/java/com/automotive/salesfinance/ui/inventory/AddBikeScreen.kt")
        if (!addBikeScreenFile.exists()) {
            addBikeScreenFile = File(userDir, "src/main/java/com/automotive/salesfinance/ui/inventory/AddBikeScreen.kt")
        }

        assertTrue("SearchableDropdown.kt file must exist", searchableDropdownFile.exists())
        assertTrue("AddBikeScreen.kt file must exist", addBikeScreenFile.exists())

        val dropdownText = searchableDropdownFile.readText()
        val addBikeText = addBikeScreenFile.readText()

        assertFalse("SearchableDropdown.kt should not contain verticalScroll", dropdownText.contains("verticalScroll"))
        assertFalse("SearchableDropdown.kt should not contain LazyColumn", dropdownText.contains("LazyColumn"))

        val prohibited = listOf(
            "ExposedDropdownMenuBox",
            "LazyColumn",
            "TabRow",
            "IntrinsicSize",
            "LazyRow",
            "ScrollableTabRow",
            "BoxWithConstraints"
        )

        for (comp in prohibited) {
            assertFalse("SearchableDropdown.kt should not contain $comp", dropdownText.contains(comp))
            assertFalse("AddBikeScreen.kt should not contain $comp", addBikeText.contains(comp))
        }
    }
}
