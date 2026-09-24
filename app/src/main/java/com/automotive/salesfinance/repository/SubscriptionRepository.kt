package com.automotive.salesfinance.repository

import android.util.Log
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.BillingCycle
import com.automotive.salesfinance.model.Subscription
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun safeLogD(tag: String, msg: String) {
    try {
        Log.d(tag, msg)
    } catch (_: Throwable) {
        println("[$tag] $msg")
    }
}

class SubscriptionRepository(private val authRepository: AuthRepository) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _subscriptionsFlow = MutableStateFlow<List<Subscription>>(
        if (authRepository.isDemoMode.value) DemoData.subscriptions else emptyList()
    )
    val subscriptionsFlow: StateFlow<List<Subscription>> = _subscriptionsFlow.asStateFlow()

    fun clearInMemoryState() {
        _subscriptionsFlow.value = emptyList()
    }

    init {
        authRepository.registerClearListener {
            clearInMemoryState()
        }
        safeLogD("ASF_RUNTIME", "SUBSCRIPTION REPO CREATED | isDemoMode = ${authRepository.isDemoMode.value} | initial count = ${_subscriptionsFlow.value.size}")
        CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch {
            authRepository.isDemoMode.collect { isDemo ->
                safeLogD("ASF_RUNTIME", "SUBSCRIPTION REPO MODE CHANGED = $isDemo")
                if (!isDemo) {
                    _subscriptionsFlow.value = _subscriptionsFlow.value.filterNot { s ->
                        DemoData.subscriptions.any { it.subscriptionId == s.subscriptionId }
                    }
                    safeLogD("ASF_RUNTIME", "SUBSCRIPTIONS AFTER LIVE RESET | count = ${_subscriptionsFlow.value.size}")
                } else if (_subscriptionsFlow.value.isEmpty()) {
                    _subscriptionsFlow.value = DemoData.subscriptions
                }
            }
        }
    }

    suspend fun fetchSubscriptionsFromFirestore(): List<Subscription> {
        if (authRepository.isDemoMode.value) {
            return _subscriptionsFlow.value
        }
        if (firestore == null) {
            _subscriptionsFlow.value = _subscriptionsFlow.value.filterNot { s -> DemoData.subscriptions.any { it.subscriptionId == s.subscriptionId } }
            return _subscriptionsFlow.value
        }
        return try {
            val snapshot = firestore?.collection("subscriptions")?.limit(100)?.get()?.await()
            val fetched = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(Subscription::class.java)
                } catch (_: Exception) {
                    null
                }
            } ?: emptyList()

            safeLogD("ASF_RUNTIME", "FIRESTORE SUBSCRIPTION RESULT COUNT = ${fetched.size}")
            _subscriptionsFlow.value = fetched
            fetched
        } catch (e: Exception) {
            val liveOnly = _subscriptionsFlow.value.filterNot { s -> DemoData.subscriptions.any { it.subscriptionId == s.subscriptionId } }
            _subscriptionsFlow.value = liveOnly
            liveOnly
        }
    }

    fun getSubscriptionForDealership(dealershipId: String): Subscription? {
        if (!authRepository.isDemoMode.value) {
            _subscriptionsFlow.value = _subscriptionsFlow.value.filterNot { s -> DemoData.subscriptions.any { it.subscriptionId == s.subscriptionId } }
        }
        val targetId = if (dealershipId.isBlank()) {
            if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else return null
        } else dealershipId
        return _subscriptionsFlow.value.find { it.dealershipId == targetId && it.status == SubscriptionStatus.ACTIVE }
            ?: _subscriptionsFlow.value.find { it.dealershipId == targetId }
    }

    fun getSubscriptionHistory(dealershipId: String): List<Subscription> {
        if (!authRepository.isDemoMode.value) {
            _subscriptionsFlow.value = _subscriptionsFlow.value.filterNot { s -> DemoData.subscriptions.any { it.subscriptionId == s.subscriptionId } }
        }
        val targetId = if (dealershipId.isBlank()) {
            if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else return emptyList()
        } else dealershipId
        return _subscriptionsFlow.value.filter { it.dealershipId == targetId }
    }

    suspend fun upgradePlan(
        dealershipId: String,
        planId: String,
        billingCycle: BillingCycle = BillingCycle.MONTHLY
    ): Result<Subscription> {
        if (dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }
        val targetId = if (dealershipId.isBlank()) DemoData.DEMO_DEALERSHIP_ID else dealershipId
        val plan = SubscriptionPlan.getPlanById(planId)
        val price = if (billingCycle == BillingCycle.YEARLY) plan.yearlyPrice else plan.monthlyPrice
        val now = System.currentTimeMillis()
        val durationMs = if (billingCycle == BillingCycle.YEARLY) 365L * 24 * 60 * 60 * 1000 else 30L * 24 * 60 * 60 * 1000

        val newSubscription = Subscription(
            subscriptionId = "SUB_" + System.currentTimeMillis(),
            dealershipId = targetId,
            planId = plan.planId,
            planName = plan.name,
            status = SubscriptionStatus.ACTIVE,
            billingCycle = billingCycle,
            price = price,
            currency = "INR",
            startDate = now,
            endDate = now + durationMs,
            nextBillingDate = now + durationMs,
            provider = "RAZORPAY",
            createdAt = now,
            updatedAt = now
        )

        val currentList = _subscriptionsFlow.value.toMutableList()
        currentList.add(0, newSubscription)
        _subscriptionsFlow.value = currentList

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                firestore?.collection("subscriptions")
                    ?.document(newSubscription.subscriptionId)
                    ?.set(newSubscription)
                    ?.await()
            } catch (_: Exception) {}
        }

        return Result.success(newSubscription)
    }

    suspend fun renewSubscription(subscriptionId: String): Result<Subscription> {
        val currentList = _subscriptionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.subscriptionId == subscriptionId }
        if (index >= 0) {
            val old = currentList[index]
            val now = System.currentTimeMillis()
            val durationMs = if (old.billingCycle == BillingCycle.YEARLY) 365L * 24 * 60 * 60 * 1000 else 30L * 24 * 60 * 60 * 1000
            val updated = old.copy(
                status = SubscriptionStatus.ACTIVE,
                startDate = now,
                endDate = now + durationMs,
                nextBillingDate = now + durationMs,
                updatedAt = now
            )
            currentList[index] = updated
            _subscriptionsFlow.value = currentList

            if (!authRepository.isDemoMode.value && firestore != null) {
                try {
                    firestore?.collection("subscriptions")?.document(subscriptionId)?.set(updated)?.await()
                } catch (_: Exception) {}
            }
            return Result.success(updated)
        }
        return Result.failure(Exception("Subscription not found: $subscriptionId"))
    }

    suspend fun cancelSubscription(subscriptionId: String): Result<Unit> {
        val currentList = _subscriptionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.subscriptionId == subscriptionId }
        if (index >= 0) {
            val updated = currentList[index].copy(
                status = SubscriptionStatus.CANCELLED,
                updatedAt = System.currentTimeMillis()
            )
            currentList[index] = updated
            _subscriptionsFlow.value = currentList

            if (!authRepository.isDemoMode.value && firestore != null) {
                try {
                    firestore?.collection("subscriptions")?.document(subscriptionId)?.set(updated)?.await()
                } catch (_: Exception) {}
            }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Subscription not found: $subscriptionId"))
    }

    suspend fun saveSubscription(subscription: Subscription): Result<Unit> {
        val currentList = _subscriptionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.subscriptionId == subscription.subscriptionId }
        if (index >= 0) {
            currentList[index] = subscription
        } else {
            currentList.add(0, subscription)
        }
        _subscriptionsFlow.value = currentList

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                firestore?.collection("subscriptions")?.document(subscription.subscriptionId)?.set(subscription)?.await()
                firestore?.collection("dealerships")
                    ?.document(subscription.dealershipId)
                    ?.collection("subscriptions")
                    ?.document(subscription.subscriptionId)
                    ?.set(subscription)
                    ?.await()
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }
}
