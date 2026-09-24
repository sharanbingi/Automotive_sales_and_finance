package com.automotive.salesfinance.model

enum class ExpenseCategory(val displayName: String) {
    STORE_RENT("Store Rent"),
    ELECTRICITY_UTILITIES("Electricity & Utilities"),
    EMPLOYEE_SALARIES("Employee Salaries"),
    TRANSPORTATION("Transportation & Logistics"),
    VEHICLE_MAINTENANCE("Vehicle Maintenance"),
    MARKETING("Marketing & Advertising"),
    OFFICE_EXPENSES("Office Supplies & Expenses"),
    MISCELLANEOUS("Miscellaneous")
}

enum class ExpenseApprovalStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    CANCELLED
}

enum class ExpensePaymentStatus {
    UNPAID,
    PAID,
    REVERSED
}

enum class ExpensePaymentMethod(val displayName: String) {
    STORE_CASHBOOK("Store Cashbook (Physical Cash)"),
    BANK_TRANSFER("Bank Transfer / NEFT / RTGS"),
    CHEQUE("Cheque"),
    CREDIT_CARD("Corporate Credit Card")
}

data class ExpenseAuditEntry(
    val eventId: String = "",
    val expenseId: String = "",
    val action: String = "",
    val previousStatus: String = "",
    val newStatus: String = "",
    val actorUserId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null
)

/**
 * Authoritative Store Expense domain model.
 * All financial amounts strictly use 64-bit integer paise (amountPaise: Long).
 */
data class Expense(
    val expenseId: String = "",
    val dealershipId: String = "",
    val storeId: String = "",
    val category: ExpenseCategory = ExpenseCategory.MISCELLANEOUS,
    val description: String = "",
    val amountPaise: Long = 0L,
    val expenseDate: Long = System.currentTimeMillis(),
    val paymentMethod: ExpensePaymentMethod = ExpensePaymentMethod.STORE_CASHBOOK,
    val approvalStatus: ExpenseApprovalStatus = ExpenseApprovalStatus.DRAFT,
    val paymentStatus: ExpensePaymentStatus = ExpensePaymentStatus.UNPAID,
    val createdByUserId: String = "",
    val submittedByUserId: String = "",
    val approvedByUserId: String? = null,
    val paidByUserId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val paidTimestamp: Long? = null,
    val rejectionReason: String? = null,
    val reversalReason: String? = null,
    val receiptAttachmentUrl: String? = null,
    val idempotencyKey: String = ""
)
