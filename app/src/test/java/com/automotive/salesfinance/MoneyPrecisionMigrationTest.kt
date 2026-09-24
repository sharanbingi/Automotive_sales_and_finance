package com.automotive.salesfinance

import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.utils.formatInr
import com.automotive.salesfinance.utils.toPaise
import com.automotive.salesfinance.utils.toRupeesBigDecimal
import com.automotive.salesfinance.utils.toRupeesDouble
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyPrecisionMigrationTest {

    @Test
    fun testMoneyUtilsConversions() {
        assertEquals(0L, 0.0.toPaise())
        assertEquals(1L, 0.01.toPaise())
        assertEquals(100L, 1.0.toPaise())
        assertEquals(110L, 1.10.toPaise())
        assertEquals(363500L, 3635.0.toPaise())
        assertEquals(363550L, 3635.50.toPaise())
        assertEquals(8362616L, 83626.15994864139.toPaise())

        // BigDecimal toPaise
        assertEquals(363550L, BigDecimal("3635.50").toPaise())

        // Long to Rupees
        assertEquals(0.0, 0L.toRupeesDouble(), 0.001)
        assertEquals(1.0, 100L.toRupeesDouble(), 0.001)
        assertEquals(3635.50, 363550L.toRupeesDouble(), 0.001)
        assertEquals(BigDecimal("3635.50"), 363550L.toRupeesBigDecimal())

        // Large values and round-trip
        val largePaise = 98765432100L
        assertEquals(largePaise, largePaise.toRupeesDouble().toPaise())

        // Format INR
        val formatted = 8362616L.formatInr()
        assert(formatted.contains("83,626.16"))
    }

    @Test
    fun testLegacyLoanFallback() {
        val loan = Loan(
            remainingBalance = 83626.15994864139,
            remainingBalancePaise = null
        )
        assertEquals(8362616L, loan.effectiveRemainingBalancePaise())
    }

    @Test
    fun testExplicitZeroPaiseAuthority() {
        // If remainingBalancePaise is explicitly 0L, it should not fall back to Double remainingBalance
        val loan = Loan(
            remainingBalance = 5000.0,
            remainingBalancePaise = 0L
        )
        assertEquals(0L, loan.effectiveRemainingBalancePaise())
    }

    @Test
    fun testTransactionFallback() {
        val txnNullPaise = Transaction(
            amount = 1500.50,
            amountPaise = null
        )
        assertEquals(150050L, txnNullPaise.effectiveAmountPaise())

        val txnExplicitZero = Transaction(
            amount = 1500.50,
            amountPaise = 0L
        )
        assertEquals(0L, txnExplicitZero.effectiveAmountPaise())

        val txnExplicitPaise = Transaction(
            amount = 1500.50,
            amountPaise = 250000L
        )
        assertEquals(250000L, txnExplicitPaise.effectiveAmountPaise())
    }

    @Test
    fun test100SequentialMonetaryDeductions_zeroPrecisionDrift() {
        var remainingPaise = 10000000L // ₹100,000.00
        val deductionPaise = 33333L // ₹333.33
        var totalDeductedPaise = 0L

        repeat(100) {
            remainingPaise -= deductionPaise
            totalDeductedPaise += deductionPaise
        }

        assertEquals(10000000L, remainingPaise + totalDeductedPaise)
        assertEquals(6666700L, remainingPaise)
    }

    @Test
    fun testMixedOldAndNewTransactionsInCollections() {
        val txns = listOf(
            Transaction(amount = 1000.50, amountPaise = null),
            Transaction(amount = 0.0, amountPaise = 250000L),
            Transaction(amount = 500.25, amountPaise = 50025L)
        )
        val totalPaise = txns.sumOf { it.effectiveAmountPaise() }
        assertEquals(100050L + 250000L + 50025L, totalPaise)
        val totalRupees = totalPaise.toRupeesDouble()
        assertEquals(4000.75, totalRupees, 0.001)
    }
}
