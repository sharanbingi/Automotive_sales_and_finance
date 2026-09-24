package com.automotive.salesfinance.model

data class SubscriptionPlan(
    val planId: String = "",
    val name: String = "",
    val monthlyPrice: Double = 0.0,
    val yearlyPrice: Double = 0.0,
    val maxStores: Int = 1,
    val maxUsers: Int = 3,
    val maxBikes: Int = 100,
    val maxCustomers: Int = 500,
    val reportsEnabled: Boolean = true,
    val advancedReportsEnabled: Boolean = false,
    val financeEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val multiStoreEnabled: Boolean = false
) {
    companion object {
        val STARTER = SubscriptionPlan(
            planId = "STARTER",
            name = "Starter Plan",
            monthlyPrice = 2999.0,
            yearlyPrice = 29990.0,
            maxStores = 1,
            maxUsers = 3,
            maxBikes = 100,
            maxCustomers = 500,
            reportsEnabled = true,
            advancedReportsEnabled = false,
            financeEnabled = true,
            notificationsEnabled = true,
            multiStoreEnabled = false
        )

        val PROFESSIONAL = SubscriptionPlan(
            planId = "PROFESSIONAL",
            name = "Professional Plan",
            monthlyPrice = 6999.0,
            yearlyPrice = 69990.0,
            maxStores = 3,
            maxUsers = 10,
            maxBikes = 500,
            maxCustomers = 2500,
            reportsEnabled = true,
            advancedReportsEnabled = true,
            financeEnabled = true,
            notificationsEnabled = true,
            multiStoreEnabled = true
        )

        val BUSINESS = SubscriptionPlan(
            planId = "BUSINESS",
            name = "Business Plan",
            monthlyPrice = 12999.0,
            yearlyPrice = 129990.0,
            maxStores = 10,
            maxUsers = 35,
            maxBikes = 2000,
            maxCustomers = 10000,
            reportsEnabled = true,
            advancedReportsEnabled = true,
            financeEnabled = true,
            notificationsEnabled = true,
            multiStoreEnabled = true
        )

        val ENTERPRISE = SubscriptionPlan(
            planId = "ENTERPRISE",
            name = "Enterprise Plan",
            monthlyPrice = 25000.0,
            yearlyPrice = 250000.0,
            maxStores = Int.MAX_VALUE,
            maxUsers = Int.MAX_VALUE,
            maxBikes = Int.MAX_VALUE,
            maxCustomers = Int.MAX_VALUE,
            reportsEnabled = true,
            advancedReportsEnabled = true,
            financeEnabled = true,
            notificationsEnabled = true,
            multiStoreEnabled = true
        )

        val ALL_PLANS = listOf(STARTER, PROFESSIONAL, BUSINESS, ENTERPRISE)

        fun getPlanById(planId: String): SubscriptionPlan {
            return ALL_PLANS.find { it.planId.equals(planId, ignoreCase = true) } ?: STARTER
        }
    }
}
