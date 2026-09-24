package com.automotive.salesfinance.repository

import android.util.Log
import com.automotive.salesfinance.model.Expense
import com.automotive.salesfinance.model.ExpenseApprovalStatus
import com.automotive.salesfinance.model.ExpenseCategory
import com.automotive.salesfinance.model.ExpensePaymentMethod
import com.automotive.salesfinance.model.ExpensePaymentStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.utils.ExpenseValidationUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private fun safeLogD(tag: String, msg: String) {
    try {
        Log.d(tag, msg)
    } catch (_: Throwable) {
        println("[$tag] $msg")
    }
}

class ExpenseRepository(
    private val authRepository: AuthRepository,
    private val storeRepository: StoreRepository
) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private var activeListener: ListenerRegistration? = null
    private var activeStoreIdListener: String? = null

    private val _expensesFlow = MutableStateFlow<List<Expense>>(emptyList())
    val expensesFlow: StateFlow<List<Expense>> = _expensesFlow.asStateFlow()

    fun clearInMemoryState() {
        activeListener?.remove()
        activeListener = null
        activeStoreIdListener = null
        _expensesFlow.value = emptyList()
    }

    init {
        authRepository.registerClearListener {
            clearInMemoryState()
        }
    }

    private fun isRoleAuthorizedForExpense(role: UserRole?): Boolean {
        return role in listOf(UserRole.DEALERSHIP_ADMIN, UserRole.STORE_MANAGER, UserRole.FINANCE_USER)
    }

    private fun isStoreAuthorizedAndActive(user: User, targetStoreId: String): Boolean {
        if (!user.active || targetStoreId.isBlank() || targetStoreId == "ALL") return false
        if (user.dealershipId.isBlank()) return false

        val store = storeRepository.getStoreById(targetStoreId) ?: return false
        if (store.dealershipId != user.dealershipId || !store.active) return false

        return when (user.role) {
            UserRole.DEALERSHIP_ADMIN -> true
            UserRole.STORE_MANAGER -> user.storeId == targetStoreId
            UserRole.FINANCE_USER -> user.storeId == "ALL" || user.storeId == targetStoreId
            else -> false
        }
    }

    private fun isSameLogicalDraft(existing: Expense, attempted: Expense): Boolean {
        return existing.expenseId == attempted.expenseId &&
                existing.idempotencyKey == attempted.idempotencyKey &&
                existing.dealershipId == attempted.dealershipId &&
                existing.storeId == attempted.storeId &&
                existing.createdByUserId == attempted.createdByUserId &&
                existing.approvalStatus == ExpenseApprovalStatus.DRAFT &&
                existing.paymentStatus == ExpensePaymentStatus.UNPAID &&
                existing.description == attempted.description &&
                existing.amountPaise == attempted.amountPaise &&
                existing.category == attempted.category &&
                existing.paymentMethod == attempted.paymentMethod &&
                existing.expenseDate == attempted.expenseDate
    }

    fun attachExpensesListener(targetStoreId: String) {
        // Immediately detach old listener & clear stale store data for privacy
        activeListener?.remove()
        activeListener = null
        activeStoreIdListener = null
        _expensesFlow.value = emptyList()

        val currentUser = authRepository.currentUser.value ?: return
        if (!currentUser.active || !isRoleAuthorizedForExpense(currentUser.role)) return
        if (!isStoreAuthorizedAndActive(currentUser, targetStoreId)) return

        activeStoreIdListener = targetStoreId

        if (authRepository.isDemoMode.value) {
            return
        }

        val db = firestore ?: return
        val query = db.collection("dealerships")
            .document(currentUser.dealershipId)
            .collection("stores")
            .document(targetStoreId)
            .collection("expenses")

        activeListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                safeLogD("ExpenseRepository", "Expenses snapshot error: ${error.message}")
                _expensesFlow.value = emptyList()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Expense::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                _expensesFlow.value = list
            }
        }
    }

    suspend fun refreshExpenses(targetStoreId: String): Result<List<Expense>> {
        val currentUser = authRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        if (!currentUser.active) {
            return Result.failure(SecurityException("User account is inactive"))
        }

        if (!isRoleAuthorizedForExpense(currentUser.role)) {
            return Result.failure(SecurityException("Role ${currentUser.role} is not authorized for Store Expenses"))
        }

        if (!isStoreAuthorizedAndActive(currentUser, targetStoreId)) {
            _expensesFlow.value = emptyList()
            return Result.failure(SecurityException("Target store $targetStoreId is unauthorized or inactive for user ${currentUser.uid}"))
        }

        if (authRepository.isDemoMode.value) {
            return Result.success(_expensesFlow.value)
        }

        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore instance unavailable"))

        return try {
            val snapshot = db.collection("dealerships")
                .document(currentUser.dealershipId)
                .collection("stores")
                .document(targetStoreId)
                .collection("expenses")
                .get()
                .await()

            val fetched = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Expense::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            _expensesFlow.value = fetched
            Result.success(fetched)
        } catch (e: Exception) {
            _expensesFlow.value = emptyList()
            Result.failure(e)
        }
    }

    fun getExpenses(): List<Expense> = _expensesFlow.value

    fun getExpenseById(expenseId: String): Expense? {
        return _expensesFlow.value.find { it.expenseId == expenseId }
    }

    suspend fun createExpenseDraft(
        targetStoreId: String,
        description: String,
        amountPaise: Long,
        category: ExpenseCategory,
        paymentMethod: ExpensePaymentMethod,
        expenseDate: Long = System.currentTimeMillis(),
        existingIdempotencyKey: String? = null
    ): Result<Expense> {
        val currentUser = authRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        if (!currentUser.active) {
            return Result.failure(SecurityException("User account is inactive"))
        }

        if (!isRoleAuthorizedForExpense(currentUser.role)) {
            return Result.failure(SecurityException("Role ${currentUser.role} is not authorized for Store Expenses"))
        }

        if (!isStoreAuthorizedAndActive(currentUser, targetStoreId)) {
            return Result.failure(SecurityException("Target store $targetStoreId is unauthorized or inactive for user ${currentUser.uid}"))
        }

        val dealershipId = currentUser.dealershipId
        val idempotencyKey = existingIdempotencyKey?.ifBlank { null }
            ?: "IDEM_EXP_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val expenseId = "EXP_${targetStoreId}_${idempotencyKey}"

        val now = System.currentTimeMillis()

        val draft = Expense(
            expenseId = expenseId,
            dealershipId = dealershipId,
            storeId = targetStoreId,
            category = category,
            description = description.trim(),
            amountPaise = amountPaise,
            expenseDate = expenseDate,
            paymentMethod = paymentMethod,
            approvalStatus = ExpenseApprovalStatus.DRAFT,
            paymentStatus = ExpensePaymentStatus.UNPAID,
            createdByUserId = currentUser.uid,
            createdAt = now,
            updatedAt = now,
            idempotencyKey = idempotencyKey
        )

        val validationError = ExpenseValidationUtils.validateExpenseCreation(draft)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        // Safe retry recognition check on existing local document
        val existingLocal = getExpenseById(expenseId)
        if (existingLocal != null) {
            if (isSameLogicalDraft(existingLocal, draft)) {
                return Result.success(existingLocal)
            } else {
                return Result.failure(IllegalStateException("Idempotency conflict: Existing expense $expenseId cannot be overwritten"))
            }
        }

        if (authRepository.isDemoMode.value) {
            updateLocalExpenseInMemory(draft)
            return Result.success(draft)
        }

        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore instance unavailable"))

        // PRODUCTION WRITE ORDER: Remote await() -> Local state update on success ONLY
        return try {
            val docRef = db.collection("dealerships")
                .document(dealershipId)
                .collection("stores")
                .document(targetStoreId)
                .collection("expenses")
                .document(draft.expenseId)

            val existingRemote = docRef.get().await()
            if (existingRemote.exists()) {
                val remoteExpense = existingRemote.toObject(Expense::class.java)
                if (remoteExpense != null && isSameLogicalDraft(remoteExpense, draft)) {
                    updateLocalExpenseInMemory(remoteExpense)
                    return Result.success(remoteExpense)
                } else {
                    return Result.failure(IllegalStateException("Idempotency conflict: Remote document $expenseId exists with different fields"))
                }
            }

            docRef.set(draft).await()
            updateLocalExpenseInMemory(draft)
            Result.success(draft)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitExpense(expenseId: String): Result<Unit> {
        val currentUser = authRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        if (!currentUser.active) {
            return Result.failure(SecurityException("User account is inactive"))
        }

        if (!isRoleAuthorizedForExpense(currentUser.role)) {
            return Result.failure(SecurityException("Role ${currentUser.role} is not authorized for Store Expenses"))
        }

        val existing = getExpenseById(expenseId)
            ?: return Result.failure(IllegalArgumentException("Expense $expenseId not found"))

        if (existing.dealershipId != currentUser.dealershipId || !isStoreAuthorizedAndActive(currentUser, existing.storeId)) {
            return Result.failure(SecurityException("Expense $expenseId store scope is not authorized for current user"))
        }

        val validationError = ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = existing.approvalStatus,
            newStatus = ExpenseApprovalStatus.SUBMITTED,
            createdByUserId = existing.createdByUserId,
            submittedByUserId = currentUser.uid,
            actorUserId = currentUser.uid
        )
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val now = System.currentTimeMillis()
        val updatedLocal = existing.copy(
            approvalStatus = ExpenseApprovalStatus.SUBMITTED,
            submittedByUserId = currentUser.uid,
            updatedAt = now
        )

        if (authRepository.isDemoMode.value) {
            updateLocalExpenseInMemory(updatedLocal)
            return Result.success(Unit)
        }

        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore instance unavailable"))

        return try {
            db.collection("dealerships")
                .document(existing.dealershipId)
                .collection("stores")
                .document(existing.storeId)
                .collection("expenses")
                .document(expenseId)
                .update(
                    mapOf(
                        "approvalStatus" to ExpenseApprovalStatus.SUBMITTED.name,
                        "submittedByUserId" to currentUser.uid,
                        "updatedAt" to now
                    )
                )
                .await()
            updateLocalExpenseInMemory(updatedLocal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveExpense(expenseId: String): Result<Unit> {
        val currentUser = authRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        if (!currentUser.active) {
            return Result.failure(SecurityException("User account is inactive"))
        }

        if (currentUser.role !in listOf(UserRole.DEALERSHIP_ADMIN, UserRole.FINANCE_USER)) {
            return Result.failure(SecurityException("Role ${currentUser.role} is not authorized to approve expenses"))
        }

        val existing = getExpenseById(expenseId)
            ?: return Result.failure(IllegalArgumentException("Expense $expenseId not found"))

        if (existing.dealershipId != currentUser.dealershipId || !isStoreAuthorizedAndActive(currentUser, existing.storeId)) {
            return Result.failure(SecurityException("Expense $expenseId store scope is not authorized for current user"))
        }

        val validationError = ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = existing.approvalStatus,
            newStatus = ExpenseApprovalStatus.APPROVED,
            createdByUserId = existing.createdByUserId,
            submittedByUserId = existing.submittedByUserId,
            actorUserId = currentUser.uid
        )
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val now = System.currentTimeMillis()
        val updatedLocal = existing.copy(
            approvalStatus = ExpenseApprovalStatus.APPROVED,
            approvedByUserId = currentUser.uid,
            updatedAt = now
        )

        if (authRepository.isDemoMode.value) {
            updateLocalExpenseInMemory(updatedLocal)
            return Result.success(Unit)
        }

        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore instance unavailable"))

        return try {
            db.collection("dealerships")
                .document(existing.dealershipId)
                .collection("stores")
                .document(existing.storeId)
                .collection("expenses")
                .document(expenseId)
                .update(
                    mapOf(
                        "approvalStatus" to ExpenseApprovalStatus.APPROVED.name,
                        "approvedByUserId" to currentUser.uid,
                        "updatedAt" to now
                    )
                )
                .await()
            updateLocalExpenseInMemory(updatedLocal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectExpense(expenseId: String, rejectionReason: String): Result<Unit> {
        val currentUser = authRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        if (!currentUser.active) {
            return Result.failure(SecurityException("User account is inactive"))
        }

        if (currentUser.role !in listOf(UserRole.DEALERSHIP_ADMIN, UserRole.FINANCE_USER)) {
            return Result.failure(SecurityException("Role ${currentUser.role} is not authorized to reject expenses"))
        }

        val existing = getExpenseById(expenseId)
            ?: return Result.failure(IllegalArgumentException("Expense $expenseId not found"))

        if (existing.dealershipId != currentUser.dealershipId || !isStoreAuthorizedAndActive(currentUser, existing.storeId)) {
            return Result.failure(SecurityException("Expense $expenseId store scope is not authorized for current user"))
        }

        val validationError = ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = existing.approvalStatus,
            newStatus = ExpenseApprovalStatus.REJECTED,
            createdByUserId = existing.createdByUserId,
            submittedByUserId = existing.submittedByUserId,
            actorUserId = currentUser.uid,
            rejectionReason = rejectionReason
        )
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val now = System.currentTimeMillis()
        val updatedLocal = existing.copy(
            approvalStatus = ExpenseApprovalStatus.REJECTED,
            rejectionReason = rejectionReason.trim(),
            updatedAt = now
        )

        if (authRepository.isDemoMode.value) {
            updateLocalExpenseInMemory(updatedLocal)
            return Result.success(Unit)
        }

        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore instance unavailable"))

        return try {
            db.collection("dealerships")
                .document(existing.dealershipId)
                .collection("stores")
                .document(existing.storeId)
                .collection("expenses")
                .document(expenseId)
                .update(
                    mapOf(
                        "approvalStatus" to ExpenseApprovalStatus.REJECTED.name,
                        "rejectionReason" to rejectionReason.trim(),
                        "updatedAt" to now
                    )
                )
                .await()
            updateLocalExpenseInMemory(updatedLocal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resubmitRejectedExpense(
        expenseId: String,
        description: String,
        amountPaise: Long,
        category: ExpenseCategory,
        paymentMethod: ExpensePaymentMethod
    ): Result<Unit> {
        val currentUser = authRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        if (!currentUser.active) {
            return Result.failure(SecurityException("User account is inactive"))
        }

        if (!isRoleAuthorizedForExpense(currentUser.role)) {
            return Result.failure(SecurityException("Role ${currentUser.role} is not authorized for Store Expenses"))
        }

        val existing = getExpenseById(expenseId)
            ?: return Result.failure(IllegalArgumentException("Expense $expenseId not found"))

        if (existing.dealershipId != currentUser.dealershipId || !isStoreAuthorizedAndActive(currentUser, existing.storeId)) {
            return Result.failure(SecurityException("Expense $expenseId store scope is not authorized for current user"))
        }

        val validationError = ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = existing.approvalStatus,
            newStatus = ExpenseApprovalStatus.DRAFT,
            createdByUserId = existing.createdByUserId,
            submittedByUserId = existing.submittedByUserId,
            actorUserId = currentUser.uid
        )
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        if (amountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Expense amount must be greater than zero"))
        }

        val now = System.currentTimeMillis()
        val updatedLocal = existing.copy(
            approvalStatus = ExpenseApprovalStatus.DRAFT,
            description = description.trim(),
            amountPaise = amountPaise,
            category = category,
            paymentMethod = paymentMethod,
            updatedAt = now
        )

        if (authRepository.isDemoMode.value) {
            updateLocalExpenseInMemory(updatedLocal)
            return Result.success(Unit)
        }

        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore instance unavailable"))

        return try {
            db.collection("dealerships")
                .document(existing.dealershipId)
                .collection("stores")
                .document(existing.storeId)
                .collection("expenses")
                .document(expenseId)
                .update(
                    mapOf(
                        "approvalStatus" to ExpenseApprovalStatus.DRAFT.name,
                        "description" to description.trim(),
                        "amountPaise" to amountPaise,
                        "category" to category.name,
                        "paymentMethod" to paymentMethod.name,
                        "updatedAt" to now
                    )
                )
                .await()
            updateLocalExpenseInMemory(updatedLocal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelExpense(expenseId: String): Result<Unit> {
        val currentUser = authRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        if (!currentUser.active) {
            return Result.failure(SecurityException("User account is inactive"))
        }

        if (!isRoleAuthorizedForExpense(currentUser.role)) {
            return Result.failure(SecurityException("Role ${currentUser.role} is not authorized for Store Expenses"))
        }

        val existing = getExpenseById(expenseId)
            ?: return Result.failure(IllegalArgumentException("Expense $expenseId not found"))

        if (existing.dealershipId != currentUser.dealershipId || !isStoreAuthorizedAndActive(currentUser, existing.storeId)) {
            return Result.failure(SecurityException("Expense $expenseId store scope is not authorized for current user"))
        }

        val validationError = ExpenseValidationUtils.validateApprovalTransition(
            currentStatus = existing.approvalStatus,
            newStatus = ExpenseApprovalStatus.CANCELLED,
            createdByUserId = existing.createdByUserId,
            submittedByUserId = existing.submittedByUserId,
            actorUserId = currentUser.uid
        )
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val now = System.currentTimeMillis()
        val updatedLocal = existing.copy(
            approvalStatus = ExpenseApprovalStatus.CANCELLED,
            updatedAt = now
        )

        if (authRepository.isDemoMode.value) {
            updateLocalExpenseInMemory(updatedLocal)
            return Result.success(Unit)
        }

        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore instance unavailable"))

        return try {
            db.collection("dealerships")
                .document(existing.dealershipId)
                .collection("stores")
                .document(existing.storeId)
                .collection("expenses")
                .document(expenseId)
                .update(
                    mapOf(
                        "approvalStatus" to ExpenseApprovalStatus.CANCELLED.name,
                        "updatedAt" to now
                    )
                )
                .await()
            updateLocalExpenseInMemory(updatedLocal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordExpensePayment(expenseId: String): Result<Unit> {
        val currentUser = authRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        if (!currentUser.active) {
            return Result.failure(SecurityException("User account is inactive"))
        }

        if (!isRoleAuthorizedForExpense(currentUser.role)) {
            return Result.failure(SecurityException("Role ${currentUser.role} is not authorized to disburse expense payments"))
        }

        val existing = getExpenseById(expenseId)
            ?: return Result.failure(IllegalArgumentException("Expense $expenseId not found"))

        if (existing.dealershipId != currentUser.dealershipId || !isStoreAuthorizedAndActive(currentUser, existing.storeId)) {
            return Result.failure(SecurityException("Expense $expenseId store scope is not authorized for current user"))
        }

        val validationError = ExpenseValidationUtils.validatePaymentTransition(
            currentPaymentStatus = existing.paymentStatus,
            approvalStatus = existing.approvalStatus,
            newPaymentStatus = ExpensePaymentStatus.PAID
        )
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val now = System.currentTimeMillis()
        val updatedLocal = existing.copy(
            paymentStatus = ExpensePaymentStatus.PAID,
            paidByUserId = currentUser.uid,
            paidTimestamp = now,
            updatedAt = now
        )

        if (authRepository.isDemoMode.value) {
            updateLocalExpenseInMemory(updatedLocal)
            return Result.success(Unit)
        }

        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore instance unavailable"))

        return try {
            db.collection("dealerships")
                .document(existing.dealershipId)
                .collection("stores")
                .document(existing.storeId)
                .collection("expenses")
                .document(expenseId)
                .update(
                    mapOf(
                        "paymentStatus" to ExpensePaymentStatus.PAID.name,
                        "paidByUserId" to currentUser.uid,
                        "paidTimestamp" to now,
                        "updatedAt" to now
                    )
                )
                .await()
            updateLocalExpenseInMemory(updatedLocal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reverseExpensePayment(expenseId: String, reversalReason: String): Result<Unit> {
        val currentUser = authRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("User is not authenticated"))

        if (!currentUser.active) {
            return Result.failure(SecurityException("User account is inactive"))
        }

        if (!isRoleAuthorizedForExpense(currentUser.role)) {
            return Result.failure(SecurityException("Role ${currentUser.role} is not authorized to reverse expense payments"))
        }

        val existing = getExpenseById(expenseId)
            ?: return Result.failure(IllegalArgumentException("Expense $expenseId not found"))

        if (existing.dealershipId != currentUser.dealershipId || !isStoreAuthorizedAndActive(currentUser, existing.storeId)) {
            return Result.failure(SecurityException("Expense $expenseId store scope is not authorized for current user"))
        }

        val validationError = ExpenseValidationUtils.validatePaymentTransition(
            currentPaymentStatus = existing.paymentStatus,
            approvalStatus = existing.approvalStatus,
            newPaymentStatus = ExpensePaymentStatus.REVERSED,
            reversalReason = reversalReason
        )
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val now = System.currentTimeMillis()
        val updatedLocal = existing.copy(
            paymentStatus = ExpensePaymentStatus.REVERSED,
            reversalReason = reversalReason.trim(),
            updatedAt = now
        )

        if (authRepository.isDemoMode.value) {
            updateLocalExpenseInMemory(updatedLocal)
            return Result.success(Unit)
        }

        val db = firestore
            ?: return Result.failure(IllegalStateException("Firestore instance unavailable"))

        return try {
            db.collection("dealerships")
                .document(existing.dealershipId)
                .collection("stores")
                .document(existing.storeId)
                .collection("expenses")
                .document(expenseId)
                .update(
                    mapOf(
                        "paymentStatus" to ExpensePaymentStatus.REVERSED.name,
                        "reversalReason" to reversalReason.trim(),
                        "updatedAt" to now
                    )
                )
                .await()
            updateLocalExpenseInMemory(updatedLocal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun updateLocalExpenseInMemory(updated: Expense) {
        val current = _expensesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.expenseId == updated.expenseId }
        if (index >= 0) {
            current[index] = updated
        } else {
            current.add(0, updated)
        }
        _expensesFlow.value = current
    }
}
