package com.automotive.salesfinance.repository

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TransactionRepository(private val authRepository: AuthRepository) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _transactionsFlow = MutableStateFlow<List<Transaction>>(
        if (authRepository.isDemoMode.value) DemoData.transactions else emptyList()
    )
    val transactionsFlow: StateFlow<List<Transaction>> = _transactionsFlow.asStateFlow()

    fun clearInMemoryState() {
        _transactionsFlow.value = emptyList()
    }

    init {
        authRepository.registerClearListener {
            clearInMemoryState()
        }
        CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch {
            authRepository.isDemoMode.collect { isDemo ->
                if (!isDemo) {
                    _transactionsFlow.value = _transactionsFlow.value.filterNot { t ->
                        DemoData.transactions.any { it.transactionId == t.transactionId }
                    }
                } else if (_transactionsFlow.value.isEmpty()) {
                    _transactionsFlow.value = DemoData.transactions
                }
            }
        }
    }

    fun getTransactions(): List<Transaction> = _transactionsFlow.value

    fun getTransactionsForDealership(dealershipId: String): List<Transaction> {
        if (dealershipId.isBlank()) {
            return if (authRepository.isDemoMode.value) {
                _transactionsFlow.value.filter { it.dealershipId == DemoData.DEMO_DEALERSHIP_ID }
            } else {
                emptyList()
            }
        }
        return _transactionsFlow.value.filter { it.dealershipId == dealershipId }
    }

    fun getTransactionsByLoan(loanId: String): List<Transaction> {
        return _transactionsFlow.value.filter { it.loanId == loanId }
    }

    fun getTransactionsByCustomer(customerId: String): List<Transaction> {
        return _transactionsFlow.value.filter { it.customerId == customerId }
    }

    suspend fun fetchTransactionsFromFirestore(dealershipId: String = ""): List<Transaction> {
        if (authRepository.isDemoMode.value) {
            return if (dealershipId.isBlank()) _transactionsFlow.value else getTransactionsForDealership(dealershipId)
        }
        if (firestore == null) {
            _transactionsFlow.value = _transactionsFlow.value.filterNot { t -> DemoData.transactions.any { it.transactionId == t.transactionId } }
            return if (dealershipId.isBlank()) _transactionsFlow.value else getTransactionsForDealership(dealershipId)
        }
        return try {
            val fetched = if (dealershipId.isBlank()) {
                val snapshot = firestore?.collectionGroup("transactions")?.limit(200)?.get()?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(Transaction::class.java) } catch (_: Exception) { null }
                } ?: emptyList()
            } else {
                val snapshot = firestore?.collection("dealerships")
                    ?.document(dealershipId)
                    ?.collection("transactions")
                    ?.limit(100)
                    ?.get()
                    ?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(Transaction::class.java) } catch (_: Exception) { null }
                } ?: emptyList()
            }

            if (dealershipId.isBlank()) {
                _transactionsFlow.value = fetched
            } else {
                val current = _transactionsFlow.value.filterNot { it.dealershipId == dealershipId || DemoData.transactions.any { demo -> demo.transactionId == it.transactionId } }.toMutableList()
                current.addAll(fetched)
                _transactionsFlow.value = current
            }
            fetched
        } catch (e: Exception) {
            val liveOnly = _transactionsFlow.value.filterNot { t -> DemoData.transactions.any { it.transactionId == t.transactionId } }
            _transactionsFlow.value = liveOnly
            if (dealershipId.isBlank()) liveOnly else liveOnly.filter { it.dealershipId == dealershipId }
        }
    }

    suspend fun addTransaction(transaction: Transaction): Result<Unit> {
        if (transaction.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }
        val preparedTxn = if (transaction.dealershipId.isBlank()) transaction.copy(dealershipId = DemoData.DEMO_DEALERSHIP_ID) else transaction
        val current = _transactionsFlow.value.toMutableList()
        current.add(0, preparedTxn)
        _transactionsFlow.value = current

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                firestore?.collection("dealerships")
                    ?.document(preparedTxn.dealershipId)
                    ?.collection("transactions")
                    ?.document(preparedTxn.transactionId)
                    ?.set(preparedTxn)
                    ?.await()
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }
}
