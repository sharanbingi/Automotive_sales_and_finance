package com.automotive.salesfinance

import com.automotive.salesfinance.ui.finance.formatAmountForInput
import com.automotive.salesfinance.ui.finance.validatePaymentAmountInput
import com.automotive.salesfinance.utils.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class Phase36CFinancialPrecisionTest {

    @Test
    fun testPaiseAndDecimalFormatting_3635_88() {
        val formatted = CurrencyUtils.formatCurrency(3635.88)
        assertEquals("₹3,635.88", formatted)
    }

    @Test
    fun testPaiseAndDecimalFormatting_12000_50() {
        val formatted = CurrencyUtils.formatCurrency(12000.50)
        assertEquals("₹12,000.50", formatted)
    }

    @Test
    fun testPaiseAndDecimalFormatting_98000_00() {
        val formattedClean = CurrencyUtils.formatCurrency(98000.00)
        assertEquals("₹98,000", formattedClean)

        val formattedExact = CurrencyUtils.formatPaiseExact(9800000L)
        assertEquals("₹98,000.00", formattedExact)
    }

    @Test
    fun testPaymentInputAmountFormat_PreservesDecimals() {
        assertEquals("3635.88", formatAmountForInput(3635.88))
        assertEquals("12000.50", formatAmountForInput(12000.50))
        assertEquals("98000", formatAmountForInput(98000.00))
    }

    @Test
    fun testPaymentInputValidation_RejectsMoreThanTwoDecimals() {
        val result = validatePaymentAmountInput("100.555", 5000.0)
        assertNotNull(result)
        assertEquals("Payment amount cannot have more than 2 decimal places", result)
    }

    @Test
    fun testPaymentInputValidation_RejectsZeroAndNegativeAmounts() {
        val zeroResult = validatePaymentAmountInput("0.00", 5000.0)
        assertNotNull(zeroResult)
        assertEquals("Payment amount must be greater than zero", zeroResult)

        val negResult = validatePaymentAmountInput("-15.00", 5000.0)
        assertNotNull(negResult)
        assertEquals("Payment amount must be greater than zero", negResult)
    }

    @Test
    fun testPaymentInputValidation_RejectsExceedingRemainingBalance() {
        val result = validatePaymentAmountInput("6000.00", 5000.0)
        assertNotNull(result)
        assert(result!!.contains("cannot exceed remaining loan balance"))
    }

    @Test
    fun testPaymentInputValidation_AcceptsValidAmount() {
        val result = validatePaymentAmountInput("3635.88", 5000.0)
        assertNull(result)
    }
}
