package com.automotive.salesfinance.viewmodel

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.Store
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DealershipAdminViewModelTest {

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
    private lateinit var viewModel: DealershipAdminViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        authRepository.setDemoMode(true)
        dealershipRepository = DealershipRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        userRepository = UserRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        loanRepository = LoanRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        transactionRepository = TransactionRepository(authRepository)
        auditLogRepository = AuditLogRepository(authRepository)

        viewModel = DealershipAdminViewModel(
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

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun metrics_calculatesScopedMetrics() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.metrics.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val metrics = viewModel.metrics.value
        assertTrue(metrics.totalBikes > 0)
        assertTrue(metrics.totalStoresCount > 0)
        assertTrue(metrics.totalUsersCount > 0)
    }

    @Test
    fun saveStore_addsNewStore() = runTest {
        dealershipRepository.updateSubscriptionPlan(DemoData.DEMO_DEALERSHIP_ID, "ENTERPRISE")
        val newStore = Store(
            storeId = "TG_Warangal",
            storeName = "Warangal Store",
            stateCode = "TG",
            city = "Warangal",
            address = "Main Road, Warangal"
        )

        var saved = false
        viewModel.saveStore(newStore) { saved = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saved)
        val fetched = storeRepository.getStoreById("TG_Warangal")
        assertNotNull(fetched)
        assertEquals("Warangal Store", fetched?.storeName)
    }

    @Test
    fun saveUser_addsNewUserAndToggleActive() = runTest {
        dealershipRepository.updateSubscriptionPlan(DemoData.DEMO_DEALERSHIP_ID, "ENTERPRISE")
        val newUser = User(
            uid = "USER_NEW_001",
            name = "Test Exec",
            email = "exec@test.com",
            role = UserRole.SALES_USER,
            storeId = "TG_Madhapur"
        )

        var saved = false
        viewModel.saveUser(newUser) { saved = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saved)
        val fetched = userRepository.getUser("USER_NEW_001")
        assertNotNull(fetched)
        assertEquals("Test Exec", fetched?.name)

        viewModel.toggleUserActiveStatus("USER_NEW_001")
        testDispatcher.scheduler.advanceUntilIdle()

        val toggled = userRepository.getUser("USER_NEW_001")
        assertEquals(false, toggled?.active)
    }
}
