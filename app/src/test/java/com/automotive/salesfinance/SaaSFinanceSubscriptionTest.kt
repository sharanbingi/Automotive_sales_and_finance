package com.automotive.salesfinance

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.FeatureAccessManager
import com.automotive.salesfinance.model.BillingCycle
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Subscription
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
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
import com.automotive.salesfinance.viewmodel.DealershipAdminViewModel
import com.automotive.salesfinance.viewmodel.SubscriptionViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaaSFinanceSubscriptionTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: AuthRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var userRepository: UserRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var supportTicketRepository: SupportTicketRepository
    private lateinit var auditLogRepository: AuditLogRepository

    private lateinit var subscriptionViewModel: SubscriptionViewModel
    private lateinit var superAdminViewModel: SuperAdminViewModel
    private lateinit var dealershipAdminViewModel: DealershipAdminViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        authRepository = AuthRepository()
        authRepository.setDemoMode(true)
        dealershipRepository = DealershipRepository(authRepository)
        subscriptionRepository = SubscriptionRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        userRepository = UserRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        loanRepository = LoanRepository(authRepository)
        transactionRepository = TransactionRepository(authRepository)
        supportTicketRepository = SupportTicketRepository(authRepository)
        auditLogRepository = AuditLogRepository(authRepository)

        subscriptionViewModel = SubscriptionViewModel(
            authRepository = authRepository,
            dealershipRepository = dealershipRepository,
            subscriptionRepository = subscriptionRepository,
            storeRepository = storeRepository,
            userRepository = userRepository,
            inventoryRepository = inventoryRepository,
            customerRepository = customerRepository,
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. SubscriptionPlan Configurations
    @Test
    fun testSubscriptionPlanConfigurations() {
        // STARTER
        val starter = SubscriptionPlan.STARTER
        assertEquals("STARTER", starter.planId)
        assertEquals(2999.0, starter.monthlyPrice, 0.01)
        assertEquals(29990.0, starter.yearlyPrice, 0.01)
        assertEquals(1, starter.maxStores)
        assertEquals(3, starter.maxUsers)
        assertEquals(100, starter.maxBikes)
        assertEquals(500, starter.maxCustomers)
        assertTrue(starter.reportsEnabled)
        assertFalse(starter.advancedReportsEnabled)
        assertTrue(starter.financeEnabled)
        assertFalse(starter.multiStoreEnabled)

        // PROFESSIONAL
        val pro = SubscriptionPlan.PROFESSIONAL
        assertEquals("PROFESSIONAL", pro.planId)
        assertEquals(6999.0, pro.monthlyPrice, 0.01)
        assertEquals(69990.0, pro.yearlyPrice, 0.01)
        assertEquals(3, pro.maxStores)
        assertEquals(10, pro.maxUsers)
        assertEquals(500, pro.maxBikes)
        assertEquals(2500, pro.maxCustomers)
        assertTrue(pro.advancedReportsEnabled)
        assertTrue(pro.multiStoreEnabled)

        // BUSINESS
        val biz = SubscriptionPlan.BUSINESS
        assertEquals("BUSINESS", biz.planId)
        assertEquals(12999.0, biz.monthlyPrice, 0.01)
        assertEquals(129990.0, biz.yearlyPrice, 0.01)
        assertEquals(10, biz.maxStores)
        assertEquals(35, biz.maxUsers)
        assertEquals(2000, biz.maxBikes)
        assertEquals(10000, biz.maxCustomers)

        // ENTERPRISE
        val ent = SubscriptionPlan.ENTERPRISE
        assertEquals("ENTERPRISE", ent.planId)
        assertEquals(25000.0, ent.monthlyPrice, 0.01)
        assertEquals(250000.0, ent.yearlyPrice, 0.01)
        assertEquals(Int.MAX_VALUE, ent.maxStores)
        assertEquals(Int.MAX_VALUE, ent.maxUsers)

        // All plans lookup
        assertEquals(4, SubscriptionPlan.ALL_PLANS.size)
        assertEquals(SubscriptionPlan.STARTER, SubscriptionPlan.getPlanById("STARTER"))
        assertEquals(SubscriptionPlan.ENTERPRISE, SubscriptionPlan.getPlanById("ENTERPRISE"))
        assertEquals(SubscriptionPlan.STARTER, SubscriptionPlan.getPlanById("UNKNOWN"))
    }

    // 2. Subscription & Dealership Data Models
    @Test
    fun testSubscriptionAndDealershipDataModels() {
        val now = System.currentTimeMillis()
        val sub = Subscription(
            subscriptionId = "SUB_TEST_001",
            dealershipId = "DEALERSHIP_001",
            planId = "PROFESSIONAL",
            planName = "Professional Plan",
            status = SubscriptionStatus.ACTIVE,
            billingCycle = BillingCycle.MONTHLY,
            price = 6999.0,
            currency = "INR",
            startDate = now,
            endDate = now + (30L * 24 * 3600 * 1000),
            provider = "RAZORPAY",
            providerCustomerId = "CUST_RAZORPAY_123",
            providerSubscriptionId = "SUB_RAZORPAY_456"
        )

        assertEquals("SUB_TEST_001", sub.subscriptionId)
        assertEquals(SubscriptionStatus.ACTIVE, sub.status)
        assertEquals("RAZORPAY", sub.provider)

        val dealership = Dealership(
            dealershipId = "DEALERSHIP_001",
            name = "Test Motors",
            subscriptionPlan = "PROFESSIONAL",
            subscriptionStatus = SubscriptionStatus.TRIAL,
            trialStartDate = now,
            trialEndDate = now + (14L * 24 * 3600 * 1000)
        )

        assertEquals(SubscriptionStatus.TRIAL, dealership.subscriptionStatus)
        assertTrue(dealership.trialEndDate > dealership.trialStartDate)
    }

    // 3. Configurable Trial Period & Days Remaining Calculation
    @Test
    fun testTrialPeriodAndDaysRemainingCalculation() = runTest {
        val now = System.currentTimeMillis()
        val trialDealership = Dealership(
            dealershipId = "DEALERSHIP_TRIAL",
            name = "Trial Dealership",
            subscriptionPlan = "STARTER",
            subscriptionStatus = SubscriptionStatus.TRIAL,
            trialStartDate = now,
            trialEndDate = now + (10L * 24 * 3600 * 1000)
        )

        assertTrue(dealershipRepository.isTrialActive(trialDealership))

        val daysLeft = dealershipRepository.getDaysRemainingInTrial(trialDealership)
        assertTrue(daysLeft in 9..10)

        // Test fallback calculation when trialEndDate is 0L
        val uninitializedTrialDealership = Dealership(
            dealershipId = "DEALERSHIP_UNINIT",
            name = "Uninitialized Trial",
            subscriptionPlan = "STARTER",
            subscriptionStatus = SubscriptionStatus.TRIAL,
            trialStartDate = 0L,
            trialEndDate = 0L,
            createdAt = now
        )

        val effectiveEnd = dealershipRepository.getEffectiveTrialEndDate(uninitializedTrialDealership)
        assertTrue(effectiveEnd > now)
        assertTrue(dealershipRepository.isTrialActive(uninitializedTrialDealership))
        val fallbackDays = dealershipRepository.getDaysRemainingInTrial(uninitializedTrialDealership)
        assertTrue(fallbackDays in 13..14)
    }

    // 4. FeatureAccessManager Quota & Read-Only Enforcement
    @Test
    fun testFeatureAccessManagerLimitsAndReadOnlyMode() {
        val starter = SubscriptionPlan.STARTER

        // Limit checks
        assertTrue(FeatureAccessManager.canAddStore(0, starter))
        assertFalse(FeatureAccessManager.canAddStore(1, starter))

        assertTrue(FeatureAccessManager.canAddUser(2, starter))
        assertFalse(FeatureAccessManager.canAddUser(3, starter))

        assertTrue(FeatureAccessManager.canAddBike(99, starter))
        assertFalse(FeatureAccessManager.canAddBike(100, starter))

        assertTrue(FeatureAccessManager.canAddCustomer(499, starter))
        assertFalse(FeatureAccessManager.canAddCustomer(500, starter))

        // Read-only status checks
        assertTrue(FeatureAccessManager.isSubscriptionActive(SubscriptionStatus.ACTIVE))
        assertTrue(FeatureAccessManager.isSubscriptionActive(SubscriptionStatus.TRIAL))
        assertFalse(FeatureAccessManager.isSubscriptionActive(SubscriptionStatus.EXPIRED))
        assertFalse(FeatureAccessManager.isSubscriptionActive(SubscriptionStatus.SUSPENDED))
        assertFalse(FeatureAccessManager.isSubscriptionActive(SubscriptionStatus.PAST_DUE))
        assertFalse(FeatureAccessManager.isSubscriptionActive(SubscriptionStatus.CANCELLED))
    }

    // 5. SubscriptionViewModel Upgrade & Renew
    @Test
    fun testSubscriptionViewModelUpgradeAndRenew() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            subscriptionViewModel.currentDealership.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            subscriptionViewModel.currentSubscription.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Test price calculation
        val monthlyPrice = subscriptionViewModel.calculatePrice(SubscriptionPlan.PROFESSIONAL, BillingCycle.MONTHLY)
        val yearlyPrice = subscriptionViewModel.calculatePrice(SubscriptionPlan.PROFESSIONAL, BillingCycle.YEARLY)
        assertEquals(6999.0, monthlyPrice, 0.01)
        assertEquals(69990.0, yearlyPrice, 0.01)

        // Upgrade to PROFESSIONAL yearly
        var upgradeSuccess = false
        subscriptionViewModel.upgradePlan("PROFESSIONAL", BillingCycle.YEARLY) { upgradeSuccess = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(upgradeSuccess)
        val currentD = dealershipRepository.getDealershipById(DemoData.DEMO_DEALERSHIP_ID)
        assertNotNull(currentD)
        assertEquals("PROFESSIONAL", currentD?.subscriptionPlan)
        assertEquals(SubscriptionStatus.ACTIVE, currentD?.subscriptionStatus)

        val activeSub = subscriptionRepository.getSubscriptionForDealership(DemoData.DEMO_DEALERSHIP_ID)
        assertNotNull(activeSub)
        assertEquals("PROFESSIONAL", activeSub?.planId)
        assertEquals(BillingCycle.YEARLY, activeSub?.billingCycle)

        // Renew Subscription
        var renewSuccess = false
        subscriptionViewModel.renewSubscription(activeSub!!.subscriptionId) { renewSuccess = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(renewSuccess)
    }

    // 6. Admin Subscription Management Operations
    @Test
    fun testSuperAdminAndDealershipAdminManagement() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            superAdminViewModel.platformStats.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val targetDId = "dealership_demo_001"

        // Super Admin extends trial
        val before = dealershipRepository.getDealershipById(targetDId)
        val beforeEnd = before?.trialEndDate ?: 0L

        superAdminViewModel.extendTrial(targetDId, 14)
        testDispatcher.scheduler.advanceUntilIdle()

        val afterTrial = dealershipRepository.getDealershipById(targetDId)
        assertTrue(afterTrial!!.trialEndDate > beforeEnd)
        assertEquals(SubscriptionStatus.TRIAL, afterTrial.subscriptionStatus)

        // Super Admin suspends dealership
        superAdminViewModel.suspendDealership(targetDId)
        testDispatcher.scheduler.advanceUntilIdle()

        val suspendedD = dealershipRepository.getDealershipById(targetDId)
        assertEquals(false, suspendedD?.active)
        assertEquals(SubscriptionStatus.SUSPENDED, suspendedD?.subscriptionStatus)

        // Super Admin activates dealership
        superAdminViewModel.activateDealership(targetDId)
        testDispatcher.scheduler.advanceUntilIdle()

        val activeD = dealershipRepository.getDealershipById(targetDId)
        assertEquals(true, activeD?.active)
        assertEquals(SubscriptionStatus.ACTIVE, activeD?.subscriptionStatus)

        // Super Admin changes plan to ENTERPRISE
        superAdminViewModel.updateDealershipPlan(targetDId, "ENTERPRISE")
        testDispatcher.scheduler.advanceUntilIdle()

        val enterpriseD = dealershipRepository.getDealershipById(targetDId)
        assertEquals("ENTERPRISE", enterpriseD?.subscriptionPlan)
    }
}
