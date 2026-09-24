package com.automotive.salesfinance

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.SubscriptionStatus
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
import com.automotive.salesfinance.viewmodel.DealershipAdminViewModel
import com.automotive.salesfinance.viewmodel.SuperAdminViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class StoreSaveDemoTest {

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
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var supportTicketRepository: SupportTicketRepository

    private lateinit var dealershipAdminViewModel: DealershipAdminViewModel
    private lateinit var superAdminViewModel: SuperAdminViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        TenantContext.reset()

        authRepository = AuthRepository()
        dealershipRepository = DealershipRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        userRepository = UserRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        loanRepository = LoanRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        transactionRepository = TransactionRepository(authRepository)
        auditLogRepository = AuditLogRepository(authRepository)
        subscriptionRepository = SubscriptionRepository(authRepository)
        supportTicketRepository = SupportTicketRepository(authRepository)

        dealershipAdminViewModel = DealershipAdminViewModel(
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

        superAdminViewModel = SuperAdminViewModel(
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
        TenantContext.reset()
        Dispatchers.resetMain()
    }

    @Test
    fun dealershipAdmin_saveStore_successInDemoMode() = runTest {
        dealershipRepository.updateSubscriptionPlan(DemoData.DEMO_DEALERSHIP_ID, "BUSINESS")
        val newStore = Store(
            storeId = "",
            storeName = "TG Nizamabad Flagship",
            stateCode = "TG",
            city = "Nizamabad",
            address = "Station Road, Nizamabad"
        )

        var onSuccessCalled = false
        dealershipAdminViewModel.saveStore(newStore) {
            onSuccessCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(onSuccessCalled)
        assertNull(dealershipAdminViewModel.errorMessage.value)
        assertNotNull(dealershipAdminViewModel.successMessage.value)
        assertTrue(dealershipAdminViewModel.successMessage.value!!.contains("TG Nizamabad Flagship saved successfully"))

        val storesInRepo = storeRepository.getStoresForDealership(DemoData.DEMO_DEALERSHIP_ID)
        val savedStore = storesInRepo.find { it.storeName == "TG Nizamabad Flagship" }
        assertNotNull(savedStore)
        assertEquals("TG", savedStore?.stateCode)
        assertEquals("Nizamabad", savedStore?.city)
    }

    @Test
    fun superAdmin_saveStore_successInDemoMode() = runTest {
        val targetDealershipId = DemoData.DEMO_DEALERSHIP_ID
        dealershipRepository.updateSubscriptionPlan(targetDealershipId, "BUSINESS")

        var onSuccessCalled = false
        superAdminViewModel.saveStore(
            dealershipId = targetDealershipId,
            storeName = "KA Mysuru Hub",
            stateCode = "KA",
            city = "Mysore",
            address = "Palace Road, Mysuru",
            active = true
        ) {
            onSuccessCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(onSuccessCalled)
        assertNull(superAdminViewModel.errorMessage.value)
        assertNotNull(superAdminViewModel.successMessage.value)
        assertTrue(superAdminViewModel.successMessage.value!!.contains("KA Mysuru Hub saved successfully"))

        val storesInRepo = storeRepository.getStoresForDealership(targetDealershipId)
        val savedStore = storesInRepo.find { it.storeName == "KA Mysuru Hub" }
        assertNotNull(savedStore)
        assertEquals("KA", savedStore?.stateCode)
        assertEquals("Mysore", savedStore?.city)
    }

    @Test
    fun quotaEnforcement_whenPlanLimitReached() = runTest {
        val testDealershipId = "dealership_quota_store_test"
        val testDealership = Dealership(
            dealershipId = testDealershipId,
            name = "Single Store Dealership",
            subscriptionPlan = "STARTER", // maxStores = 1
            subscriptionStatus = SubscriptionStatus.ACTIVE
        )
        dealershipRepository.saveDealership(testDealership)

        // Add 1 initial store to reach STARTER limit (1 store)
        val store1 = Store(
            storeId = "STORE_Q1",
            dealershipId = testDealershipId,
            storeName = "Store One",
            stateCode = "TG",
            city = "Hyd",
            address = "Hyd Address"
        )
        storeRepository.saveStore(store1)

        dealershipAdminViewModel.setSelectedDealershipId(testDealershipId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Attempt to add a 2nd store via DealershipAdminViewModel
        val store2 = Store(
            storeId = "",
            storeName = "Store Two Overflow",
            stateCode = "TG",
            city = "Hyd",
            address = "Hyd Address 2"
        )

        var adminCallbackCalled = false
        dealershipAdminViewModel.saveStore(store2) {
            adminCallbackCalled = false
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!adminCallbackCalled)
        val adminError = dealershipAdminViewModel.errorMessage.value
        assertNotNull(adminError)
        assertTrue(adminError!!.contains("Store limit reached for current plan (1 max stores). Please upgrade."))

        // Attempt to add a 2nd store via SuperAdminViewModel
        var superAdminCallbackCalled = false
        superAdminViewModel.saveStore(
            dealershipId = testDealershipId,
            storeName = "Store Two Overflow Super",
            stateCode = "TG",
            city = "Hyd",
            address = "Hyd Address 3"
        ) {
            superAdminCallbackCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!superAdminCallbackCalled)
        val superAdminError = superAdminViewModel.errorMessage.value
        assertNotNull(superAdminError)
        assertTrue(superAdminError!!.contains("Store limit reached for current plan (1 max stores). Please upgrade."))
    }

    @Test
    fun inputFieldValidation_errorsOnBlankInputs() = runTest {
        // DealershipAdminViewModel validation on blank inputs
        val blankStore = Store(
            storeId = "",
            storeName = " ",
            stateCode = "TG",
            city = "",
            address = ""
        )

        var adminSuccess = false
        dealershipAdminViewModel.saveStore(blankStore) {
            adminSuccess = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!adminSuccess)
        assertEquals("Please fill in all store details", dealershipAdminViewModel.errorMessage.value)

        // SuperAdminViewModel validation on blank inputs
        var superAdminSuccess = false
        superAdminViewModel.saveStore(
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            storeName = "Valid Name",
            stateCode = "",
            city = "City",
            address = ""
        ) {
            superAdminSuccess = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!superAdminSuccess)
        assertEquals("Please fill in all store details", superAdminViewModel.errorMessage.value)
    }
}
