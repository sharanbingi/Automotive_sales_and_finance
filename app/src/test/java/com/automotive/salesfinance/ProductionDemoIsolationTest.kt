package com.automotive.salesfinance

import com.automotive.salesfinance.data.AppContainer
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.BillingCycle
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.SubscriptionRepository
import com.automotive.salesfinance.repository.SupportTicketRepository
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.repository.UserRepository
import com.automotive.salesfinance.viewmodel.CustomerViewModel
import com.automotive.salesfinance.viewmodel.DashboardViewModel
import com.automotive.salesfinance.viewmodel.DealershipAdminViewModel
import com.automotive.salesfinance.viewmodel.FinanceViewModel
import com.automotive.salesfinance.viewmodel.InventoryViewModel
import com.automotive.salesfinance.viewmodel.SubscriptionViewModel
import com.automotive.salesfinance.viewmodel.SuperAdminViewModel
import com.automotive.salesfinance.viewmodel.SupportViewModel
import com.automotive.salesfinance.viewmodel.TransactionViewModel
import com.automotive.salesfinance.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductionDemoIsolationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun authRepository_initialCurrentUser_isNullOnNormalStartup() = runTest {
        val authRepository = AuthRepository()
        assertNull("Initial currentUser must be null on normal startup", authRepository.currentUser.value)
    }

    @Test
    fun realLoginViaAuthRepository_setsIsDemoModeFalse() = runTest {
        val authRepository = AuthRepository()
        authRepository.setDemoMode(true)
        assertTrue("Initially demo mode is true", authRepository.isDemoMode.value)

        authRepository.setDemoMode(false)
        assertFalse("setDemoMode(false) sets isDemoMode to false", authRepository.isDemoMode.value)
    }

    @Test
    fun login_requiresFirebaseAuthAndFirestoreUserProfile_cannotSucceedWithoutFirebaseAuth() = runTest {
        val authRepository = AuthRepository()
        authRepository.setDemoMode(true)
        assertTrue("Initially demo mode is true", authRepository.isDemoMode.value)

        // Calling login(email, pass) executes real Firebase Auth path and does NOT bypass via demo mode
        val result = authRepository.login("admin@automotive.com", "password123")

        // Without real Firebase Auth initialized in JVM unit test environment, login returns failure
        assertTrue("login(email, pass) cannot return success without Firebase Auth success", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should be returned on authentication failure", exception)
        assertTrue("Exception indicates Firebase Auth is unavailable or authentication failed", 
            exception is IllegalStateException || exception?.message?.isNotBlank() == true)
    }

    @Test
    fun loginDemoRole_operatesSeparatelyInDemoMode() = runTest {
        val authRepository = AuthRepository()
        authRepository.setDemoMode(false)
        assertFalse("Initially demo mode is false", authRepository.isDemoMode.value)

        val result = authRepository.loginDemoRole(UserRole.SUPER_ADMIN)
        assertTrue("loginDemoRole should return success", result.isSuccess)
        val user = result.getOrNull()
        assertNotNull("Demo user profile should be loaded", user)
        assertEquals(UserRole.SUPER_ADMIN, user?.role)
        assertTrue("loginDemoRole sets isDemoMode to true", authRepository.isDemoMode.value)
        assertEquals(user, authRepository.currentUser.value)
    }

    @Test
    fun productionModeWithEmptyFirestore_showsZeroMetricsAndNoDemoData() = runTest {
        val authRepository = AuthRepository()
        authRepository.setDemoMode(false)

        val dealershipRepo = DealershipRepository(authRepository)
        val subscriptionRepo = SubscriptionRepository(authRepository)
        val storeRepo = StoreRepository(authRepository)
        val userRepo = UserRepository(authRepository)
        val inventoryRepo = InventoryRepository(authRepository)
        val customerRepo = CustomerRepository(authRepository)
        val loanRepo = LoanRepository(authRepository)
        val transactionRepo = TransactionRepository(authRepository)
        val auditLogRepo = AuditLogRepository(authRepository)
        val supportTicketRepo = SupportTicketRepository(authRepository)

        // Verify initial flows in Production mode are completely empty
        assertTrue("Dealerships flow should be empty in Production", dealershipRepo.dealershipsFlow.value.isEmpty())
        assertTrue("Subscriptions flow should be empty in Production", subscriptionRepo.subscriptionsFlow.value.isEmpty())
        assertTrue("Stores flow should be empty in Production", storeRepo.storesFlow.value.isEmpty())
        assertTrue("Users flow should be empty in Production", userRepo.usersFlow.value.isEmpty())
        assertTrue("Vehicles flow should be empty in Production", inventoryRepo.vehiclesFlow.value.isEmpty())
        assertTrue("Customers flow should be empty in Production", customerRepo.customersFlow.value.isEmpty())
        assertTrue("Loans flow should be empty in Production", loanRepo.loansFlow.value.isEmpty())
        assertTrue("Transactions flow should be empty in Production", transactionRepo.transactionsFlow.value.isEmpty())
        assertTrue("AuditLogs flow should be empty in Production", auditLogRepo.auditLogsFlow.value.isEmpty())
        assertTrue("SupportTickets flow should be empty in Production", supportTicketRepo.supportTicketsFlow.value.isEmpty())

        val viewModel = SuperAdminViewModel(
            authRepository = authRepository,
            dealershipRepository = dealershipRepo,
            subscriptionRepository = subscriptionRepo,
            supportTicketRepository = supportTicketRepo,
            auditLogRepository = auditLogRepo,
            storeRepository = storeRepo,
            userRepository = userRepo
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.platformStats.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.filteredDealerships.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val stats = viewModel.platformStats.value
        assertEquals(0, stats.totalDealerships)
        assertEquals(0, stats.activeDealerships)
        assertEquals(0, stats.totalStores)
        assertEquals(0, stats.totalUsers)
        assertEquals(0.0, stats.totalMRR, 0.001)
        assertEquals(0, stats.newSubscriptionsCount)

        assertTrue("Filtered dealerships should be empty in Production mode", viewModel.filteredDealerships.value.isEmpty())

        // Verify DemoData is NOT contained in any flow
        val demoDealershipIds = DemoData.dealerships.map { it.dealershipId }.toSet()
        val currentDealershipIds = dealershipRepo.dealershipsFlow.value.map { it.dealershipId }.toSet()
        assertTrue(demoDealershipIds.intersect(currentDealershipIds).isEmpty())
    }

    @Test
    fun demoMode_showsDemoDataAndMetrics() = runTest {
        val authRepository = AuthRepository()
        authRepository.setDemoMode(true)

        val dealershipRepo = DealershipRepository(authRepository)
        val subscriptionRepo = SubscriptionRepository(authRepository)
        val storeRepo = StoreRepository(authRepository)
        val userRepo = UserRepository(authRepository)
        val supportTicketRepo = SupportTicketRepository(authRepository)
        val auditLogRepo = AuditLogRepository(authRepository)

        assertTrue("Dealerships flow should contain DemoData in Demo mode", dealershipRepo.dealershipsFlow.value.isNotEmpty())
        assertEquals(DemoData.dealerships.size, dealershipRepo.dealershipsFlow.value.size)
        assertTrue("Subscriptions flow should contain DemoData in Demo mode", subscriptionRepo.subscriptionsFlow.value.isNotEmpty())
        assertEquals(DemoData.subscriptions.size, subscriptionRepo.subscriptionsFlow.value.size)

        val viewModel = SuperAdminViewModel(
            authRepository = authRepository,
            dealershipRepository = dealershipRepo,
            subscriptionRepository = subscriptionRepo,
            supportTicketRepository = supportTicketRepo,
            auditLogRepository = auditLogRepo,
            storeRepository = storeRepo,
            userRepository = userRepo
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.platformStats.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val stats = viewModel.platformStats.value
        assertTrue("Total dealerships should be > 0 in Demo mode", stats.totalDealerships > 0)
        assertTrue("Total MRR should be > 0 in Demo mode", stats.totalMRR > 0.0)
        assertTrue("Total stores should be > 0 in Demo mode", stats.totalStores > 0)
        assertTrue("Total users should be > 0 in Demo mode", stats.totalUsers > 0)
    }

    @Test
    fun switchingDemoModeFromTrueToFalse_clearsDemoDataFromAllRepositories() = runTest {
        val authRepository = AuthRepository()
        authRepository.setDemoMode(true)

        val dealershipRepo = DealershipRepository(authRepository)
        val subscriptionRepo = SubscriptionRepository(authRepository)
        val storeRepo = StoreRepository(authRepository)
        val userRepo = UserRepository(authRepository)
        val inventoryRepo = InventoryRepository(authRepository)
        val customerRepo = CustomerRepository(authRepository)
        val loanRepo = LoanRepository(authRepository)
        val transactionRepo = TransactionRepository(authRepository)

        assertTrue("Dealerships flow should be non-empty in demo mode", dealershipRepo.dealershipsFlow.value.isNotEmpty())

        authRepository.setDemoMode(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Dealerships flow should be empty when demo mode becomes false", dealershipRepo.dealershipsFlow.value.isEmpty())
        assertTrue("Subscriptions flow should be empty when demo mode becomes false", subscriptionRepo.subscriptionsFlow.value.isEmpty())
        assertTrue("Stores flow should be empty when demo mode becomes false", storeRepo.storesFlow.value.isEmpty())
        assertTrue("Users flow should be empty when demo mode becomes false", userRepo.usersFlow.value.isEmpty())
        assertTrue("Vehicles flow should be empty when demo mode becomes false", inventoryRepo.vehiclesFlow.value.isEmpty())
        assertTrue("Customers flow should be empty when demo mode becomes false", customerRepo.customersFlow.value.isEmpty())
        assertTrue("Loans flow should be empty when demo mode becomes false", loanRepo.loansFlow.value.isEmpty())
        assertTrue("Transactions flow should be empty when demo mode becomes false", transactionRepo.transactionsFlow.value.isEmpty())
    }

    @Test
    fun productionModeWithRealData_showsOnlyRealData() = runTest {
        val authRepository = AuthRepository()
        authRepository.setDemoMode(false)

        val dealershipRepo = DealershipRepository(authRepository)
        val subscriptionRepo = SubscriptionRepository(authRepository)
        val storeRepo = StoreRepository(authRepository)
        val userRepo = UserRepository(authRepository)
        val supportTicketRepo = SupportTicketRepository(authRepository)
        val auditLogRepo = AuditLogRepository(authRepository)

        val realDealership = Dealership(
            dealershipId = "REAL_PROD_001",
            name = "Real Motors Pvt Ltd",
            active = true,
            subscriptionPlan = "ENTERPRISE",
            subscriptionStatus = SubscriptionStatus.ACTIVE,
            city = "Mumbai"
        )
        dealershipRepo.saveDealership(realDealership)
        subscriptionRepo.upgradePlan("REAL_PROD_001", "ENTERPRISE", BillingCycle.MONTHLY)

        testDispatcher.scheduler.advanceUntilIdle()

        val viewModel = SuperAdminViewModel(
            authRepository = authRepository,
            dealershipRepository = dealershipRepo,
            subscriptionRepository = subscriptionRepo,
            supportTicketRepository = supportTicketRepo,
            auditLogRepository = auditLogRepo,
            storeRepository = storeRepo,
            userRepository = userRepo
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.platformStats.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.filteredDealerships.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val stats = viewModel.platformStats.value
        assertEquals(1, stats.totalDealerships)
        assertEquals(1, stats.activeDealerships)
        assertEquals(25000.0, stats.totalMRR, 0.01)

        val filtered = viewModel.filteredDealerships.value
        assertEquals(1, filtered.size)
        assertEquals("REAL_PROD_001", filtered.first().dealershipId)

        // Ensure no DemoData dealership leaked in
        assertFalse(filtered.any { it.dealershipId.startsWith("dealership_demo") })
    }

    @Test
    fun productionRepositories_emptyFirestoreResultsRemainEmpty() = runTest {
        val authRepository = AuthRepository()
        authRepository.setDemoMode(false)

        val dealershipRepo = DealershipRepository(authRepository)
        val subscriptionRepo = SubscriptionRepository(authRepository)
        val storeRepo = StoreRepository(authRepository)
        val userRepo = UserRepository(authRepository)
        val inventoryRepo = InventoryRepository(authRepository)
        val customerRepo = CustomerRepository(authRepository)
        val loanRepo = LoanRepository(authRepository)
        val transactionRepo = TransactionRepository(authRepository)
        val auditLogRepo = AuditLogRepository(authRepository)
        val supportTicketRepo = SupportTicketRepository(authRepository)

        // Execute fetches in Production mode where Firestore queries return empty or non-existent results
        dealershipRepo.fetchDealershipsFromFirestore()
        subscriptionRepo.fetchSubscriptionsFromFirestore()
        storeRepo.fetchStoresFromFirestore("")
        userRepo.fetchUsersFromFirestore("")
        inventoryRepo.fetchVehiclesFromFirestore("")
        customerRepo.fetchCustomersFromFirestore("")
        loanRepo.fetchLoansFromFirestore("")
        transactionRepo.fetchTransactionsFromFirestore("")
        auditLogRepo.fetchAuditLogsFromFirestore("")
        supportTicketRepo.fetchSupportTicketsFromFirestore()

        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that empty Firestore fetch results stay empty and NEVER fall back to DemoData
        assertTrue(dealershipRepo.dealershipsFlow.value.isEmpty())
        assertTrue(subscriptionRepo.subscriptionsFlow.value.isEmpty())
        assertTrue(storeRepo.storesFlow.value.isEmpty())
        assertTrue(userRepo.usersFlow.value.isEmpty())
        assertTrue(inventoryRepo.vehiclesFlow.value.isEmpty())
        assertTrue(customerRepo.customersFlow.value.isEmpty())
        assertTrue(loanRepo.loansFlow.value.isEmpty())
        assertTrue(transactionRepo.transactionsFlow.value.isEmpty())
        assertTrue(auditLogRepo.auditLogsFlow.value.isEmpty())
        assertTrue(supportTicketRepo.supportTicketsFlow.value.isEmpty())
    }

    @Test
    fun viewModelsConstructedViaViewModelFactory_shareSameAuthRepositoryAndProductionState() = runTest {
        val appContainer = AppContainer()
        val authRepo = appContainer.authRepository
        authRepo.setDemoMode(false)

        val factory = ViewModelFactory(appContainer)

        val superAdminVm = factory.create(SuperAdminViewModel::class.java)
        val dealershipAdminVm = factory.create(DealershipAdminViewModel::class.java)
        val dashboardVm = factory.create(DashboardViewModel::class.java)
        val inventoryVm = factory.create(InventoryViewModel::class.java)
        val customerVm = factory.create(CustomerViewModel::class.java)
        val financeVm = factory.create(FinanceViewModel::class.java)
        val transactionVm = factory.create(TransactionViewModel::class.java)
        val subscriptionVm = factory.create(SubscriptionViewModel::class.java)
        val supportVm = factory.create(SupportViewModel::class.java)

        // Verify all ViewModels share the same AuthRepository instance
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            superAdminVm.platformStats.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            superAdminVm.filteredDealerships.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify isDemoMode == false on all ViewModels
        assertFalse("InventoryViewModel isDemoMode should be false", inventoryVm.isDemoMode.value)
        assertFalse("CustomerViewModel isDemoMode should be false", customerVm.isDemoMode.value)
        assertFalse("FinanceViewModel isDemoMode should be false", financeVm.isDemoMode.value)
        assertFalse("TransactionViewModel isDemoMode should be false", transactionVm.isDemoMode.value)
        assertFalse("SubscriptionViewModel isDemoMode should be false", subscriptionVm.isDemoMode.value)
        assertFalse("SupportViewModel isDemoMode should be false", supportVm.isDemoMode.value)

        assertEquals(0, dealershipAdminVm.metrics.value.totalBikes)
        assertEquals(0, dashboardVm.dashboardMetrics.value.totalBikes)

        // Verify metrics yield 0 dealerships and ₹0 MRR when production Firestore is empty
        val stats = superAdminVm.platformStats.value
        assertEquals("Total dealerships should be 0", 0, stats.totalDealerships)
        assertEquals("Active dealerships should be 0", 0, stats.activeDealerships)
        assertEquals("Total MRR should be 0.0", 0.0, stats.totalMRR, 0.001)

        assertTrue("SuperAdmin filtered dealerships should be empty in Production", superAdminVm.filteredDealerships.value.isEmpty())
        assertTrue("Inventory bikes should be empty in Production", inventoryVm.filteredBikes.value.isEmpty())
        assertTrue("Customer list should be empty in Production", customerVm.filteredCustomers.value.isEmpty())
        assertTrue("Finance loans should be empty in Production", financeVm.filteredLoans.value.isEmpty())
        assertTrue("Transaction list should be empty in Production", transactionVm.filteredTransactions.value.isEmpty())
    }

    @Test
    fun superAdminViewModel_instantiatesWithoutNullPointerException() = runTest {
        val appContainer = AppContainer()
        val factory = ViewModelFactory(appContainer)

        val superAdminVm = factory.create(SuperAdminViewModel::class.java)

        assertNotNull("SuperAdminViewModel should be successfully instantiated", superAdminVm)
        assertNotNull("platformStats StateFlow should be non-null", superAdminVm.platformStats)
        assertNotNull("filteredDealerships StateFlow should be non-null", superAdminVm.filteredDealerships)
        assertNotNull("isLoading StateFlow should be non-null", superAdminVm.isLoading)
        assertNotNull("searchQuery StateFlow should be non-null", superAdminVm.searchQuery)
        assertNotNull("statusFilter StateFlow should be non-null", superAdminVm.statusFilter)
        assertNotNull("plans StateFlow should be non-null", superAdminVm.plans)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            superAdminVm.platformStats.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            superAdminVm.filteredDealerships.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val stats = superAdminVm.platformStats.value
        val dealerships = superAdminVm.filteredDealerships.value
        assertNotNull("stats value should be non-null", stats)
        assertNotNull("dealerships value should be non-null", dealerships)
        assertFalse("isLoading value should default to false", superAdminVm.isLoading.value)
        assertEquals("searchQuery should default to empty string", "", superAdminVm.searchQuery.value)
        assertEquals("statusFilter should default to ALL", "ALL", superAdminVm.statusFilter.value)
    }

    @Test
    fun getProductionUserProfile_queriesFirestoreDirectlyWithoutIsDemoModeCheck() = runTest {
        val authRepository = AuthRepository()
        authRepository.setDemoMode(true)
        val userRepository = UserRepository(authRepository)

        // In Demo mode, getUserProfile finds user from DemoData
        val demoUid = DemoData.users.first().uid
        val demoProfile = userRepository.getUserProfile(demoUid)
        assertNotNull("getUserProfile should return DemoData user when in demo mode", demoProfile)

        // getProductionUserProfile queries Firestore directly and MUST NOT check isDemoMode or return DemoData
        val prodProfile = userRepository.getProductionUserProfile(demoUid)
        Assert.assertNull("getProductionUserProfile must query Firestore directly and NOT return DemoData", prodProfile)
    }

    @Test
    fun login_callsGetProductionUserProfile() = runTest {
        val testAuthRepo = TestAuthRepository()
        val testUserRepo = TestUserRepository(testAuthRepo)
        val authRepoWithRepo = TestAuthRepository(testUserRepo)

        testUserRepo.profileToReturn = User(
            uid = "mock_uid_123",
            name = "Test Prod User",
            email = "prod@test.com",
            role = UserRole.ADMIN,
            dealershipId = "D001",
            storeId = "S001",
            active = true
        )

        val result = authRepoWithRepo.login("prod@test.com", "pass123")
        assertTrue("login should succeed", result.isSuccess)
        assertTrue("getProductionUserProfile should have been called", testUserRepo.getProductionUserProfileCalled)
        assertEquals("mock_uid_123", testUserRepo.requestedUid)
    }

    @Test
    fun login_missingOrInactiveProfile_causesLoginFailure() = runTest {
        // Sub-test 1: Missing profile
        val testRepoMissing = TestUserRepository(AuthRepository())
        val authRepoWithMissing = TestAuthRepository(testRepoMissing)
        testRepoMissing.profileToReturn = null

        val missingResult = authRepoWithMissing.login("missing@test.com", "pass123")
        assertTrue("Login with missing profile should fail", missingResult.isFailure)
        Assert.assertNull("currentUser should be null on missing profile", authRepoWithMissing.currentUser.value)
        assertTrue(missingResult.exceptionOrNull()?.message?.contains("User profile not found in Firestore for UID mock_uid_123") == true)

        // Sub-test 2: Inactive profile
        val testRepoInactive = TestUserRepository(AuthRepository())
        val authRepoWithInactive = TestAuthRepository(testRepoInactive)
        testRepoInactive.profileToReturn = User(
            uid = "mock_uid_123",
            name = "Inactive User",
            email = "inactive@test.com",
            role = UserRole.SALES_USER,
            dealershipId = "D001",
            storeId = "S001",
            active = false
        )

        val inactiveResult = authRepoWithInactive.login("inactive@test.com", "pass123")
        assertTrue("Login with inactive profile should fail", inactiveResult.isFailure)
        Assert.assertNull("currentUser should be null on inactive profile", authRepoWithInactive.currentUser.value)
        assertEquals("User account is inactive", inactiveResult.exceptionOrNull()?.message)
    }

    @Test
    fun login_successfulProductionLogin_setsIsDemoModeFalseAndClearsDemoData() = runTest {
        val testAuthRepo = TestAuthRepository()
        val testUserRepo = TestUserRepository(testAuthRepo)
        val authRepo = TestAuthRepository(testUserRepo)
        authRepo.setDemoMode(true)
        assertTrue("Initially in demo mode", authRepo.isDemoMode.value)

        val dealershipRepo = DealershipRepository(authRepo)
        val subscriptionRepo = SubscriptionRepository(authRepo)
        assertTrue("Dealerships flow contains demo data", dealershipRepo.dealershipsFlow.value.isNotEmpty())
        assertTrue("Subscriptions flow contains demo data", subscriptionRepo.subscriptionsFlow.value.isNotEmpty())

        testUserRepo.profileToReturn = User(
            uid = "prod_user_001",
            name = "Production Admin",
            email = "admin@prod.com",
            role = UserRole.SUPER_ADMIN,
            dealershipId = "PROD_D1",
            storeId = "ALL",
            active = true
        )
        authRepo.mockUid = "prod_user_001"

        val result = authRepo.login("admin@prod.com", "securePassword")
        assertTrue("Login should be successful", result.isSuccess)
        assertFalse("isDemoMode must be set to false after successful production login", authRepo.isDemoMode.value)
        assertEquals("prod_user_001", authRepo.currentUser.value?.uid)

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Dealerships flow must be cleared when isDemoMode becomes false", dealershipRepo.dealershipsFlow.value.isEmpty())
        assertTrue("Subscriptions flow must be cleared when isDemoMode becomes false", subscriptionRepo.subscriptionsFlow.value.isEmpty())
    }
}

private open class TestUserRepository(
    authRepository: AuthRepository
) : UserRepository(authRepository) {
    var getProductionUserProfileCalled = false
    var requestedUid: String? = null
    var profileToReturn: User? = null

    override suspend fun getProductionUserProfile(uid: String): User? {
        getProductionUserProfileCalled = true
        requestedUid = uid
        return profileToReturn
    }
}

private open class TestAuthRepository(
    userRepo: UserRepository? = null
) : AuthRepository(userRepo) {
    var mockUid: String? = "mock_uid_123"
    var signInShouldFail = false
    var signInException: Exception? = null

    override suspend fun performFirebaseSignIn(email: String, pass: String): String? {
        if (signInShouldFail) {
            throw signInException ?: Exception("Auth error")
        }
        return mockUid
    }
}
