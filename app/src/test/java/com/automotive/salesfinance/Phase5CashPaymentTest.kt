package com.automotive.salesfinance

import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.PaymentMethodOption
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
import com.automotive.salesfinance.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Phase5CashPaymentTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var auditLogRepository: AuditLogRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var paymentService: MockPaymentService
    private lateinit var financeViewModel: FinanceViewModel

    class MockPaymentService(
        private val delegate: PaymentService = RazorpayPaymentServiceImpl()
    ) : PaymentService by delegate {
        var wasInvoked = false
        override suspend fun processCustomerEmiPayment(
            loanId: String,
            customerId: String,
            amount: Double,
            purpose: String,
            paymentMethod: String,
            razorpayPaymentId: String?,
            upiApp: String?,
            vpaId: String?
        ): Result<PaymentResult> {
            wasInvoked = true
            return delegate.processCustomerEmiPayment(
                loanId, customerId, amount, purpose, paymentMethod, razorpayPaymentId, upiApp, vpaId
            )
        }
    }

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
        paymentService = MockPaymentService()
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

    // Test 1: DEALERSHIP_ADMIN, STORE_MANAGER, FINANCE_USER can record cash payment
    @Test
    fun testAuthorizedRoles_canRecordCashPayment() = runTest {
        val authorizedRoles = listOf(
            UserRole.DEALERSHIP_ADMIN,
            UserRole.STORE_MANAGER,
            UserRole.FINANCE_USER
        )

        for (role in authorizedRoles) {
            val user = User(
                uid = "user_${role.name}",
                name = "Test ${role.name}",
                role = role,
                dealershipId = "dealership_demo_001"
            )
            authRepository.setCurrentUser(user)

            var success = false
            financeViewModel.recordCashPayment(
                loanId = "LOAN_001",
                amount = 1000.0,
                purpose = "PARTIAL_PAYMENT",
                idempotencyKey = "test_key",
                onSuccess = { success = true }
            )
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue("Role $role should be authorized to record cash payment", success)
            assertTrue(financeViewModel.uiState.value.isSuccess)
        }
    }

    // Test 2: CUSTOMER and SALES_USER cannot record cash payment (unauthorized error)
    @Test
    fun testUnauthorizedRoles_cannotRecordCashPayment() = runTest {
        val unauthorizedRoles = listOf(
            UserRole.CUSTOMER,
            UserRole.SALES_USER
        )

        for (role in unauthorizedRoles) {
            val user = User(
                uid = "user_${role.name}",
                name = "Test ${role.name}",
                role = role,
                dealershipId = "dealership_demo_001"
            )
            authRepository.setCurrentUser(user)

            var errorMessage: String? = null
            financeViewModel.recordCashPayment(
                loanId = "LOAN_001",
                amount = 1000.0,
                purpose = "PARTIAL_PAYMENT",
                idempotencyKey = "test_key",
                onError = { err -> errorMessage = err }
            )
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse("Role $role should not be authorized", financeViewModel.uiState.value.isSuccess)
            assertNotNull(errorMessage)
            assertTrue("Error should mention unauthorized", errorMessage!!.contains("Unauthorized"))
        }
    }

    // Test 3: Cash transaction uses paymentMethod = CASH
    @Test
    fun testCashTransaction_usesPaymentMethodCash() = runTest {
        val adminUser = User(
            uid = "admin_01",
            name = "Admin User",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "dealership_demo_001"
        )
        authRepository.setCurrentUser(adminUser)

        var createdTxnId = ""
        financeViewModel.recordCashPayment(
            loanId = "LOAN_001",
            amount = 2000.0,
            purpose = "PARTIAL_PAYMENT",
            idempotencyKey = "test_key",
            onSuccess = { txn -> createdTxnId = txn.transactionId }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(createdTxnId.isNotBlank())
        val txns = transactionRepository.getTransactionsByLoan("LOAN_001")
        val recordedTxn = txns.find { it.transactionId == createdTxnId }
        assertNotNull(recordedTxn)
        assertEquals(PaymentMethodOption.CASH.name, recordedTxn!!.paymentMethod)
        assertEquals("CASH", recordedTxn.paymentMethod)
        assertEquals(TransactionStatus.SUCCESS, recordedTxn.status)
    }

    // Test 4: Cash transaction does not invoke Razorpay SDK
    @Test
    fun testCashTransaction_doesNotInvokeRazorpaySDK() = runTest {
        val adminUser = User(
            uid = "admin_01",
            name = "Admin User",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "dealership_demo_001"
        )
        authRepository.setCurrentUser(adminUser)

        paymentService.wasInvoked = false

        financeViewModel.recordCashPayment(
            loanId = "LOAN_001",
            amount = 1500.0,
            purpose = "REGULAR_EMI",
            idempotencyKey = "test_key"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("PaymentService (Razorpay SDK) should NOT be invoked for cash payments", paymentService.wasInvoked)
    }

    // Test 5: Cash transaction + loan balance reduction + audit log are committed atomically
    @Test
    fun testCashTransaction_loanBalanceReduction_auditLog_committedAtomically() = runTest {
        val adminUser = User(
            uid = "admin_01",
            name = "Admin User",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "dealership_demo_001"
        )
        authRepository.setCurrentUser(adminUser)

        val initialLoan = loanRepository.getLoanById("LOAN_001")
        assertNotNull(initialLoan)
        val initialBalance = initialLoan!!.remainingBalance
        val cashAmount = 3000.0

        financeViewModel.recordCashPayment(
            loanId = "LOAN_001",
            amount = cashAmount,
            purpose = "PARTIAL_PAYMENT",
            notes = "Collected in showroom",
            idempotencyKey = "test_key"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Loan balance reduced
        val updatedLoan = loanRepository.getLoanById("LOAN_001")
        assertNotNull(updatedLoan)
        assertEquals(initialBalance - cashAmount, updatedLoan!!.remainingBalance, 0.01)

        // 2. Transaction recorded
        val txns = transactionRepository.getTransactionsByLoan("LOAN_001")
        val cashTxn = txns.find { it.paymentMethod == "CASH" && it.amount == cashAmount }
        assertNotNull(cashTxn)

        // 3. Audit log created
        val auditLogs = auditLogRepository.getAuditLogsForDealership("dealership_demo_001")
        assertTrue("Audit log should be recorded", auditLogs.any { it.action == "RECORD_CASH_PAYMENT" })
    }

    // Test 6: Regular cash EMI reduces balance and advances next EMI date
    @Test
    fun testRegularCashEmi_reducesBalance_advancesNextEmiDate() = runTest {
        val manager = User(
            uid = "mgr_01",
            name = "Store Manager",
            role = UserRole.STORE_MANAGER,
            dealershipId = "dealership_demo_001"
        )
        authRepository.setCurrentUser(manager)

        val initialLoan = loanRepository.getLoanById("LOAN_001")!!
        val initialNextDate = initialLoan.nextEmiDate
        val initialBalance = initialLoan.remainingBalance
        val emiAmount = initialLoan.emiAmount

        financeViewModel.recordCashPayment(
            loanId = "LOAN_001",
            amount = emiAmount,
            purpose = "REGULAR_EMI",
            idempotencyKey = "test_key"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedLoan = loanRepository.getLoanById("LOAN_001")!!
        assertEquals(initialBalance - emiAmount, updatedLoan.remainingBalance, 0.01)
        val expectedNextDate = initialNextDate + (30L * 24 * 60 * 60 * 1000)
        assertEquals(expectedNextDate, updatedLoan.nextEmiDate)
    }

    // Test 7: Partial cash payment reduces balance without advancing next EMI date
    @Test
    fun testPartialCashPayment_reducesBalance_withoutAdvancingNextEmiDate() = runTest {
        val manager = User(
            uid = "mgr_01",
            name = "Store Manager",
            role = UserRole.STORE_MANAGER,
            dealershipId = "dealership_demo_001"
        )
        authRepository.setCurrentUser(manager)

        val initialLoan = loanRepository.getLoanById("LOAN_001")!!
        val initialNextDate = initialLoan.nextEmiDate
        val initialBalance = initialLoan.remainingBalance
        val partialAmount = 2000.0

        financeViewModel.recordCashPayment(
            loanId = "LOAN_001",
            amount = partialAmount,
            purpose = "PARTIAL_PAYMENT",
            idempotencyKey = "test_key"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedLoan = loanRepository.getLoanById("LOAN_001")!!
        assertEquals(initialBalance - partialAmount, updatedLoan.remainingBalance, 0.01)
        assertEquals(initialNextDate, updatedLoan.nextEmiDate)
    }

    // Test 8: Full settlement reduces balance to 0.0 and sets status to SETTLED
    @Test
    fun testFullSettlement_reducesBalanceToZero_andSetsStatusSettled() = runTest {
        val financeUser = User(
            uid = "fin_01",
            name = "Finance Executive",
            role = UserRole.FINANCE_USER,
            dealershipId = "dealership_demo_001"
        )
        authRepository.setCurrentUser(financeUser)

        val initialLoan = loanRepository.getLoanById("LOAN_001")!!
        val fullBalance = initialLoan.remainingBalance

        financeViewModel.recordCashPayment(
            loanId = "LOAN_001",
            amount = fullBalance,
            purpose = "FULL_SETTLEMENT",
            idempotencyKey = "test_key"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedLoan = loanRepository.getLoanById("LOAN_001")!!
        assertEquals(0.0, updatedLoan.remainingBalance, 0.001)
        assertEquals(LoanStatus.SETTLED, updatedLoan.loanStatus)
    }

    // Test 9: Failed cash payment does not alter loan balance or create transaction
    @Test
    fun testFailedCashPayment_doesNotAlterLoanBalance_orCreateTransaction() = runTest {
        val customerUser = User(
            uid = "cust_01",
            name = "Customer User",
            role = UserRole.CUSTOMER,
            dealershipId = "dealership_demo_001"
        )
        authRepository.setCurrentUser(customerUser)

        val initialLoan = loanRepository.getLoanById("LOAN_001")!!
        val initialBalance = initialLoan.remainingBalance
        val initialTxnCount = transactionRepository.getTransactionsByLoan("LOAN_001").size

        financeViewModel.recordCashPayment(
            loanId = "LOAN_001",
            amount = 5000.0,
            purpose = "REGULAR_EMI",
            idempotencyKey = "test_key"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedLoan = loanRepository.getLoanById("LOAN_001")!!
        assertEquals("Loan balance should remain unchanged on failure", initialBalance, updatedLoan.remainingBalance, 0.001)
        val newTxnCount = transactionRepository.getTransactionsByLoan("LOAN_001").size
        assertEquals("No transaction should be created on failure", initialTxnCount, newTxnCount)
    }

    // Test 10: Double-tap during saving is prevented
    @Test
    fun testDoubleTap_duringSaving_isPrevented() = runTest {
        val adminUser = User(
            uid = "admin_01",
            name = "Admin User",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "dealership_demo_001"
        )
        authRepository.setCurrentUser(adminUser)

        var callCount = 0
        financeViewModel.recordCashPayment(
            loanId = "LOAN_001",
            amount = 1000.0,
            purpose = "PARTIAL_PAYMENT",
            idempotencyKey = "test_key_1",
            onSuccess = { callCount++ }
        )

        // While first call is loading/in flight, attempt a second call (simulating double tap)
        financeViewModel.recordCashPayment(
            loanId = "LOAN_001",
            amount = 1000.0,
            purpose = "PARTIAL_PAYMENT",
            idempotencyKey = "test_key_2",
            onSuccess = { callCount++ }
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Second call during loading should be ignored", 1, callCount)
    }

    // Test 11: Legacy cash payment migration-by-touch populates paise fields
    @Test
    fun testLegacyCashPaymentMigrationByTouch() = runTest {
        val adminUser = User(
            uid = "admin_01",
            name = "Admin User",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "dealership_demo_001"
        )
        authRepository.setCurrentUser(adminUser)

        val legacyLoan = Loan(
            loanId = "LEGACY_LOAN_01",
            dealershipId = "dealership_demo_001",
            customerId = "cust_001",
            bikeId = "bike_001",
            totalAmount = 50000.0,
            paidAmount = 10000.0,
            remainingBalance = 40000.0,
            emiAmount = 5000.0,
            remainingBalancePaise = null,
            paidAmountPaise = null,
            emiAmountPaise = null,
            principalAmountPaise = null
        )
        loanRepository.saveLoan(legacyLoan)

        var success = false
        loanRepository.recordCashPaymentAtomic(
            loanId = "LEGACY_LOAN_01",
            amountPaise = 500000L,
            paymentPurpose = "REGULAR_EMI",
            currentUser = adminUser,
            idempotencyKey = "legacy_test_key"
        ).onSuccess { success = true }

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        val updated = loanRepository.getLoanById("LEGACY_LOAN_01")
        assertNotNull(updated)
        assertEquals(3500000L, updated!!.remainingBalancePaise)
        assertEquals(1500000L, updated.paidAmountPaise)
        assertEquals(500000L, updated.emiAmountPaise)
        assertEquals(35000.0, updated.remainingBalance, 0.001)
        assertEquals(15000.0, updated.paidAmount, 0.001)
    }
}
