package com.automotive.salesfinance.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

    fun calculateDaysInStock(inwardTimestamp: Long): Long {
        if (inwardTimestamp <= 0) return 0
        val diffMillis = System.currentTimeMillis() - inwardTimestamp
        return if (diffMillis < 0) 0 else TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    fun isDeadStock(inwardTimestamp: Long, thresholdDays: Int = 60): Boolean {
        return calculateDaysInStock(inwardTimestamp) > thresholdDays
    }

    fun formatDate(timestamp: Long, pattern: String = "dd MMM yyyy"): String {
        if (timestamp <= 0) return "N/A"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return formatDate(timestamp, "dd MMM yyyy, hh:mm a")
    }
}
