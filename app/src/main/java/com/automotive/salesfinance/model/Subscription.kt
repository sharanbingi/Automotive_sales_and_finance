package com.automotive.salesfinance.model

enum class BillingCycle {
    MONTHLY,
    YEARLY
}

data class Subscription(
    val subscriptionId: String = "",
    val dealershipId: String = "",
    val planId: String = "",
    val planName: String = "",
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val price: Double = 0.0,
    val currency: String = "INR",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val trialStartDate: Long = 0L,
    val trialEndDate: Long = 0L,
    val nextBillingDate: Long = 0L,
    val provider: String = "RAZORPAY",
    val providerCustomerId: String = "",
    val providerSubscriptionId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
