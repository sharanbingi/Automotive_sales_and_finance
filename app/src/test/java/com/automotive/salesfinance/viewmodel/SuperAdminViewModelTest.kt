package com.automotive.salesfinance.viewmodel

import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.TicketStatus
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.SubscriptionRepository
import com.automotive.salesfinance.repository.SupportTicketRepository
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
class SuperAdminViewModelTest {

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
    fun platformStats_calculatesMRRAndCounts() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.platformStats.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val stats = viewModel.platformStats.value
        assertTrue(stats.totalDealerships > 0)
        assertTrue(stats.totalMRR > 0)
        assertTrue(stats.totalStores > 0)
        assertTrue(stats.totalUsers > 0)
    }

    @Test
    fun suspendAndActivateDealership_updatesStatus() = runTest {
        val dId = "dealership_demo_001"

        viewModel.suspendDealership(dId)
        testDispatcher.scheduler.advanceUntilIdle()

        val suspended = dealershipRepository.getDealershipById(dId)
        assertNotNull(suspended)
        assertEquals(false, suspended?.active)
        assertEquals(SubscriptionStatus.SUSPENDED, suspended?.subscriptionStatus)

        viewModel.activateDealership(dId)
        testDispatcher.scheduler.advanceUntilIdle()

        val activated = dealershipRepository.getDealershipById(dId)
        assertNotNull(activated)
        assertEquals(true, activated?.active)
        assertEquals(SubscriptionStatus.ACTIVE, activated?.subscriptionStatus)
    }

    @Test
    fun extendTrial_extendsEndDate() = runTest {
        val dId = "dealership_demo_001"
        val before = dealershipRepository.getDealershipById(dId)
        val beforeTrialEnd = before?.trialEndDate ?: 0L

        viewModel.extendTrial(dId, 14)
        testDispatcher.scheduler.advanceUntilIdle()

        val after = dealershipRepository.getDealershipById(dId)
        assertNotNull(after)
        assertTrue(after!!.trialEndDate > beforeTrialEnd)
        assertEquals(SubscriptionStatus.TRIAL, after.subscriptionStatus)
    }

    @Test
    fun updateTicketStatus_updatesStatusInRepository() = runTest {
        val ticketId = "TICKET_001"
        viewModel.updateTicketStatus(ticketId, TicketStatus.RESOLVED)
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = supportTicketRepository.getAllTickets().find { it.ticketId == ticketId }
        assertNotNull(updated)
        assertEquals(TicketStatus.RESOLVED, updated?.status)
    }

    @Test
    fun updatePlan_modifiesPlanList() = runTest {
        val starterPlan = viewModel.plans.value.first { it.planId == "STARTER" }
        val modifiedPlan = starterPlan.copy(
            monthlyPrice = 3499.0,
            maxStores = 2,
            maxBikes = 200
        )

        viewModel.updatePlan(modifiedPlan)

        val updatedStarter = viewModel.plans.value.first { it.planId == "STARTER" }
        assertEquals(3499.0, updatedStarter.monthlyPrice, 0.01)
        assertEquals(2, updatedStarter.maxStores)
        assertEquals(200, updatedStarter.maxBikes)
    }

    @Test
    fun postAdminResponse_addsResponseToSupportTicket() = runTest {
        val ticketId = "TICKET_001"
        viewModel.postAdminResponse(ticketId, "Invoice has been sent to your email.", TicketStatus.RESOLVED)
        testDispatcher.scheduler.advanceUntilIdle()

        val ticket = supportTicketRepository.getAllTickets().find { it.ticketId == ticketId }
        assertNotNull(ticket)
        assertEquals("Invoice has been sent to your email.", ticket?.adminResponse)
        assertEquals(TicketStatus.RESOLVED, ticket?.status)
        assertTrue(ticket!!.respondedAt > 0)
    }

    @Test
    fun searchAndFilterDealerships_returnsFilteredList() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.filteredDealerships.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setSearchQuery("DEMO")
        testDispatcher.scheduler.advanceUntilIdle()

        val searchResults = viewModel.filteredDealerships.value
        assertTrue(searchResults.isNotEmpty())
        assertTrue(searchResults.all { it.name.contains("DEMO", ignoreCase = true) })

        viewModel.setSearchQuery("NON_EXISTENT_NAME_XYZ")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.filteredDealerships.value.isEmpty())
    }
}
