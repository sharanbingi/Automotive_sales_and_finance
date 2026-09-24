package com.automotive.salesfinance.repository

import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryInitializationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AuthRepository.resetInstance()
        TenantContext.clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        AuthRepository.resetInstance()
        TenantContext.clear()
    }

    @Test
    fun testAuthRepositoryConstructionCompletesWithoutNpe() {
        val authRepo = AuthRepository()
        assertNotNull("AuthRepository instance should not be null", authRepo)
        assertNotNull("currentUser StateFlow should be initialized", authRepo.currentUser)
        assertNotNull("isDemoMode StateFlow should be initialized", authRepo.isDemoMode)
    }

    @Test
    fun testClearListenersIsNonNullBeforeRepositoryRegistration() {
        var listenerInvoked = false
        val authRepo = AuthRepository()

        // Register listener directly before accessing any secondary properties
        authRepo.registerClearListener {
            listenerInvoked = true
        }

        authRepo.clearAllRepositoryState()
        assertTrue("Clear listener should be successfully registered and executed", listenerInvoked)
    }

    @Test
    fun testUserRepositoryCanBeInitializedDuringAuthRepositoryStartup() {
        val authRepo = AuthRepository()
        val userRepo = authRepo.userRepository
        assertNotNull("UserRepository lazy property should initialize without NPE", userRepo)
        assertNotNull("UserRepository usersFlow should be initialized", userRepo.usersFlow)
    }

    @Test
    fun testAllTenRepositoriesRegisterClearListenersSafelyWithoutNpe() {
        val authRepo = AuthRepository()

        val userRepo = UserRepository(authRepo)
        val dealershipRepo = DealershipRepository(authRepo)
        val storeRepo = StoreRepository(authRepo)
        val inventoryRepo = InventoryRepository(authRepo)
        val customerRepo = CustomerRepository(authRepo)
        val loanRepo = LoanRepository(authRepo)
        val transactionRepo = TransactionRepository(authRepo)
        val subscriptionRepo = SubscriptionRepository(authRepo)
        val auditLogRepo = AuditLogRepository(authRepo)
        val supportTicketRepo = SupportTicketRepository(authRepo)

        assertNotNull(userRepo)
        assertNotNull(dealershipRepo)
        assertNotNull(storeRepo)
        assertNotNull(inventoryRepo)
        assertNotNull(customerRepo)
        assertNotNull(loanRepo)
        assertNotNull(transactionRepo)
        assertNotNull(subscriptionRepo)
        assertNotNull(auditLogRepo)
        assertNotNull(supportTicketRepo)
    }

    @Test
    fun testLogoutClearsAllRegisteredRepositoryStateFlowsWithoutError() = runTest {
        val authRepo = AuthRepository.getInstance()
        authRepo.setDemoMode(false)

        val userRepo = UserRepository(authRepo)
        val dealershipRepo = DealershipRepository(authRepo)
        val storeRepo = StoreRepository(authRepo)
        val inventoryRepo = InventoryRepository(authRepo)
        val customerRepo = CustomerRepository(authRepo)
        val loanRepo = LoanRepository(authRepo)
        val transactionRepo = TransactionRepository(authRepo)
        val subscriptionRepo = SubscriptionRepository(authRepo)
        val auditLogRepo = AuditLogRepository(authRepo)
        val supportTicketRepo = SupportTicketRepository(authRepo)

        authRepo.logout()

        assertTrue("UserRepository usersFlow should be empty", userRepo.usersFlow.value.isEmpty())
        assertTrue("DealershipRepository dealershipsFlow should be empty", dealershipRepo.dealershipsFlow.value.isEmpty())
        assertTrue("StoreRepository storesFlow should be empty", storeRepo.storesFlow.value.isEmpty())
        assertTrue("InventoryRepository vehiclesFlow should be empty", inventoryRepo.vehiclesFlow.value.isEmpty())
        assertTrue("InventoryRepository bikesFlow should be empty", inventoryRepo.bikesFlow.value.isEmpty())
        assertTrue("CustomerRepository customersFlow should be empty", customerRepo.customersFlow.value.isEmpty())
        assertTrue("LoanRepository loansFlow should be empty", loanRepo.loansFlow.value.isEmpty())
        assertTrue("TransactionRepository transactionsFlow should be empty", transactionRepo.transactionsFlow.value.isEmpty())
        assertTrue("SubscriptionRepository subscriptionsFlow should be empty", subscriptionRepo.subscriptionsFlow.value.isEmpty())
        assertTrue("AuditLogRepository auditLogsFlow should be empty", auditLogRepo.auditLogsFlow.value.isEmpty())
        assertTrue("SupportTicketRepository supportTicketsFlow should be empty", supportTicketRepo.supportTicketsFlow.value.isEmpty())
    }

    @Test
    fun testTenantContextClearAndCurrentUserNullPreservedOnLogout() = runTest {
        val authRepo = AuthRepository.getInstance()
        val testUser = User(
            uid = "TEST_USER_123",
            email = "testuser@automotive.com",
            dealershipId = "DEALERSHIP_TEST",
            role = UserRole.DEALERSHIP_ADMIN,
            storeId = "STORE_TEST",
            active = true
        )

        authRepo.setCurrentUser(testUser)

        assertEquals("TEST_USER_123", authRepo.currentUser.value?.uid)
        assertEquals("DEALERSHIP_TEST", TenantContext.dealershipId.value)

        authRepo.logout()

        assertNull("currentUser should be null after logout", authRepo.currentUser.value)
        assertNull("TenantContext currentUser should be null after logout", TenantContext.currentUser.value)
        assertEquals("TenantContext dealershipId should be empty after logout", "", TenantContext.dealershipId.value)
    }
}
