package com.automotive.salesfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.model.Expense
import com.automotive.salesfinance.model.ExpenseCategory
import com.automotive.salesfinance.model.ExpensePaymentMethod
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.ExpenseRepository
import com.automotive.salesfinance.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ExpenseActionType {
    CREATE,
    SUBMIT,
    APPROVE,
    REJECT,
    CORRECT,
    CANCEL,
    RECORD_PAID,
    REVERSE,
    REFRESH
}

sealed class ExpenseActionState {
    object Idle : ExpenseActionState()
    data class Loading(val actionType: ExpenseActionType) : ExpenseActionState()
    data class Success(val actionType: ExpenseActionType, val message: String) : ExpenseActionState()
    data class Error(val actionType: ExpenseActionType, val message: String) : ExpenseActionState()
}

class ExpenseViewModel(
    private val expenseRepository: ExpenseRepository,
    private val storeRepository: StoreRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val expenses: StateFlow<List<Expense>> = expenseRepository.expensesFlow

    private val _actionState = MutableStateFlow<ExpenseActionState>(ExpenseActionState.Idle)
    val actionState: StateFlow<ExpenseActionState> = _actionState.asStateFlow()

    private val _selectedStoreId = MutableStateFlow<String>("")
    val selectedStoreId: StateFlow<String> = _selectedStoreId.asStateFlow()

    private val _availableStores = MutableStateFlow<List<Store>>(emptyList())
    val availableStores: StateFlow<List<Store>> = _availableStores.asStateFlow()

    private var pendingCreateIdempotencyKey: String? = null
    private var lastUid: String? = null
    private var lastDealershipId: String? = null

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                resolveUserAndStores(user)
            }
        }
    }

    private suspend fun resolveUserAndStores(user: User?) {
        if (user == null || !user.active || user.dealershipId.isBlank()) {
            lastUid = null
            lastDealershipId = null
            _availableStores.value = emptyList()
            _selectedStoreId.value = ""
            expenseRepository.clearInMemoryState()
            pendingCreateIdempotencyKey = null
            return
        }

        val userChanged = user.uid != lastUid || user.dealershipId != lastDealershipId
        if (userChanged) {
            lastUid = user.uid
            lastDealershipId = user.dealershipId
            _selectedStoreId.value = ""
            expenseRepository.clearInMemoryState()
            pendingCreateIdempotencyKey = null
        }

        var cachedStores = storeRepository.getStoresForDealership(user.dealershipId)
        if (cachedStores.isEmpty() && !authRepository.isDemoMode.value) {
            cachedStores = storeRepository.fetchStoresFromFirestore(user.dealershipId)
        }

        val activeDealershipStores = cachedStores.filter { it.active && it.storeId.isNotBlank() && it.storeId != "ALL" }

        val authorizedStores = when (user.role) {
            UserRole.DEALERSHIP_ADMIN -> activeDealershipStores
            UserRole.STORE_MANAGER -> activeDealershipStores.filter { it.storeId == user.storeId }
            UserRole.FINANCE_USER -> if (user.storeId == "ALL") activeDealershipStores else activeDealershipStores.filter { it.storeId == user.storeId }
            else -> emptyList()
        }

        _availableStores.value = authorizedStores

        val currentSelected = _selectedStoreId.value
        val isValidCurrentSelected = authorizedStores.any { it.storeId == currentSelected }

        if (!isValidCurrentSelected) {
            val initialRealStore = authorizedStores.firstOrNull()?.storeId ?: ""
            _selectedStoreId.value = initialRealStore
            if (initialRealStore.isNotBlank()) {
                expenseRepository.attachExpensesListener(initialRealStore)
            } else {
                expenseRepository.clearInMemoryState()
            }
        } else {
            expenseRepository.attachExpensesListener(currentSelected)
        }
    }

    fun selectStore(storeId: String) {
        val currentUser = authRepository.currentUser.value ?: return
        if (!currentUser.active || storeId.isBlank() || storeId == "ALL") {
            _actionState.value = ExpenseActionState.Error(ExpenseActionType.REFRESH, "Invalid store selection: store ID cannot be blank or 'ALL'")
            return
        }

        val isAuthorized = _availableStores.value.any { it.storeId == storeId }
        if (!isAuthorized) {
            _actionState.value = ExpenseActionState.Error(ExpenseActionType.REFRESH, "Unauthorized store selection: $storeId is not in authorized store scope")
            return
        }

        if (_selectedStoreId.value == storeId) return

        _selectedStoreId.value = storeId
        expenseRepository.attachExpensesListener(storeId)
    }

    fun resetActionState() {
        _actionState.value = ExpenseActionState.Idle
    }

    fun cancelPendingDraftAttempt() {
        pendingCreateIdempotencyKey = null
    }

    fun refresh() {
        val targetStoreId = _selectedStoreId.value
        if (targetStoreId.isBlank() || targetStoreId == "ALL") return

        _actionState.value = ExpenseActionState.Loading(ExpenseActionType.REFRESH)
        viewModelScope.launch {
            val result = expenseRepository.refreshExpenses(targetStoreId)
            result.fold(
                onSuccess = {
                    _actionState.value = ExpenseActionState.Success(ExpenseActionType.REFRESH, "Expenses refreshed successfully")
                },
                onFailure = { err ->
                    _actionState.value = ExpenseActionState.Error(ExpenseActionType.REFRESH, err.message ?: "Failed to refresh expenses")
                }
            )
        }
    }

    fun createDraft(
        description: String,
        amountPaise: Long,
        category: ExpenseCategory,
        paymentMethod: ExpensePaymentMethod,
        expenseDate: Long = System.currentTimeMillis(),
        explicitIdempotencyKey: String? = null
    ) {
        if (_actionState.value is ExpenseActionState.Loading) return

        val targetStoreId = _selectedStoreId.value
        if (targetStoreId.isBlank() || targetStoreId == "ALL") {
            _actionState.value = ExpenseActionState.Error(ExpenseActionType.CREATE, "Cannot create expense: no valid real store selected")
            return
        }

        _actionState.value = ExpenseActionState.Loading(ExpenseActionType.CREATE)

        val keyToUse = explicitIdempotencyKey?.ifBlank { null }
            ?: pendingCreateIdempotencyKey
            ?: "IDEM_EXP_${System.currentTimeMillis()}_${(1000..9999).random()}"

        pendingCreateIdempotencyKey = keyToUse

        viewModelScope.launch {
            val result = expenseRepository.createExpenseDraft(
                targetStoreId = targetStoreId,
                description = description,
                amountPaise = amountPaise,
                category = category,
                paymentMethod = paymentMethod,
                expenseDate = expenseDate,
                existingIdempotencyKey = keyToUse
            )
            result.fold(
                onSuccess = { draft ->
                    pendingCreateIdempotencyKey = null
                    _actionState.value = ExpenseActionState.Success(ExpenseActionType.CREATE, "Draft created: ${draft.expenseId}")
                },
                onFailure = { err ->
                    _actionState.value = ExpenseActionState.Error(ExpenseActionType.CREATE, err.message ?: "Failed to create expense draft")
                }
            )
        }
    }

    fun submit(expenseId: String) {
        if (_actionState.value is ExpenseActionState.Loading) return
        _actionState.value = ExpenseActionState.Loading(ExpenseActionType.SUBMIT)
        viewModelScope.launch {
            val result = expenseRepository.submitExpense(expenseId)
            result.fold(
                onSuccess = {
                    _actionState.value = ExpenseActionState.Success(ExpenseActionType.SUBMIT, "Expense submitted for approval")
                },
                onFailure = { err ->
                    _actionState.value = ExpenseActionState.Error(ExpenseActionType.SUBMIT, err.message ?: "Failed to submit expense")
                }
            )
        }
    }

    fun approve(expenseId: String) {
        if (_actionState.value is ExpenseActionState.Loading) return
        _actionState.value = ExpenseActionState.Loading(ExpenseActionType.APPROVE)
        viewModelScope.launch {
            val result = expenseRepository.approveExpense(expenseId)
            result.fold(
                onSuccess = {
                    _actionState.value = ExpenseActionState.Success(ExpenseActionType.APPROVE, "Expense approved successfully")
                },
                onFailure = { err ->
                    _actionState.value = ExpenseActionState.Error(ExpenseActionType.APPROVE, err.message ?: "Failed to approve expense")
                }
            )
        }
    }

    fun reject(expenseId: String, reason: String) {
        if (_actionState.value is ExpenseActionState.Loading) return
        _actionState.value = ExpenseActionState.Loading(ExpenseActionType.REJECT)
        viewModelScope.launch {
            val result = expenseRepository.rejectExpense(expenseId, reason)
            result.fold(
                onSuccess = {
                    _actionState.value = ExpenseActionState.Success(ExpenseActionType.REJECT, "Expense rejected")
                },
                onFailure = { err ->
                    _actionState.value = ExpenseActionState.Error(ExpenseActionType.REJECT, err.message ?: "Failed to reject expense")
                }
            )
        }
    }

    fun resubmitRejected(
        expenseId: String,
        description: String,
        amountPaise: Long,
        category: ExpenseCategory,
        paymentMethod: ExpensePaymentMethod
    ) {
        if (_actionState.value is ExpenseActionState.Loading) return
        _actionState.value = ExpenseActionState.Loading(ExpenseActionType.CORRECT)
        viewModelScope.launch {
            val result = expenseRepository.resubmitRejectedExpense(
                expenseId = expenseId,
                description = description,
                amountPaise = amountPaise,
                category = category,
                paymentMethod = paymentMethod
            )
            result.fold(
                onSuccess = {
                    _actionState.value = ExpenseActionState.Success(ExpenseActionType.CORRECT, "Expense corrected and returned to draft")
                },
                onFailure = { err ->
                    _actionState.value = ExpenseActionState.Error(ExpenseActionType.CORRECT, err.message ?: "Failed to correct expense")
                }
            )
        }
    }

    fun cancel(expenseId: String) {
        if (_actionState.value is ExpenseActionState.Loading) return
        _actionState.value = ExpenseActionState.Loading(ExpenseActionType.CANCEL)
        viewModelScope.launch {
            val result = expenseRepository.cancelExpense(expenseId)
            result.fold(
                onSuccess = {
                    _actionState.value = ExpenseActionState.Success(ExpenseActionType.CANCEL, "Expense cancelled")
                },
                onFailure = { err ->
                    _actionState.value = ExpenseActionState.Error(ExpenseActionType.CANCEL, err.message ?: "Failed to cancel expense")
                }
            )
        }
    }

    fun recordPayment(expenseId: String) {
        if (_actionState.value is ExpenseActionState.Loading) return
        _actionState.value = ExpenseActionState.Loading(ExpenseActionType.RECORD_PAID)
        viewModelScope.launch {
            val result = expenseRepository.recordExpensePayment(expenseId)
            result.fold(
                onSuccess = {
                    _actionState.value = ExpenseActionState.Success(ExpenseActionType.RECORD_PAID, "Expense marked as PAID")
                },
                onFailure = { err ->
                    _actionState.value = ExpenseActionState.Error(ExpenseActionType.RECORD_PAID, err.message ?: "Failed to record payment")
                }
            )
        }
    }

    fun reversePayment(expenseId: String, reason: String) {
        if (_actionState.value is ExpenseActionState.Loading) return
        _actionState.value = ExpenseActionState.Loading(ExpenseActionType.REVERSE)
        viewModelScope.launch {
            val result = expenseRepository.reverseExpensePayment(expenseId, reason)
            result.fold(
                onSuccess = {
                    _actionState.value = ExpenseActionState.Success(ExpenseActionType.REVERSE, "Expense payment reversed")
                },
                onFailure = { err ->
                    _actionState.value = ExpenseActionState.Error(ExpenseActionType.REVERSE, err.message ?: "Failed to reverse payment")
                }
            )
        }
    }
}
