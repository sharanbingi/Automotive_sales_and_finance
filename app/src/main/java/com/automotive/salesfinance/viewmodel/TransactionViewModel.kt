package com.automotive.salesfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.utils.toRupeesDouble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class CollectionsSummary(
    val todaysCollections: Double = 0.0,
    val todaysCount: Int = 0,
    val totalVolume: Double = 0.0,
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0
)

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val loanRepository: LoanRepository,
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val isDemoMode: StateFlow<Boolean> = authRepository.isDemoMode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow(true)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _methodFilter = MutableStateFlow("ALL")
    val methodFilter: StateFlow<String> = _methodFilter.asStateFlow()

    private val _purposeFilter = MutableStateFlow("ALL")
    val purposeFilter: StateFlow<String> = _purposeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allTransactions: StateFlow<List<Transaction>> = transactionRepository.transactionsFlow

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        transactionRepository.transactionsFlow,
        customerRepository.customersFlow,
        authRepository.currentUser,
        _statusFilter,
        _methodFilter,
        _purposeFilter,
        _searchQuery
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[0] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val customers = flows[1] as List<Customer>
        val user = flows[2] as? User
        val status = flows[3] as String
        val method = flows[4] as String
        val purpose = flows[5] as String
        val query = flows[6] as String

        val dId = user?.dealershipId.orEmpty()

        transactions.filter { txn ->
            val matchesDealership = dId.isBlank() || txn.dealershipId == dId
            val matchesStatus = when (status.uppercase()) {
                "ALL" -> true
                "SUCCESS" -> txn.status == TransactionStatus.SUCCESS
                "PENDING" -> txn.status == TransactionStatus.PENDING
                "FAILED" -> txn.status == TransactionStatus.FAILED
                else -> true
            }

            val matchesMethod = when (method.uppercase()) {
                "ALL" -> true
                "RAZORPAY" -> txn.paymentMethod.contains("RAZORPAY", ignoreCase = true)
                "UPI" -> txn.paymentMethod.contains("UPI", ignoreCase = true)
                "CASH" -> txn.paymentMethod.equals("CASH", ignoreCase = true)
                "NET_BANKING" -> txn.paymentMethod.contains("NET", ignoreCase = true) || txn.paymentMethod.contains("BANK", ignoreCase = true)
                else -> txn.paymentMethod.contains(method, ignoreCase = true)
            }

            val matchesPurpose = when (purpose.uppercase()) {
                "ALL" -> true
                else -> txn.paymentPurpose.contains(purpose, ignoreCase = true)
            }

            val q = query.trim().lowercase()
            val customer = customers.find { it.customerId == txn.customerId }

            val matchesQuery = q.isEmpty() ||
                txn.transactionId.lowercase().contains(q) ||
                txn.loanId.lowercase().contains(q) ||
                txn.razorpayPaymentId.lowercase().contains(q) ||
                txn.paymentPurpose.lowercase().contains(q) ||
                txn.paymentMethod.lowercase().contains(q) ||
                (customer != null && customer.fullName.lowercase().contains(q))

            matchesDealership && matchesStatus && matchesMethod && matchesPurpose && matchesQuery
        }.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collectionsSummary: StateFlow<CollectionsSummary> = combine(
        transactionRepository.transactionsFlow,
        authRepository.currentUser
    ) { transactions, user ->
        val dId = user?.dealershipId.orEmpty()
        val todayStart = getTodayStartMillis()

        val filtered = transactions.filter { dId.isBlank() || it.dealershipId == dId }
        val successTxns = filtered.filter { it.status == TransactionStatus.SUCCESS }
        val todaysTxns = successTxns.filter { it.createdAt >= todayStart }

        val todaysTotal = todaysTxns.sumOf { it.effectiveAmountPaise() }.toRupeesDouble()
        val totalVol = successTxns.sumOf { it.effectiveAmountPaise() }.toRupeesDouble()
        val pendingCount = filtered.count { it.status == TransactionStatus.PENDING }
        val failedCount = filtered.count { it.status == TransactionStatus.FAILED }

        CollectionsSummary(
            todaysCollections = todaysTotal,
            todaysCount = todaysTxns.size,
            totalVolume = totalVol,
            totalCount = successTxns.size,
            pendingCount = pendingCount,
            failedCount = failedCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionsSummary())

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun setMethodFilter(filter: String) {
        _methodFilter.value = filter
    }

    fun setPurposeFilter(filter: String) {
        _purposeFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getCustomerName(customerId: String): String {
        return customerRepository.getCustomerById(customerId)?.fullName ?: customerId
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
