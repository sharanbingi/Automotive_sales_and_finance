package com.automotive.salesfinance

import com.automotive.salesfinance.model.Expense
import com.automotive.salesfinance.model.ExpenseApprovalStatus
import com.automotive.salesfinance.model.ExpenseCategory
import com.automotive.salesfinance.model.ExpensePaymentStatus
import com.automotive.salesfinance.utils.ExpenseValidationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseModelAndValidationTest {

    @Test
    fun testValidExpenseCreation() {
        val expense = Expense(
            expenseId = "EXP_001",
            dealershipId = "dealership_001",
            storeId = "TG_Madhapur",
            category = ExpenseCategory.STORE_RENT,
            description = "Monthly showroom lease rent",
            amountPaise = 5000000L, // ₹50,000.00
            createdByUserId = "usr_sales_001",
            submittedByUserId = "usr_sales_001",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val error = ExpenseValidationUtils.validateExpenseCreation(expense)
        assertNull(error)
    }

    @Test
    fun testMissingRequiredIdsAndDescription() {
        val base = Expense(
            expenseId = "EXP_001",
            dealershipId = "dealership_001",
            storeId = "TG_Madhapur",
            description = "Rent",
            amountPaise = 1000L,
            createdByUserId = "usr_001",
            submittedByUserId = "usr_001"
        )

        assertEquals("Dealership ID is required", ExpenseValidationUtils.validateExpenseCreation(base.copy(dealershipId = "")))
        assertEquals("Store ID is required", ExpenseValidationUtils.validateExpenseCreation(base.copy(storeId = "")))
        assertEquals("Created By User ID is required", ExpenseValidationUtils.validateExpenseCreation(base.copy(createdByUserId = "")))
        assertEquals("Expense description cannot be empty", ExpenseValidationUtils.validateExpenseCreation(base.copy(description = "   ")))
    }

    @Test
    fun testZeroAndNegativeMonetaryAmounts() {
        val base = Expense(
            expenseId = "EXP_001",
            dealershipId = "d_01",
            storeId = "s_01",
            description = "Rent",
            createdByUserId = "usr_001"
        )
        assertEquals("Expense amount must be greater than zero", ExpenseValidationUtils.validateExpenseCreation(base.copy(amountPaise = 0L)))
        assertEquals("Expense amount must be greater than zero", ExpenseValidationUtils.validateExpenseCreation(base.copy(amountPaise = -5000L)))
    }

    @Test
    fun testInvalidInitialStatusAndTimestamps() {
        val base = Expense(
            expenseId = "EXP_001",
            dealershipId = "d_01",
            storeId = "s_01",
            description = "Rent",
            amountPaise = 1000L,
            createdByUserId = "usr_001"
        )

        val invalidInitialApproval = ExpenseValidationUtils.validateExpenseCreation(base.copy(approvalStatus = ExpenseApprovalStatus.APPROVED))
        assertNotNull(invalidInitialApproval)
        assert(invalidInitialApproval!!.contains("Invalid initial approval status"))

        val invalidInitialPayment = ExpenseValidationUtils.validateExpenseCreation(base.copy(paymentStatus = ExpensePaymentStatus.PAID))
        assertNotNull(invalidInitialPayment)
        assert(invalidInitialPayment!!.contains("Invalid initial payment status"))

        val zeroTimestamp = ExpenseValidationUtils.validateExpenseCreation(base.copy(createdAt = 0L))
        assertEquals("Invalid creation timestamp", zeroTimestamp)

        val pastUpdate = ExpenseValidationUtils.validateExpenseCreation(base.copy(createdAt = 1000L, updatedAt = 500L))
        assertEquals("Modification timestamp cannot precede creation timestamp", pastUpdate)
    }

    @Test
    fun testApprovalStateMachineTransitions() {
        // DRAFT -> SUBMITTED (valid)
        assertNull(ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = ExpenseApprovalStatus.DRAFT,
            newStatus = ExpenseApprovalStatus.SUBMITTED,
            createdByUserId = "usr_creator",
            submittedByUserId = "usr_submitter",
            actorUserId = "usr_submitter"
        ))

        // SUBMITTED -> APPROVED by manager (valid)
        assertNull(ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = ExpenseApprovalStatus.SUBMITTED,
            newStatus = ExpenseApprovalStatus.APPROVED,
            createdByUserId = "usr_creator",
            submittedByUserId = "usr_submitter",
            actorUserId = "usr_approver_manager"
        ))

        // SUBMITTED -> REJECTED with reason (valid)
        assertNull(ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = ExpenseApprovalStatus.SUBMITTED,
            newStatus = ExpenseApprovalStatus.REJECTED,
            createdByUserId = "usr_creator",
            submittedByUserId = "usr_submitter",
            actorUserId = "usr_approver_manager",
            rejectionReason = "Invalid receipt attachment"
        ))

        // REJECTED -> DRAFT (valid for resubmission)
        assertNull(ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = ExpenseApprovalStatus.REJECTED,
            newStatus = ExpenseApprovalStatus.DRAFT,
            createdByUserId = "usr_creator",
            submittedByUserId = "usr_submitter",
            actorUserId = "usr_submitter"
        ))

        // Direct DRAFT -> APPROVED (invalid)
        val invalidDirect = ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = ExpenseApprovalStatus.DRAFT,
            newStatus = ExpenseApprovalStatus.APPROVED,
            createdByUserId = "usr_creator",
            submittedByUserId = "usr_submitter",
            actorUserId = "usr_approver"
        )
        assertNotNull(invalidDirect)
        assert(invalidDirect!!.contains("Invalid approval status transition"))
    }

    @Test
    fun testCreatorSelfApprovalPrevention() {
        val creatorSelfApprove = ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = ExpenseApprovalStatus.SUBMITTED,
            newStatus = ExpenseApprovalStatus.APPROVED,
            createdByUserId = "usr_creator_01",
            submittedByUserId = "usr_sales_02",
            actorUserId = "usr_creator_01"
        )
        assertNotNull(creatorSelfApprove)
        assertEquals("Unauthorized self-approval: Expense creator cannot approve their own expense", creatorSelfApprove)
    }

    @Test
    fun testSubmitterSelfApprovalPrevention() {
        val submitterSelfApprove = ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = ExpenseApprovalStatus.SUBMITTED,
            newStatus = ExpenseApprovalStatus.APPROVED,
            createdByUserId = "usr_sales_01",
            submittedByUserId = "usr_submitter_02",
            actorUserId = "usr_submitter_02"
        )
        assertNotNull(submitterSelfApprove)
        assertEquals("Unauthorized self-approval: Submitter cannot approve their own expense", submitterSelfApprove)
    }

    @Test
    fun testMissingCreatorOrSubmitterIdOnApproval() {
        val missingCreator = ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = ExpenseApprovalStatus.SUBMITTED,
            newStatus = ExpenseApprovalStatus.APPROVED,
            createdByUserId = "",
            submittedByUserId = "usr_submitter",
            actorUserId = "usr_manager"
        )
        assertEquals("Created By User ID is required for approval validation", missingCreator)

        val missingSubmitter = ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = ExpenseApprovalStatus.SUBMITTED,
            newStatus = ExpenseApprovalStatus.APPROVED,
            createdByUserId = "usr_creator",
            submittedByUserId = "",
            actorUserId = "usr_manager"
        )
        assertEquals("Submitted By User ID is required for approval validation", missingSubmitter)
    }

    @Test
    fun testRequiredRejectionReason() {
        val missingReason = ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = ExpenseApprovalStatus.SUBMITTED,
            newStatus = ExpenseApprovalStatus.REJECTED,
            createdByUserId = "usr_creator",
            submittedByUserId = "usr_submitter",
            actorUserId = "usr_manager",
            rejectionReason = "   "
        )
        assertNotNull(missingReason)
        assertEquals("Rejection reason is required when rejecting an expense", missingReason)
    }

    @Test
    fun testPaymentTransitionAllowlist() {
        // UNPAID -> PAID on APPROVED expense (valid)
        assertNull(ExpenseValidationUtils.validatePaymentTransition(
            currentPaymentStatus = ExpensePaymentStatus.UNPAID,
            approvalStatus = ExpenseApprovalStatus.APPROVED,
            newPaymentStatus = ExpensePaymentStatus.PAID
        ))

        // PAID -> REVERSED with reason (valid)
        assertNull(ExpenseValidationUtils.validatePaymentTransition(
            currentPaymentStatus = ExpensePaymentStatus.PAID,
            approvalStatus = ExpenseApprovalStatus.APPROVED,
            newPaymentStatus = ExpensePaymentStatus.REVERSED,
            reversalReason = "Vendor refund"
        ))
    }

    @Test
    fun testPaymentTransitionDenylist() {
        // PAID -> UNPAID (invalid)
        val paidToUnpaid = ExpenseValidationUtils.validatePaymentTransition(
            ExpensePaymentStatus.PAID, ExpenseApprovalStatus.APPROVED, ExpensePaymentStatus.UNPAID
        )
        assertNotNull(paidToUnpaid)

        // REVERSED -> PAID (invalid)
        val reversedToPaid = ExpenseValidationUtils.validatePaymentTransition(
            ExpensePaymentStatus.REVERSED, ExpenseApprovalStatus.APPROVED, ExpensePaymentStatus.PAID
        )
        assertNotNull(reversedToPaid)

        // REVERSED -> UNPAID (invalid)
        val reversedToUnpaid = ExpenseValidationUtils.validatePaymentTransition(
            ExpensePaymentStatus.REVERSED, ExpenseApprovalStatus.APPROVED, ExpensePaymentStatus.UNPAID
        )
        assertNotNull(reversedToUnpaid)

        // UNPAID -> REVERSED (invalid)
        val unpaidToReversed = ExpenseValidationUtils.validatePaymentTransition(
            ExpensePaymentStatus.UNPAID, ExpenseApprovalStatus.APPROVED, ExpensePaymentStatus.REVERSED, reversalReason = "Reason"
        )
        assertNotNull(unpaidToReversed)

        // UNPAID -> UNPAID (invalid duplicate)
        val unpaidToUnpaid = ExpenseValidationUtils.validatePaymentTransition(
            ExpensePaymentStatus.UNPAID, ExpenseApprovalStatus.APPROVED, ExpensePaymentStatus.UNPAID
        )
        assertNotNull(unpaidToUnpaid)

        // PAID -> PAID (invalid duplicate)
        val paidToPaid = ExpenseValidationUtils.validatePaymentTransition(
            ExpensePaymentStatus.PAID, ExpenseApprovalStatus.APPROVED, ExpensePaymentStatus.PAID
        )
        assertNotNull(paidToPaid)
    }

    @Test
    fun testUnapprovedPaymentRejection() {
        val unapprovedPaid = ExpenseValidationUtils.validatePaymentTransition(
            currentPaymentStatus = ExpensePaymentStatus.UNPAID,
            approvalStatus = ExpenseApprovalStatus.SUBMITTED,
            newPaymentStatus = ExpensePaymentStatus.PAID
        )
        assertNotNull(unapprovedPaid)
        assertEquals("Payment prohibited: Expense must be in APPROVED status before recording payment", unapprovedPaid)
    }

    @Test
    fun testMissingReversalReason() {
        val missingReason = ExpenseValidationUtils.validatePaymentTransition(
            currentPaymentStatus = ExpensePaymentStatus.PAID,
            approvalStatus = ExpenseApprovalStatus.APPROVED,
            newPaymentStatus = ExpensePaymentStatus.REVERSED,
            reversalReason = "   "
        )
        assertNotNull(missingReason)
        assertEquals("Reversal reason is required when reversing an expense payment", missingReason)
    }

    @Test
    fun testCashbookReconciliationFormula() {
        val opening = 1000000L // ₹10,000.00
        val cashCollections = 5000000L // ₹50,000.00
        val paidCashExpenses = 1500000L // ₹15,000.00
        val adjustments = 50000L // ₹500.00

        val closing = ExpenseValidationUtils.calculateStoreCashbookBalancePaise(
            openingCashPaise = opening,
            verifiedCashCollectionsPaise = cashCollections,
            verifiedPaidCashExpensesPaise = paidCashExpenses,
            cashAdjustmentsPaise = adjustments
        )

        // ₹10,000 + ₹50,000 - ₹15,000 + ₹500 = ₹45,500.00 = 4550000L paise
        assertEquals(4550000L, closing)
    }

    @Test
    fun testNegativeCashbookInputsRejection() {
        try {
            ExpenseValidationUtils.calculateStoreCashbookBalancePaise(-100L, 1000L, 500L)
            assert(false) { "Expected IllegalArgumentException for negative opening cash" }
        } catch (e: IllegalArgumentException) {
            assertEquals("Opening cash balance cannot be negative", e.message)
        }

        try {
            ExpenseValidationUtils.calculateStoreCashbookBalancePaise(1000L, -500L, 500L)
            assert(false) { "Expected IllegalArgumentException for negative cash collections" }
        } catch (e: IllegalArgumentException) {
            assertEquals("Verified cash collections cannot be negative", e.message)
        }

        try {
            ExpenseValidationUtils.calculateStoreCashbookBalancePaise(1000L, 1000L, -200L)
            assert(false) { "Expected IllegalArgumentException for negative paid cash expenses" }
        } catch (e: IllegalArgumentException) {
            assertEquals("Verified paid cash expenses cannot be negative", e.message)
        }
    }

    @Test
    fun testNegativeCalculatedClosingCashbookBalance() {
        val opening = 100000L // ₹1,000.00
        val collections = 200000L // ₹2,000.00
        val expenses = 500000L // ₹5,000.00

        val closing = ExpenseValidationUtils.calculateStoreCashbookBalancePaise(opening, collections, expenses)
        // ₹1,000 + ₹2,000 - ₹5,000 = -₹2,000.00 = -200000L paise
        assertEquals(-200000L, closing)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCashbookOverflowProtection() {
        ExpenseValidationUtils.calculateStoreCashbookBalancePaise(
            openingCashPaise = Long.MAX_VALUE - 100L,
            verifiedCashCollectionsPaise = 500L,
            verifiedPaidCashExpensesPaise = 0L
        )
    }
}
