package com.automotive.salesfinance

import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.ui.finance.CashPaymentPurposeOption
import com.automotive.salesfinance.ui.finance.calculateDefaultCashAmountPaise
import com.automotive.salesfinance.ui.finance.formatPaiseToInputString
import com.automotive.salesfinance.ui.finance.validateCashPaymentInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class Phase37BCashPaymentPrecisionTest {

    @Test
    fun testFractionalEmiDefaultPrecision() {
        val loan = Loan(
            emiAmount = 3635.88,
            emiAmountPaise = 363588L,
            remainingBalance = 50000.0,
            remainingBalancePaise = 5000000L
        )
        val defaultPaise = calculateDefaultCashAmountPaise(CashPaymentPurposeOption.REGULAR_EMI, loan)
        assertEquals(363588L, defaultPaise)

        val formattedString = formatPaiseToInputString(defaultPaise)
        assertEquals("3635.88", formattedString)
    }

    @Test
    fun testFullSettlementPrecision() {
        val loan = Loan(
            remainingBalance = 12000.50,
            remainingBalancePaise = 1200050L,
            emiAmount = 3000.0,
            emiAmountPaise = 300000L
        )
        val defaultPaise = calculateDefaultCashAmountPaise(CashPaymentPurposeOption.FULL_SETTLEMENT, loan)
        assertEquals(1200050L, defaultPaise)

        val formattedString = formatPaiseToInputString(defaultPaise)
        assertEquals("12000.50", formattedString)
    }

    @Test
    fun testMultipleEmiPrecision() {
        val loan = Loan(
            emiAmount = 3635.88,
            emiAmountPaise = 363588L,
            remainingBalance = 50000.0,
            remainingBalancePaise = 5000000L
        )
        val defaultPaise = calculateDefaultCashAmountPaise(CashPaymentPurposeOption.MULTIPLE_EMIS, loan)
        assertEquals(727176L, defaultPaise) // 363588 * 2 = 727176
    }

    @Test
    fun testMultipleEmiOverflowProtection() {
        val hugeEmiLoan = Loan(
            emiAmountPaise = Long.MAX_VALUE / 2L + 1000L,
            remainingBalancePaise = Long.MAX_VALUE
        )
        val defaultPaise = calculateDefaultCashAmountPaise(CashPaymentPurposeOption.MULTIPLE_EMIS, hugeEmiLoan)
        // Must not overflow into negative numbers
        assert(defaultPaise > 0L)
        assertEquals(Long.MAX_VALUE, defaultPaise)
    }

    @Test
    fun testMultipleEmiExceedingBalance() {
        val loan = Loan(
            emiAmountPaise = 3000000L, // ₹30,000.00
            remainingBalancePaise = 4000000L // ₹40,000.00
        )
        // 2x EMI would be ₹60,000, but remaining balance is ₹40,000
        val defaultPaise = calculateDefaultCashAmountPaise(CashPaymentPurposeOption.MULTIPLE_EMIS, loan)
        assertEquals(4000000L, defaultPaise)
    }

    @Test
    fun testHalfEmiRoundingPrecision() {
        val loan = Loan(
            emiAmount = 3635.88,
            emiAmountPaise = 363588L,
            remainingBalance = 50000.0,
            remainingBalancePaise = 5000000L
        )
        val defaultPaise = calculateDefaultCashAmountPaise(CashPaymentPurposeOption.PARTIAL_PAYMENT, loan)
        assertEquals(181794L, defaultPaise) // 363588 / 2 = 181794 paise = ₹1,817.94

        val formattedString = formatPaiseToInputString(defaultPaise)
        assertEquals("1817.94", formattedString)
    }

    @Test
    fun testOddPaiseHalfEmiDivision() {
        val oddLoan = Loan(
            emiAmountPaise = 363589L, // ₹3,635.89
            remainingBalancePaise = 5000000L
        )
        val defaultPaise = calculateDefaultCashAmountPaise(CashPaymentPurposeOption.PARTIAL_PAYMENT, oddLoan)
        assertEquals(181794L, defaultPaise) // 363589 / 2 = 181794 floor division in integer paise
    }

    @Test
    fun testExactLargeAmountFormattingWithoutDouble() {
        assertEquals("0.01", formatPaiseToInputString(1L))
        assertEquals("-3635.88", formatPaiseToInputString(-363588L))
        assertEquals("9876543210123456.78", formatPaiseToInputString(987654321012345678L))
    }

    @Test
    fun testMaximumBalanceValidation() {
        val remainingPaise = 1200050L // ₹12,000.50
        val (paiseOver, errorOver) = validateCashPaymentInput("12000.51", remainingPaise)
        assertNull(paiseOver)
        assertNotNull(errorOver)
        assert(errorOver!!.contains("cannot exceed remaining loan balance"))

        val (paiseExact, errorExact) = validateCashPaymentInput("12000.50", remainingPaise)
        assertEquals(1200050L, paiseExact)
        assertNull(errorExact)
    }

    @Test
    fun testMoreThanTwoDecimalPlacesValidation() {
        val remainingPaise = 500000L
        val (paise, error) = validateCashPaymentInput("100.555", remainingPaise)
        assertNull(paise)
        assertNotNull(error)
        assertEquals("Payment amount cannot have more than 2 decimal places", error)
    }

    @Test
    fun testNegativeAndZeroValuesValidation() {
        val remainingPaise = 500000L
        val (zeroPaise, zeroError) = validateCashPaymentInput("0.00", remainingPaise)
        assertNull(zeroPaise)
        assertEquals("Payment amount must be greater than zero", zeroError)

        val (negPaise, negError) = validateCashPaymentInput("-15.00", remainingPaise)
        assertNull(negPaise)
        assertEquals("Payment amount must be greater than zero", negError)
    }

    @Test
    fun testInvalidAndBlankInputValidation() {
        val remainingPaise = 500000L
        val (blankPaise, blankErr) = validateCashPaymentInput("   ", remainingPaise)
        assertNull(blankPaise)
        assertEquals("Please enter a valid payment amount", blankErr)

        val (abcPaise, abcErr) = validateCashPaymentInput("abc", remainingPaise)
        assertNull(abcPaise)
        assertEquals("Invalid payment amount format", abcErr)
    }

    @Test
    fun testLongOverflowValidation() {
        val remainingPaise = 500000L
        val (paise, error) = validateCashPaymentInput("999999999999999999999.00", remainingPaise)
        assertNull(paise)
        assertNotNull(error)
    }
}
