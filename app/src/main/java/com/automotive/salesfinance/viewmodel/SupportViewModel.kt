package com.automotive.salesfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.SupportTicket
import com.automotive.salesfinance.model.TicketPriority
import com.automotive.salesfinance.model.TicketStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.SupportTicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SupportViewModel(
    private val authRepository: AuthRepository,
    private val supportTicketRepository: SupportTicketRepository,
    private val auditLogRepository: AuditLogRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
    val isDemoMode: StateFlow<Boolean> = authRepository.isDemoMode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _categoryFilter = MutableStateFlow("ALL")
    val categoryFilter: StateFlow<String> = _categoryFilter.asStateFlow()

    private val _priorityFilter = MutableStateFlow<TicketPriority?>(null)
    val priorityFilter: StateFlow<TicketPriority?> = _priorityFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow<TicketStatus?>(null)
    val statusFilter: StateFlow<TicketStatus?> = _statusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()
    val errorMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow(true)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    val filteredTickets: StateFlow<List<SupportTicket>> = combine(
        supportTicketRepository.supportTicketsFlow,
        currentUser,
        _categoryFilter,
        _priorityFilter,
        _statusFilter,
        _searchQuery
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val tickets = flows[0] as List<SupportTicket>
        @Suppress("UNCHECKED_CAST")
        val user = flows[1] as User?
        val category = flows[2] as String
        val priority = flows[3] as TicketPriority?
        val status = flows[4] as TicketStatus?
        val query = flows[5] as String

        val userRole = user?.role ?: UserRole.SALES_USER
        val dId = user?.dealershipId?.ifBlank { DemoData.DEMO_DEALERSHIP_ID } ?: DemoData.DEMO_DEALERSHIP_ID

        tickets.filter { ticket ->
            val matchesScope = if (userRole == UserRole.SUPER_ADMIN) {
                true
            } else {
                ticket.dealershipId == dId
            }

            val matchesCategory = category == "ALL" || ticket.category.equals(category, ignoreCase = true)
            val matchesPriority = priority == null || ticket.priority == priority
            val matchesStatus = status == null || ticket.status == status
            val matchesQuery = query.isBlank() ||
                    ticket.subject.contains(query, ignoreCase = true) ||
                    ticket.description.contains(query, ignoreCase = true) ||
                    ticket.ticketId.contains(query, ignoreCase = true) ||
                    ticket.dealershipId.contains(query, ignoreCase = true)

            matchesScope && matchesCategory && matchesPriority && matchesStatus && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    fun setPriorityFilter(priority: TicketPriority?) {
        _priorityFilter.value = priority
    }

    fun setStatusFilter(status: TicketStatus?) {
        _statusFilter.value = status
    }

    fun createTicket(
        category: String,
        subject: String,
        description: String,
        priority: TicketPriority,
        onSuccess: () -> Unit = {}
    ) {
        if (subject.isBlank() || description.isBlank()) {
            _operationMessage.value = "Subject and Description cannot be empty"
            _isSuccess.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val u = currentUser.value
            val ticket = SupportTicket(
                ticketId = "TICKET_${System.currentTimeMillis()}",
                dealershipId = u?.dealershipId?.ifBlank { DemoData.DEMO_DEALERSHIP_ID } ?: DemoData.DEMO_DEALERSHIP_ID,
                userId = u?.uid ?: "USER_001",
                category = category,
                subject = subject,
                description = description,
                priority = priority,
                createdAt = System.currentTimeMillis(),
                status = TicketStatus.OPEN
            )

            val res = supportTicketRepository.createTicket(ticket)
            _isLoading.value = false
            if (res.isSuccess) {
                _operationMessage.value = "Ticket created successfully!"
                _isSuccess.value = true
                auditLogRepository.logEvent(
                    ticket.dealershipId,
                    ticket.userId,
                    "CREATE_TICKET",
                    "SupportTicket",
                    ticket.ticketId,
                    mapOf("subject" to subject, "priority" to priority.name)
                )
                onSuccess()
            } else {
                _operationMessage.value = res.exceptionOrNull()?.message ?: "Failed to create ticket"
                _isSuccess.value = false
            }
        }
    }

    fun updateTicketStatus(ticketId: String, newStatus: TicketStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = supportTicketRepository.updateTicketStatus(ticketId, newStatus)
            _isLoading.value = false
            if (res.isSuccess) {
                _operationMessage.value = "Ticket status updated to ${newStatus.name}"
                _isSuccess.value = true
                val u = currentUser.value
                val dId = u?.dealershipId ?: DemoData.DEMO_DEALERSHIP_ID
                auditLogRepository.logEvent(
                    dId,
                    u?.uid ?: "ADMIN",
                    "UPDATE_TICKET_STATUS",
                    "SupportTicket",
                    ticketId,
                    mapOf("status" to newStatus.name)
                )
            } else {
                _operationMessage.value = res.exceptionOrNull()?.message ?: "Failed to update ticket status"
                _isSuccess.value = false
            }
        }
    }

    fun postAdminResponse(
        ticketId: String,
        response: String,
        newStatus: TicketStatus? = null,
        onSuccess: () -> Unit = {}
    ) {
        if (response.isBlank()) {
            _operationMessage.value = "Admin response cannot be blank"
            _isSuccess.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val res = supportTicketRepository.addAdminResponse(ticketId, response, newStatus)
            _isLoading.value = false
            if (res.isSuccess) {
                _operationMessage.value = "Response posted successfully!"
                _isSuccess.value = true
                val u = currentUser.value
                val dId = u?.dealershipId ?: DemoData.DEMO_DEALERSHIP_ID
                auditLogRepository.logEvent(
                    dId,
                    u?.uid ?: "ADMIN",
                    "POST_TICKET_RESPONSE",
                    "SupportTicket",
                    ticketId,
                    mapOf("responseLength" to response.length.toString(), "status" to (newStatus?.name ?: "UNCHANGED"))
                )
                onSuccess()
            } else {
                _operationMessage.value = res.exceptionOrNull()?.message ?: "Failed to post response"
                _isSuccess.value = false
            }
        }
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }
}
