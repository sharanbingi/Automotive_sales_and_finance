package com.automotive.salesfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.utils.DateUtils
import com.automotive.salesfinance.utils.toCanonicalStateCode
import com.automotive.salesfinance.utils.toPaise
import com.automotive.salesfinance.utils.toRupeesDouble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportPeriod(val label: String) {
    ALL_TIME("All Time"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_MONTH("This Month")
}

data class DashboardMetrics(
    val totalBikes: Int = 0,
    val availableBikes: Int = 0,
    val reservedBikes: Int = 0,
    val financedBikes: Int = 0,
    val totalBikesValuation: Double = 0.0,
    val financedBikesValuation: Double = 0.0,
    val financedBikesValuationPaise: Long = 0L,
    val activeLoansCount: Int = 0,
    val activeLoansAmount: Double = 0.0,
    val activeLoansAmountPaise: Long = 0L,
    val overdueLoansCount: Int = 0,
    val overdueLoansAmount: Double = 0.0,
    val overdueLoansAmountPaise: Long = 0L,
    val deadStockCount: Int = 0,
    val deadStockCapital: Double = 0.0,
    val todaysCollections: Double = 0.0,
    val totalCollectionsVolume: Double = 0.0,
    val totalStoresCount: Int = 0,
    val totalCustomers: Int = 0
)

data class CustomerDashboardInfo(
    val customer: Customer? = null,
    val activeLoans: List<Loan> = emptyList(),
    val totalOutstandingBalance: Double = 0.0,
    val nextEmiDueDate: Long = 0L,
    val nextEmiAmount: Double = 0.0,
    val vehiclesOwned: List<Bike> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList()
)

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val inventoryRepository: InventoryRepository,
    private val loanRepository: LoanRepository,
    private val customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
    private val storeRepository: StoreRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
    val isDemoMode: StateFlow<Boolean> = authRepository.isDemoMode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow(true)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _selectedState = MutableStateFlow("ALL")
    val selectedState: StateFlow<String> = _selectedState.asStateFlow()

    private val _selectedStore = MutableStateFlow("ALL")
    val selectedStore: StateFlow<String> = _selectedStore.asStateFlow()

    private val _selectedReportPeriod = MutableStateFlow(ReportPeriod.ALL_TIME)
    val selectedReportPeriod: StateFlow<ReportPeriod> = _selectedReportPeriod.asStateFlow()

    fun setSelectedReportPeriod(period: ReportPeriod) {
        _selectedReportPeriod.value = period
    }

    fun getPeriodStartMillis(period: ReportPeriod): Long {
        return when (period) {
            ReportPeriod.ALL_TIME -> 0L
            ReportPeriod.LAST_7_DAYS -> System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            ReportPeriod.LAST_30_DAYS -> System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            ReportPeriod.THIS_MONTH -> Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    val availableStores: StateFlow<List<Store>> = combine(
        storeRepository.storesFlow,
        _selectedState,
        authRepository.currentUser
    ) { stores, selectedStateCode, user ->
        val userDealershipId = user?.dealershipId.orEmpty()
        val canonSelected = selectedStateCode.toCanonicalStateCode()
        stores.filter { store ->
            val matchesDealership = userDealershipId.isBlank() || store.dealershipId == userDealershipId
            val matchesUserRole = when {
                user == null || user.role == UserRole.ADMIN || user.role == UserRole.DEALERSHIP_ADMIN -> true
                user.role == UserRole.STATE_MANAGER -> store.stateCode.toCanonicalStateCode() == user.stateCode.toCanonicalStateCode() || user.stateCode == "ALL"
                else -> store.storeId == user.storeId || user.storeId == "ALL"
            }
            val matchesState = canonSelected == "ALL" || store.stateCode.toCanonicalStateCode() == canonSelected
            matchesDealership && matchesUserRole && matchesState
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        inventoryRepository.bikesFlow,
        loanRepository.loansFlow,
        transactionRepository.transactionsFlow,
        storeRepository.storesFlow,
        customerRepository.customersFlow,
        authRepository.currentUser,
        _selectedState,
        _selectedStore,
        _selectedReportPeriod
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val bikes = flows[0] as List<Bike>
        @Suppress("UNCHECKED_CAST")
        val loans = flows[1] as List<Loan>
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[2] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val stores = flows[3] as List<Store>
        @Suppress("UNCHECKED_CAST")
        val customers = flows[4] as List<Customer>
        val user = flows[5] as? User
        val stateCode = flows[6] as String
        val storeId = flows[7] as String
        val reportPeriod = flows[8] as ReportPeriod

        val dId = user?.dealershipId.orEmpty()

        val effState = when {
            user?.role == UserRole.STATE_MANAGER && user.stateCode != "ALL" -> user.stateCode
            stateCode != "ALL" -> stateCode
            else -> "ALL"
        }.toCanonicalStateCode()

        val effStore = when {
            user?.role == UserRole.STORE_MANAGER && user.storeId != "ALL" -> user.storeId
            storeId != "ALL" -> storeId
            else -> "ALL"
        }

        val periodStart = getPeriodStartMillis(reportPeriod)

        fun resolveBikeState(bike: Bike): String {
            val direct = bike.stateCode.toCanonicalStateCode()
            if (direct.isNotBlank()) return direct
            val store = stores.find {
                it.storeId.equals(bike.storeLocation, ignoreCase = true) ||
                it.storeName.equals(bike.storeLocation, ignoreCase = true)
            }
            return store?.stateCode.toCanonicalStateCode()
        }

        // Filtering hierarchy: dealershipId -> stateCode -> storeId -> entities
        val filteredBikes = bikes.filter { b ->
            val matchesDealership = dId.isBlank() || b.dealershipId == dId
            val bikeState = resolveBikeState(b)
            val matchesState = effState == "ALL" || bikeState == effState
            val matchesStore = effStore == "ALL" || b.storeLocation.equals(effStore, ignoreCase = true)
            matchesDealership && matchesState && matchesStore
        }

        val avail = filteredBikes.count { it.status == BikeStatus.AVAILABLE }
        val res = filteredBikes.count { it.status == BikeStatus.RESERVED }
        
        // Calculate financedBikes (vehicles where bikeStatus == VehicleType.FINANCED or VehicleStatus.SOLD or status string "FINANCED"/"SOLD" or status enum BikeStatus.FINANCED)
        val financedBikesList = filteredBikes.filter {
            it.status == BikeStatus.FINANCED || it.status.name.equals("FINANCED", ignoreCase = true) || it.status.name.equals("SOLD", ignoreCase = true)
        }
        val fin = financedBikesList.size
        val financedBikesValuation = financedBikesList.sumOf { it.listedPrice }
        val financedBikesValuationPaise = financedBikesList.sumOf { it.listedPrice.toPaise() }

        val bikesVal = filteredBikes.sumOf { it.listedPrice }

        val deadStock = filteredBikes.filter {
            it.status == BikeStatus.AVAILABLE && DateUtils.isDeadStock(it.inwardTimestamp, 60)
        }
        val deadStockCount = deadStock.size
        val deadStockCapital = deadStock.sumOf { it.costPrice }

        val filteredStores = stores.filter { s ->
            (dId.isBlank() || s.dealershipId == dId) &&
            (effState == "ALL" || s.stateCode.toCanonicalStateCode() == effState) &&
            (effStore == "ALL" || s.storeId.equals(effStore, ignoreCase = true))
        }

        // Customer filtering
        val filteredCustomers = customers.filter { customer ->
            val matchesDealership = dId.isBlank() || customer.dealershipId == dId
            val canonCustomerState = customer.state.toCanonicalStateCode()
            val matchesState = effState == "ALL" || canonCustomerState == effState
            val matchesPeriod = customer.createdAt >= periodStart
            matchesDealership && matchesState && matchesPeriod
        }
        val totalCustomersCount = filteredCustomers.size

        val filteredLoans = loans.filter { loan ->
            val matchesDealership = dId.isBlank() || loan.dealershipId == dId
            val bike = bikes.find { it.bikeId == loan.bikeId }
            val bikeState = if (bike != null) resolveBikeState(bike) else ""
            val matchesState = effState == "ALL" || bikeState == effState
            val matchesStore = effStore == "ALL" || (bike != null && bike.storeLocation.equals(effStore, ignoreCase = true))
            matchesDealership && matchesState && matchesStore
        }

        val activeLoans = filteredLoans.filter { it.loanStatus == LoanStatus.ACTIVE }
        val overdueLoans = filteredLoans.filter {
            it.loanStatus == LoanStatus.OVERDUE ||
            (it.loanStatus == LoanStatus.ACTIVE && it.nextEmiDate > 0 && it.nextEmiDate < System.currentTimeMillis())
        }

        // Loan Money Precision
        val activeLoansAmountPaise = activeLoans.sumOf { it.effectiveRemainingBalancePaise() }
        val overdueLoansAmountPaise = overdueLoans.sumOf { it.effectiveRemainingBalancePaise() }
        val activeLoansAmount = activeLoansAmountPaise.toRupeesDouble()
        val overdueLoansAmount = overdueLoansAmountPaise.toRupeesDouble()

        val todayStart = getTodayStartMillis()
        val filteredTxns = transactions.filter { txn ->
            val matchesDealership = dId.isBlank() || txn.dealershipId == dId
            val loan = loans.find { it.loanId == txn.loanId }
            val bike = bikes.find { it.bikeId == txn.bikeId || (loan != null && it.bikeId == loan.bikeId) }
            val bikeState = if (bike != null) resolveBikeState(bike) else ""
            val matchesState = effState == "ALL" || bikeState == effState
            val matchesStore = effStore == "ALL" || (bike != null && bike.storeLocation.equals(effStore, ignoreCase = true))
            matchesDealership && matchesState && matchesStore && txn.status == TransactionStatus.SUCCESS
        }

        val todaysCollections = filteredTxns.filter { it.createdAt >= todayStart }.sumOf { it.effectiveAmountPaise() }.toRupeesDouble()
        val periodTxns = filteredTxns.filter { it.createdAt >= periodStart }
        val totalVolume = periodTxns.sumOf { it.effectiveAmountPaise() }.toRupeesDouble()

        DashboardMetrics(
            totalBikes = filteredBikes.size,
            availableBikes = avail,
            reservedBikes = res,
            financedBikes = fin,
            totalBikesValuation = bikesVal,
            financedBikesValuation = financedBikesValuation,
            financedBikesValuationPaise = financedBikesValuationPaise,
            activeLoansCount = activeLoans.size,
            activeLoansAmount = activeLoansAmount,
            activeLoansAmountPaise = activeLoansAmountPaise,
            overdueLoansCount = overdueLoans.size,
            overdueLoansAmount = overdueLoansAmount,
            overdueLoansAmountPaise = overdueLoansAmountPaise,
            deadStockCount = deadStockCount,
            deadStockCapital = deadStockCapital,
            todaysCollections = todaysCollections,
            totalCollectionsVolume = totalVolume,
            totalStoresCount = filteredStores.size,
            totalCustomers = totalCustomersCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    val customerDashboardInfo: StateFlow<CustomerDashboardInfo> = combine(
        authRepository.currentUser,
        customerRepository.customersFlow,
        loanRepository.loansFlow,
        inventoryRepository.bikesFlow,
        transactionRepository.transactionsFlow
    ) { flows: Array<Any?> ->
        val user = flows[0] as? User
        @Suppress("UNCHECKED_CAST")
        val customers = flows[1] as List<Customer>
        @Suppress("UNCHECKED_CAST")
        val loans = flows[2] as List<Loan>
        @Suppress("UNCHECKED_CAST")
        val bikes = flows[3] as List<Bike>
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[4] as List<Transaction>

        if (user == null || user.role != UserRole.CUSTOMER) {
            CustomerDashboardInfo()
        } else {
            val dId = user.dealershipId
            val customer = customers.find {
                (dId.isBlank() || it.dealershipId == dId) && it.email.equals(user.email, ignoreCase = true)
            } ?: customers.firstOrNull { dId.isBlank() || it.dealershipId == dId }

            val custId = customer?.customerId ?: ""

            val myLoans = loans.filter { (dId.isBlank() || it.dealershipId == dId) && it.customerId == custId }
            val myBikes = bikes.filter { b -> (dId.isBlank() || b.dealershipId == dId) && myLoans.any { l -> l.bikeId == b.bikeId } }
            val myTxns = transactions.filter { (dId.isBlank() || it.dealershipId == dId) && it.customerId == custId }.sortedByDescending { it.createdAt }

            val totalOutstanding = myLoans.sumOf { it.remainingBalance }
            val activeLoan = myLoans.find { it.loanStatus == LoanStatus.ACTIVE || it.loanStatus == LoanStatus.OVERDUE }

            CustomerDashboardInfo(
                customer = customer,
                activeLoans = myLoans,
                totalOutstandingBalance = totalOutstanding,
                nextEmiDueDate = activeLoan?.nextEmiDate ?: 0L,
                nextEmiAmount = activeLoan?.emiAmount ?: 0.0,
                vehiclesOwned = myBikes,
                recentTransactions = myTxns
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomerDashboardInfo())

    val recentBikes: StateFlow<List<Bike>> = combine(
        inventoryRepository.bikesFlow,
        authRepository.currentUser,
        _selectedState
    ) { bikes, user, state ->
        val dId = user?.dealershipId.orEmpty()
        bikes.filter {
            (dId.isBlank() || it.dealershipId == dId) && (state == "ALL" || it.stateCode.equals(state, ignoreCase = true))
        }.sortedByDescending { it.inwardTimestamp }.take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<Transaction>> = combine(
        transactionRepository.transactionsFlow,
        authRepository.currentUser,
        _selectedState
    ) { txns, user, state ->
        val dId = user?.dealershipId.orEmpty()
        txns.filter { dId.isBlank() || it.dealershipId == dId }
            .sortedByDescending { it.createdAt }.take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentCustomers: StateFlow<List<Customer>> = combine(
        customerRepository.customersFlow,
        authRepository.currentUser
    ) { customers, user ->
        val dId = user?.dealershipId.orEmpty()
        customers.filter { dId.isBlank() || it.dealershipId == dId }
            .sortedByDescending { it.createdAt }.take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            if (!authRepository.isDemoMode.value) {
                val dId = currentUser.value?.dealershipId.orEmpty()
                inventoryRepository.fetchVehiclesFromFirestore(dId)
                loanRepository.fetchLoansFromFirestore(dId)
                customerRepository.fetchCustomersFromFirestore(dId)
                transactionRepository.fetchTransactionsFromFirestore(dId)
                storeRepository.fetchStoresFromFirestore(dId)
            }
        }
    }

    fun setSelectedState(stateCode: String) {
        _selectedState.value = stateCode
        if (stateCode != "ALL" && _selectedStore.value != "ALL") {
            val store = storeRepository.getStoreById(_selectedStore.value)
            if (store != null && store.stateCode != stateCode) {
                _selectedStore.value = "ALL"
            }
        }
    }

    fun setSelectedStore(storeId: String) {
        _selectedStore.value = storeId
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
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
