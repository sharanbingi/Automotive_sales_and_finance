package com.automotive.salesfinance.data

import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus

object FeatureAccessManager {

    fun isSubscriptionActive(status: SubscriptionStatus?): Boolean {
        if (status == null) return true
        return when (status) {
            SubscriptionStatus.ACTIVE,
            SubscriptionStatus.TRIAL -> true
            SubscriptionStatus.PAST_DUE,
            SubscriptionStatus.EXPIRED,
            SubscriptionStatus.SUSPENDED,
            SubscriptionStatus.CANCELLED -> false
        }
    }

    fun isSubscriptionActive(dealership: Dealership?): Boolean {
        val target = dealership ?: TenantContext.currentDealership.value
        if (target != null) {
            if (!isSubscriptionActive(target.subscriptionStatus)) return false
            if (target.subscriptionStatus == SubscriptionStatus.TRIAL) {
                val now = System.currentTimeMillis()
                return target.trialEndDate == 0L || target.trialEndDate >= now
            }
            return true
        }
        val status = TenantContext.subscriptionStatus.value
        return isSubscriptionActive(status)
    }

    fun canAddStore(currentStoreCount: Int, plan: SubscriptionPlan): Boolean {
        return currentStoreCount < plan.maxStores
    }

    fun canAddUser(currentUserCount: Int, plan: SubscriptionPlan): Boolean {
        return currentUserCount < plan.maxUsers
    }

    fun canAddBike(currentBikeCount: Int, plan: SubscriptionPlan): Boolean {
        return currentBikeCount < plan.maxBikes
    }

    fun canAddVehicle(currentVehicleCount: Int, plan: SubscriptionPlan): Boolean {
        return currentVehicleCount < plan.maxBikes
    }

    fun canAddCustomer(currentCustomerCount: Int, plan: SubscriptionPlan): Boolean {
        return currentCustomerCount < plan.maxCustomers
    }

    fun hasFeature(featureKey: String, plan: SubscriptionPlan): Boolean {
        return when (featureKey.lowercase()) {
            "reports" -> plan.reportsEnabled
            "advanced_reports", "advancedreports" -> plan.advancedReportsEnabled
            "finance" -> plan.financeEnabled
            "notifications" -> plan.notificationsEnabled
            "multistore", "multi_store" -> plan.multiStoreEnabled
            else -> true
        }
    }

    fun hasAdvancedReports(plan: SubscriptionPlan): Boolean = plan.advancedReportsEnabled
    fun hasMultiStore(plan: SubscriptionPlan): Boolean = plan.multiStoreEnabled
    fun hasFinance(plan: SubscriptionPlan): Boolean = plan.financeEnabled
    fun hasNotifications(plan: SubscriptionPlan): Boolean = plan.notificationsEnabled
}
