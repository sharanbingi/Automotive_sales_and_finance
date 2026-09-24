package com.automotive.salesfinance.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun formatCurrency_formatsIndianNumbersCorrectly() {
        assertEquals("₹95,000", CurrencyUtils.formatCurrency(95000.0))
        assertEquals("₹1,20,000", CurrencyUtils.formatCurrency(120000.0))
        assertEquals("₹2,20,000", CurrencyUtils.formatCurrency(220000.0))
        assertEquals("₹1,00,00,000", CurrencyUtils.formatCurrency(10000000.0))
    }

    @Test
    fun formatCurrency_withoutSymbol() {
        assertEquals("85,000", CurrencyUtils.formatCurrency(85000L, includeSymbol = false))
    }
}
