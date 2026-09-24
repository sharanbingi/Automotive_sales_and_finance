package com.automotive.salesfinance.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.FeatureAccessManager
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.repository.UserRepository
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.utils.toRupeesDouble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DealershipAdminMetrics(
    val totalBikes: Int = 0,
    val availableBikes: Int = 0,
    val reservedBikes: Int = 0,
    val financedBikes: Int = 0,
    val totalCustomers: Int = 0,
    val activeLoansCount: Int = 0,
    val overdueLoansCount: Int = 0,
    val deadStockCount: Int = 0,
    val deadStockCapital: Double = 0.0,
    val todaysCollections: Double = 0.0,
    val totalStoresCount: Int = 0,
    val totalUsersCount: Int = 0
)

class DealershipAdminViewModel(
    private val authRepository: AuthRepository,
    private val dealershipRepository: DealershipRepository,
    private val storeRepository: StoreRepository,
    private val userRepository: UserRepository,
    private val inventoryRepository: InventoryRepository,
    private val loanRepository: LoanRepository,
    private val customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
    private val auditLogRepository: AuditLogRepository
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

    private val _targetDealershipId = MutableStateFlow<String?>(null)

    val currentDealership: StateFlow<Dealership?> = combine(
        authRepository.currentUser,
        dealershipRepository.dealershipsFlow,
        _targetDealershipId
    ) { user, dealerships, targetId ->
        val effId = targetId ?: user?.dealershipId.orEmpty()
        dealerships.find { it.dealershipId == effId }
            ?: if (authRepository.isDemoMode.value) dealerships.firstOrNull() else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentPlan: StateFlow<SubscriptionPlan> = currentDealership.map { dealership ->
        SubscriptionPlan.getPlanById(dealership?.subscriptionPlan ?: "STARTER")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionPlan.STARTER)

    val trialDaysRemaining: StateFlow<Int> = currentDealership.map { d ->
        if (d != null) dealershipRepository.getDaysRemainingInTrial(d) else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isTrialActive: StateFlow<Boolean> = currentDealership.map { d ->
        if (d != null) dealershipRepository.isTrialActive(d) else false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val stores: StateFlow<List<Store>> = combine(
        storeRepository.storesFlow,
        currentDealership
    ) { storesList, dealership ->
        val dId = dealership?.dealershipId ?: if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
        storesList.filter { it.dealershipId == dId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<User>> = combine(
        userRepository.usersFlow,
        currentDealership
    ) { usersList, dealership ->
        val dId = dealership?.dealershipId ?: if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
        usersList.filter { it.dealershipId == dId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metrics: StateFlow<DealershipAdminMetrics> = combine(
        currentDealership,
        inventoryRepository.bikesFlow,
        customerRepository.customersFlow,
        loanRepository.loansFlow,
        transactionRepository.transactionsFlow,
        stores,
        users
    ) { flows: Array<Any?> ->
        val dealership = flows[0] as? Dealership
        @Suppress("UNCHECKED_CAST")
        val bikes = flows[1] as List<Bike>
        @Suppress("UNCHECKED_CAST")
        val customers = flows[2] as List<Customer>
        @Suppress("UNCHECKED_CAST")
        val loans = flows[3] as List<Loan>
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[4] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val dStores = flows[5] as List<Store>
        @Suppress("UNCHECKED_CAST")
        val dUsers = flows[6] as List<User>

        val dId = dealership?.dealershipId ?: if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""

        val dBikes = bikes.filter { it.dealershipId == dId }
        val dCusts = customers.filter { it.dealershipId == dId }
        val dLoans = loans.filter { it.dealershipId == dId }
        val dTxns = transactions.filter { it.dealershipId == dId && it.status == TransactionStatus.SUCCESS }

        val avail = dBikes.count { it.status == BikeStatus.AVAILABLE }
        val res = dBikes.count { it.status == BikeStatus.RESERVED }
        val fin = dBikes.count { it.status == BikeStatus.FINANCED }

        val activeLoans = dLoans.filter { it.loanStatus == LoanStatus.ACTIVE }
        val overdueLoans = dLoans.filter {
            it.loanStatus == LoanStatus.OVERDUE ||
                    (it.loanStatus == LoanStatus.ACTIVE && it.nextEmiDate > 0 && it.nextEmiDate < System.currentTimeMillis())
        }

        val deadStock = dBikes.filter {
            it.status == BikeStatus.AVAILABLE && DateUtils.isDeadStock(it.inwardTimestamp, 60)
        }

        val todayStart = getTodayStartMillis()
        val todaysColl = dTxns.filter { it.createdAt >= todayStart }.sumOf { it.effectiveAmountPaise() }.toRupeesDouble()

        DealershipAdminMetrics(
            totalBikes = dBikes.size,
            availableBikes = avail,
            reservedBikes = res,
            financedBikes = fin,
            totalCustomers = dCusts.size,
            activeLoansCount = activeLoans.size,
            overdueLoansCount = overdueLoans.size,
            deadStockCount = deadStock.size,
            deadStockCapital = deadStock.sumOf { it.costPrice },
            todaysCollections = todaysColl,
            totalStoresCount = dStores.size,
            totalUsersCount = dUsers.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DealershipAdminMetrics())

    init {
        viewModelScope.launch {
            authRepository.currentUser
                .map { user ->
                    val targetId = _targetDealershipId.value
                    if (!targetId.isNullOrBlank()) {
                        targetId
                    } else if (user != null && user.dealershipId.isNotBlank()) {
                        user.dealershipId
                    } else {
                        ""
                    }
                }
                .distinctUntilChanged()
                .collectLatest { dealershipId ->
                    if (dealershipId.isNotBlank()) {
                        loadDataForDealership(dealershipId)
                    } else {
                        clearTenantData()
                    }
                }
        }
    }

    suspend fun loadDataForDealership(dealershipId: String) {
        if (dealershipId.isBlank()) return
        if (!authRepository.isDemoMode.value) {
            storeRepository.fetchStoresFromFirestore(dealershipId)
            userRepository.fetchUsersFromFirestore(dealershipId)
            dealershipRepository.fetchSingleDealershipFromFirestore(dealershipId)
            inventoryRepository.fetchVehiclesFromFirestore(dealershipId)
            loanRepository.fetchLoansFromFirestore(dealershipId)
            customerRepository.fetchCustomersFromFirestore(dealershipId)
            transactionRepository.fetchTransactionsFromFirestore(dealershipId)
            auditLogRepository.fetchAuditLogsFromFirestore(dealershipId)
        }
    }

    fun clearTenantData() {
        _errorMessage.value = null
        _successMessage.value = null
        storeRepository.clearInMemoryState()
        userRepository.clearInMemoryState()
        inventoryRepository.clearInMemoryState()
        loanRepository.clearInMemoryState()
        customerRepository.clearInMemoryState()
        transactionRepository.clearInMemoryState()
        auditLogRepository.clearInMemoryState()
        dealershipRepository.clearInMemoryState()
    }

    fun loadData() {
        val dId = _targetDealershipId.value ?: currentUser.value?.dealershipId.orEmpty()
        if (dId.isNotBlank()) {
            viewModelScope.launch {
                loadDataForDealership(dId)
            }
        } else {
            clearTenantData()
        }
    }

    fun setSelectedDealershipId(dealershipId: String?) {
        _targetDealershipId.value = dealershipId
        val effId = dealershipId ?: currentUser.value?.dealershipId.orEmpty()
        if (effId.isNotBlank()) {
            viewModelScope.launch {
                loadDataForDealership(effId)
            }
        } else {
            clearTenantData()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun saveStore(
        storeName: String,
        stateCode: String,
        city: String,
        address: String,
        active: Boolean = true,
        storeId: String = "",
        onSuccess: () -> Unit = {}
    ) {
        saveStore(
            Store(
                storeId = storeId,
                storeName = storeName,
                stateCode = stateCode,
                city = city,
                address = address,
                active = active
            ),
            onSuccess
        )
    }

    fun saveStore(store: Store, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (store.storeName.isBlank() || store.stateCode.isBlank() || store.city.isBlank()) {
                _errorMessage.value = "Please fill in all store details"
                _isSuccess.value = false
                return@launch
            }

            val dId = _targetDealershipId.value ?: currentUser.value?.dealershipId.orEmpty().ifBlank {
                if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
            }
            val dealership = dealershipRepository.dealershipsFlow.value.find { it.dealershipId == dId }
                ?: if (authRepository.isDemoMode.value) DemoData.dealerships.firstOrNull() else null
            val plan = SubscriptionPlan.getPlanById(dealership?.subscriptionPlan ?: "STARTER")

            if (!FeatureAccessManager.isSubscriptionActive(dealership)) {
                val err = "Subscription inactive: account is in read-only mode"
                _errorMessage.value = err
                _isSuccess.value = false
                return@launch
            }

            val currentStores = storeRepository.getStoresForDealership(dId)
            val generatedStoreId = store.storeId.ifBlank {
                "${store.stateCode.trim().uppercase()}_${store.city.trim().replace("\\s+".toRegex(), "")}_${System.currentTimeMillis() % 1000}"
            }
            val isNewStore = currentStores.none { it.storeId == generatedStoreId }
            if (isNewStore) {
                val currentStoreCount = currentStores.size
                if (!FeatureAccessManager.canAddStore(currentStoreCount, plan)) {
                    val err = "Plan limit reached: Store limit reached for current plan (${plan.maxStores} max stores). Please upgrade."
                    _errorMessage.value = err
                    _isSuccess.value = false
                    return@launch
                }
            }

            _isLoading.value = true
            val newStore = store.copy(
                dealershipId = dId,
                storeId = generatedStoreId,
                storeName = store.storeName.trim(),
                stateCode = store.stateCode.trim().uppercase(),
                city = store.city.trim(),
                address = store.address.trim()
            )
            val result = storeRepository.saveStore(newStore)
            _isLoading.value = false
            if (result.isSuccess) {
                logAudit("SAVE_STORE", "Store", newStore.storeId, mapOf("storeName" to newStore.storeName))
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

    fun saveUser(user: User, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val dId = _targetDealershipId.value ?: currentUser.value?.dealershipId.orEmpty().ifBlank {
                if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
            }
            val dealership = dealershipRepository.dealershipsFlow.value.find { it.dealershipId == dId }
                ?: if (authRepository.isDemoMode.value) DemoData.dealerships.firstOrNull() else null
            val plan = SubscriptionPlan.getPlanById(dealership?.subscriptionPlan ?: "STARTER")

            if (!FeatureAccessManager.isSubscriptionActive(dealership)) {
                val err = "Subscription inactive: account is in read-only mode"
                _errorMessage.value = err
                _isSuccess.value = false
                return@launch
            }

            val currentUsers = userRepository.usersFlow.value.filter { it.dealershipId == dId }
            val isNewUser = user.uid.isBlank() || currentUsers.none { it.uid == user.uid }
            if (isNewUser) {
                val currentUserCount = currentUsers.size
                if (!FeatureAccessManager.canAddUser(currentUserCount, plan)) {
                    val err = "Plan limit reached: your current plan allows up to ${plan.maxUsers} users"
                    _errorMessage.value = err
                    _isSuccess.value = false
                    return@launch
                }
            }

            _isLoading.value = true
            val prepared = user.copy(
                dealershipId = dId,
                uid = user.uid.ifBlank { "USER_${System.currentTimeMillis()}" }
            )
            val result = userRepository.saveUser(prepared)
            _isLoading.value = false
            if (result.isSuccess) {
                logAudit("SAVE_USER", "User", prepared.uid, mapOf("name" to prepared.name, "role" to prepared.role.name))
                _isSuccess.value = true
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to save user"
                _isSuccess.value = false
            }
        }
    }

    fun toggleUserActiveStatus(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val u = userRepository.getUser(userId)
            if (u != null) {
                val updated = u.copy(active = !u.active)
                userRepository.saveUser(updated)
                logAudit("TOGGLE_USER_ACTIVE", "User", userId, mapOf("active" to updated.active.toString()))
                _isSuccess.value = true
            } else {
                _errorMessage.value = "User not found"
                _isSuccess.value = false
            }
            _isLoading.value = false
        }
    }

    fun updateSupportedVehicleTypes(types: List<VehicleType>) {
        viewModelScope.launch {
            _isLoading.value = true
            val d = currentDealership.value
            if (d != null) {
                val updated = d.copy(
                    supportedVehicleTypes = types.ifEmpty { listOf(VehicleType.BIKE) },
                    updatedAt = System.currentTimeMillis()
                )
                dealershipRepository.saveDealership(updated)
                logAudit("UPDATE_SUPPORTED_VEHICLE_TYPES", "Dealership", d.dealershipId, mapOf("types" to types.joinToString()))
                _successMessage.value = "Updated supported vehicle types"
                _isSuccess.value = true
            }
            _isLoading.value = false
        }
    }

    private fun logAudit(action: String, entityType: String, entityId: String, metadata: Map<String, String>) {
        viewModelScope.launch {
            val uid = currentUser.value?.uid ?: "DEALERSHIP_ADMIN"
            val dId = currentDealership.value?.dealershipId ?: if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
            if (dId.isNotBlank()) {
                auditLogRepository.logEvent(dId, uid, action, entityType, entityId, metadata)
            }
        }
    }

    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
