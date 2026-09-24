package com.automotive.salesfinance.model

enum class TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}

data class SupportTicket(
    val ticketId: String = "",
    val dealershipId: String = "",
    val userId: String = "",
    val category: String = "",
    val subject: String = "",
    val description: String = "",
    val priority: TicketPriority = TicketPriority.MEDIUM,
    val createdAt: Long = System.currentTimeMillis(),
    val status: TicketStatus = TicketStatus.OPEN,
    val adminResponse: String = "",
    val respondedAt: Long = 0L
)
