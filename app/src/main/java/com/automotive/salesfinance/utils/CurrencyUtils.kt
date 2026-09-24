package com.automotive.salesfinance.utils

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object CurrencyUtils {

    fun formatCurrency(amount: Double, includeSymbol: Boolean = true): String {
        val bd = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
        val paise = bd.multiply(BigDecimal(100)).longValueExact()
        return formatPaise(paise, includeSymbol)
    }

    fun formatCurrency(amount: Long, includeSymbol: Boolean = true): String {
        val formatted = formatIndianNumber(amount)
        return if (includeSymbol) "₹$formatted" else formatted
    }

    fun formatPaise(paise: Long, includeSymbol: Boolean = true): String {
        val isNegative = paise < 0
        val absPaise = abs(paise)
        val rupees = absPaise / 100
        val remainder = absPaise % 100
        val formattedRupees = formatIndianNumber(rupees)
        val formatted = if (remainder != 0L) {
            "$formattedRupees.${"%02d".format(remainder)}"
        } else {
            formattedRupees
        }
        val withSign = if (isNegative) "-$formatted" else formatted
        return if (includeSymbol) "₹$withSign" else withSign
    }

    fun formatPaiseExact(paise: Long, includeSymbol: Boolean = true): String {
        val isNegative = paise < 0
        val absPaise = abs(paise)
        val rupees = absPaise / 100
        val remainder = absPaise % 100
        val formattedRupees = formatIndianNumber(rupees)
        val formatted = "$formattedRupees.${"%02d".format(remainder)}"
        val withSign = if (isNegative) "-$formatted" else formatted
        return if (includeSymbol) "₹$withSign" else withSign
    }

    private fun formatIndianNumber(number: Long): String {
        val isNegative = number < 0
        val absNumStr = abs(number).toString()
        if (absNumStr.length <= 3) {
            val result = absNumStr
            return if (isNegative) "-$result" else result
        }
        val last3 = absNumStr.substring(absNumStr.length - 3)
        val remaining = absNumStr.substring(0, absNumStr.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(',')
            }
        }
        val formattedRemaining = sb.reverse().toString()
        val result = "$formattedRemaining,$last3"
        return if (isNegative) "-$result" else result
    }
}
