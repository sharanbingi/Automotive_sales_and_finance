package com.automotive.salesfinance.viewmodel

import com.automotive.salesfinance.model.TicketPriority
import com.automotive.salesfinance.model.TicketStatus
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.SupportTicketRepository
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
class SupportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var supportTicketRepository: SupportTicketRepository
    private lateinit var auditLogRepository: AuditLogRepository
    private lateinit var viewModel: SupportViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        supportTicketRepository = SupportTicketRepository(authRepository)
        auditLogRepository = AuditLogRepository(authRepository)

        viewModel = SupportViewModel(
            authRepository = authRepository,
            supportTicketRepository = supportTicketRepository,
            auditLogRepository = auditLogRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createTicket_createsNewSupportTicket() = runTest {
        var created = false
        viewModel.createTicket(
            category = "Billing",
            subject = "GST Tax Invoice Query",
            description = "Please issue tax invoice for Q3.",
            priority = TicketPriority.HIGH,
            onSuccess = { created = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(created)
        val list = supportTicketRepository.getAllTickets()
        val found = list.find { it.subject == "GST Tax Invoice Query" }
        assertNotNull(found)
        assertEquals(TicketPriority.HIGH, found?.priority)
        assertEquals(TicketStatus.OPEN, found?.status)
    }

    @Test
    fun updateTicketStatus_updatesStatus() = runTest {
        val ticketId = "TICKET_001"
        viewModel.updateTicketStatus(ticketId, TicketStatus.RESOLVED)
        testDispatcher.scheduler.advanceUntilIdle()

        val found = supportTicketRepository.getAllTickets().find { it.ticketId == ticketId }
        assertNotNull(found)
        assertEquals(TicketStatus.RESOLVED, found?.status)
    }
}
