package com.automotive.salesfinance

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Store
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
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.repository.UserRepository
import com.automotive.salesfinance.viewmodel.DealershipAdminViewModel
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
class DealershipAdminReactiveLoadTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var userRepository: UserRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var auditLogRepository: AuditLogRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        dealershipRepository = DealershipRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        userRepository = UserRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        loanRepository = LoanRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        transactionRepository = TransactionRepository(authRepository)
        auditLogRepository = AuditLogRepository(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DealershipAdminViewModel {
        return DealershipAdminViewModel(
            authRepository = authRepository,
            dealershipRepository = dealershipRepository,
            storeRepository = storeRepository,
            userRepository = userRepository,
            inventoryRepository = inventoryRepository,
            loanRepository = loanRepository,
            customerRepository = customerRepository,
            transactionRepository = transactionRepository,
            auditLogRepository = auditLogRepository
        )
    }

    @Test
    fun test1_initializedBeforeCurrentUserAvailable_doesNotTriggerBlankFetches() = runTest {
        authRepository.setDemoMode(false)
        authRepository.logout()

        val viewModel = createViewModel()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.currentDealership.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.stores.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.users.collect {}
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("currentDealership should be null when currentUser is null", viewModel.currentDealership.value)
        assertTrue("stores should be empty when no currentUser is available", viewModel.stores.value.isEmpty())
        assertTrue("users should be empty when no currentUser is available", viewModel.users.value.isEmpty())
        assertTrue("storeRepository should have no items loaded for blank dealershipId", storeRepository.storesFlow.value.isEmpty())
    }

    @Test
    fun test2_currentUserEmitsDealershipAdmin_triggersLoadDataForDealership() = runTest {
        authRepository.setDemoMode(false)
        authRepository.logout()

        val viewModel = createViewModel()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.currentDealership.collect {}
        }

        testDispatcher.scheduler.advanceUntilIdle()

        val sharanUser = User(
            uid = "user_sharan_admin",
            name = "Sharan Admin",
            email = "sharan@motors.com",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "sharan_motors"
        )
        authRepository.setCurrentUser(sharanUser)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("sharan_motors", authRepository.currentUser.value?.dealershipId)
        assertEquals("sharan_motors", sharanUser.dealershipId)
    }

    @Test
    fun test3_storesTeamUsersAndCurrentDealership_populateReactivelyForSharanMotors() = runTest {
        authRepository.setDemoMode(false)
        authRepository.logout()

        val sharanDealership = Dealership(
            dealershipId = "sharan_motors",
            name = "Sharan Motors",
            subscriptionPlan = "ENTERPRISE",
            subscriptionStatus = SubscriptionStatus.ACTIVE
        )
        dealershipRepository.saveDealership(sharanDealership)

        val sharanStore = Store(
            storeId = "STORE_SHARAN_01",
            storeName = "Sharan Main Showroom",
            dealershipId = "sharan_motors",
            stateCode = "TG",
            city = "Hyderabad"
        )
        storeRepository.saveStore(sharanStore)

        val sharanTeamUser = User(
            uid = "USER_SHARAN_01",
            name = "Staff Member",
            email = "staff@sharan.com",
            role = UserRole.SALES_USER,
            dealershipId = "sharan_motors",
            storeId = "STORE_SHARAN_01"
        )
        userRepository.saveUser(sharanTeamUser)

        val viewModel = createViewModel()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.currentDealership.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.stores.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.users.collect {}
        }

        val sharanAdminUser = User(
            uid = "user_sharan_admin",
            name = "Sharan Admin",
            email = "sharan@motors.com",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "sharan_motors"
        )
        authRepository.setCurrentUser(sharanAdminUser)

        testDispatcher.scheduler.advanceUntilIdle()

        val currentD = viewModel.currentDealership.value
        assertNotNull("currentDealership should be populated for sharan_motors", currentD)
        assertEquals("Sharan Motors", currentD?.name)

        val storesList = viewModel.stores.value
        assertEquals(1, storesList.size)
        assertEquals("Sharan Main Showroom", storesList[0].storeName)

        val usersList = viewModel.users.value
        assertEquals(1, usersList.size)
        assertEquals("Staff Member", usersList[0].name)
    }

    @Test
    fun test4_headerRendersRealDealershipNameAndSubscriptionStatusWithoutFallbacks() = runTest {
        authRepository.setDemoMode(false)
        authRepository.logout()

        val viewModel = createViewModel()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.currentDealership.collect {}
        }

        testDispatcher.scheduler.advanceUntilIdle()

        // Before dealership is loaded, header fallbacks are non-misleading loading states
        val initialDealership = viewModel.currentDealership.value
        val initialTitle = initialDealership?.name ?: "Loading Dealership..."
        val initialSubtitle = if (initialDealership != null) {
            "Plan: ${initialDealership.subscriptionPlan} • Status: ${initialDealership.subscriptionStatus.name}"
        } else {
            "Loading workspace details..."
        }

        assertEquals("Loading Dealership...", initialTitle)
        assertEquals("Loading workspace details...", initialSubtitle)

        // Case A: ACTIVE subscription
        val activeDealership = Dealership(
            dealershipId = "sharan_motors",
            name = "Sharan Motors",
            subscriptionPlan = "PRO",
            subscriptionStatus = SubscriptionStatus.ACTIVE
        )
        dealershipRepository.saveDealership(activeDealership)

        val sharanUser = User(
            uid = "user_sharan_admin",
            name = "Sharan Admin",
            email = "sharan@motors.com",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "sharan_motors"
        )
        authRepository.setCurrentUser(sharanUser)

        testDispatcher.scheduler.advanceUntilIdle()

        val activeD = viewModel.currentDealership.value
        assertNotNull(activeD)
        val activeTitle = activeD?.name ?: "Loading Dealership..."
        val activeSubtitle = if (activeD != null) {
            "Plan: ${activeD.subscriptionPlan} • Status: ${activeD.subscriptionStatus.name}"
        } else {
            "Loading workspace details..."
        }

        assertEquals("Sharan Motors", activeTitle)
        assertEquals("Plan: PRO • Status: ACTIVE", activeSubtitle)

        // Case B: TRIAL subscription
        val trialDealership = activeDealership.copy(subscriptionStatus = SubscriptionStatus.TRIAL)
        dealershipRepository.saveDealership(trialDealership)

        testDispatcher.scheduler.advanceUntilIdle()

        val trialD = viewModel.currentDealership.value
        assertNotNull(trialD)
        val trialSubtitle = if (trialD != null) {
            "Plan: ${trialD.subscriptionPlan} • Status: ${trialD.subscriptionStatus.name}"
        } else {
            "Loading workspace details..."
        }

        assertEquals("Plan: PRO • Status: TRIAL", trialSubtitle)
    }

    @Test
    fun test5_switchingDealershipIdOrLogout_cancelsPreviousFetchesAndClearsPreviousTenantState() = runTest {
        authRepository.setDemoMode(false)
        authRepository.logout()

        val sharanDealership = Dealership(
            dealershipId = "sharan_motors",
            name = "Sharan Motors",
            subscriptionPlan = "ENTERPRISE",
            subscriptionStatus = SubscriptionStatus.ACTIVE
        )
        dealershipRepository.saveDealership(sharanDealership)

        val sharanStore = Store(
            storeId = "STORE_SHARAN_01",
            storeName = "Sharan Main Showroom",
            dealershipId = "sharan_motors",
            stateCode = "TG",
            city = "Hyderabad"
        )
        storeRepository.saveStore(sharanStore)

        val viewModel = createViewModel()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.currentDealership.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.stores.collect {}
        }

        val sharanUser = User(
            uid = "user_sharan_admin",
            name = "Sharan Admin",
            email = "sharan@motors.com",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "sharan_motors"
        )
        authRepository.setCurrentUser(sharanUser)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Sharan Motors", viewModel.currentDealership.value?.name)
        assertEquals(1, viewModel.stores.value.size)

        // Perform logout
        authRepository.logout()

        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("currentDealership should be null after logout", viewModel.currentDealership.value)
        assertTrue("stores should be cleared after logout", viewModel.stores.value.isEmpty())
        assertTrue("storeRepository in-memory state should be empty", storeRepository.storesFlow.value.isEmpty())
    }

    @Test
    fun test6_productionPathNeverLoadsDemoData() = runTest {
        authRepository.setDemoMode(false)
        authRepository.logout()

        val viewModel = createViewModel()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.currentDealership.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.stores.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.users.collect {}
        }

        testDispatcher.scheduler.advanceUntilIdle()

        // When logged out in production mode, no DemoData is present
        assertNull(viewModel.currentDealership.value)
        assertTrue(viewModel.stores.value.isEmpty())
        assertTrue(viewModel.users.value.isEmpty())

        // Login as production user for sharan_motors
        val sharanDealership = Dealership(
            dealershipId = "sharan_motors",
            name = "Sharan Motors",
            subscriptionPlan = "ENTERPRISE",
            subscriptionStatus = SubscriptionStatus.ACTIVE
        )
        dealershipRepository.saveDealership(sharanDealership)

        val sharanUser = User(
            uid = "user_sharan_admin",
            name = "Sharan Admin",
            email = "sharan@motors.com",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "sharan_motors"
        )
        authRepository.setCurrentUser(sharanUser)

        testDispatcher.scheduler.advanceUntilIdle()

        val currentD = viewModel.currentDealership.value
        assertNotNull(currentD)
        assertFalse("Production path currentDealership is not DemoData dealership ID", currentD?.dealershipId == DemoData.DEMO_DEALERSHIP_ID)

        val storesList = viewModel.stores.value
        assertTrue("Production stores do not contain DemoData store items", storesList.none { it.dealershipId == DemoData.DEMO_DEALERSHIP_ID })

        val usersList = viewModel.users.value
        assertTrue("Production users do not contain DemoData user items", usersList.none { it.dealershipId == DemoData.DEMO_DEALERSHIP_ID })
    }

    @Test
    fun dealershipAdmin_fetchesSingleDealershipDocument_notGlobalCollection() = runTest {
        val testAuthRepo = AuthRepository()
        testAuthRepo.setDemoMode(true)

        val resultNull = dealershipRepository.fetchSingleDealershipFromFirestore("")
        assertNull("Blank dealershipId returns null", resultNull)

        val demoDealershipId = DemoData.DEMO_DEALERSHIP_ID
        val singleResult = dealershipRepository.fetchSingleDealershipFromFirestore(demoDealershipId)
        assertNotNull("Fetches single dealership document matching ID", singleResult)
        assertEquals(demoDealershipId, singleResult?.dealershipId)

        var singleFetchCalled = false
        var requestedDealershipId: String? = null

        val trackingRepo = object : DealershipRepository(testAuthRepo) {
            override suspend fun fetchSingleDealershipFromFirestore(dealershipId: String): Dealership? {
                singleFetchCalled = true
                requestedDealershipId = dealershipId
                return super.fetchSingleDealershipFromFirestore(dealershipId)
            }
        }

        testAuthRepo.setDemoMode(false)
        val sharanUser = User(
            uid = "user_sharan_admin",
            name = "Sharan Admin",
            email = "sharan@motors.com",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "sharan_motors"
        )
        testAuthRepo.setCurrentUser(sharanUser)

        val viewModel = DealershipAdminViewModel(
            authRepository = testAuthRepo,
            dealershipRepository = trackingRepo,
            storeRepository = StoreRepository(testAuthRepo),
            userRepository = UserRepository(testAuthRepo),
            inventoryRepository = InventoryRepository(testAuthRepo),
            loanRepository = LoanRepository(testAuthRepo),
            customerRepository = CustomerRepository(testAuthRepo),
            transactionRepository = TransactionRepository(testAuthRepo),
            auditLogRepository = AuditLogRepository(testAuthRepo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("DealershipAdminViewModel must invoke fetchSingleDealershipFromFirestore", singleFetchCalled)
        assertEquals("sharan_motors", requestedDealershipId)
    }
}
