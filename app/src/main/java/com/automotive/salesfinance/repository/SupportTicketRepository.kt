package com.automotive.salesfinance.repository

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.SupportTicket
import com.automotive.salesfinance.model.TicketStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SupportTicketRepository(private val authRepository: AuthRepository) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _supportTicketsFlow = MutableStateFlow<List<SupportTicket>>(
        if (authRepository.isDemoMode.value) DemoData.supportTickets else emptyList()
    )
    val supportTicketsFlow: StateFlow<List<SupportTicket>> = _supportTicketsFlow.asStateFlow()

    fun clearInMemoryState() {
        _supportTicketsFlow.value = emptyList()
    }

    init {
        authRepository.registerClearListener {
            clearInMemoryState()
        }
        CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch {
            authRepository.isDemoMode.collect { isDemo ->
                if (!isDemo) {
                    _supportTicketsFlow.value = _supportTicketsFlow.value.filterNot { st ->
                        DemoData.supportTickets.any { it.ticketId == st.ticketId }
                    }
                } else if (_supportTicketsFlow.value.isEmpty()) {
                    _supportTicketsFlow.value = DemoData.supportTickets
                }
            }
        }
    }

    fun getTicketsForDealership(dealershipId: String): List<SupportTicket> {
        if (dealershipId.isBlank()) {
            return if (authRepository.isDemoMode.value) {
                _supportTicketsFlow.value.filter { it.dealershipId == DemoData.DEMO_DEALERSHIP_ID }
            } else {
                emptyList()
            }
        }
        return _supportTicketsFlow.value.filter { it.dealershipId == dealershipId }
    }

    fun getAllTickets(): List<SupportTicket> = _supportTicketsFlow.value

    suspend fun fetchSupportTicketsFromFirestore(): List<SupportTicket> {
        if (authRepository.isDemoMode.value) {
            return _supportTicketsFlow.value
        }
        if (firestore == null) {
            _supportTicketsFlow.value = _supportTicketsFlow.value.filterNot { st -> DemoData.supportTickets.any { it.ticketId == st.ticketId } }
            return _supportTicketsFlow.value
        }
        return try {
            val snapshot = firestore?.collection("support_tickets")?.limit(100)?.get()?.await()
            val fetched = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(SupportTicket::class.java)
                } catch (_: Exception) {
                    null
                }
            } ?: emptyList()

            _supportTicketsFlow.value = fetched
            fetched
        } catch (e: Exception) {
            val liveOnly = _supportTicketsFlow.value.filterNot { st -> DemoData.supportTickets.any { it.ticketId == st.ticketId } }
            _supportTicketsFlow.value = liveOnly
            liveOnly
        }
    }

    suspend fun createTicket(ticket: SupportTicket): Result<Unit> {
        if (ticket.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }
        val preparedTicket = if (ticket.dealershipId.isBlank()) ticket.copy(dealershipId = DemoData.DEMO_DEALERSHIP_ID) else ticket
        val currentList = _supportTicketsFlow.value.toMutableList()
        currentList.add(0, preparedTicket)
        _supportTicketsFlow.value = currentList

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                firestore?.collection("support_tickets")
                    ?.document(preparedTicket.ticketId)
                    ?.set(preparedTicket)
                    ?.await()
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }

    suspend fun updateTicketStatus(ticketId: String, newStatus: TicketStatus): Result<Unit> {
        val currentList = _supportTicketsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.ticketId == ticketId }
        if (index >= 0) {
            val updated = currentList[index].copy(status = newStatus)
            if (updated.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
                return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
            }
            currentList[index] = updated
            _supportTicketsFlow.value = currentList

            if (!authRepository.isDemoMode.value && firestore != null) {
                try {
                    firestore?.collection("support_tickets")
                        ?.document(ticketId)
                        ?.set(updated)
                        ?.await()
                } catch (_: Exception) {}
            }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Ticket not found: $ticketId"))
    }

    suspend fun addAdminResponse(
        ticketId: String,
        response: String,
        newStatus: TicketStatus? = null
    ): Result<Unit> {
        val currentList = _supportTicketsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.ticketId == ticketId }
        if (index >= 0) {
            val old = currentList[index]
            val updated = old.copy(
                adminResponse = response,
                respondedAt = System.currentTimeMillis(),
                status = newStatus ?: old.status
            )
            if (updated.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
                return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
            }
            currentList[index] = updated
            _supportTicketsFlow.value = currentList

            if (!authRepository.isDemoMode.value && firestore != null) {
                try {
                    firestore?.collection("support_tickets")
                        ?.document(ticketId)
                        ?.set(updated)
                        ?.await()
                } catch (_: Exception) {}
            }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Ticket not found: $ticketId"))
    }
}
