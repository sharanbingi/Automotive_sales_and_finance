package com.automotive.salesfinance.viewmodel

import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.DealershipRepository

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        loanRepository = LoanRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        transactionRepository = TransactionRepository(authRepository)
        val auditLogRepository = AuditLogRepository(authRepository)
        val dealershipRepository = DealershipRepository(authRepository)
        viewModel = FinanceViewModel(
            loanRepository = loanRepository,
            inventoryRepository = inventoryRepository,
            customerRepository = customerRepository,
            transactionRepository = transactionRepository,
            auditLogRepository = auditLogRepository,
            authRepository = authRepository,
            dealershipRepository = dealershipRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun calculateEmi_calculatesReducingBalanceCorrectly() {
        // Vehicle Price = 1,00,000, Down Payment = 20,000 -> Principal = 80,000
        // Interest Rate = 12% p.a. -> Monthly Rate r = 0.01
        // Tenure = 12 months
        // EMI = 80000 * 0.01 * (1.01^12) / ((1.01^12) - 1) ~ 7107.94
        val result = FinanceViewModel.calculateEmi(
            vehiclePrice = 100000.0,
            downPayment = 20000.0,
            annualRatePercent = 12.0,
            tenureMonths = 12
        )

        assertEquals(80000.0, result.principal, 0.01)
        assertEquals(7107.94, result.monthlyEmi, 1.0)
        assertEquals(85295.28, result.totalRepayment, 10.0)
        assertEquals(5295.28, result.totalInterest, 10.0)
    }

    @Test
    fun createLoanAndFinanceBike_updatesBikeStatusAndSavesLoan() = runTest {
        // BIKE_001 is AVAILABLE in TG_Madhapur, CUST_001 exists
        var createdLoanId: String? = null
        viewModel.createLoanAndFinanceBike(
            customerId = "CUST_001",
            bikeId = "BIKE_001",
            vehiclePrice = 95000.0,
            downPayment = 15000.0,
            interestRate = 10.5,
            tenureMonths = 12,
            onSuccess = { createdLoanId = it.loanId }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(createdLoanId)
        val bike = inventoryRepository.getBikeById("BIKE_001")
        assertEquals(BikeStatus.FINANCED, bike?.status)

        val loan = loanRepository.getLoanById(createdLoanId!!)
        assertNotNull(loan)
        assertEquals("CUST_001", loan?.customerId)
        assertEquals("BIKE_001", loan?.bikeId)
    }

    @Test
    fun createLoanAndFinanceBike_preventsDoubleFinancing() = runTest {
        // First finance BIKE_001 successfully
        var firstLoanId: String? = null
        viewModel.createLoanAndFinanceBike(
            customerId = "CUST_001",
            bikeId = "BIKE_001",
            vehiclePrice = 95000.0,
            downPayment = 15000.0,
            interestRate = 10.5,
            tenureMonths = 12,
            onSuccess = { firstLoanId = it.loanId }
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(firstLoanId)

        // Attempting to finance BIKE_001 a second time must fail
        var doubleFinanceError: String? = null
        viewModel.createLoanAndFinanceBike(
            customerId = "CUST_002",
            bikeId = "BIKE_001",
            vehiclePrice = 95000.0,
            downPayment = 15000.0,
            interestRate = 10.5,
            tenureMonths = 12,
            onError = { doubleFinanceError = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(doubleFinanceError)
        assertTrue(doubleFinanceError!!.contains("must be AVAILABLE") || doubleFinanceError!!.contains("active loan"))
    }

    @Test
    fun payEmi_updatesRemainingBalance() = runTest {
        // LOAN_001 has total 140000, remaining 110000
        var success = false
        viewModel.payEmi("LOAN_001", 5500.0, onSuccess = { success = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        val updated = loanRepository.getLoanById("LOAN_001")
        assertEquals(104500.0, updated?.remainingBalance ?: 0.0, 0.01)
    }

    @Test
    fun processCustomerPayment_updatesPaymentStateToSuccessAndSettlesLoanWhenZeroBalance() = runTest {
        val initialLoan = loanRepository.getLoanById("LOAN_001")
        assertNotNull(initialLoan)

        // Full settlement payment of entire remaining balance
        val fullAmount = initialLoan!!.remainingBalance

        var successCalled = false
        viewModel.processCustomerPayment(
            loanId = "LOAN_001",
            amount = fullAmount,
            purpose = "Full Settlement",
            onSuccess = { successCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(PaymentState.SUCCESS, viewModel.paymentState.value)

        val updatedLoan = loanRepository.getLoanById("LOAN_001")
        assertEquals(0.0, updatedLoan?.remainingBalance ?: -1.0, 0.01)
        assertEquals(LoanStatus.SETTLED, updatedLoan?.loanStatus)

        val txns = transactionRepository.getTransactionsByLoan("LOAN_001")
        assertTrue(txns.isNotEmpty())
    }

    @Test
    fun processCustomerPayment_setsPaymentStateToFailedOnInvalidAmount() = runTest {
        var errorCalled = false
        viewModel.processCustomerPayment(
            loanId = "LOAN_001",
            amount = 0.0,
            purpose = "Monthly EMI",
            onError = { errorCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(errorCalled)
        assertEquals(PaymentState.FAILED, viewModel.paymentState.value)
    }
}
