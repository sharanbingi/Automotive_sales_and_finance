package com.automotive.salesfinance

import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.viewmodel.DashboardViewModel
import com.automotive.salesfinance.viewmodel.ReportPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardReportingTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepository()
        inventoryRepository = InventoryRepository(authRepository)
        loanRepository = LoanRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        transactionRepository = TransactionRepository(authRepository)
        storeRepository = StoreRepository(authRepository)

        inventoryRepository.clearInMemoryState()
        loanRepository.clearInMemoryState()
        customerRepository.clearInMemoryState()
        transactionRepository.clearInMemoryState()
        storeRepository.clearInMemoryState()

        viewModel = DashboardViewModel(
            authRepository = authRepository,
            inventoryRepository = inventoryRepository,
            loanRepository = loanRepository,
            customerRepository = customerRepository,
            transactionRepository = transactionRepository,
            storeRepository = storeRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSalesVolumeIncludesOnlyFinancedOrSoldVehicles() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardMetrics.collect {}
        }

        val availableBike = Bike(
            bikeId = "B1",
            dealershipId = "dealership_demo_001",
            listedPrice = 100000.0,
            status = BikeStatus.AVAILABLE
        )
        val reservedBike = Bike(
            bikeId = "B2",
            dealershipId = "dealership_demo_001",
            listedPrice = 120000.0,
            status = BikeStatus.RESERVED
        )
        val financedBike = Bike(
            bikeId = "B3",
            dealershipId = "dealership_demo_001",
            listedPrice = 150000.0,
            status = BikeStatus.FINANCED
        )

        inventoryRepository.addOrUpdateBike(availableBike)
        inventoryRepository.addOrUpdateBike(reservedBike)
        inventoryRepository.addOrUpdateBike(financedBike)

        val metrics = viewModel.dashboardMetrics.value
        assertEquals(3, metrics.totalBikes)
        assertEquals(1, metrics.financedBikes)
        assertEquals(150000.0, metrics.financedBikesValuation, 0.001)
        assertEquals(15000000L, metrics.financedBikesValuationPaise)

        collectJob.cancel()
    }

    @Test
    fun testRealCustomerCountUsesCustomerRepository() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardMetrics.collect {}
        }

        val c1 = Customer(customerId = "C1", dealershipId = "dealership_demo_001", fullName = "John Doe", state = "TG")
        val c2 = Customer(customerId = "C2", dealershipId = "dealership_demo_001", fullName = "Jane Smith", state = "TG")
        customerRepository.saveCustomer(c1)
        customerRepository.saveCustomer(c2)

        val metrics = viewModel.dashboardMetrics.value
        assertEquals(2, metrics.totalCustomers)

        collectJob.cancel()
    }

    @Test
    fun testActiveLoanOutstandingCalculatesInLongPaise() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardMetrics.collect {}
        }

        val loan1 = Loan(
            loanId = "L1",
            dealershipId = "dealership_demo_001",
            remainingBalance = 50000.50,
            remainingBalancePaise = 5000050L,
            loanStatus = LoanStatus.ACTIVE
        )
        val loan2 = Loan(
            loanId = "L2",
            dealershipId = "dealership_demo_001",
            remainingBalance = 25000.25,
            remainingBalancePaise = 2500025L,
            loanStatus = LoanStatus.ACTIVE
        )
        loanRepository.saveLoan(loan1)
        loanRepository.saveLoan(loan2)

        val metrics = viewModel.dashboardMetrics.value
        assertEquals(2, metrics.activeLoansCount)
        assertEquals(7500075L, metrics.activeLoansAmountPaise)
        assertEquals(75000.75, metrics.activeLoansAmount, 0.001)

        collectJob.cancel()
    }

    @Test
    fun testDateRangeFilteringFiltersTransactionsByCreatedAt() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardMetrics.collect {}
        }

        val now = System.currentTimeMillis()
        val oldTimestamp = now - (10L * 24 * 60 * 60 * 1000) // 10 days ago
        val recentTimestamp = now - (2L * 24 * 60 * 60 * 1000) // 2 days ago

        val oldTxn = Transaction(
            transactionId = "T_OLD",
            dealershipId = "dealership_demo_001",
            amount = 1000.0,
            amountPaise = 100000L,
            createdAt = oldTimestamp,
            status = TransactionStatus.SUCCESS
        )
        val recentTxn = Transaction(
            transactionId = "T_RECENT",
            dealershipId = "dealership_demo_001",
            amount = 2000.0,
            amountPaise = 200000L,
            createdAt = recentTimestamp,
            status = TransactionStatus.SUCCESS
        )

        transactionRepository.addTransaction(oldTxn)
        transactionRepository.addTransaction(recentTxn)

        // ALL_TIME includes both
        viewModel.setSelectedReportPeriod(ReportPeriod.ALL_TIME)
        assertEquals(3000.0, viewModel.dashboardMetrics.value.totalCollectionsVolume, 0.001)

        // LAST_7_DAYS includes only recentTxn
        viewModel.setSelectedReportPeriod(ReportPeriod.LAST_7_DAYS)
        assertEquals(2000.0, viewModel.dashboardMetrics.value.totalCollectionsVolume, 0.001)

        collectJob.cancel()
    }

    @Test
    fun testCanonicalStateFilteringForCustomersAndBikes() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardMetrics.collect {}
        }

        val c1 = Customer(customerId = "C1", dealershipId = "dealership_demo_001", fullName = "Cust TS", state = "TELANGANA")
        val c2 = Customer(customerId = "C2", dealershipId = "dealership_demo_001", fullName = "Cust KA", state = "KARNATAKA")
        customerRepository.saveCustomer(c1)
        customerRepository.saveCustomer(c2)

        viewModel.setSelectedState("ALL")
        assertEquals(2, viewModel.dashboardMetrics.value.totalCustomers)

        viewModel.setSelectedState("TS")
        assertEquals(1, viewModel.dashboardMetrics.value.totalCustomers)

        viewModel.setSelectedState("TG")
        assertEquals(1, viewModel.dashboardMetrics.value.totalCustomers)

        viewModel.setSelectedState("KA")
        assertEquals(1, viewModel.dashboardMetrics.value.totalCustomers)

        collectJob.cancel()
    }
}
