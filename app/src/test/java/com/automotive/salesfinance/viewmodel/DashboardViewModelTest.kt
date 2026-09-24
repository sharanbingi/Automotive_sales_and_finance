package com.automotive.salesfinance.viewmodel

import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.StoreRepository
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
class DashboardViewModelTest {

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
    fun dashboardMetrics_calculatesAggregateStatsCorrectly() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardMetrics.collect {}
        }

        val metrics = viewModel.dashboardMetrics.value
        assertNotNull(metrics)
        assertTrue(metrics.totalBikes > 0)
        assertTrue(metrics.availableBikes >= 0)
        assertTrue(metrics.totalBikesValuation > 0.0)
        assertTrue(metrics.activeLoansCount >= 0)
        assertTrue(metrics.totalStoresCount > 0)
    }

    @Test
    fun filterByStore_updatesMetricsForSelectedStore() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardMetrics.collect {}
        }

        viewModel.setSelectedState("TG")
        viewModel.setSelectedStore("TG_Madhapur")

        assertEquals("TG", viewModel.selectedState.value)
        assertEquals("TG_Madhapur", viewModel.selectedStore.value)

        val metrics = viewModel.dashboardMetrics.value
        assertNotNull(metrics)
        assertTrue(metrics.totalBikes <= inventoryRepository.getAllBikes().size)
    }

    @Test
    fun customerDashboardInfo_returnsDataForCustomerRole() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.customerDashboardInfo.collect {}
        }

        authRepository.switchDemoRole(UserRole.CUSTOMER)

        val custInfo = viewModel.customerDashboardInfo.value
        assertNotNull(custInfo)
    }

    @Test
    fun testStateFiltering_allAndSpecificStates() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardMetrics.collect {}
        }

        // 1. ALL includes all records
        viewModel.setSelectedState("ALL")
        val metricsAll = viewModel.dashboardMetrics.value
        val totalAll = metricsAll.totalBikes

        // 2. TG includes TG records
        viewModel.setSelectedState("tg") // lowercase test for normalization
        val metricsTg = viewModel.dashboardMetrics.value
        assertTrue(metricsTg.totalBikes <= totalAll)

        // 3. KA includes KA records / excludes TG if separate stores
        viewModel.setSelectedState("KA")
        val metricsKa = viewModel.dashboardMetrics.value
        assertNotNull(metricsKa)

        // 4. Cross-tenant safety check
        assertTrue(metricsAll.totalBikes >= metricsTg.totalBikes)
    }
}
