package com.automotive.salesfinance.viewmodel

import com.automotive.salesfinance.model.BillingCycle
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.SubscriptionRepository
import com.automotive.salesfinance.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class SubscriptionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var userRepository: UserRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var auditLogRepository: AuditLogRepository
    private lateinit var viewModel: SubscriptionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        dealershipRepository = DealershipRepository(authRepository)
        subscriptionRepository = SubscriptionRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        userRepository = UserRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        auditLogRepository = AuditLogRepository(authRepository)

        viewModel = SubscriptionViewModel(
            authRepository = authRepository,
            dealershipRepository = dealershipRepository,
            subscriptionRepository = subscriptionRepository,
            storeRepository = storeRepository,
            userRepository = userRepository,
            inventoryRepository = inventoryRepository,
            customerRepository = customerRepository,
            auditLogRepository = auditLogRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun calculatePrice_returnsCorrectPriceByBillingCycle() {
        val plan = SubscriptionPlan.BUSINESS
        val monthlyPrice = viewModel.calculatePrice(plan, BillingCycle.MONTHLY)
        val yearlyPrice = viewModel.calculatePrice(plan, BillingCycle.YEARLY)

        assertEquals(12999.0, monthlyPrice, 0.01)
        assertEquals(129990.0, yearlyPrice, 0.01)
    }

    @Test
    fun upgradePlan_updatesDealershipAndSubscription() = runTest {
        var success = false
        viewModel.upgradePlan("BUSINESS", BillingCycle.YEARLY) { success = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        val d = dealershipRepository.getDealershipById("dealership_demo_001")
        assertNotNull(d)
        assertEquals("BUSINESS", d?.subscriptionPlan)
        assertEquals(SubscriptionStatus.ACTIVE, d?.subscriptionStatus)

        val sub = subscriptionRepository.getSubscriptionForDealership("dealership_demo_001")
        assertNotNull(sub)
        assertEquals("BUSINESS", sub?.planId)
        assertEquals(BillingCycle.YEARLY, sub?.billingCycle)
    }
}
