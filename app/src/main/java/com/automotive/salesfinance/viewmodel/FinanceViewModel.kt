package com.automotive.salesfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.FeatureAccessManager
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.services.PaymentResult
import com.automotive.salesfinance.services.PaymentService
import com.automotive.salesfinance.services.RazorpayPaymentServiceImpl
import com.automotive.salesfinance.utils.toPaise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.pow

enum class PaymentState {
    IDLE,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED
}

data class EmiCalculationResult(
    val vehiclePrice: Double = 0.0,
    val downPayment: Double = 0.0,
    val principal: Double = 0.0,
    val annualInterestRate: Double = 0.0,
    val tenureMonths: Int = 0,
    val monthlyEmi: Double = 0.0,
    val totalRepayment: Double = 0.0,
    val totalInterest: Double = 0.0
)

data class LoanMetrics(
    val totalFinancedValue: Double = 0.0,
    val activeLoansCount: Int = 0,
    val overdueCount: Int = 0,
    val settledCount: Int = 0
)

data class FinanceUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class FinanceViewModel(
    private val loanRepository: LoanRepository,
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
    private val auditLogRepository: AuditLogRepository,
    val authRepository: AuthRepository,
    private val paymentService: PaymentService = RazorpayPaymentServiceImpl(),
    private val dealershipRepository: DealershipRepository
) : ViewModel() {

    val isDemoMode: StateFlow<Boolean> = authRepository.isDemoMode

    val currentPlan: StateFlow<SubscriptionPlan> = combine(
        authRepository.currentUser,
        dealershipRepository.dealershipsFlow
    ) { user, dealerships ->
        val dId = user?.dealershipId.orEmpty().ifBlank {
            if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
        }
        val d = dealerships.find { it.dealershipId == dId }
            ?: if (authRepository.isDemoMode.value) DemoData.dealerships.firstOrNull() else null
        SubscriptionPlan.getPlanById(d?.subscriptionPlan ?: "STARTER")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionPlan.STARTER)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private val _paymentState = MutableStateFlow(PaymentState.IDLE)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()
    val errorMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow(true)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    val loansFlow: StateFlow<List<Loan>> = loanRepository.loansFlow
    val customersFlow: StateFlow<List<Customer>> = customerRepository.customersFlow

    val filteredLoans: StateFlow<List<Loan>> = combine(
        loanRepository.loansFlow,
        customerRepository.customersFlow,
        inventoryRepository.bikesFlow,
        authRepository.currentUser,
        _statusFilter,
        _searchQuery
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val loans = flows[0] as List<Loan>
        @Suppress("UNCHECKED_CAST")
        val customers = flows[1] as List<Customer>
        @Suppress("UNCHECKED_CAST")
        val bikes = flows[2] as List<Bike>
        val user = flows[3] as? User
        val status = flows[4] as String
        val query = flows[5] as String

        val dId = user?.dealershipId.orEmpty()
        loans.filter { loan ->
            // Hierarchy: dealershipId -> stateCode -> storeId -> entities
            val matchesDealership = dId.isBlank() || loan.dealershipId == dId
            val matchesStatus = when (status.uppercase()) {
                "ALL" -> true
                "ACTIVE" -> loan.loanStatus == LoanStatus.ACTIVE
                "OVERDUE" -> loan.loanStatus == LoanStatus.OVERDUE ||
                    (loan.loanStatus == LoanStatus.ACTIVE && loan.nextEmiDate > 0 && loan.nextEmiDate < System.currentTimeMillis())
                "SETTLED" -> loan.loanStatus == LoanStatus.SETTLED
                else -> true
            }

            val customer = customers.find { it.customerId == loan.customerId }
            val bike = bikes.find { it.bikeId == loan.bikeId }
            val q = query.trim().lowercase()

            val matchesQuery = q.isEmpty() ||
                loan.loanId.lowercase().contains(q) ||
                (customer != null && customer.fullName.lowercase().contains(q)) ||
                (bike != null && (bike.make.lowercase().contains(q) || bike.model.lowercase().contains(q) || bike.chassisNumber.lowercase().contains(q)))

            matchesDealership && matchesStatus && matchesQuery
        }.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loanMetrics: StateFlow<LoanMetrics> = combine(
        loanRepository.loansFlow,
        authRepository.currentUser
    ) { loans, user ->
        val dId = user?.dealershipId.orEmpty()
        val filtered = loans.filter { dId.isBlank() || it.dealershipId == dId }
        val active = filtered.filter { it.loanStatus == LoanStatus.ACTIVE }
        val overdue = filtered.filter {
            it.loanStatus == LoanStatus.OVERDUE ||
                (it.loanStatus == LoanStatus.ACTIVE && it.nextEmiDate > 0 && it.nextEmiDate < System.currentTimeMillis())
        }
        val settled = filtered.filter { it.loanStatus == LoanStatus.SETTLED }
        val totalFinanced = filtered.sumOf { it.totalAmount }

        LoanMetrics(
            totalFinancedValue = totalFinanced,
            activeLoansCount = active.size,
            overdueCount = overdue.size,
            settledCount = settled.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoanMetrics())

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentState.IDLE
    }

    fun cancelPayment() {
        _paymentState.value = PaymentState.CANCELLED
        _operationMessage.value = "Payment cancelled by user"
    }

    companion object {
        fun calculateEmi(
            vehiclePrice: Double,
            downPayment: Double,
            annualRatePercent: Double,
            tenureMonths: Int
        ): EmiCalculationResult {
            val principal = (vehiclePrice - downPayment).coerceAtLeast(0.0)
            if (principal <= 0.0 || tenureMonths <= 0) {
                return EmiCalculationResult(
                    vehiclePrice = vehiclePrice,
                    downPayment = downPayment,
                    principal = principal,
                    annualInterestRate = annualRatePercent,
                    tenureMonths = tenureMonths,
                    monthlyEmi = 0.0,
                    totalRepayment = 0.0,
                    totalInterest = 0.0
                )
            }

            if (annualRatePercent <= 0.0) {
                val emi = principal / tenureMonths
                return EmiCalculationResult(
                    vehiclePrice = vehiclePrice,
                    downPayment = downPayment,
                    principal = principal,
                    annualInterestRate = annualRatePercent,
                    tenureMonths = tenureMonths,
                    monthlyEmi = emi,
                    totalRepayment = principal,
                    totalInterest = 0.0
                )
            }

            val r = annualRatePercent / (12.0 * 100.0)
            val compound = (1.0 + r).pow(tenureMonths.toDouble())
            val emi = principal * r * compound / (compound - 1.0)
            val totalRepayment = emi * tenureMonths
            val totalInterest = totalRepayment - principal

            return EmiCalculationResult(
                vehiclePrice = vehiclePrice,
                downPayment = downPayment,
                principal = principal,
                annualInterestRate = annualRatePercent,
                tenureMonths = tenureMonths,
                monthlyEmi = emi,
                totalRepayment = totalRepayment,
                totalInterest = totalInterest
            )
        }
    }

    fun getLoanById(loanId: String): Loan? {
        return loanRepository.getLoanById(loanId)
    }

    fun getCustomerById(customerId: String): Customer? {
        return customerRepository.getCustomerById(customerId)
    }

    fun getBikeById(bikeId: String) = inventoryRepository.getBikeById(bikeId)

    fun getTransactionsByLoan(loanId: String) = transactionRepository.getTransactionsByLoan(loanId)

    fun calculateMultipleEmiAmount(loanId: String, emiCount: Int): Double {
        val loan = loanRepository.getLoanById(loanId) ?: return 0.0
        val total = loan.emiAmount * emiCount
        return if (loan.remainingBalance > 0) total.coerceAtMost(loan.remainingBalance) else total
    }

    fun calculateFullSettlementAmount(loanId: String): Double {
        val loan = loanRepository.getLoanById(loanId) ?: return 0.0
        return loan.remainingBalance
    }

    fun createLoanAndFinanceBike(
        customerId: String,
        bikeId: String,
        vehiclePrice: Double,
        downPayment: Double,
        interestRate: Double,
        tenureMonths: Int,
        onSuccess: (Loan) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val plan = currentPlan.value
        if (!FeatureAccessManager.hasFinance(plan)) {
            val err = "Feature unavailable: finance module is disabled on your current plan (${plan.name})"
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        val user = authRepository.currentUser.value
        val effectiveDId = user?.dealershipId.orEmpty().ifBlank {
            if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
        }
        val dealership = dealershipRepository.dealershipsFlow.value.find { it.dealershipId == effectiveDId }
            ?: if (authRepository.isDemoMode.value) DemoData.dealerships.firstOrNull() else null

        if (!FeatureAccessManager.isSubscriptionActive(dealership)) {
            val err = "Subscription inactive: account is in read-only mode"
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        val customer = customerRepository.getCustomerById(customerId)
        if (customer == null) {
            val err = "Customer not found (ID: $customerId)"
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        val bike = inventoryRepository.getBikeById(bikeId)
        if (bike == null) {
            val err = "Bike not found (ID: $bikeId)"
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        if (bike.status != BikeStatus.AVAILABLE) {
            val err = "Cannot finance bike $bikeId: Bike status is ${bike.status} (must be AVAILABLE)"
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        val existingActiveLoan = loanRepository.getLoans().find { it.bikeId == bikeId && it.loanStatus == LoanStatus.ACTIVE }
        if (existingActiveLoan != null) {
            val err = "Cannot finance bike $bikeId: Bike already has an active loan (${existingActiveLoan.loanId})"
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        val dId = if (effectiveDId.isNotBlank()) effectiveDId else bike.dealershipId

        val emiResult = calculateEmi(vehiclePrice, downPayment, interestRate, tenureMonths)
        val loanId = "LOAN_${System.currentTimeMillis()}"
        val upiMandateHash = "MANDATE_UPI_${(100000..999999).random()}"
        val now = System.currentTimeMillis()
        val nextEmiDate = now + (30L * 24 * 60 * 60 * 1000)

        val newLoan = Loan(
            loanId = loanId,
            dealershipId = dId,
            customerId = customerId,
            bikeId = bikeId,
            totalAmount = emiResult.totalRepayment,
            paidAmount = downPayment,
            remainingBalance = emiResult.totalRepayment,
            emiAmount = emiResult.monthlyEmi,
            nextEmiDate = nextEmiDate,
            upiAutoPayMandateHash = upiMandateHash,
            loanStatus = LoanStatus.ACTIVE,
            tenureMonths = tenureMonths,
            interestRate = interestRate,
            downPayment = downPayment,
            createdAt = now
        )

        val txn = Transaction(
            transactionId = "TXN_${now}_${(1000..9999).random()}",
            dealershipId = dId,
            customerId = customerId,
            loanId = loanId,
            bikeId = bikeId,
            amount = downPayment,
            paymentMethod = "DOWN_PAYMENT",
            paymentPurpose = "DOWN_PAYMENT",
            razorpayPaymentId = "PAY_DOWN_${now}",
            status = TransactionStatus.SUCCESS,
            createdAt = now
        )

        viewModelScope.launch {
            _isLoading.value = true
            val saveResult = loanRepository.saveLoanAtomic(
                loan = newLoan,
                bike = bike.copy(dealershipId = dId, status = BikeStatus.FINANCED),
                transaction = txn
            )

            if (saveResult.isSuccess) {
                inventoryRepository.markBikeFinanced(bike.storeLocation, bikeId)
                _operationMessage.value = "Loan $loanId created and bike $bikeId marked as FINANCED"
                _isSuccess.value = true
                onSuccess(newLoan)
            } else {
                val msg = saveResult.exceptionOrNull()?.message ?: "Failed to save loan"
                _operationMessage.value = msg
                _isSuccess.value = false
                onError(msg)
            }
            _isLoading.value = false
        }
    }

    /**
     * Complete Customer Payment Processing via Razorpay / UPI.
     * Persists transaction, updates loan balance & next EMI date, recalculates loan status,
     * logs audit entry, and updates bike status if down payment.
     */
    fun processCustomerPayment(
        loanId: String,
        amount: Double,
        purpose: String = "REGULAR_EMI",
        paymentMethod: String = "UPI_INTENT",
        razorpayPaymentId: String? = null,
        upiApp: String? = null,
        vpaId: String? = null,
        onSuccess: (PaymentResult) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val plan = currentPlan.value
        if (!FeatureAccessManager.hasFinance(plan)) {
            val err = "Feature unavailable: finance module is disabled on your current plan (${plan.name})"
            _operationMessage.value = err
            _paymentState.value = PaymentState.FAILED
            _isSuccess.value = false
            onError(err)
            return
        }

        if (!authRepository.isDemoMode.value) {
            val err = "Live Payment Verification Unavailable — Backend Deployment Required"
            _operationMessage.value = err
            _paymentState.value = PaymentState.FAILED
            _isSuccess.value = false
            onError(err)
            return
        }

        val loan = loanRepository.getLoanById(loanId)
        if (loan == null) {
            val err = "Loan not found (ID: $loanId)"
            _operationMessage.value = err
            _paymentState.value = PaymentState.FAILED
            _isSuccess.value = false
            onError(err)
            return
        }

        if (amount <= 0.0) {
            val err = "Payment amount must be greater than zero"
            _operationMessage.value = err
            _paymentState.value = PaymentState.FAILED
            _isSuccess.value = false
            onError(err)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _paymentState.value = PaymentState.PROCESSING

            val paymentResult = paymentService.processCustomerEmiPayment(
                loanId = loanId,
                customerId = loan.customerId,
                amount = amount,
                purpose = purpose,
                paymentMethod = paymentMethod,
                razorpayPaymentId = razorpayPaymentId,
                upiApp = upiApp,
                vpaId = vpaId
            )

            if (paymentResult.isSuccess) {
                val res = paymentResult.getOrThrow()

                // Record EMI payment in repository (updates loan balance, status, next EMI date, saves transaction & audit log, sends notification)
                val recordResult = loanRepository.recordEmiPayment(
                    loanId = loanId,
                    paidAmount = amount,
                    paymentPurpose = purpose,
                    paymentMethod = res.paymentMethod,
                    razorpayPaymentId = res.paymentId
                )

                if (recordResult.isFailure) {
                    val err = recordResult.exceptionOrNull()?.message ?: "Failed to record payment"
                    _isLoading.value = false
                    _paymentState.value = PaymentState.FAILED
                    _operationMessage.value = err
                    _isSuccess.value = false
                    onError(err)
                    return@launch
                }

                // Update Bike Status if Down Payment
                if (purpose.equals("Down Payment", ignoreCase = true) || purpose.equals("DOWN_PAYMENT", ignoreCase = true)) {
                    val bike = inventoryRepository.getBikeById(loan.bikeId)
                    if (bike != null) {
                        inventoryRepository.markBikeFinanced(bike.storeLocation, bike.bikeId)
                    }
                }

                _isLoading.value = false
                _paymentState.value = PaymentState.SUCCESS
                _operationMessage.value = "Payment of ₹${amount.toInt()} verified and recorded successfully ($purpose)"
                _isSuccess.value = true
                onSuccess(res)
            } else {
                val err = paymentResult.exceptionOrNull()?.message ?: "Payment processing failed"
                _isLoading.value = false
                _paymentState.value = PaymentState.FAILED
                _operationMessage.value = err
                _isSuccess.value = false
                onError(err)
            }
        }
    }

    fun processDemoPaymentSimulation(
        loanId: String,
        amount: Double,
        purpose: String = "REGULAR_EMI",
        paymentMethod: String = "UPI_INTENT",
        onSuccess: (PaymentResult) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val simulatedPaymentId = "pay_demo_${System.currentTimeMillis()}_${(1000..9999).random()}"
        processCustomerPayment(
            loanId = loanId,
            amount = amount,
            purpose = purpose,
            paymentMethod = "$paymentMethod (DEMO)",
            razorpayPaymentId = simulatedPaymentId,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun payEmi(
        loanId: String,
        amount: Double,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        processCustomerPayment(
            loanId = loanId,
            amount = amount,
            purpose = "REGULAR_EMI",
            paymentMethod = "UPI_INTENT",
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    /**
     * Record cash / manual payment collected by authorized users.
     */
    fun recordCashPayment(
        loanId: String,
        amountPaise: Long,
        purpose: String = "REGULAR_EMI",
        receiptNumber: String = "",
        notes: String = "",
        storeId: String = "",
        idempotencyKey: String,
        onSuccess: (Transaction) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = authRepository.currentUser.value
        if (currentUser == null || !currentUser.canCollectCashPayment()) {
            val errMsg = "Unauthorized: Only Dealership Admin, Store Manager, or Finance User can collect cash payments"
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isSuccess = false,
                errorMessage = errMsg
            )
            _operationMessage.value = errMsg
            _isSuccess.value = false
            onError(errMsg)
            return
        }

        if (_uiState.value.isLoading) {
            // Prevent double-tap during saving
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = loanRepository.recordCashPaymentAtomic(
                loanId = loanId,
                amountPaise = amountPaise,
                paymentPurpose = purpose,
                currentUser = currentUser,
                receiptNumber = receiptNumber,
                notes = notes,
                storeId = storeId,
                idempotencyKey = idempotencyKey
            )

            if (result.isSuccess) {
                val transaction = result.getOrThrow()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    successMessage = "Cash payment recorded successfully",
                    errorMessage = null
                )
                _operationMessage.value = "Cash payment recorded successfully"
                _isSuccess.value = true

                val dId = currentUser.dealershipId.ifBlank {
                    if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
                }
                transactionRepository.fetchTransactionsFromFirestore(dId)

                onSuccess(transaction)
            } else {
                val ex = result.exceptionOrNull()
                val msg = ex?.message ?: "Failed to record cash payment"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = false,
                    errorMessage = msg
                )
                _operationMessage.value = msg
                _isSuccess.value = false
                onError(msg)
            }
        }
    }

    fun recordCashPayment(
        loanId: String,
        amount: Double,
        purpose: String = "REGULAR_EMI",
        receiptNumber: String = "",
        notes: String = "",
        storeId: String = "",
        idempotencyKey: String,
        onSuccess: (Transaction) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        recordCashPayment(
            loanId = loanId,
            amountPaise = amount.toPaise(),
            purpose = purpose,
            receiptNumber = receiptNumber,
            notes = notes,
            storeId = storeId,
            idempotencyKey = idempotencyKey,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
