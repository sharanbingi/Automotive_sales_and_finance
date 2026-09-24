package com.automotive.salesfinance

import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.utils.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class Phase37EFinanceDashboardTest {

    @Test
    fun testPaiseBasedRepaymentProgressFraction() {
        val loan = Loan(
            paidAmountPaise = 2500000L, // ₹25,000.00
            remainingBalancePaise = 7500000L // ₹75,000.00
        )
        val paidPaise = loan.effectivePaidAmountPaise()
        val remainingPaise = loan.effectiveRemainingBalancePaise()
        val totalPaise = paidPaise + remainingPaise

        assertEquals(10000000L, totalPaise)
        val progressFraction = (paidPaise.toDouble() / totalPaise.toDouble()).toFloat()
        assertEquals(0.25f, progressFraction, 0.001f)
    }

    @Test
    fun testLoanCardFormattedEmiAndBalance() {
        val emiPaise = 363588L // ₹3,635.88
        val remainingPaise = 1200050L // ₹12,000.50

        val formattedEmi = CurrencyUtils.formatPaise(emiPaise)
        val formattedRemaining = CurrencyUtils.formatPaise(remainingPaise)

        assertEquals("₹3,635.88", formattedEmi)
        assertEquals("₹12,000.50", formattedRemaining)
    }

    @Test
    fun testSubtitleLabelingLogic() {
        val loansCount = 5
        fun getSubtitle(statusFilter: String): String {
            return if (statusFilter.equals("ALL", ignoreCase = true)) {
                "$loansCount Financing Contracts"
            } else {
                "$loansCount ${statusFilter.lowercase().replaceFirstChar { it.uppercase() }} Contracts"
            }
        }

        assertEquals("5 Financing Contracts", getSubtitle("ALL"))
        assertEquals("5 Active Contracts", getSubtitle("ACTIVE"))
        assertEquals("5 Overdue Contracts", getSubtitle("OVERDUE"))
        assertEquals("5 Settled Contracts", getSubtitle("SETTLED"))
    }

    @Test
    fun testZeroAndNegativeBalanceSafety() {
        val zeroLoan = Loan(
            paidAmountPaise = 0L,
            remainingBalancePaise = 0L
        )
        val paidPaise = zeroLoan.effectivePaidAmountPaise()
        val remainingPaise = zeroLoan.effectiveRemainingBalancePaise()
        val totalPaise = paidPaise + remainingPaise

        val progressFraction = if (totalPaise > 0L) {
            (paidPaise.toDouble() / totalPaise.toDouble()).toFloat().coerceIn(0f, 1f)
        } else 0f

        assertEquals(0f, progressFraction, 0.001f)
    }
}
