package com.automotive.salesfinance

import com.automotive.salesfinance.data.FeatureAccessManager
import com.automotive.salesfinance.data.SessionManager
import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BillingCycle
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.SupportTicket
import com.automotive.salesfinance.model.TicketPriority
import com.automotive.salesfinance.model.TicketStatus
import com.automotive.salesfinance.model.Transaction
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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MultiTenantArchitectureTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var auditLogRepository: AuditLogRepository
    private lateinit var supportTicketRepository: SupportTicketRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var userRepository: UserRepository

    @Before
    fun setUp() {
        authRepository = AuthRepository()
        dealershipRepository = DealershipRepository(authRepository)
        subscriptionRepository = SubscriptionRepository(authRepository)
        auditLogRepository = AuditLogRepository(authRepository)
        supportTicketRepository = SupportTicketRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        loanRepository = LoanRepository(authRepository)
        transactionRepository = TransactionRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        userRepository = UserRepository(authRepository)
        TenantContext.reset()
    }

    @Test
    fun defaultDealershipId_isSetOnAllModels() {
        val user = User()
        val store = Store()
        val bike = Bike()
        val customer = Customer()
        val loan = Loan()
        val transaction = Transaction()
        val dealership = Dealership()

        assertEquals("dealership_demo_001", user.dealershipId)
        assertEquals("dealership_demo_001", store.dealershipId)
        assertEquals("", bike.dealershipId)
        assertEquals("dealership_demo_001", customer.dealershipId)
        assertEquals("dealership_demo_001", loan.dealershipId)
        assertEquals("dealership_demo_001", transaction.dealershipId)
        assertTrue(dealership.dealershipId.isEmpty() || dealership.dealershipId == "")
    }

    @Test
    fun subscriptionPlans_haveCorrectTierLimits() {
        val starter = SubscriptionPlan.STARTER
        val pro = SubscriptionPlan.PROFESSIONAL
        val business = SubscriptionPlan.BUSINESS
        val enterprise = SubscriptionPlan.ENTERPRISE

        assertEquals(1, starter.maxStores)
        assertEquals(3, starter.maxUsers)
        assertEquals(3, pro.maxStores)
        assertEquals(10, pro.maxUsers)
        assertEquals(10, business.maxStores)
        assertEquals(35, business.maxUsers)
        assertEquals(Int.MAX_VALUE, enterprise.maxStores)
        assertEquals(Int.MAX_VALUE, enterprise.maxUsers)

        assertFalse(starter.advancedReportsEnabled)
        assertTrue(pro.advancedReportsEnabled)
        assertTrue(pro.multiStoreEnabled)
    }

    @Test
    fun featureAccessManager_validatesLimitsAndFlags() {
        val plan = SubscriptionPlan.STARTER

        assertTrue(FeatureAccessManager.canAddStore(0, plan))
        assertFalse(FeatureAccessManager.canAddStore(1, plan))

        assertTrue(FeatureAccessManager.canAddUser(2, plan))
        assertFalse(FeatureAccessManager.canAddUser(3, plan))

        assertTrue(FeatureAccessManager.hasFeature("reports", plan))
        assertFalse(FeatureAccessManager.hasFeature("advanced_reports", plan))
    }

    @Test
    fun tenantContextAndSessionManager_trackState() {
        val sessionManager = SessionManager(authRepository)
        val user = User(uid = "U100", dealershipId = "dealership_abc", role = UserRole.DEALERSHIP_ADMIN)

        TenantContext.setCurrentUser(user)

        assertEquals("dealership_abc", sessionManager.dealershipId.value)
        assertEquals(UserRole.DEALERSHIP_ADMIN, sessionManager.role.value)

        sessionManager.updateActiveStore("STORE_HYD")
        assertEquals("STORE_HYD", sessionManager.activeStoreId.value)
    }

    @Test
    fun repositories_filterDataByDealershipId() = runBlocking {
        val dealership = dealershipRepository.getDealershipById("dealership_demo_001")
        assertNotNull(dealership)
        assertEquals("DEMO AUTOMOTIVE", dealership?.name)

        val bikes = inventoryRepository.getBikesForDealership("dealership_demo_001")
        assertTrue(bikes.isNotEmpty())

        val customers = customerRepository.getCustomersForDealership("dealership_demo_001")
        assertTrue(customers.isNotEmpty())

        val loans = loanRepository.getLoansForDealership("dealership_demo_001")
        assertTrue(loans.isNotEmpty())

        val transactions = transactionRepository.getTransactionsForDealership("dealership_demo_001")
        assertTrue(transactions.isNotEmpty())

        val stores = storeRepository.getStoresForDealership("dealership_demo_001")
        assertTrue(stores.isNotEmpty())

        val users = userRepository.getUsersForDealership("dealership_demo_001")
        assertTrue(users.isNotEmpty())
    }

    @Test
    fun subscriptionRepository_upgradesAndRenewsPlan() = runBlocking {
        val upgraded = subscriptionRepository.upgradePlan("dealership_demo_001", "BUSINESS", BillingCycle.YEARLY)
        assertTrue(upgraded.isSuccess)
        val sub = upgraded.getOrNull()
        assertEquals("BUSINESS", sub?.planId)
        assertEquals(BillingCycle.YEARLY, sub?.billingCycle)

        val renewed = subscriptionRepository.renewSubscription(sub!!.subscriptionId)
        assertTrue(renewed.isSuccess)
        assertEquals(SubscriptionStatus.ACTIVE, renewed.getOrNull()?.status)
    }

    @Test
    fun auditLogRepository_logsAndRetrievesEvents() = runBlocking {
        val result = auditLogRepository.logEvent(
            dealershipId = "dealership_demo_001",
            userId = "USER_001",
            action = "TEST_ACTION",
            entityType = "Dealership",
            entityId = "dealership_demo_001"
        )
        assertTrue(result.isSuccess)

        val logs = auditLogRepository.getAuditLogsForDealership("dealership_demo_001")
        assertTrue(logs.any { it.action == "TEST_ACTION" })
    }

    @Test
    fun supportTicketRepository_createsAndUpdatesTicket() = runBlocking {
        val ticket = SupportTicket(
            ticketId = "TICK_100",
            dealershipId = "dealership_demo_001",
            userId = "USER_001",
            category = "General",
            subject = "Need help with stores",
            description = "Cannot add store",
            priority = TicketPriority.HIGH,
            status = TicketStatus.OPEN
        )

        val createResult = supportTicketRepository.createTicket(ticket)
        assertTrue(createResult.isSuccess)

        val updateResult = supportTicketRepository.updateTicketStatus("TICK_100", TicketStatus.RESOLVED)
        assertTrue(updateResult.isSuccess)

        val tickets = supportTicketRepository.getTicketsForDealership("dealership_demo_001")
        val found = tickets.find { it.ticketId == "TICK_100" }
        assertEquals(TicketStatus.RESOLVED, found?.status)
    }
}
