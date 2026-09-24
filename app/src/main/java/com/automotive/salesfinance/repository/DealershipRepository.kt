package com.automotive.salesfinance.repository

import android.util.Log
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.AuditLog
import com.automotive.salesfinance.model.BillingCycle
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.Subscription
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.model.VehicleType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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

open class DealershipRepository(private val authRepository: AuthRepository) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _dealershipsFlow = MutableStateFlow<List<Dealership>>(
        if (authRepository.isDemoMode.value) DemoData.dealerships else emptyList()
    )
    val dealershipsFlow: StateFlow<List<Dealership>> = _dealershipsFlow.asStateFlow()

    fun clearInMemoryState() {
        _dealershipsFlow.value = emptyList()
    }

    init {
        authRepository.registerClearListener {
            clearInMemoryState()
        }
        safeLogD("ASF_RUNTIME", "DEALERSHIP REPO CREATED | isDemoMode = ${authRepository.isDemoMode.value} | initial count = ${_dealershipsFlow.value.size} | IDs = ${_dealershipsFlow.value.map { it.dealershipId }}")
        CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch {
            authRepository.isDemoMode.collect { isDemo ->
                safeLogD("ASF_RUNTIME", "DEALERSHIP REPO MODE CHANGED = $isDemo")
                if (!isDemo) {
                    _dealershipsFlow.value = _dealershipsFlow.value.filterNot { d ->
                        DemoData.dealerships.any { it.dealershipId == d.dealershipId }
                    }
                    safeLogD("ASF_RUNTIME", "DEALERSHIPS AFTER LIVE RESET | count = ${_dealershipsFlow.value.size} | IDs = ${_dealershipsFlow.value.map { it.dealershipId }}")
                } else if (_dealershipsFlow.value.isEmpty()) {
                    _dealershipsFlow.value = DemoData.dealerships
                }
            }
        }
    }

    open suspend fun fetchDealershipsFromFirestore(): List<Dealership> {
        if (authRepository.isDemoMode.value) {
            return _dealershipsFlow.value
        }
        if (firestore == null) {
            _dealershipsFlow.value = _dealershipsFlow.value.filterNot { d -> DemoData.dealerships.any { it.dealershipId == d.dealershipId } }
            return _dealershipsFlow.value
        }
        return try {
            val snapshot = firestore?.collection("dealerships")?.limit(100)?.get()?.await()
            val fetched = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(Dealership::class.java)
                } catch (_: Exception) {
                    null
                }
            } ?: emptyList()

            safeLogD("ASF_RUNTIME", "FIRESTORE DEALERSHIP RESULT COUNT = ${fetched.size} | IDs = ${fetched.map { it.dealershipId }}")
            _dealershipsFlow.value = fetched
            fetched
        } catch (e: Exception) {
            safeLogD("DealershipRepository", "DEALERSHIP FETCH FAILED | exception = ${e.javaClass.simpleName} | code = ${(e as? FirebaseFirestoreException)?.code} | message = ${e.message}")
            val liveOnly = _dealershipsFlow.value.filterNot { d -> DemoData.dealerships.any { it.dealershipId == d.dealershipId } }
            _dealershipsFlow.value = liveOnly
            liveOnly
        }
    }

    open suspend fun fetchSingleDealershipFromFirestore(dealershipId: String): Dealership? {
        if (dealershipId.isBlank()) return null
        if (authRepository.isDemoMode.value) {
            return _dealershipsFlow.value.find { it.dealershipId == dealershipId }
        }
        if (firestore == null) {
            safeLogD("DealershipRepository", "FETCH SINGLE DEALERSHIP FAILED | firestore is null | id = $dealershipId")
            return null
        }
        return try {
            val docSnap = firestore?.collection("dealerships")?.document(dealershipId)?.get()?.await()
            if (docSnap != null && docSnap.exists()) {
                val fetched = docSnap.toObject(Dealership::class.java)
                if (fetched != null) {
                    _dealershipsFlow.value = listOf(fetched)
                    fetched
                } else {
                    safeLogD("DealershipRepository", "FETCH SINGLE DEALERSHIP NULL DESERIALIZATION | id = $dealershipId")
                    null
                }
            } else {
                safeLogD("DealershipRepository", "FETCH SINGLE DEALERSHIP NOT FOUND | id = $dealershipId")
                null
            }
        } catch (e: Exception) {
            safeLogD("DealershipRepository", "FETCH SINGLE DEALERSHIP FAILED | exception = ${e.javaClass.simpleName} | code = ${(e as? FirebaseFirestoreException)?.code} | message = ${e.message}")
            null
        }
    }

    suspend fun getDealershipById(dealershipId: String): Dealership? {
        if (!authRepository.isDemoMode.value) {
            _dealershipsFlow.value = _dealershipsFlow.value.filterNot { d -> DemoData.dealerships.any { it.dealershipId == d.dealershipId } }
        }
        if (dealershipId.isBlank()) {
            if (!authRepository.isDemoMode.value) return null
            val targetId = DemoData.DEMO_DEALERSHIP_ID
            return _dealershipsFlow.value.find { it.dealershipId == targetId }
        }
        if (authRepository.isDemoMode.value || firestore == null) {
            return _dealershipsFlow.value.find { it.dealershipId == dealershipId }
        }
        return try {
            val snapshot = firestore?.collection("dealerships")?.document(dealershipId)?.get()?.await()
            val fetched = snapshot?.toObject(Dealership::class.java)
            if (fetched != null) {
                val currentList = _dealershipsFlow.value.toMutableList()
                val index = currentList.indexOfFirst { it.dealershipId == fetched.dealershipId }
                if (index >= 0) {
                    currentList[index] = fetched
                } else {
                    currentList.add(fetched)
                }
                _dealershipsFlow.value = currentList
                fetched
            } else {
                _dealershipsFlow.value.find { it.dealershipId == dealershipId }
            }
        } catch (e: Exception) {
            _dealershipsFlow.value.find { it.dealershipId == dealershipId }
        }
    }

    suspend fun getDealershipForUser(user: User?): Dealership? {
        val dId = user?.dealershipId.orEmpty()
        if (dId.isBlank()) {
            if (!authRepository.isDemoMode.value) return null
            return getDealershipById(DemoData.DEMO_DEALERSHIP_ID)
        }
        return getDealershipById(dId)
    }

    suspend fun saveDealership(dealership: Dealership): Result<Unit> {
        if (dealership.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }
        val currentList = _dealershipsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.dealershipId == dealership.dealershipId }
        if (index >= 0) {
            currentList[index] = dealership
        } else {
            currentList.add(dealership)
        }
        _dealershipsFlow.value = currentList

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                firestore?.collection("dealerships")?.document(dealership.dealershipId)?.set(dealership)?.await()
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }

    suspend fun activateDealership(dealershipId: String): Result<Unit> {
        val dealership = getDealershipById(dealershipId) ?: return Result.failure(Exception("Dealership not found"))
        val updated = dealership.copy(
            active = true,
            subscriptionStatus = SubscriptionStatus.ACTIVE,
            updatedAt = System.currentTimeMillis()
        )
        return saveDealership(updated)
    }

    suspend fun suspendDealership(dealershipId: String): Result<Unit> {
        val dealership = getDealershipById(dealershipId) ?: return Result.failure(Exception("Dealership not found"))
        val updated = dealership.copy(
            active = false,
            subscriptionStatus = SubscriptionStatus.SUSPENDED,
            updatedAt = System.currentTimeMillis()
        )
        return saveDealership(updated)
    }

    suspend fun updateSubscriptionPlan(
        dealershipId: String,
        newPlanId: String,
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE
    ): Result<Unit> {
        if (dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }
        val dealership = getDealershipById(dealershipId) ?: return Result.failure(Exception("Dealership not found"))
        val updated = dealership.copy(
            subscriptionPlan = newPlanId,
            subscriptionStatus = status,
            updatedAt = System.currentTimeMillis()
        )
        return saveDealership(updated)
    }

    fun getEffectiveTrialEndDate(dealership: Dealership): Long {
        if (dealership.trialEndDate > 0L) return dealership.trialEndDate
        val start = if (dealership.trialStartDate > 0L) dealership.trialStartDate else dealership.createdAt
        val base = if (start > 0L) start else System.currentTimeMillis()
        return base + (14L * 24 * 60 * 60 * 1000)
    }

    fun isTrialActive(dealership: Dealership): Boolean {
        if (dealership.subscriptionStatus != SubscriptionStatus.TRIAL) return false
        val now = System.currentTimeMillis()
        val trialEnd = getEffectiveTrialEndDate(dealership)
        return trialEnd >= now
    }

    fun getDaysRemainingInTrial(dealership: Dealership): Int {
        if (dealership.subscriptionStatus != SubscriptionStatus.TRIAL) return 0
        val now = System.currentTimeMillis()
        val trialEnd = getEffectiveTrialEndDate(dealership)
        val diff = trialEnd - now
        return if (diff > 0) (diff / (24 * 60 * 60 * 1000)).toInt() else 0
    }

    suspend fun createDealershipAtomic(
        dealership: Dealership,
        primaryStore: Store,
        adminUser: User,
        subscription: Subscription,
        auditLog: AuditLog,
        subscriptionRepository: SubscriptionRepository? = null,
        storeRepository: StoreRepository? = null,
        userRepository: UserRepository? = null,
        auditLogRepository: AuditLogRepository? = null
    ): Result<Dealership> {
        val targetId = dealership.dealershipId
        if (targetId.isBlank()) {
            return Result.failure(IllegalArgumentException("Dealership ID cannot be blank"))
        }

        if (_dealershipsFlow.value.any { it.dealershipId == targetId }) {
            return Result.failure(IllegalArgumentException("Dealership ID '$targetId' already exists"))
        }

        val isLiveMode = !authRepository.isDemoMode.value && firestore != null

        if (isLiveMode) {
            val fs = firestore!!
            try {
                val existingDoc = fs.collection("dealerships").document(targetId).get().await()
                if (existingDoc != null && existingDoc.exists()) {
                    return Result.failure(IllegalArgumentException("Dealership ID '$targetId' already exists"))
                }

                val batch = fs.batch()

                val dealershipRef = fs.collection("dealerships").document(targetId)
                val subRef = dealershipRef.collection("subscriptions").document(subscription.subscriptionId)
                val storeRef = dealershipRef.collection("stores").document(primaryStore.storeId)
                val userRef = dealershipRef.collection("users").document(adminUser.uid)
                val userRootRef = fs.collection("users").document(adminUser.uid)
                val auditRef = dealershipRef.collection("audit_logs").document(auditLog.auditLogId)

                batch.set(dealershipRef, dealership)
                batch.set(subRef, subscription)
                batch.set(storeRef, primaryStore)
                batch.set(userRef, adminUser)
                batch.set(userRootRef, adminUser)
                batch.set(auditRef, auditLog)

                batch.commit().await()
            } catch (e: Exception) {
                if (e is IllegalArgumentException) {
                    return Result.failure(e)
                }
                safeLogD("DealershipRepository", "Firestore batch creation error: ${e.message}")
                return Result.failure(e)
            }
        }

        val currentList = _dealershipsFlow.value.toMutableList()
        currentList.add(dealership)
        _dealershipsFlow.value = currentList

        subscriptionRepository?.saveSubscription(subscription)
        storeRepository?.saveStore(primaryStore)
        userRepository?.saveUser(adminUser)
        auditLogRepository?.saveAuditLog(auditLog)

        return Result.success(dealership)
    }

    suspend fun createDealershipAtomic(
        dealershipId: String,
        name: String,
        legalName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        state: String,
        supportedVehicleTypes: List<VehicleType>,
        subscriptionPlan: String = "STARTER",
        trialDays: Int = 14,
        storeName: String,
        storeCity: String,
        storeState: String,
        storeAddress: String = "",
        adminName: String,
        adminEmail: String,
        adminPhone: String = "",
        subscriptionRepository: SubscriptionRepository? = null,
        storeRepository: StoreRepository? = null,
        userRepository: UserRepository? = null,
        auditLogRepository: AuditLogRepository? = null
    ): Result<Dealership> {
        val now = System.currentTimeMillis()
        val trialEnd = now + (trialDays.toLong() * 24 * 60 * 60 * 1000L)

        val dealership = Dealership(
            dealershipId = dealershipId,
            name = name,
            legalName = legalName,
            email = email,
            phone = phone,
            address = address,
            city = city,
            state = state,
            supportedVehicleTypes = supportedVehicleTypes,
            subscriptionPlan = subscriptionPlan,
            subscriptionStatus = SubscriptionStatus.TRIAL,
            trialStartDate = now,
            trialEndDate = trialEnd,
            active = true,
            createdAt = now,
            updatedAt = now
        )

        val planObj = SubscriptionPlan.getPlanById(subscriptionPlan)
        val subId = "SUB_" + dealershipId + "_" + (now % 100000)
        val subscription = Subscription(
            subscriptionId = subId,
            dealershipId = dealershipId,
            planId = planObj.planId,
            planName = planObj.name,
            status = SubscriptionStatus.TRIAL,
            billingCycle = BillingCycle.MONTHLY,
            price = planObj.monthlyPrice,
            currency = "INR",
            startDate = now,
            endDate = trialEnd,
            trialStartDate = now,
            trialEndDate = trialEnd,
            nextBillingDate = trialEnd,
            provider = "RAZORPAY",
            createdAt = now,
            updatedAt = now
        )

        val storeId = "STORE_" + dealershipId + "_PRIMARY"
        val store = Store(
            storeId = storeId,
            dealershipId = dealershipId,
            storeName = storeName,
            stateCode = storeState,
            city = storeCity,
            address = storeAddress.ifBlank { address },
            active = true
        )

        val adminId = "USER_" + dealershipId + "_ADMIN"
        val adminUser = User(
            uid = adminId,
            dealershipId = dealershipId,
            name = adminName,
            email = adminEmail,
            role = UserRole.DEALERSHIP_ADMIN,
            stateCode = storeState,
            storeId = storeId,
            active = false
        )

        val auditId = "AUDIT_" + dealershipId + "_" + (now % 100000)
        val auditLog = AuditLog(
            auditLogId = auditId,
            dealershipId = dealershipId,
            userId = "SUPER_ADMIN",
            action = "CREATE_DEALERSHIP",
            entityType = "Dealership",
            entityId = dealershipId,
            timestamp = now,
            metadata = mapOf(
                "name" to name,
                "legalName" to legalName,
                "plan" to subscriptionPlan,
                "adminEmail" to adminEmail,
                "storeName" to storeName
            )
        )

        return createDealershipAtomic(
            dealership = dealership,
            primaryStore = store,
            adminUser = adminUser,
            subscription = subscription,
            auditLog = auditLog,
            subscriptionRepository = subscriptionRepository,
            storeRepository = storeRepository,
            userRepository = userRepository,
            auditLogRepository = auditLogRepository
        )
    }
}
