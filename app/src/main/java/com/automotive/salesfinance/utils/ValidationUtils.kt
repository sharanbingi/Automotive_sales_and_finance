package com.automotive.salesfinance.utils

import java.util.Calendar

object ValidationUtils {

    fun isValidChassisNumber(chassisNumber: String): Boolean {
        val trimmed = chassisNumber.trim().uppercase()
        return trimmed.length in 5..17 && trimmed.matches(Regex("^[A-Z0-9]+$"))
    }

    fun isValidEngineNumber(engineNumber: String): Boolean {
        val trimmed = engineNumber.trim().uppercase()
        return trimmed.length in 5..20 && trimmed.matches(Regex("^[A-Z0-9]+$"))
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        val trimmed = phone.trim()
        return trimmed.matches(Regex("^[6-9]\\d{9}$"))
    }

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotBlank() && EMAIL_REGEX.matches(trimmed)
    }

    fun isValidPrice(price: Double): Boolean {
        return price > 0.0
    }

    fun isValidYear(year: Int): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return year in 1990..(currentYear + 1)
    }
}
