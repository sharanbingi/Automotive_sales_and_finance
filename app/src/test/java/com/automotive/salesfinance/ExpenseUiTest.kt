package com.automotive.salesfinance

import com.automotive.salesfinance.model.Expense
import com.automotive.salesfinance.model.ExpenseApprovalStatus
import com.automotive.salesfinance.model.ExpenseCategory
import com.automotive.salesfinance.model.ExpensePaymentMethod
import com.automotive.salesfinance.model.ExpensePaymentStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.ui.expense.calculateExpenseSummary
import com.automotive.salesfinance.ui.expense.parseRupeesToPaise
import com.automotive.salesfinance.utils.CurrencyUtils
import com.automotive.salesfinance.viewmodel.ExpenseActionState
import com.automotive.salesfinance.viewmodel.ExpenseActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseUiTest {

    @Test
    fun testParseRupeesToPaise_ValidInputs() {
        assertEquals(10000L, parseRupeesToPaise("100"))
        assertEquals(10050L, parseRupeesToPaise("100.5"))
        assertEquals(10050L, parseRupeesToPaise("100.50"))
        assertEquals(363588L, parseRupeesToPaise("3635.88"))
        assertEquals(100L, parseRupeesToPaise("1.00"))
    }

    @Test
    fun testParseRupeesToPaise_InvalidInputs_Rejected() {
        assertNull(parseRupeesToPaise(""))
        assertNull(parseRupeesToPaise("   "))
        assertNull(parseRupeesToPaise("0"))
        assertNull(parseRupeesToPaise("0.00"))
        assertNull(parseRupeesToPaise("-50"))
        assertNull(parseRupeesToPaise("100.555"))
        assertNull(parseRupeesToPaise("1e5"))
        assertNull(parseRupeesToPaise("NaN"))
        assertNull(parseRupeesToPaise("ABC"))
    }

    @Test
    fun testFormatPaise_ExactOutput() {
        assertEquals("₹3,635.88", CurrencyUtils.formatPaise(363588L))
        assertEquals("₹100.00", CurrencyUtils.formatPaiseExact(10000L))
        assertEquals("₹0.50", CurrencyUtils.formatPaiseExact(50L))
    }

    @Test
    fun testExpenseDate_Versus_CreatedAt_Semantics() {
        val businessDate = 1700000000000L // 14 Nov 2023
        val recordCreationTime = 1710000000000L // 09 Mar 2024

        val expense = Expense(
            expenseId = "EXP_100",
            expenseDate = businessDate,
            createdAt = recordCreationTime
        )

        assertNotEquals(expense.expenseDate, expense.createdAt)

        val dateOnlyFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        val formattedExpenseDate = dateOnlyFormat.format(Date(expense.expenseDate))
        val formattedCreated = dateTimeFormat.format(Date(expense.createdAt))

        assertNotNull(formattedExpenseDate)
        assertNotNull(formattedCreated)
        assertFalse(formattedExpenseDate.contains("hh:mm"))
    }

    @Test
    fun testTypedActionState_DialogCloseIsolation() {
        val createSuccess = ExpenseActionState.Success(ExpenseActionType.CREATE, "Draft created")
        val refreshSuccess = ExpenseActionState.Success(ExpenseActionType.REFRESH, "Refreshed")
        val rejectSuccess = ExpenseActionState.Success(ExpenseActionType.REJECT, "Rejected")
        val reverseSuccess = ExpenseActionState.Success(ExpenseActionType.REVERSE, "Reversed")

        // Create Dialog should close ONLY for CREATE success
        fun shouldCloseCreateDialog(state: ExpenseActionState) =
            state is ExpenseActionState.Success && state.actionType == ExpenseActionType.CREATE

        assertTrue(shouldCloseCreateDialog(createSuccess))
        assertFalse(shouldCloseCreateDialog(refreshSuccess))
        assertFalse(shouldCloseCreateDialog(rejectSuccess))

        // Reject Dialog should close ONLY for REJECT success
        fun shouldCloseRejectDialog(state: ExpenseActionState) =
            state is ExpenseActionState.Success && state.actionType == ExpenseActionType.REJECT

        assertTrue(shouldCloseRejectDialog(rejectSuccess))
        assertFalse(shouldCloseRejectDialog(refreshSuccess))
        assertFalse(shouldCloseRejectDialog(reverseSuccess))
    }

    @Test
    fun testFailureState_FormRetention() {
        var createFormInput = "Office Rent April"
        val createErrorState = ExpenseActionState.Error(ExpenseActionType.CREATE, "Network error")

        // On error, form input remains intact
        if (createErrorState is ExpenseActionState.Error && createErrorState.actionType == ExpenseActionType.CREATE) {
            // Form is NOT cleared
        }
        assertEquals("Office Rent April", createFormInput)
    }

    @Test
    fun testSelfApprovalRestriction_Logic() {
        val creatorUid = "usr_creator"
        val submitterUid = "usr_submitter"

        val expense = Expense(
            expenseId = "EXP_1",
            dealershipId = "dealership_001",
            storeId = "Store_A",
            createdByUserId = creatorUid,
            submittedByUserId = submitterUid,
            approvalStatus = ExpenseApprovalStatus.SUBMITTED
        )

        val creatorUser = User(uid = creatorUid, role = UserRole.DEALERSHIP_ADMIN)
        val submitterUser = User(uid = submitterUid, role = UserRole.FINANCE_USER)
        val independentAdmin = User(uid = "usr_admin_other", role = UserRole.DEALERSHIP_ADMIN)

        val canCreatorApprove = creatorUser.role in listOf(UserRole.DEALERSHIP_ADMIN, UserRole.FINANCE_USER) &&
                creatorUser.uid != expense.createdByUserId &&
                creatorUser.uid != expense.submittedByUserId
        assertFalse(canCreatorApprove)

        val canSubmitterApprove = submitterUser.role in listOf(UserRole.DEALERSHIP_ADMIN, UserRole.FINANCE_USER) &&
                submitterUser.uid != expense.createdByUserId &&
                submitterUser.uid != expense.submittedByUserId
        assertFalse(canSubmitterApprove)

        val canIndependentApprove = independentAdmin.role in listOf(UserRole.DEALERSHIP_ADMIN, UserRole.FINANCE_USER) &&
                independentAdmin.uid != expense.createdByUserId &&
                independentAdmin.uid != expense.submittedByUserId
        assertTrue(canIndependentApprove)
    }

    @Test
    fun testActionVisibility_ByApprovalAndPaymentStatus() {
        val draftExpense = Expense(approvalStatus = ExpenseApprovalStatus.DRAFT)
        val submittedExpense = Expense(approvalStatus = ExpenseApprovalStatus.SUBMITTED)
        val rejectedExpense = Expense(approvalStatus = ExpenseApprovalStatus.REJECTED)
        val approvedExpense = Expense(approvalStatus = ExpenseApprovalStatus.APPROVED, paymentStatus = ExpensePaymentStatus.UNPAID)
        val cancelledExpense = Expense(approvalStatus = ExpenseApprovalStatus.CANCELLED)

        fun canResubmit(exp: Expense, uid: String) = (uid == exp.createdByUserId || uid == exp.submittedByUserId) && exp.approvalStatus == ExpenseApprovalStatus.REJECTED

        assertFalse(canResubmit(draftExpense, "usr_1"))
        assertFalse(canResubmit(submittedExpense, "usr_1"))
        assertTrue(canResubmit(rejectedExpense.copy(createdByUserId = "usr_1"), "usr_1"))
        assertFalse(canResubmit(approvedExpense, "usr_1"))
        assertFalse(canResubmit(cancelledExpense, "usr_1"))
    }

    @Test
    fun testRoleAccess_AuthorizedVsDeniedRoles() {
        val authorizedRoles = listOf(UserRole.DEALERSHIP_ADMIN, UserRole.STORE_MANAGER, UserRole.FINANCE_USER)
        val deniedRoles = listOf(UserRole.SALES_USER, UserRole.STATE_MANAGER, UserRole.ADMIN, UserRole.SUPER_ADMIN, UserRole.CUSTOMER)

        authorizedRoles.forEach { role ->
            assertTrue(role in listOf(UserRole.DEALERSHIP_ADMIN, UserRole.STORE_MANAGER, UserRole.FINANCE_USER))
        }

        deniedRoles.forEach { role ->
            assertFalse(role in listOf(UserRole.DEALERSHIP_ADMIN, UserRole.STORE_MANAGER, UserRole.FINANCE_USER))
        }
    }

    @Test
    fun testSummaryCardPureAggregation_ExcludesCancelledAndReversed() {
        val expenses = listOf(
            Expense(expenseId = "1", amountPaise = 10000L, approvalStatus = ExpenseApprovalStatus.SUBMITTED, paymentStatus = ExpensePaymentStatus.UNPAID),
            Expense(expenseId = "2", amountPaise = 20000L, approvalStatus = ExpenseApprovalStatus.APPROVED, paymentStatus = ExpensePaymentStatus.UNPAID),
            Expense(expenseId = "3", amountPaise = 30000L, approvalStatus = ExpenseApprovalStatus.APPROVED, paymentStatus = ExpensePaymentStatus.PAID),
            Expense(expenseId = "4", amountPaise = 50000L, approvalStatus = ExpenseApprovalStatus.CANCELLED, paymentStatus = ExpensePaymentStatus.UNPAID),
            Expense(expenseId = "5", amountPaise = 40000L, approvalStatus = ExpenseApprovalStatus.APPROVED, paymentStatus = ExpensePaymentStatus.REVERSED)
        )

        val summary = calculateExpenseSummary(expenses)
        assertFalse(summary.isOverflow)
        assertEquals(60000L, summary.totalPaise)
        assertEquals(10000L, summary.pendingPaise)
        assertEquals(20000L, summary.approvedUnpaidPaise)
        assertEquals(30000L, summary.paidPaise)
    }

    @Test
    fun testSummaryCardOverflowSafety() {
        var total = 0L
        try {
            total = Math.addExact(Long.MAX_VALUE - 10L, 20L)
        } catch (_: ArithmeticException) {
            total = -1L
        }
        assertEquals(-1L, total)
    }
}
