package com.automotive.salesfinance.model

data class AuditLog(
    val auditLogId: String = "",
    val dealershipId: String = "",
    val userId: String = "",
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)
