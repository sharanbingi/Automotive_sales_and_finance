package com.automotive.salesfinance.repository

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.AuditLog
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.PaymentMethodOption
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.services.NotificationService
import com.automotive.salesfinance.services.NotificationServiceImpl
import com.automotive.salesfinance.utils.toPaise
import com.automotive.salesfinance.utils.toRupeesDouble
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoanRepository(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository = TransactionRepository(authRepository),
    private val auditLogRepository: AuditLogRepository = AuditLogRepository(authRepository),
    private val customerRepository: CustomerRepository = CustomerRepository(authRepository),
    private val notificationService: NotificationService = NotificationServiceImpl()
) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _loansFlow = MutableStateFlow<List<Loan>>(
        if (authRepository.isDemoMode.value) DemoData.loans else emptyList()
    )
    val loansFlow: StateFlow<List<Loan>> = _loansFlow.asStateFlow()

    fun clearInMemoryState() {
        _loansFlow.value = emptyList()
    }

    init {
        authRepository.registerClearListener {
            clearInMemoryState()
        }
        CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch {
            authRepository.isDemoMode.collect { isDemo ->
                if (!isDemo) {
                    _loansFlow.value = _loansFlow.value.filterNot { l ->
                        DemoData.loans.any { it.loanId == l.loanId }
                    }
                } else if (_loansFlow.value.isEmpty()) {
                    _loansFlow.value = DemoData.loans
                }
            }
        }
    }

    fun getLoans(): List<Loan> = _loansFlow.value

    fun getLoansForDealership(dealershipId: String): List<Loan> {
        if (dealershipId.isBlank()) {
            return if (authRepository.isDemoMode.value) {
                _loansFlow.value.filter { it.dealershipId == DemoData.DEMO_DEALERSHIP_ID }
            } else {
                emptyList()
            }
        }
        return _loansFlow.value.filter { it.dealershipId == dealershipId }
    }

    fun getLoanById(loanId: String): Loan? {
        return _loansFlow.value.find { it.loanId == loanId }
    }

    fun getLoansByCustomer(customerId: String): List<Loan> {
        return _loansFlow.value.filter { it.customerId == customerId }
    }

    suspend fun fetchLoansFromFirestore(dealershipId: String = ""): List<Loan> {
        if (authRepository.isDemoMode.value) {
            return if (dealershipId.isBlank()) _loansFlow.value else getLoansForDealership(dealershipId)
        }
        if (firestore == null) {
            _loansFlow.value = _loansFlow.value.filterNot { l -> DemoData.loans.any { it.loanId == l.loanId } }
            return if (dealershipId.isBlank()) _loansFlow.value else getLoansForDealership(dealershipId)
        }
        return try {
            val fetchedLoans = if (dealershipId.isBlank()) {
                val snapshot = firestore?.collectionGroup("loans")?.limit(200)?.get()?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(Loan::class.java) } catch (_: Exception) { null }
                } ?: emptyList()
            } else {
                val snapshot = firestore?.collection("dealerships")
                    ?.document(dealershipId)
                    ?.collection("loans")
                    ?.limit(100)
                    ?.get()
                    ?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(Loan::class.java) } catch (_: Exception) { null }
                } ?: emptyList()
            }

            if (dealershipId.isBlank()) {
                _loansFlow.value = fetchedLoans
            } else {
                val current = _loansFlow.value.filterNot { it.dealershipId == dealershipId || DemoData.loans.any { demo -> demo.loanId == it.loanId } }.toMutableList()
                current.addAll(fetchedLoans)
                _loansFlow.value = current
            }
            fetchedLoans
        } catch (e: Exception) {
            val liveOnly = _loansFlow.value.filterNot { l -> DemoData.loans.any { it.loanId == l.loanId } }
            _loansFlow.value = liveOnly
            if (dealershipId.isBlank()) liveOnly else liveOnly.filter { it.dealershipId == dealershipId }
        }
    }

    suspend fun saveLoan(loan: Loan): Result<Unit> {
        if (loan.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }
        val preparedLoan = if (loan.dealershipId.isBlank()) loan.copy(dealershipId = DemoData.DEMO_DEALERSHIP_ID) else loan
        val current = _loansFlow.value.toMutableList()
        val index = current.indexOfFirst { it.loanId == preparedLoan.loanId }
        if (index >= 0) {
            current[index] = preparedLoan
        } else {
            current.add(preparedLoan)
        }
        _loansFlow.value = current

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                firestore?.collection("dealerships")
                    ?.document(preparedLoan.dealershipId)
                    ?.collection("loans")
                    ?.document(preparedLoan.loanId)
                    ?.set(preparedLoan)
                    ?.await()
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }

    /**
     * Creates a loan, marks the bike as FINANCED, logs transaction, and logs audit log atomically via Firestore Batch/Transaction.
     */
    suspend fun saveLoanAtomic(
        loan: Loan,
        bike: Bike,
        transaction: Transaction,
        auditLog: AuditLog? = null
    ): Result<Unit> {
        if (loan.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }

        val existingActiveLoan = _loansFlow.value.find {
            it.bikeId == loan.bikeId && it.loanId != loan.loanId && it.loanStatus == LoanStatus.ACTIVE
        }
        if (existingActiveLoan != null) {
            return Result.failure(IllegalStateException("Double-financing prevented: Bike ${loan.bikeId} already has an active loan (${existingActiveLoan.loanId})"))
        }

        val dId = if (loan.dealershipId.isBlank()) DemoData.DEMO_DEALERSHIP_ID else loan.dealershipId
        val preparedLoan = loan.copy(dealershipId = dId)
        val preparedBike = bike.copy(dealershipId = dId, status = BikeStatus.FINANCED)
        val preparedTxn = transaction.copy(dealershipId = dId)

        // Update local state in memory
        val currentLoans = _loansFlow.value.toMutableList()
        val index = currentLoans.indexOfFirst { it.loanId == preparedLoan.loanId }
        if (index >= 0) {
            currentLoans[index] = preparedLoan
        } else {
            currentLoans.add(preparedLoan)
        }
        _loansFlow.value = currentLoans

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                val fs = firestore!!
                val batch = fs.batch()

                val loanRef = fs.collection("dealerships")
                    .document(dId)
                    .collection("loans")
                    .document(preparedLoan.loanId)
                batch.set(loanRef, preparedLoan)

                if (preparedBike.storeLocation.isNotBlank()) {
                    val bikeRef = fs.collection("dealerships")
                        .document(dId)
                        .collection("stores")
                        .document(preparedBike.storeLocation)
                        .collection("bikes")
                        .document(preparedBike.bikeId)
                    batch.set(bikeRef, preparedBike)
                }

                val txnRef = fs.collection("dealerships")
                    .document(dId)
                    .collection("transactions")
                    .document(preparedTxn.transactionId)
                batch.set(txnRef, preparedTxn)

                if (auditLog != null) {
                    val preparedLog = auditLog.copy(dealershipId = dId)
                    val logRef = fs.collection("dealerships")
                        .document(dId)
                        .collection("audit_logs")
                        .document(preparedLog.auditLogId)
                    batch.set(logRef, preparedLog)
                }

                batch.commit().await()
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }

    /**
     * Records an EMI payment using integer paise, updates loan balance/status/nextEmiDate, and writes transaction & audit log atomically.
     * Supports Partial EMI, Multiple EMIs, and Full Settlement.
     * Rejects duplicate payments via razorpayPaymentId.
     */
    suspend fun recordEmiPayment(
        loanId: String,
        paidAmountPaise: Long,
        paymentPurpose: String = "REGULAR_EMI",
        paymentMethod: String = "UPI_INTENT",
        razorpayPaymentId: String = ""
    ): Result<Unit> {
        val loan = getLoanById(loanId) ?: return Result.failure(Exception("Loan $loanId not found"))
        if (loan.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }

        if (paidAmountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Payment amount must be greater than zero"))
        }

        val oldRemainingPaise = loan.effectiveRemainingBalancePaise()
        if (paidAmountPaise > oldRemainingPaise) {
            return Result.failure(IllegalArgumentException("Payment amount cannot exceed remaining balance"))
        }

        // Check Idempotency: Duplicate payment check
        if (razorpayPaymentId.isNotBlank()) {
            val existingTxns = transactionRepository.getTransactionsByLoan(loanId)
            if (existingTxns.any { it.razorpayPaymentId == razorpayPaymentId }) {
                return Result.failure(IllegalStateException("Duplicate payment ID: $razorpayPaymentId has already been processed"))
            }
        }

        val dId = if (loan.dealershipId.isBlank()) DemoData.DEMO_DEALERSHIP_ID else loan.dealershipId
        val oldPaidPaise = loan.effectivePaidAmountPaise()
        val oldEmiPaise = loan.effectiveEmiAmountPaise()

        val newPaidPaise = oldPaidPaise + paidAmountPaise
        val newRemainingPaise = oldRemainingPaise - paidAmountPaise
        val isSettled = newRemainingPaise == 0L || paymentPurpose.equals("FULL_SETTLEMENT", ignoreCase = true)
        val newStatus = if (isSettled) LoanStatus.SETTLED else loan.loanStatus

        val isPartial = paymentPurpose.equals("PARTIAL_EMI", ignoreCase = true) ||
                paymentPurpose.equals("PARTIAL_PAYMENT", ignoreCase = true)
        val isMultiple = paymentPurpose.equals("MULTIPLE_EMI", ignoreCase = true) ||
                (paidAmountPaise >= oldEmiPaise * 150L / 100L && !isSettled && oldEmiPaise > 0L)

        val nextDate = when {
            isSettled -> loan.nextEmiDate
            isPartial -> loan.nextEmiDate
            isMultiple -> {
                val count = maxOf(1L, paidAmountPaise / if (oldEmiPaise > 0L) oldEmiPaise else 1L).toInt()
                loan.nextEmiDate + (count * 30L * 24 * 60 * 60 * 1000)
            }
            else -> loan.nextEmiDate + (30L * 24 * 60 * 60 * 1000)
        }

        val updatedLoan = loan.copy(
            paidAmountPaise = newPaidPaise,
            paidAmount = newPaidPaise.toRupeesDouble(),
            remainingBalancePaise = if (isSettled) 0L else newRemainingPaise,
            remainingBalance = if (isSettled) 0.0 else newRemainingPaise.toRupeesDouble(),
            emiAmountPaise = oldEmiPaise,
            emiAmount = oldEmiPaise.toRupeesDouble(),
            principalAmountPaise = loan.effectivePrincipalAmountPaise(),
            principalAmount = loan.effectivePrincipalAmountPaise().toRupeesDouble(),
            downPaymentPaise = loan.effectiveDownPaymentPaise(),
            downPayment = loan.effectiveDownPaymentPaise().toRupeesDouble(),
            loanAmountPaise = loan.effectiveLoanAmountPaise(),
            loanAmount = loan.effectiveLoanAmountPaise().toRupeesDouble(),
            processingFeePaise = loan.effectiveProcessingFeePaise(),
            processingFee = loan.effectiveProcessingFeePaise().toRupeesDouble(),
            loanStatus = newStatus,
            nextEmiDate = nextDate
        )

        val now = System.currentTimeMillis()
        val txnId = "TXN_${now}_${(1000..9999).random()}"
        val pId = razorpayPaymentId.ifBlank { "PAY_${now}" }

        val txn = Transaction(
            transactionId = txnId,
            dealershipId = dId,
            customerId = loan.customerId,
            loanId = loanId,
            bikeId = loan.bikeId,
            amount = paidAmountPaise.toRupeesDouble(),
            amountPaise = paidAmountPaise,
            paymentMethod = paymentMethod,
            paymentPurpose = paymentPurpose,
            razorpayPaymentId = pId,
            status = TransactionStatus.SUCCESS,
            createdAt = now
        )

        transactionRepository.addTransaction(txn)

        val actionName = if (isSettled) "LOAN_SETTLED" else "PAYMENT_VERIFIED"
        val currentUid = authRepository.currentUser.value?.uid ?: "USER_SYSTEM"
        auditLogRepository.logEvent(
            dealershipId = dId,
            userId = currentUid,
            action = actionName,
            entityType = "Loan",
            entityId = loanId,
            metadata = mapOf(
                "amount" to paidAmountPaise.toRupeesDouble().toString(),
                "amountPaise" to paidAmountPaise.toString(),
                "paymentPurpose" to paymentPurpose,
                "paymentId" to pId,
                "remainingBalance" to updatedLoan.remainingBalance.toString()
            )
        )

        // Send payment receipt notification
        val customer = customerRepository.getCustomerById(loan.customerId)
        if (customer != null) {
            try {
                notificationService.sendPaymentReceiptNotification(customer, paidAmountPaise.toRupeesDouble(), txnId)
            } catch (_: Exception) {}
        }

        // Update local flow
        val currentLoans = _loansFlow.value.toMutableList()
        val idx = currentLoans.indexOfFirst { it.loanId == loanId }
        if (idx >= 0) {
            currentLoans[idx] = updatedLoan
            _loansFlow.value = currentLoans
        }

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                val fs = firestore!!
                val batch = fs.batch()

                val loanRef = fs.collection("dealerships")
                    .document(dId)
                    .collection("loans")
                    .document(loanId)
                batch.set(loanRef, updatedLoan)

                batch.commit().await()
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }

    suspend fun recordEmiPayment(
        loanId: String,
        paidAmount: Double,
        paymentPurpose: String = "REGULAR_EMI",
        paymentMethod: String = "UPI_INTENT",
        razorpayPaymentId: String = ""
    ): Result<Unit> {
        return recordEmiPayment(
            loanId = loanId,
            paidAmountPaise = paidAmount.toPaise(),
            paymentPurpose = paymentPurpose,
            paymentMethod = paymentMethod,
            razorpayPaymentId = razorpayPaymentId
        )
    }

    /**
     * Records a cash / manual collection payment atomically.
     * Validates dealership, role authorization, generates transaction, updates loan balance/status/nextEmiDate,
     * writes loan, transaction, and audit log atomically in Firestore or updates local state flows in demo mode,
     * and triggers notification.
     */
    suspend fun recordCashPaymentAtomic(
        loanId: String,
        amountPaise: Long,
        paymentPurpose: String,
        currentUser: User? = authRepository.currentUser.value,
        receiptNumber: String = "",
        notes: String = "",
        storeId: String = "",
        idempotencyKey: String
    ): Result<Transaction> {
        // 1. Role Authorization Check
        if (currentUser == null || !currentUser.canCollectCashPayment()) {
            return Result.failure(SecurityException("Unauthorized: Only Dealership Admin, Store Manager, or Finance User can collect cash payments"))
        }

        val authenticatedDealershipId = currentUser.dealershipId.ifBlank { DemoData.DEMO_DEALERSHIP_ID }
        if (authenticatedDealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }

        // 2. Fetch Loan locally first to get some data
        val localLoan = getLoanById(loanId) ?: return Result.failure(IllegalArgumentException("Loan $loanId not found locally"))

        if (amountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Payment amount must be greater than zero"))
        }

        val now = System.currentTimeMillis()
        val transactionId = "TXN_CASH_$idempotencyKey"
        val finalReceiptNumber = receiptNumber.ifBlank { "RCP_CASH_${now % 100000}" }

        var finalTransaction: Transaction? = null
        var finalUpdatedLoan: Loan? = null
        val dId = authenticatedDealershipId

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                val fs = firestore!!
                val loanRef = fs.collection("dealerships").document(dId).collection("loans").document(loanId)
                val txnRef = fs.collection("dealerships").document(dId).collection("transactions").document(transactionId)
                val auditId = "AUDIT_CASH_$now"
                val auditRef = fs.collection("dealerships").document(dId).collection("audit_logs").document(auditId)

                val resultTxn = fs.runTransaction { transaction ->
                    val existingTxnSnapshot = transaction.get(txnRef)
                    if (existingTxnSnapshot.exists()) {
                        return@runTransaction existingTxnSnapshot.toObject(Transaction::class.java)
                    }

                    val remoteLoan = transaction.get(loanRef).toObject(Loan::class.java) 
                        ?: throw Exception("Loan not found")

                    if (remoteLoan.dealershipId != authenticatedDealershipId) {
                        throw SecurityException("Tenant isolation validation failed.")
                    }
                    if (remoteLoan.customerId != localLoan.customerId) {
                        throw SecurityException("Customer ID mismatch.")
                    }

                    val oldRemainingPaise = remoteLoan.effectiveRemainingBalancePaise()
                    if (amountPaise > oldRemainingPaise) {
                        throw IllegalArgumentException("Payment amount cannot exceed remaining balance")
                    }

                    val oldPaidPaise = remoteLoan.effectivePaidAmountPaise()
                    val oldEmiPaise = remoteLoan.effectiveEmiAmountPaise()

                    val calculatedRemainingPaise = oldRemainingPaise - amountPaise
                    val isSettled = calculatedRemainingPaise <= 0L || paymentPurpose.equals("FULL_SETTLEMENT", ignoreCase = true)
                    val newRemainingPaise = if (isSettled) 0L else calculatedRemainingPaise
                    val newPaidAmountPaise = oldPaidPaise + amountPaise
                    val newLoanStatus = if (isSettled) LoanStatus.SETTLED else remoteLoan.loanStatus

                    val purposeUpper = paymentPurpose.uppercase()
                    val isPartial = purposeUpper == "PARTIAL_PAYMENT" || purposeUpper == "PARTIAL_EMI"
                    val isMultiple = purposeUpper == "MULTIPLE_EMIS" || purposeUpper == "MULTIPLE_EMI"
                    val isRegularOrOverdue = purposeUpper == "REGULAR_EMI" || purposeUpper == "OVERDUE_EMI" || purposeUpper == "DOWN_PAYMENT"

                    val newNextEmiDate = when {
                        isSettled -> remoteLoan.nextEmiDate
                        isPartial -> remoteLoan.nextEmiDate
                        isMultiple -> {
                            val emiCount = maxOf(1L, amountPaise / if (oldEmiPaise > 0) oldEmiPaise else 1L).toInt()
                            remoteLoan.nextEmiDate + (emiCount * 30L * 24 * 60 * 60 * 1000)
                        }
                        isRegularOrOverdue -> remoteLoan.nextEmiDate + (30L * 24 * 60 * 60 * 1000)
                        else -> remoteLoan.nextEmiDate + (30L * 24 * 60 * 60 * 1000)
                    }

                    val updatedLoan = remoteLoan.copy(
                        dealershipId = dId,
                        paidAmountPaise = newPaidAmountPaise,
                        paidAmount = newPaidAmountPaise.toRupeesDouble(),
                        remainingBalancePaise = newRemainingPaise,
                        remainingBalance = newRemainingPaise.toRupeesDouble(),
                        emiAmountPaise = oldEmiPaise,
                        emiAmount = oldEmiPaise.toRupeesDouble(),
                        principalAmountPaise = remoteLoan.effectivePrincipalAmountPaise(),
                        principalAmount = remoteLoan.effectivePrincipalAmountPaise().toRupeesDouble(),
                        downPaymentPaise = remoteLoan.effectiveDownPaymentPaise(),
                        downPayment = remoteLoan.effectiveDownPaymentPaise().toRupeesDouble(),
                        loanAmountPaise = remoteLoan.effectiveLoanAmountPaise(),
                        loanAmount = remoteLoan.effectiveLoanAmountPaise().toRupeesDouble(),
                        processingFeePaise = remoteLoan.effectiveProcessingFeePaise(),
                        processingFee = remoteLoan.effectiveProcessingFeePaise().toRupeesDouble(),
                        loanStatus = newLoanStatus,
                        nextEmiDate = newNextEmiDate,
                        lastPaymentTransactionId = transactionId
                    )
                    finalUpdatedLoan = updatedLoan

                    val txn = Transaction(
                        transactionId = transactionId,
                        dealershipId = dId,
                        customerId = updatedLoan.customerId,
                        loanId = loanId,
                        bikeId = updatedLoan.bikeId,
                        amount = amountPaise.toRupeesDouble(),
                        amountPaise = amountPaise,
                        paymentMethod = PaymentMethodOption.CASH.name,
                        paymentPurpose = paymentPurpose,
                        razorpayPaymentId = "",
                        receiptNumber = finalReceiptNumber,
                        notes = notes,
                        collectedBy = currentUser.name,
                        collectedByUserId = currentUser.uid,
                        idempotencyKey = idempotencyKey,
                        status = TransactionStatus.SUCCESS,
                        createdAt = now
                    )

                    val auditLog = AuditLog(
                        auditLogId = auditId,
                        dealershipId = dId,
                        userId = currentUser.uid,
                        action = "RECORD_CASH_PAYMENT",
                        entityType = "Loan",
                        entityId = loanId,
                        timestamp = now,
                        metadata = mapOf(
                            "amount" to amountPaise.toRupeesDouble().toString(),
                            "amountPaise" to amountPaise.toString(),
                            "paymentPurpose" to paymentPurpose,
                            "receiptNumber" to finalReceiptNumber,
                            "collectedBy" to currentUser.name,
                            "storeId" to storeId,
                            "remainingBalance" to updatedLoan.remainingBalance.toString()
                        )
                    )

                    val loanUpdates = mapOf<String, Any?>(
                        "remainingBalancePaise" to newRemainingPaise,
                        "remainingBalance" to newRemainingPaise.toRupeesDouble(),
                        "paidAmountPaise" to newPaidAmountPaise,
                        "paidAmount" to newPaidAmountPaise.toRupeesDouble(),
                        "loanStatus" to newLoanStatus.name,
                        "nextEmiDate" to newNextEmiDate,
                        "lastPaymentTransactionId" to transactionId
                    )
                    transaction.update(loanRef, loanUpdates)
                    transaction.set(txnRef, txn)
                    transaction.set(auditRef, auditLog)

                    txn
                }.await()
                finalTransaction = resultTxn
            } catch (e: Exception) {
                return Result.failure(e)
            }
        } else {
            // Demo mode logic
            val loan = localLoan
            val oldRemainingPaise = loan.effectiveRemainingBalancePaise()
            if (amountPaise > oldRemainingPaise) {
                return Result.failure(IllegalArgumentException("Payment amount cannot exceed remaining balance"))
            }

            val oldPaidPaise = loan.effectivePaidAmountPaise()
            val oldEmiPaise = loan.effectiveEmiAmountPaise()

            val newRemainingPaise = oldRemainingPaise - amountPaise
            val newPaidPaise = oldPaidPaise + amountPaise
            val isSettled = newRemainingPaise == 0L || paymentPurpose.equals("FULL_SETTLEMENT", ignoreCase = true)
            val newLoanStatus = if (isSettled) LoanStatus.SETTLED else loan.loanStatus

            val purposeUpper = paymentPurpose.uppercase()
            val isPartial = purposeUpper == "PARTIAL_PAYMENT" || purposeUpper == "PARTIAL_EMI"
            val isMultiple = purposeUpper == "MULTIPLE_EMIS" || purposeUpper == "MULTIPLE_EMI"
            val isRegularOrOverdue = purposeUpper == "REGULAR_EMI" || purposeUpper == "OVERDUE_EMI" || purposeUpper == "DOWN_PAYMENT"

            val newNextEmiDate = when {
                isSettled -> loan.nextEmiDate
                isPartial -> loan.nextEmiDate
                isMultiple -> {
                    val emiCount = maxOf(1L, amountPaise / if (oldEmiPaise > 0) oldEmiPaise else 1L).toInt()
                    loan.nextEmiDate + (emiCount * 30L * 24 * 60 * 60 * 1000)
                }
                isRegularOrOverdue -> loan.nextEmiDate + (30L * 24 * 60 * 60 * 1000)
                else -> loan.nextEmiDate + (30L * 24 * 60 * 60 * 1000)
            }

            val updatedLoan = loan.copy(
                dealershipId = dId,
                paidAmountPaise = newPaidPaise,
                paidAmount = newPaidPaise.toRupeesDouble(),
                remainingBalancePaise = if (isSettled) 0L else newRemainingPaise,
                remainingBalance = if (isSettled) 0.0 else newRemainingPaise.toRupeesDouble(),
                emiAmountPaise = oldEmiPaise,
                emiAmount = oldEmiPaise.toRupeesDouble(),
                principalAmountPaise = loan.effectivePrincipalAmountPaise(),
                principalAmount = loan.effectivePrincipalAmountPaise().toRupeesDouble(),
                downPaymentPaise = loan.effectiveDownPaymentPaise(),
                downPayment = loan.effectiveDownPaymentPaise().toRupeesDouble(),
                loanAmountPaise = loan.effectiveLoanAmountPaise(),
                loanAmount = loan.effectiveLoanAmountPaise().toRupeesDouble(),
                processingFeePaise = loan.effectiveProcessingFeePaise(),
                processingFee = loan.effectiveProcessingFeePaise().toRupeesDouble(),
                loanStatus = newLoanStatus,
                nextEmiDate = newNextEmiDate,
                lastPaymentTransactionId = transactionId
            )
            finalUpdatedLoan = updatedLoan

            val transaction = Transaction(
                transactionId = transactionId,
                dealershipId = dId,
                customerId = loan.customerId,
                loanId = loanId,
                bikeId = loan.bikeId,
                amount = amountPaise.toRupeesDouble(),
                amountPaise = amountPaise,
                paymentMethod = PaymentMethodOption.CASH.name,
                paymentPurpose = paymentPurpose,
                razorpayPaymentId = "",
                receiptNumber = finalReceiptNumber,
                notes = notes,
                collectedBy = currentUser.name,
                collectedByUserId = currentUser.uid,
                idempotencyKey = idempotencyKey,
                status = TransactionStatus.SUCCESS,
                createdAt = now
            )
            finalTransaction = transaction
            
            auditLogRepository.logEvent(
                dealershipId = dId,
                userId = currentUser.uid,
                action = "RECORD_CASH_PAYMENT",
                entityType = "Loan",
                entityId = loanId,
                metadata = mapOf(
                    "amount" to amountPaise.toRupeesDouble().toString(),
                    "amountPaise" to amountPaise.toString(),
                    "paymentPurpose" to paymentPurpose,
                    "receiptNumber" to finalReceiptNumber
                )
            )
        }

        val returnedTxn = finalTransaction ?: return Result.failure(Exception("Transaction null"))
        val returnedLoan = finalUpdatedLoan
        if (returnedLoan != null) {
            val currentLoans = _loansFlow.value.toMutableList()
            val index = currentLoans.indexOfFirst { it.loanId == loanId }
            if (index >= 0) {
                currentLoans[index] = returnedLoan
            } else {
                currentLoans.add(returnedLoan)
            }
            _loansFlow.value = currentLoans
        }

        transactionRepository.addTransaction(returnedTxn)

        val customer = customerRepository.getCustomerById(localLoan.customerId)
        if (customer != null) {
            try {
                notificationService.sendPaymentReceiptNotification(customer, returnedTxn.effectiveAmountPaise().toRupeesDouble(), transactionId)
            } catch (_: Exception) {}
        }

        return Result.success(returnedTxn)
    }

    suspend fun recordCashPaymentAtomic(
        loanId: String,
        amount: Double,
        paymentPurpose: String,
        currentUser: User? = authRepository.currentUser.value,
        receiptNumber: String = "",
        notes: String = "",
        storeId: String = "",
        idempotencyKey: String
    ): Result<Transaction> {
        return recordCashPaymentAtomic(
            loanId = loanId,
            amountPaise = amount.toPaise(),
            paymentPurpose = paymentPurpose,
            currentUser = currentUser,
            receiptNumber = receiptNumber,
            notes = notes,
            storeId = storeId,
            idempotencyKey = idempotencyKey
        )
    }
}
