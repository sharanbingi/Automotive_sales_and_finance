package com.automotive.salesfinance.model

enum class SubscriptionStatus {
    TRIAL,
    ACTIVE,
    PAST_DUE,
    EXPIRED,
    SUSPENDED,
    CANCELLED
}

data class Dealership(
    val dealershipId: String = "",
    val name: String = "",
    val legalName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "India",
    val active: Boolean = true,
    val subscriptionPlan: String = "STARTER",
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.TRIAL,
    val trialStartDate: Long = 0L,
    val trialEndDate: Long = 0L,
    val subscriptionStartDate: Long = 0L,
    val subscriptionEndDate: Long = 0L,
    val supportedVehicleTypes: List<VehicleType> = listOf(VehicleType.BIKE, VehicleType.CAR),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
