package com.automotive.salesfinance.model

enum class BikeStatus {
    AVAILABLE,
    RESERVED,
    FINANCED
}

data class Bike(
    val bikeId: String = "",
    val dealershipId: String = "",
    val chassisNumber: String = "",
    val engineNumber: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 2024,
    val color: String = "",
    val registrationNumber: String = "",
    val costPrice: Double = 0.0,
    val listedPrice: Double = 0.0,
    val stateCode: String = "",
    val storeLocation: String = "",
    val status: BikeStatus = BikeStatus.AVAILABLE,
    val inwardTimestamp: Long = System.currentTimeMillis(),
    val imageUrl: String = ""
)
