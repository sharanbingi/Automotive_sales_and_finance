package com.automotive.salesfinance.utils

import com.automotive.salesfinance.model.Expense
import com.automotive.salesfinance.model.ExpenseApprovalStatus
import com.automotive.salesfinance.model.ExpensePaymentStatus

object ExpenseValidationUtils {

    /**
     * Validates pure domain rules for newly created or submitted expenses.
     */
    fun validateExpenseCreation(expense: Expense): String? {
        if (expense.expenseId.isBlank()) {
            return "Expense ID is required"
        }
        if (expense.dealershipId.isBlank()) {
            return "Dealership ID is required"
        }
        if (expense.storeId.isBlank()) {
            return "Store ID is required"
        }
        if (expense.createdByUserId.isBlank()) {
            return "Created By User ID is required"
        }
        if (expense.description.trim().isBlank()) {
            return "Expense description cannot be empty"
        }
        if (expense.amountPaise <= 0L) {
            return "Expense amount must be greater than zero"
        }
        if (expense.expenseDate <= 0L) {
            return "Expense date is required"
        }
        if (expense.expenseDate > System.currentTimeMillis() + 86400000L) {
            return "Expense date cannot be in the future"
        }
        if (expense.createdAt <= 0L) {
            return "Invalid creation timestamp"
        }
        if (expense.updatedAt < expense.createdAt) {
            return "Modification timestamp cannot precede creation timestamp"
        }
        if (expense.approvalStatus !in listOf(ExpenseApprovalStatus.DRAFT, ExpenseApprovalStatus.SUBMITTED)) {
            return "Invalid initial approval status: ${expense.approvalStatus} (must be DRAFT or SUBMITTED)"
        }
        if (expense.paymentStatus != ExpensePaymentStatus.UNPAID) {
            return "Invalid initial payment status: ${expense.paymentStatus} (must be UNPAID on creation)"
        }
        return null
    }

    /**
     * Validates approval state machine transitions and self-approval restrictions.
     */
    fun validateApprovalTransition(
        currentStatus: ExpenseApprovalStatus,
        newStatus: ExpenseApprovalStatus,
        createdByUserId: String = "",
        submittedByUserId: String = "",
        actorUserId: String = "",
        rejectionReason: String? = null
    ): String? {
        if (newStatus == ExpenseApprovalStatus.APPROVED) {
            if (createdByUserId.isBlank()) {
                return "Created By User ID is required for approval validation"
            }
            if (submittedByUserId.isBlank()) {
                return "Submitted By User ID is required for approval validation"
            }
            if (actorUserId.isNotBlank()) {
                if (actorUserId == createdByUserId) {
                    return "Unauthorized self-approval: Expense creator cannot approve their own expense"
                }
                if (actorUserId == submittedByUserId) {
                    return "Unauthorized self-approval: Submitter cannot approve their own expense"
                }
            }
        }

        val isValidTransition = when (currentStatus) {
            ExpenseApprovalStatus.DRAFT -> newStatus in listOf(ExpenseApprovalStatus.SUBMITTED, ExpenseApprovalStatus.CANCELLED)
            ExpenseApprovalStatus.SUBMITTED -> newStatus in listOf(ExpenseApprovalStatus.APPROVED, ExpenseApprovalStatus.REJECTED, ExpenseApprovalStatus.CANCELLED)
            ExpenseApprovalStatus.REJECTED -> newStatus == ExpenseApprovalStatus.DRAFT
            ExpenseApprovalStatus.APPROVED, ExpenseApprovalStatus.CANCELLED -> false
        }

        if (!isValidTransition) {
            return "Invalid approval status transition from $currentStatus to $newStatus"
        }

        if (newStatus == ExpenseApprovalStatus.REJECTED && rejectionReason.orEmpty().trim().isBlank()) {
            return "Rejection reason is required when rejecting an expense"
        }

        return null
    }

    /**
     * Validates payment state machine transitions against an explicit allowlist.
     * Allowlist:
     * - UNPAID -> PAID (Only when approvalStatus == APPROVED)
     * - PAID -> REVERSED (Only when reversalReason is non-blank)
     * Every other transition is rejected.
     */
    fun validatePaymentTransition(
        currentPaymentStatus: ExpensePaymentStatus,
        approvalStatus: ExpenseApprovalStatus,
        newPaymentStatus: ExpensePaymentStatus,
        reversalReason: String? = null
    ): String? {
        if (currentPaymentStatus == newPaymentStatus) {
            return "Invalid payment status transition: Expense is already in $newPaymentStatus payment status"
        }

        val isValidTransition = when (currentPaymentStatus) {
            ExpensePaymentStatus.UNPAID -> newPaymentStatus == ExpensePaymentStatus.PAID
            ExpensePaymentStatus.PAID -> newPaymentStatus == ExpensePaymentStatus.REVERSED
            ExpensePaymentStatus.REVERSED -> false
        }

        if (!isValidTransition) {
            return "Invalid payment status transition from $currentPaymentStatus to $newPaymentStatus"
        }

        if (newPaymentStatus == ExpensePaymentStatus.PAID && approvalStatus != ExpenseApprovalStatus.APPROVED) {
            return "Payment prohibited: Expense must be in APPROVED status before recording payment"
        }

        if (newPaymentStatus == ExpensePaymentStatus.REVERSED && reversalReason.orEmpty().trim().isBlank()) {
            return "Reversal reason is required when reversing an expense payment"
        }

        return null
    }

    /**
     * Calculates store cashbook closing balance in 64-bit integer paise.
     * Note: Negative closing cashbook balances are permitted for diagnostic reporting and ledger reconciliation,
     * but actual cash disbursement authorization must be prohibited by the caller when closing cash would be negative.
     */
    fun calculateStoreCashbookBalancePaise(
        openingCashPaise: Long,
        verifiedCashCollectionsPaise: Long,
        verifiedPaidCashExpensesPaise: Long,
        cashAdjustmentsPaise: Long = 0L
    ): Long {
        if (openingCashPaise < 0L) {
            throw IllegalArgumentException("Opening cash balance cannot be negative")
        }
        if (verifiedCashCollectionsPaise < 0L) {
            throw IllegalArgumentException("Verified cash collections cannot be negative")
        }
        if (verifiedPaidCashExpensesPaise < 0L) {
            throw IllegalArgumentException("Verified paid cash expenses cannot be negative")
        }

        return try {
            val step1 = Math.addExact(openingCashPaise, verifiedCashCollectionsPaise)
            val step2 = Math.subtractExact(step1, verifiedPaidCashExpensesPaise)
            Math.addExact(step2, cashAdjustmentsPaise)
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException("Cashbook balance calculation caused 64-bit integer overflow", e)
        }
    }
}
