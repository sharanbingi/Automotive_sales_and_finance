package com.automotive.salesfinance.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.BuildConfig
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.FeatureAccessManager
import com.automotive.salesfinance.model.AuditLog
import com.automotive.salesfinance.model.BillingCycle
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.SupportTicket
import com.automotive.salesfinance.model.TicketStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.SubscriptionRepository
import com.automotive.salesfinance.repository.SupportTicketRepository
import com.automotive.salesfinance.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private fun safeLogD(tag: String, msg: String) {
    try {
        Log.d(tag, msg)
    } catch (_: Throwable) {
        println("[$tag] $msg")
    }
}

data class PlatformStats(
    val totalDealerships: Int = 0,
    val activeDealerships: Int = 0,
    val trialDealerships: Int = 0,
    val expiredDealerships: Int = 0,
    val suspendedDealerships: Int = 0,
    val totalStores: Int = 0,
    val totalUsers: Int = 0,
    val totalMRR: Double = 0.0,
    val newSubscriptionsCount: Int = 0
)

class SuperAdminViewModel(
    private val authRepository: AuthRepository,
    private val dealershipRepository: DealershipRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val supportTicketRepository: SupportTicketRepository,
    private val auditLogRepository: AuditLogRepository,
    private val storeRepository: StoreRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
    val isDemoMode: StateFlow<Boolean> = authRepository.isDemoMode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow(true)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _plans = MutableStateFlow<List<SubscriptionPlan>>(SubscriptionPlan.ALL_PLANS)
    val plans: StateFlow<List<SubscriptionPlan>> = _plans.asStateFlow()

    val platformStats: StateFlow<PlatformStats> = combine(
        dealershipRepository.dealershipsFlow,
        subscriptionRepository.subscriptionsFlow,
        storeRepository.storesFlow,
        userRepository.usersFlow
    ) { dealerships, subscriptions, stores, users ->
        val total = dealerships.size
        val active = dealerships.count { it.active && it.subscriptionStatus == SubscriptionStatus.ACTIVE }
        val trial = dealerships.count { it.subscriptionStatus == SubscriptionStatus.TRIAL }
        val expired = dealerships.count { it.subscriptionStatus == SubscriptionStatus.EXPIRED || it.subscriptionStatus == SubscriptionStatus.PAST_DUE }
        val suspended = dealerships.count { !it.active || it.subscriptionStatus == SubscriptionStatus.SUSPENDED }

        val mrr = subscriptions.filter { it.status == SubscriptionStatus.ACTIVE }.sumOf { sub ->
            if (sub.billingCycle == BillingCycle.YEARLY) {
                sub.price / 12.0
            } else {
                sub.price
            }
        }

        PlatformStats(
            totalDealerships = total,
            activeDealerships = active,
            trialDealerships = trial,
            expiredDealerships = expired,
            suspendedDealerships = suspended,
            totalStores = stores.size,
            totalUsers = users.size,
            totalMRR = mrr,
            newSubscriptionsCount = subscriptions.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlatformStats())

    val filteredDealerships: StateFlow<List<Dealership>> = combine(
        dealershipRepository.dealershipsFlow,
        _searchQuery,
        _statusFilter
    ) { list, query, filter ->
        list.filter { d ->
            val matchesQuery = query.isBlank() ||
                    d.name.contains(query, ignoreCase = true) ||
                    d.legalName.contains(query, ignoreCase = true) ||
                    d.city.contains(query, ignoreCase = true) ||
                    d.dealershipId.contains(query, ignoreCase = true)

            val matchesFilter = when (filter.uppercase()) {
                "ALL" -> true
                "ACTIVE" -> d.active && d.subscriptionStatus == SubscriptionStatus.ACTIVE
                "TRIAL" -> d.subscriptionStatus == SubscriptionStatus.TRIAL
                "EXPIRED" -> d.subscriptionStatus == SubscriptionStatus.EXPIRED || d.subscriptionStatus == SubscriptionStatus.PAST_DUE
                "SUSPENDED" -> !d.active || d.subscriptionStatus == SubscriptionStatus.SUSPENDED
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supportTickets: StateFlow<List<SupportTicket>> = supportTicketRepository.supportTicketsFlow
    val auditLogs: StateFlow<List<AuditLog>> = auditLogRepository.auditLogsFlow

    fun loadPlatformData() {
        safeLogD("ASF_RUNTIME", "LOAD PLATFORM DATA START | isDemoMode = ${authRepository.isDemoMode.value}")
        val currentUser = authRepository.currentUser.value
        if (currentUser?.role != UserRole.SUPER_ADMIN && !authRepository.isDemoMode.value) {
            safeLogD("SuperAdminViewModel", "SUPER ADMIN LOAD SKIPPED | role = ${currentUser?.role}")
            return
        }
        viewModelScope.launch {
            if (!authRepository.isDemoMode.value) {
                dealershipRepository.fetchDealershipsFromFirestore()
                subscriptionRepository.fetchSubscriptionsFromFirestore()
                storeRepository.fetchStoresFromFirestore("")
                userRepository.fetchUsersFromFirestore("")
                supportTicketRepository.fetchSupportTicketsFromFirestore()
                auditLogRepository.fetchAuditLogsFromFirestore("")
            }
        }
    }

    init {
        safeLogD("ASF_RUNTIME", "SUPER ADMIN VM CREATED | isDemoMode = ${authRepository.isDemoMode.value} | dealerships = ${filteredDealerships.value.size} | IDs = ${filteredDealerships.value.map { it.dealershipId }} | subscriptions = ${subscriptionRepository.subscriptionsFlow.value.size}")
        loadPlatformData()
        viewModelScope.launch {
            authRepository.isDemoMode.collect {
                val currentUser = authRepository.currentUser.value
                if (currentUser?.role == UserRole.SUPER_ADMIN) {
                    loadPlatformData()
                }
            }
        }
        viewModelScope.launch {
            combine(filteredDealerships, subscriptionRepository.subscriptionsFlow, platformStats) { dealerships, subscriptions, stats ->
                Triple(dealerships, subscriptions, stats)
            }.collect { (dealerships, subscriptions, stats) ->
                safeLogD("ASF_RUNTIME", "FINAL SUPER ADMIN STATE | isDemoMode = ${authRepository.isDemoMode.value} | dealerships = ${dealerships.size} | IDs = ${dealerships.map { it.dealershipId }} | subscriptions = ${subscriptions.size} | MRR = ${stats.totalMRR}")
                if (BuildConfig.DEBUG) {
                    val isDemo = authRepository.isDemoMode.value
                    val dealershipIds = dealerships.map { it.dealershipId }
                    try {
                        Log.d("SuperAdminViewModel", "isDemoMode = $isDemo, dealerships = $dealershipIds, MRR = ${stats.totalMRR}")
                    } catch (_: Throwable) {
                        println("[SuperAdminViewModel] isDemoMode = $isDemo, dealerships = $dealershipIds, MRR = ${stats.totalMRR}")
                    }
                    if (!isDemo && dealershipIds.contains("dealership_demo_001")) {
                        try {
                            Log.e("SuperAdminViewModel", "CRITICAL ERROR: Demo data leaked into production SuperAdmin view!")
                        } catch (_: Throwable) {
                            System.err.println("[SuperAdminViewModel] CRITICAL ERROR: Demo data leaked into production SuperAdmin view!")
                        }
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun normalizeDealershipId(name: String): String {
        val normalized = name.trim().lowercase()
            .replace("[^a-z0-9\\s_]".toRegex(), "")
            .trim()
            .replace("\\s+".toRegex(), "_")
        return normalized.ifBlank { "dealership_${System.currentTimeMillis()}" }
    }

    fun createDealership(
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
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            if (user?.role != UserRole.SUPER_ADMIN) {
                _errorMessage.value = "Unauthorized: Only Super Admin can create dealerships"
                _isSuccess.value = false
                return@launch
            }

            if (name.isBlank()) {
                _errorMessage.value = "Dealership Name is required"
                _isSuccess.value = false
                return@launch
            }
            if (legalName.isBlank()) {
                _errorMessage.value = "Legal Name is required"
                _isSuccess.value = false
                return@launch
            }
            if (email.isBlank()) {
                _errorMessage.value = "Contact Email is required"
                _isSuccess.value = false
                return@launch
            }
            if (phone.isBlank()) {
                _errorMessage.value = "Phone is required"
                _isSuccess.value = false
                return@launch
            }
            if (address.isBlank()) {
                _errorMessage.value = "Address is required"
                _isSuccess.value = false
                return@launch
            }
            if (city.isBlank()) {
                _errorMessage.value = "City is required"
                _isSuccess.value = false
                return@launch
            }
            if (state.isBlank()) {
                _errorMessage.value = "State is required"
                _isSuccess.value = false
                return@launch
            }
            if (supportedVehicleTypes.isEmpty()) {
                _errorMessage.value = "At least one supported vehicle type is required"
                _isSuccess.value = false
                return@launch
            }
            if (trialDays < 0) {
                _errorMessage.value = "Trial days must be 0 or greater"
                _isSuccess.value = false
                return@launch
            }
            if (storeName.isBlank()) {
                _errorMessage.value = "Primary store name is required"
                _isSuccess.value = false
                return@launch
            }
            if (storeCity.isBlank()) {
                _errorMessage.value = "Primary store city is required"
                _isSuccess.value = false
                return@launch
            }
            if (storeState.isBlank()) {
                _errorMessage.value = "Primary store state is required"
                _isSuccess.value = false
                return@launch
            }
            if (adminName.isBlank()) {
                _errorMessage.value = "Admin name is required"
                _isSuccess.value = false
                return@launch
            }
            if (adminEmail.isBlank()) {
                _errorMessage.value = "Admin email is required"
                _isSuccess.value = false
                return@launch
            }

            _isLoading.value = true
            _errorMessage.value = null

            val dealershipId = normalizeDealershipId(name)

            val result = dealershipRepository.createDealershipAtomic(
                dealershipId = dealershipId,
                name = name.trim(),
                legalName = legalName.trim(),
                email = email.trim(),
                phone = phone.trim(),
                address = address.trim(),
                city = city.trim(),
                state = state.trim(),
                supportedVehicleTypes = supportedVehicleTypes,
                subscriptionPlan = subscriptionPlan,
                trialDays = trialDays,
                storeName = storeName.trim(),
                storeCity = storeCity.trim(),
                storeState = storeState.trim().uppercase(),
                storeAddress = storeAddress.ifBlank { address }.trim(),
                adminName = adminName.trim(),
                adminEmail = adminEmail.trim(),
                adminPhone = adminPhone.trim(),
                subscriptionRepository = subscriptionRepository,
                storeRepository = storeRepository,
                userRepository = userRepository,
                auditLogRepository = auditLogRepository
            )

            _isLoading.value = false

            if (result.isSuccess) {
                val created = result.getOrNull()
                val dealershipName = created?.name ?: name.trim()
                _successMessage.value = "Dealership '$dealershipName' created successfully"
                _isSuccess.value = true
                loadPlatformData()
                onSuccess()
            } else {
                val exception = result.exceptionOrNull()
                _errorMessage.value = exception?.message ?: "Failed to create dealership"
                _isSuccess.value = false
            }
        }
    }

    fun saveStore(store: Store, onSuccess: () -> Unit = {}) {
        saveStore(
            dealershipId = store.dealershipId,
            storeName = store.storeName,
            stateCode = store.stateCode,
            city = store.city,
            address = store.address,
            active = store.active,
            storeId = store.storeId,
            onSuccess = onSuccess
        )
    }

    fun saveStore(
        dealershipId: String,
        storeName: String,
        stateCode: String,
        city: String,
        address: String,
        active: Boolean = true,
        storeId: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (storeName.isBlank() || stateCode.isBlank() || city.isBlank() || dealershipId.isBlank()) {
                _errorMessage.value = "Please fill in all store details"
                _isSuccess.value = false
                return@launch
            }

            val dealership = dealershipRepository.getDealershipById(dealershipId)
                ?: dealershipRepository.dealershipsFlow.value.find { it.dealershipId == dealershipId }
                ?: if (authRepository.isDemoMode.value) DemoData.dealerships.find { it.dealershipId == dealershipId } else null

            if (dealership == null) {
                _errorMessage.value = "Dealership not found"
                _isSuccess.value = false
                return@launch
            }

            val plan = SubscriptionPlan.getPlanById(dealership.subscriptionPlan)

            val currentStores = storeRepository.getStoresForDealership(dealershipId)
            val generatedStoreId = storeId.ifBlank {
                "${stateCode.trim().uppercase()}_${city.trim().replace("\\s+".toRegex(), "")}_${System.currentTimeMillis() % 1000}"
            }
            val isNewStore = currentStores.none { it.storeId == generatedStoreId }

            if (isNewStore) {
                if (!FeatureAccessManager.canAddStore(currentStores.size, plan)) {
                    _errorMessage.value = "Plan limit reached: Store limit reached for current plan (${plan.maxStores} max stores). Please upgrade."
                    _isSuccess.value = false
                    return@launch
                }
            }

            _isLoading.value = true
            val newStore = Store(
                storeId = generatedStoreId,
                dealershipId = dealershipId,
                storeName = storeName.trim(),
                stateCode = stateCode.trim().uppercase(),
                city = city.trim(),
                address = address.trim(),
                active = active
            )

            val result = storeRepository.saveStore(newStore)
            _isLoading.value = false
            if (result.isSuccess) {
                logAudit("SAVE_STORE", "Store", newStore.storeId, mapOf("storeName" to newStore.storeName, "dealershipId" to dealershipId))
                _errorMessage.value = null
                _successMessage.value = "Store ${newStore.storeName} saved successfully"
                _isSuccess.value = true
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to save store"
                _isSuccess.value = false
            }
        }
    }

    fun updatePlan(updatedPlan: SubscriptionPlan) {
        val currentList = _plans.value.toMutableList()
        val index = currentList.indexOfFirst { it.planId == updatedPlan.planId }
        if (index >= 0) {
            currentList[index] = updatedPlan
            _plans.value = currentList
            logAudit(
                action = "UPDATE_PLAN_DEFINITIONS",
                entityType = "SubscriptionPlan",
                entityId = updatedPlan.planId,
                metadata = mapOf(
                    "planName" to updatedPlan.name,
                    "monthlyPrice" to updatedPlan.monthlyPrice.toString(),
                    "yearlyPrice" to updatedPlan.yearlyPrice.toString(),
                    "maxStores" to updatedPlan.maxStores.toString(),
                    "maxUsers" to updatedPlan.maxUsers.toString(),
                    "maxBikes" to updatedPlan.maxBikes.toString(),
                    "maxCustomers" to updatedPlan.maxCustomers.toString()
                )
            )
        }
    }

    fun activateDealership(dealershipId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val d = dealershipRepository.getDealershipById(dealershipId)
            if (d == null) {
                _isLoading.value = false
                _errorMessage.value = "Dealership not found"
                _isSuccess.value = false
                return@launch
            }
            val updated = d.copy(
                active = true,
                subscriptionStatus = SubscriptionStatus.ACTIVE,
                updatedAt = System.currentTimeMillis()
            )
            dealershipRepository.saveDealership(updated)
            logAudit("ACTIVATE_DEALERSHIP", "Dealership", dealershipId, mapOf("name" to d.name))
            _isLoading.value = false
            _isSuccess.value = true
        }
    }

    fun suspendDealership(dealershipId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val d = dealershipRepository.getDealershipById(dealershipId)
            if (d == null) {
                _isLoading.value = false
                _errorMessage.value = "Dealership not found"
                _isSuccess.value = false
                return@launch
            }
            val updated = d.copy(
                active = false,
                subscriptionStatus = SubscriptionStatus.SUSPENDED,
                updatedAt = System.currentTimeMillis()
            )
            dealershipRepository.saveDealership(updated)
            logAudit("SUSPEND_DEALERSHIP", "Dealership", dealershipId, mapOf("name" to d.name))
            _isLoading.value = false
            _isSuccess.value = true
        }
    }

    fun extendTrial(dealershipId: String, additionalDays: Int = 14) {
        viewModelScope.launch {
            _isLoading.value = true
            val d = dealershipRepository.getDealershipById(dealershipId)
            if (d == null) {
                _isLoading.value = false
                _errorMessage.value = "Dealership not found"
                _isSuccess.value = false
                return@launch
            }
            val now = System.currentTimeMillis()
            val baseTime = if (d.trialEndDate > now) d.trialEndDate else now
            val newTrialEnd = baseTime + (additionalDays.toLong() * 24 * 60 * 60 * 1000)

            val updated = d.copy(
                active = true,
                subscriptionStatus = SubscriptionStatus.TRIAL,
                trialEndDate = newTrialEnd,
                updatedAt = now
            )
            dealershipRepository.saveDealership(updated)
            logAudit("EXTEND_TRIAL", "Dealership", dealershipId, mapOf("extendedDays" to additionalDays.toString()))
            _isLoading.value = false
            _isSuccess.value = true
        }
    }

    fun updateDealershipPlan(dealershipId: String, newPlanId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            dealershipRepository.updateSubscriptionPlan(dealershipId, newPlanId, SubscriptionStatus.ACTIVE)
            subscriptionRepository.upgradePlan(dealershipId, newPlanId)
            logAudit("UPDATE_PLAN", "Dealership", dealershipId, mapOf("newPlanId" to newPlanId))
            _isLoading.value = false
            _isSuccess.value = true
        }
    }

    fun updateSupportedVehicleTypes(dealershipId: String, types: List<VehicleType>) {
        viewModelScope.launch {
            _isLoading.value = true
            val d = dealershipRepository.getDealershipById(dealershipId)
            if (d != null) {
                val updated = d.copy(
                    supportedVehicleTypes = types.ifEmpty { listOf(VehicleType.BIKE) },
                    updatedAt = System.currentTimeMillis()
                )
                dealershipRepository.saveDealership(updated)
                logAudit("UPDATE_SUPPORTED_VEHICLE_TYPES", "Dealership", dealershipId, mapOf("types" to types.joinToString()))
                _successMessage.value = "Updated supported vehicle types for ${d.name}"
                _isSuccess.value = true
            }
            _isLoading.value = false
        }
    }

    fun updateTicketStatus(ticketId: String, newStatus: TicketStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            supportTicketRepository.updateTicketStatus(ticketId, newStatus)
            logAudit("UPDATE_TICKET_STATUS", "SupportTicket", ticketId, mapOf("status" to newStatus.name))
            _isLoading.value = false
            _isSuccess.value = true
        }
    }

    fun postAdminResponse(ticketId: String, responseText: String, newStatus: TicketStatus? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = supportTicketRepository.addAdminResponse(ticketId, responseText, newStatus)
            _isLoading.value = false
            if (res.isSuccess) {
                _isSuccess.value = true
                logAudit(
                    action = "POST_ADMIN_RESPONSE",
                    entityType = "SupportTicket",
                    entityId = ticketId,
                    metadata = mapOf("status" to (newStatus?.name ?: "UNCHANGED"))
                )
            } else {
                _errorMessage.value = res.exceptionOrNull()?.message ?: "Failed to post response"
                _isSuccess.value = false
            }
        }
    }

    private fun logAudit(action: String, entityType: String, entityId: String, metadata: Map<String, String>) {
        viewModelScope.launch {
            val uid = currentUser.value?.uid ?: "SUPER_ADMIN"
            val dId = currentUser.value?.dealershipId ?: "SYSTEM"
            auditLogRepository.logEvent(dId, uid, action, entityType, entityId, metadata)
        }
    }
}
