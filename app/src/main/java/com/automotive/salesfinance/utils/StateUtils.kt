package com.automotive.salesfinance.utils

import java.util.Locale

object StateUtils {
    /**
     * Reusable canonical state code normalization utility.
     * Maps common variations (e.g. TELANGANA, TS, TG, KARNATAKA, KA, etc.)
     * to canonical 2-letter state codes (TG, KA, TN, MH) or ALL.
     * Unknown nonblank values return trimmed uppercase without guessing.
     */
    fun canonicalStateCode(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val cleaned = input.trim().uppercase(Locale.ROOT).replace("\\s+".toRegex(), "")
        return when (cleaned) {
            "ALL" -> "ALL"
            "TG", "TS", "TELANGANA" -> "TG"
            "KA", "KARNATAKA" -> "KA"
            "TN", "TAMILNADU" -> "TN"
            "MH", "MAHARASHTRA" -> "MH"
            else -> cleaned
        }
    }
}

fun String?.toCanonicalStateCode(): String = StateUtils.canonicalStateCode(this)
