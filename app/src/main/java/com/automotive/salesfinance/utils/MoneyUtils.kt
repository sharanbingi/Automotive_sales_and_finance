package com.automotive.salesfinance.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

fun Double.toPaise(): Long {
    return BigDecimal.valueOf(this)
        .setScale(2, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}

fun BigDecimal.toPaise(): Long {
    return this.setScale(2, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}

fun Long.toRupeesBigDecimal(): BigDecimal {
    return BigDecimal(this).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
}

fun Long.toRupeesDouble(): Double {
    return this.toRupeesBigDecimal().toDouble()
}

fun Long.formatInr(): String {
    val rupees = this.toRupeesBigDecimal()
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    return formatter.format(rupees)
}
