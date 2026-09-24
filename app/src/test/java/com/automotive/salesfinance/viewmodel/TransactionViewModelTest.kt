package com.automotive.salesfinance.viewmodel

import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class TransactionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        transactionRepository = TransactionRepository(authRepository)
        loanRepository = LoanRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        viewModel = TransactionViewModel(
            transactionRepository = transactionRepository,
            loanRepository = loanRepository,
            customerRepository = customerRepository,
            authRepository = authRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun collectionsSummary_calculatesTotalVolumeCorrectly() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.collectionsSummary.collect {}
        }

        val summary = viewModel.collectionsSummary.value
        assertNotNull(summary)
        assertTrue(summary.totalVolume >= 11000.0)
        assertTrue(summary.totalCount >= 2)
    }

    @Test
    fun filtering_byStatusAndPaymentMethodWorks() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.filteredTransactions.collect {}
        }

        viewModel.setStatusFilter("SUCCESS")
        viewModel.setMethodFilter("RAZORPAY")

        val filtered = viewModel.filteredTransactions.value
        assertTrue(filtered.all { it.status == TransactionStatus.SUCCESS && it.paymentMethod == "RAZORPAY" })
    }

    @Test
    fun search_matchesTransactionIdOrCustomerName() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.filteredTransactions.collect {}
        }

        viewModel.setSearchQuery("TXN_001")

        val filtered = viewModel.filteredTransactions.value
        assertEquals(1, filtered.size)
        assertEquals("TXN_001", filtered.first().transactionId)
    }
}
