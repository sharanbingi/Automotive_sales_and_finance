package com.automotive.salesfinance

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.SubscriptionRepository
import com.automotive.salesfinance.repository.SupportTicketRepository
import com.automotive.salesfinance.repository.UserRepository
import com.automotive.salesfinance.viewmodel.SuperAdminViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class Phase7AOnboardingTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var supportTicketRepository: SupportTicketRepository
    private lateinit var auditLogRepository: AuditLogRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: SuperAdminViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        authRepository.setDemoMode(false)

        dealershipRepository = DealershipRepository(authRepository)
        subscriptionRepository = SubscriptionRepository(authRepository)
        supportTicketRepository = SupportTicketRepository(authRepository)
        auditLogRepository = AuditLogRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        userRepository = UserRepository(authRepository)

        viewModel = SuperAdminViewModel(
            authRepository = authRepository,
            dealershipRepository = dealershipRepository,
            subscriptionRepository = subscriptionRepository,
            supportTicketRepository = supportTicketRepository,
            auditLogRepository = auditLogRepository,
            storeRepository = storeRepository,
            userRepository = userRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test1_superAdmin_canInitiateDealershipCreation() = runTest {
        authRepository.loginDemoRole(UserRole.SUPER_ADMIN)
        authRepository.setDemoMode(false)

        var successCallbackTriggered = false

        viewModel.createDealership(
            name = "Apex Motors",
            legalName = "Apex Motors Private Limited",
            email = "contact@apexmotors.com",
            phone = "9876543210",
            address = "123 High Street",
            city = "Hyderabad",
            state = "Telangana",
            supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR),
            subscriptionPlan = "STARTER",
            trialDays = 14,
            storeName = "Apex Motors Flagship Store",
            storeCity = "Hyderabad",
            storeState = "TG",
            storeAddress = "123 High Street",
            adminName = "Rajesh Kumar",
            adminEmail = "rajesh@apexmotors.com",
            adminPhone = "9876543211",
            onSuccess = { successCallbackTriggered = true }
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Success callback should be triggered", successCallbackTriggered)
        assertNotNull("Success message should be set", viewModel.successMessage.value)
        assertTrue("Success message contains dealership name", viewModel.successMessage.value?.contains("Apex Motors") == true)
        assertNull("Error message should be null", viewModel.errorMessage.value)

        val created = dealershipRepository.getDealershipById("apex_motors")
        assertNotNull("Dealership should exist in repository", created)
        assertEquals("Apex Motors", created?.name)
        assertEquals("Apex Motors Private Limited", created?.legalName)
        assertEquals(SubscriptionStatus.TRIAL, created?.subscriptionStatus)
    }

    @Test
    fun test2_nonSuperAdmin_cannotCreateDealerships() = runTest {
        authRepository.loginDemoRole(UserRole.SALES_USER)

        viewModel.createDealership(
            name = "Unauthorized Dealership",
            legalName = "Unauthorized Pvt Ltd",
            email = "test@unauthorized.com",
            phone = "9876543210",
            address = "123 Street",
            city = "City",
            state = "State",
            supportedVehicleTypes = listOf(VehicleType.BIKE),
            subscriptionPlan = "STARTER",
            trialDays = 14,
            storeName = "Primary Store",
            storeCity = "City",
            storeState = "TG",
            storeAddress = "123 Street",
            adminName = "Admin User",
            adminEmail = "admin@unauthorized.com"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull("Error message should be set for non-Super Admin", viewModel.errorMessage.value)
        assertTrue("Error message mentions unauthorized", viewModel.errorMessage.value?.contains("Unauthorized") == true)
        assertFalse("ViewModel isSuccess should be false", viewModel.isSuccess.value)
        assertNull("Dealership should not be created", dealershipRepository.getDealershipById("unauthorized_dealership"))
    }

    @Test
    fun test3_formFieldValidations_requireEssentialFields() = runTest {
        authRepository.loginDemoRole(UserRole.SUPER_ADMIN)

        // Blank Name
        viewModel.createDealership(
            name = "",
            legalName = "Legal Name",
            email = "email@test.com",
            phone = "9876543210",
            address = "Address",
            city = "City",
            state = "State",
            supportedVehicleTypes = listOf(VehicleType.BIKE),
            storeName = "Store Name",
            storeCity = "Store City",
            storeState = "TG",
            adminName = "Admin Name",
            adminEmail = "admin@test.com"
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Dealership Name is required", viewModel.errorMessage.value)

        // Blank Legal Name
        viewModel.createDealership(
            name = "Valid Name",
            legalName = "",
            email = "email@test.com",
            phone = "9876543210",
            address = "Address",
            city = "City",
            state = "State",
            supportedVehicleTypes = listOf(VehicleType.BIKE),
            storeName = "Store Name",
            storeCity = "Store City",
            storeState = "TG",
            adminName = "Admin Name",
            adminEmail = "admin@test.com"
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Legal Name is required", viewModel.errorMessage.value)

        // Blank Contact Email
        viewModel.createDealership(
            name = "Valid Name",
            legalName = "Legal Name",
            email = "",
            phone = "9876543210",
            address = "Address",
            city = "City",
            state = "State",
            supportedVehicleTypes = listOf(VehicleType.BIKE),
            storeName = "Store Name",
            storeCity = "Store City",
            storeState = "TG",
            adminName = "Admin Name",
            adminEmail = "admin@test.com"
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Contact Email is required", viewModel.errorMessage.value)

        // Blank Store Name
        viewModel.createDealership(
            name = "Valid Name",
            legalName = "Legal Name",
            email = "test@test.com",
            phone = "9876543210",
            address = "Address",
            city = "City",
            state = "State",
            supportedVehicleTypes = listOf(VehicleType.BIKE),
            storeName = "",
            storeCity = "Store City",
            storeState = "TG",
            adminName = "Admin Name",
            adminEmail = "admin@test.com"
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Primary store name is required", viewModel.errorMessage.value)

        // Blank Admin Email
        viewModel.createDealership(
            name = "Valid Name",
            legalName = "Legal Name",
            email = "test@test.com",
            phone = "9876543210",
            address = "Address",
            city = "City",
            state = "State",
            supportedVehicleTypes = listOf(VehicleType.BIKE),
            storeName = "Primary Store",
            storeCity = "Store City",
            storeState = "TG",
            adminName = "Admin Name",
            adminEmail = ""
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Admin email is required", viewModel.errorMessage.value)
    }

    @Test
    fun test4_vehicleTypeRequirement_requiresAtLeastOneVehicleType() = runTest {
        authRepository.loginDemoRole(UserRole.SUPER_ADMIN)

        viewModel.createDealership(
            name = "No Vehicle Motors",
            legalName = "No Vehicle Motors Pvt Ltd",
            email = "contact@novehicle.com",
            phone = "9876543210",
            address = "123 Street",
            city = "City",
            state = "State",
            supportedVehicleTypes = emptyList(),
            subscriptionPlan = "STARTER",
            trialDays = 14,
            storeName = "Primary Store",
            storeCity = "City",
            storeState = "TG",
            adminName = "Admin User",
            adminEmail = "admin@novehicle.com"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("At least one supported vehicle type is required", viewModel.errorMessage.value)
        assertFalse("ViewModel isSuccess should be false", viewModel.isSuccess.value)
    }

    @Test
    fun test5_dealershipIdNormalization_convertsNameToCleanId() = runTest {
        assertEquals("apex_motors", viewModel.normalizeDealershipId("Apex Motors!"))
        assertEquals("sk_automobiles_co_1", viewModel.normalizeDealershipId("  S.K.  Automobiles & Co. #1 "))
        assertEquals("city_riders_bikes_cars", viewModel.normalizeDealershipId("City Riders - Bikes & Cars"))
    }

    @Test
    fun test6_duplicateDealershipIdRejection_failsIfIdExists() = runTest {
        authRepository.loginDemoRole(UserRole.SUPER_ADMIN)
        authRepository.setDemoMode(false)

        viewModel.createDealership(
            name = "Apex Motors",
            legalName = "Apex Motors Pvt Ltd",
            email = "info@apex.com",
            phone = "9876543210",
            address = "123 Road",
            city = "Hyderabad",
            state = "TG",
            supportedVehicleTypes = listOf(VehicleType.BIKE),
            storeName = "Store 1",
            storeCity = "Hyderabad",
            storeState = "TG",
            adminName = "Admin 1",
            adminEmail = "admin1@apex.com"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull("First dealership created", dealershipRepository.getDealershipById("apex_motors"))

        viewModel.createDealership(
            name = "Apex Motors!",
            legalName = "Apex Motors Duplicate Pvt Ltd",
            email = "info2@apex.com",
            phone = "9876543211",
            address = "456 Road",
            city = "Hyderabad",
            state = "TG",
            supportedVehicleTypes = listOf(VehicleType.BIKE),
            storeName = "Store 2",
            storeCity = "Hyderabad",
            storeState = "TG",
            adminName = "Admin 2",
            adminEmail = "admin2@apex.com"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull("Error message set for duplicate ID", viewModel.errorMessage.value)
        assertTrue("Error message mentions duplicate ID", viewModel.errorMessage.value?.contains("already exists") == true)
    }

    @Test
    fun test7_atomicBatchCreation_createsAllEntities() = runTest {
        authRepository.loginDemoRole(UserRole.SUPER_ADMIN)
        authRepository.setDemoMode(false)

        val dId = "quantum_wheels"
        val res = dealershipRepository.createDealershipAtomic(
            dealershipId = dId,
            name = "Quantum Wheels",
            legalName = "Quantum Wheels Pvt Ltd",
            email = "contact@quantum.com",
            phone = "9998887770",
            address = "789 Tech Park",
            city = "Bengaluru",
            state = "Karnataka",
            supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR),
            subscriptionPlan = "PROFESSIONAL",
            trialDays = 30,
            storeName = "Quantum Wheels Bengaluru Central",
            storeCity = "Bengaluru",
            storeState = "KA",
            storeAddress = "789 Tech Park",
            adminName = "Anil Sharma",
            adminEmail = "anil@quantum.com",
            subscriptionRepository = subscriptionRepository,
            storeRepository = storeRepository,
            userRepository = userRepository,
            auditLogRepository = auditLogRepository
        )

        assertTrue("createDealershipAtomic returned success", res.isSuccess)

        val dealership = dealershipRepository.getDealershipById(dId)
        assertNotNull("Dealership created", dealership)
        assertEquals("Quantum Wheels", dealership?.name)
        assertEquals("PROFESSIONAL", dealership?.subscriptionPlan)

        val subscription = subscriptionRepository.subscriptionsFlow.value.find { it.dealershipId == dId }
        assertNotNull("Subscription created", subscription)
        assertEquals("PROFESSIONAL", subscription?.planId)
        assertEquals(SubscriptionStatus.TRIAL, subscription?.status)

        val stores = storeRepository.getStoresForDealership(dId)
        assertEquals("One primary store created", 1, stores.size)
        val store = stores.first()
        assertEquals("Quantum Wheels Bengaluru Central", store.storeName)
        assertEquals("KA", store.stateCode)

        val users = userRepository.getUsersForDealership(dId)
        assertEquals("One admin user created", 1, users.size)
        val admin = users.first()
        assertEquals("Anil Sharma", admin.name)
        assertEquals("anil@quantum.com", admin.email)
        assertEquals(UserRole.DEALERSHIP_ADMIN, admin.role)
        assertEquals(false, admin.active)

        val auditLogs = auditLogRepository.getAuditLogsForDealership(dId)
        assertEquals("One audit log created", 1, auditLogs.size)
        assertEquals("CREATE_DEALERSHIP", auditLogs.first().action)
    }

    @Test
    fun test8_trialDatesGeneration_setsTrialStartAndEndCorrectly() = runTest {
        authRepository.loginDemoRole(UserRole.SUPER_ADMIN)
        authRepository.setDemoMode(false)

        val beforeTime = System.currentTimeMillis()

        viewModel.createDealership(
            name = "Trial Motors",
            legalName = "Trial Motors Pvt Ltd",
            email = "trial@motors.com",
            phone = "9876543210",
            address = "Trial Street",
            city = "Chennai",
            state = "Tamil Nadu",
            supportedVehicleTypes = listOf(VehicleType.BIKE),
            subscriptionPlan = "STARTER",
            trialDays = 21,
            storeName = "Trial Store",
            storeCity = "Chennai",
            storeState = "TN",
            adminName = "Trial Admin",
            adminEmail = "admin@trial.com"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val afterTime = System.currentTimeMillis()

        val dealership = dealershipRepository.getDealershipById("trial_motors")
        assertNotNull("Dealership created", dealership)
        assertEquals(SubscriptionStatus.TRIAL, dealership?.subscriptionStatus)

        assertTrue("Trial start date >= beforeTime", dealership!!.trialStartDate >= beforeTime)
        assertTrue("Trial start date <= afterTime", dealership.trialStartDate <= afterTime)

        val expectedDurationMs = 21L * 24 * 60 * 60 * 1000L
        val diff = dealership.trialEndDate - dealership.trialStartDate
        assertEquals(expectedDurationMs, diff)
    }

    @Test
    fun test9_productionPath_neverUsesDemoData() = runTest {
        authRepository.setDemoMode(false)

        val initialDealerships = dealershipRepository.dealershipsFlow.value
        assertTrue("Production dealerships flow is empty", initialDealerships.isEmpty())

        authRepository.loginDemoRole(UserRole.SUPER_ADMIN)
        authRepository.setDemoMode(false)

        viewModel.createDealership(
            name = "Production Motors",
            legalName = "Production Motors Pvt Ltd",
            email = "prod@motors.com",
            phone = "9876543210",
            address = "Prod Way",
            city = "Delhi",
            state = "Delhi",
            supportedVehicleTypes = listOf(VehicleType.CAR),
            subscriptionPlan = "BUSINESS",
            trialDays = 14,
            storeName = "Delhi Store",
            storeCity = "Delhi",
            storeState = "DL",
            adminName = "Delhi Admin",
            adminEmail = "admin@delhimotors.com"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val currentDealerships = dealershipRepository.dealershipsFlow.value
        assertEquals("Only 1 dealership in production flow", 1, currentDealerships.size)
        assertEquals("production_motors", currentDealerships.first().dealershipId)

        val demoIds = DemoData.dealerships.map { it.dealershipId }.toSet()
        assertFalse("No demo dealership IDs exist in production flow", currentDealerships.any { demoIds.contains(it.dealershipId) })
    }

    @Test
    fun test10_dashboardRefresh_updatesPlatformStatsAfterCreation() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.platformStats.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.filteredDealerships.collect {}
        }

        authRepository.setDemoMode(false)
        authRepository.loginDemoRole(UserRole.SUPER_ADMIN)
        authRepository.setDemoMode(false)

        testDispatcher.scheduler.advanceUntilIdle()

        val statsBefore = viewModel.platformStats.value
        assertEquals(0, statsBefore.totalDealerships)
        assertEquals(0, statsBefore.trialDealerships)

        viewModel.createDealership(
            name = "Refresh Motors",
            legalName = "Refresh Motors Pvt Ltd",
            email = "refresh@motors.com",
            phone = "9876543210",
            address = "Refresh Street",
            city = "Pune",
            state = "Maharashtra",
            supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR),
            subscriptionPlan = "STARTER",
            trialDays = 14,
            storeName = "Pune Store",
            storeCity = "Pune",
            storeState = "MH",
            adminName = "Pune Admin",
            adminEmail = "admin@refresh.com"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val statsAfter = viewModel.platformStats.value
        assertEquals("Total dealerships updated to 1", 1, statsAfter.totalDealerships)
        assertEquals("Trial dealerships updated to 1", 1, statsAfter.trialDealerships)
        assertEquals("Filtered dealerships updated to 1", 1, viewModel.filteredDealerships.value.size)
    }

    @Test
    fun superAdmin_preservesGlobalDealershipCollectionFetch() = runTest {
        val testAuthRepo = AuthRepository()
        testAuthRepo.setDemoMode(false)
        testAuthRepo.setCurrentUser(
            User(
                uid = "SUPER_ADMIN_TEST",
                email = "superadmin@test.com",
                role = UserRole.SUPER_ADMIN,
                active = true
            )
        )

        var globalFetchCalled = false
        val trackingRepo = object : DealershipRepository(testAuthRepo) {
            override suspend fun fetchDealershipsFromFirestore(): List<Dealership> {
                globalFetchCalled = true
                return super.fetchDealershipsFromFirestore()
            }
        }

        val testVm = SuperAdminViewModel(
            authRepository = testAuthRepo,
            dealershipRepository = trackingRepo,
            subscriptionRepository = SubscriptionRepository(testAuthRepo),
            supportTicketRepository = SupportTicketRepository(testAuthRepo),
            auditLogRepository = AuditLogRepository(testAuthRepo),
            storeRepository = StoreRepository(testAuthRepo),
            userRepository = UserRepository(testAuthRepo)
        )

        testVm.loadPlatformData()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("SuperAdminViewModel must preserve global collection fetch via fetchDealershipsFromFirestore", globalFetchCalled)
    }
}
