package com.automotive.salesfinance

import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
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
import com.automotive.salesfinance.services.UpiPaymentHelper
import com.automotive.salesfinance.utils.toRupeesDouble
import com.automotive.salesfinance.viewmodel.FinanceViewModel
import com.automotive.salesfinance.viewmodel.PaymentState
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

@OptIn(ExperimentalCoroutinesApi::class)
class Phase5PaymentTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var auditLogRepository: AuditLogRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var paymentService: PaymentService
    private lateinit var financeViewModel: FinanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        dealershipRepository = DealershipRepository(authRepository)
        transactionRepository = TransactionRepository(authRepository)
        auditLogRepository = AuditLogRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        loanRepository = LoanRepository(
            authRepository = authRepository,
            transactionRepository = transactionRepository,
            auditLogRepository = auditLogRepository,
            customerRepository = customerRepository
        )
        paymentService = RazorpayPaymentServiceImpl()
        financeViewModel = FinanceViewModel(
            loanRepository = loanRepository,
            inventoryRepository = inventoryRepository,
            customerRepository = customerRepository,
            transactionRepository = transactionRepository,
            auditLogRepository = auditLogRepository,
            authRepository = authRepository,
            paymentService = paymentService,
            dealershipRepository = dealershipRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPartialEmiPayment_reducesBalance_keepsNextEmiDateUnchanged() = runTest {
        val initialLoan = loanRepository.getLoanById("LOAN_001")
        assertNotNull(initialLoan)
        val initialNextDate = initialLoan!!.nextEmiDate
        val initialBalance = initialLoan.remainingBalance

        val partialAmount = 2500.0 // emiAmount is 5500.0
        val pId = "pay_test_partial_${System.currentTimeMillis()}"

        val result = loanRepository.recordEmiPayment(
            loanId = "LOAN_001",
            paidAmount = partialAmount,
            paymentPurpose = "PARTIAL_EMI",
            paymentMethod = "UPI_EXPRESS",
            razorpayPaymentId = pId
        )

        assertTrue(result.isSuccess)
        val updatedLoan = loanRepository.getLoanById("LOAN_001")
        assertNotNull(updatedLoan)
        assertEquals(initialBalance - partialAmount, updatedLoan!!.remainingBalance, 0.01)
        assertEquals(partialAmount, updatedLoan.paidAmount, 0.01)
        assertEquals(initialNextDate, updatedLoan.nextEmiDate)
        assertEquals(LoanStatus.ACTIVE, updatedLoan.loanStatus)
    }

    @Test
    fun testMultipleEmiPayment_reducesBalance_advancesNextEmiDate() = runTest {
        val initialLoan = loanRepository.getLoanById("LOAN_001")
        assertNotNull(initialLoan)
        val initialNextDate = initialLoan!!.nextEmiDate
        val initialBalance = initialLoan.remainingBalance
        val emiAmount = initialLoan.emiAmount

        val emiCount = 3
        val totalMultipleAmount = emiAmount * emiCount // 16500.0
        val pId = "pay_test_multiple_${System.currentTimeMillis()}"

        val result = loanRepository.recordEmiPayment(
            loanId = "LOAN_001",
            paidAmount = totalMultipleAmount,
            paymentPurpose = "MULTIPLE_EMI",
            paymentMethod = "UPI_EXPRESS",
            razorpayPaymentId = pId
        )

        assertTrue(result.isSuccess)
        val updatedLoan = loanRepository.getLoanById("LOAN_001")
        assertNotNull(updatedLoan)
        assertEquals(initialBalance - totalMultipleAmount, updatedLoan!!.remainingBalance, 0.01)

        val expectedNextDate = initialNextDate + (emiCount * 30L * 24 * 60 * 60 * 1000)
        assertEquals(expectedNextDate, updatedLoan.nextEmiDate)
    }

    @Test
    fun testFullLoanSettlement_setsBalanceZero_andStatusSettled() = runTest {
        val initialLoan = loanRepository.getLoanById("LOAN_001")
        assertNotNull(initialLoan)
        val fullBalance = initialLoan!!.remainingBalance
        val pId = "pay_test_settle_${System.currentTimeMillis()}"

        val result = loanRepository.recordEmiPayment(
            loanId = "LOAN_001",
            paidAmount = fullBalance,
            paymentPurpose = "FULL_SETTLEMENT",
            paymentMethod = "UPI_EXPRESS",
            razorpayPaymentId = pId
        )

        assertTrue(result.isSuccess)
        val updatedLoan = loanRepository.getLoanById("LOAN_001")
        assertNotNull(updatedLoan)
        assertEquals(0.0, updatedLoan!!.remainingBalance, 0.001)
        assertEquals(LoanStatus.SETTLED, updatedLoan.loanStatus)
    }

    @Test
    fun testIdempotencyCheck_rejectsDuplicateRazorpayPaymentId() = runTest {
        val duplicateId = "pay_duplicate_12345"

        val result1 = loanRepository.recordEmiPayment(
            loanId = "LOAN_001",
            paidAmount = 5500.0,
            paymentPurpose = "REGULAR_EMI",
            paymentMethod = "UPI_EXPRESS",
            razorpayPaymentId = duplicateId
        )
        assertTrue(result1.isSuccess)

        val result2 = loanRepository.recordEmiPayment(
            loanId = "LOAN_001",
            paidAmount = 5500.0,
            paymentPurpose = "REGULAR_EMI",
            paymentMethod = "UPI_EXPRESS",
            razorpayPaymentId = duplicateId
        )
        assertTrue(result2.isFailure)
        val exception = result2.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("Duplicate payment ID"))
    }

    @Test
    fun testUpiPaymentMethodOptions_generatesValidRazorpayOptions() {
        val apps = UpiPaymentHelper.getSupportedUpiApps()
        assertTrue(apps.isNotEmpty())
        assertTrue(apps.any { it.appId == "gpay" })
        assertTrue(apps.any { it.appId == "phonepe" })

        val options = UpiPaymentHelper.createUpiCheckoutOptions(
            amount = 8500.0,
            orderId = "order_upi_123",
            description = "Monthly EMI - LOAN_001",
            upiApp = "gpay",
            isExpress = true
        )

        assertEquals(850000L, options.amountInPaise)
        assertEquals("order_upi_123", options.orderId)
        assertEquals("gpay", options.upiApp)
        assertTrue(options.isUpiExpress)
    }

    @Test
    fun testDemoPaymentModeExecution_succeedsAndUpdatesRecords() = runTest {
        var successResult: PaymentResult? = null

        financeViewModel.processDemoPaymentSimulation(
            loanId = "LOAN_001",
            amount = 5500.0,
            purpose = "REGULAR_EMI",
            paymentMethod = "UPI_EXPRESS",
            onSuccess = { res -> successResult = res }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(successResult)
        assertTrue(successResult!!.success)
        assertEquals(PaymentState.SUCCESS, financeViewModel.paymentState.value)

        val updatedLoan = loanRepository.getLoanById("LOAN_001")
        assertNotNull(updatedLoan)

        val txns = transactionRepository.getTransactionsByLoan("LOAN_001")
        assertTrue(txns.isNotEmpty())
        assertTrue(txns.any { it.status == TransactionStatus.SUCCESS && it.amount == 5500.0 })
    }

    @Test
    fun testPaisePaymentPrecision_3635_3635_88_and_1Paise() = runTest {
        val loanId = "LOAN_001"
        // 1. ₹3,635 payment
        val res1 = loanRepository.recordEmiPayment(loanId, 363500L, "REGULAR_EMI", "UPI", "pay_3635")
        assertTrue(res1.isSuccess)

        // 2. ₹3,635.88 payment
        val res2 = loanRepository.recordEmiPayment(loanId, 363588L, "REGULAR_EMI", "UPI", "pay_3635_88")
        assertTrue(res2.isSuccess)

        // 3. 1 paise payment
        val res3 = loanRepository.recordEmiPayment(loanId, 1L, "PARTIAL_EMI", "UPI", "pay_1_paise")
        assertTrue(res3.isSuccess)
    }

    @Test
    fun testSettlementAndRejections_overpayment_zero_and_negative() = runTest {
        val loanId = "LOAN_001"
        val loan = loanRepository.getLoanById(loanId)!!
        val rem = loan.effectiveRemainingBalancePaise()

        // Overpayment rejected
        val resOver = loanRepository.recordEmiPayment(loanId, rem + 100L, "REGULAR_EMI", "UPI", "pay_over")
        assertTrue(resOver.isFailure)

        // Zero rejected
        val resZero = loanRepository.recordEmiPayment(loanId, 0L, "REGULAR_EMI", "UPI", "pay_zero")
        assertTrue(resZero.isFailure)

        // Negative rejected
        val resNeg = loanRepository.recordEmiPayment(loanId, -500L, "REGULAR_EMI", "UPI", "pay_neg")
        assertTrue(resNeg.isFailure)

        // Exact full settlement
        val resSettle = loanRepository.recordEmiPayment(loanId, rem, "FULL_SETTLEMENT", "UPI", "pay_settle_exact")
        assertTrue(resSettle.isSuccess)
        val settledLoan = loanRepository.getLoanById(loanId)!!
        assertEquals(0L, settledLoan.effectiveRemainingBalancePaise())
        assertEquals(LoanStatus.SETTLED, settledLoan.loanStatus)
    }

    @Test
    fun testRepeated100PaymentUpdatesWithoutDrift() = runTest {
        // Let's test repeated payments on LOAN_001:
        val initialLoan = loanRepository.getLoanById("LOAN_001")!!
        val startRem = initialLoan.effectiveRemainingBalancePaise()
        val paymentPaise = 100L // ₹1.00
        for (i in 1..10) {
            val res = loanRepository.recordEmiPayment("LOAN_001", paymentPaise, "PARTIAL_EMI", "UPI", "pay_drift_$i")
            assertTrue(res.isSuccess)
        }
        val endLoan = loanRepository.getLoanById("LOAN_001")!!
        assertEquals(startRem - 1000L, endLoan.effectiveRemainingBalancePaise())
    }

    @Test
    fun testLoanAuthorityAndTransactionVerification() = runTest {
        val loanId = "LOAN_001"
        val paymentPaise = 550000L // ₹5500.00
        val pId = "pay_auth_test_${System.currentTimeMillis()}"

        val res = loanRepository.recordEmiPayment(loanId, paymentPaise, "REGULAR_EMI", "UPI", pId)
        assertTrue(res.isSuccess)

        val txns = transactionRepository.getTransactionsByLoan(loanId)
        val txn = txns.find { it.razorpayPaymentId == pId }
        assertNotNull(txn)
        assertEquals(paymentPaise, txn!!.effectiveAmountPaise())
        assertEquals(paymentPaise.toRupeesDouble(), txn.amount, 0.001)

        val loan = loanRepository.getLoanById(loanId)!!
        assertNotNull(loan.paidAmountPaise)
        assertNotNull(loan.remainingBalancePaise)
        assertTrue(loan.effectiveRemainingBalancePaise() >= 0L)
    }

    @Test
    fun testCashCollectionRegression() = runTest {
        val loanId = "LOAN_001"
        val user =
            User(uid = "user_admin", role = UserRole.DEALERSHIP_ADMIN, name = "Admin", dealershipId = "dealership_demo_001")
        val cashRes = loanRepository.recordCashPaymentAtomic(
            loanId = loanId,
            amountPaise = 550000L,
            paymentPurpose = "REGULAR_EMI",
            currentUser = user,
            idempotencyKey = "idemp_cash_test_${System.currentTimeMillis()}"
        )
        assertTrue(cashRes.isSuccess)
    }
}
