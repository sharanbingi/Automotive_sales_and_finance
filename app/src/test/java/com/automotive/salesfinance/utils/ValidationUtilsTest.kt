package com.automotive.salesfinance.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun isValidChassisNumber_validatesCorrectly() {
        assertTrue(ValidationUtils.isValidChassisNumber("TEST001"))
        assertTrue(ValidationUtils.isValidChassisNumber("CHASSIS002"))
        assertFalse(ValidationUtils.isValidChassisNumber("12")) // Too short
        assertFalse(ValidationUtils.isValidChassisNumber("INVALID_CHASSIS_123456789")) // Special char or too long
    }

    @Test
    fun isValidEngineNumber_validatesCorrectly() {
        assertTrue(ValidationUtils.isValidEngineNumber("ENG001"))
        assertFalse(ValidationUtils.isValidEngineNumber("E1")) // Too short
    }

    @Test
    fun isValidPhoneNumber_validatesIndianMobileNumbers() {
        assertTrue(ValidationUtils.isValidPhoneNumber("9876543210"))
        assertTrue(ValidationUtils.isValidPhoneNumber("7890123456"))
        assertFalse(ValidationUtils.isValidPhoneNumber("1234567890")) // Doesn't start with 6-9
        assertFalse(ValidationUtils.isValidPhoneNumber("987654")) // Short
    }

    @Test
    fun isValidPriceAndYear_validatesCorrectly() {
        assertTrue(ValidationUtils.isValidPrice(95000.0))
        assertFalse(ValidationUtils.isValidPrice(0.0))
        assertFalse(ValidationUtils.isValidPrice(-100.0))

        assertTrue(ValidationUtils.isValidYear(2024))
        assertFalse(ValidationUtils.isValidYear(1850))
    }
}
