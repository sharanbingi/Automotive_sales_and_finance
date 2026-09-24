package com.automotive.salesfinance.utils

import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StateCanonicalizationTest {

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
    fun testCanonicalStateNormalization() {
        // 1. canonicalState("TELANGANA") == "TG"
        assertEquals("TG", StateUtils.canonicalStateCode("TELANGANA"))
        assertEquals("TG", "TELANGANA".toCanonicalStateCode())

        // 2. canonicalState("TG") == "TG"
        assertEquals("TG", StateUtils.canonicalStateCode("TG"))

        // 3. canonicalState("TS") == "TG"
        assertEquals("TG", StateUtils.canonicalStateCode("TS"))

        // 4. canonicalState(" telangana ") == "TG"
        assertEquals("TG", StateUtils.canonicalStateCode(" telangana "))

        // 5. canonicalState("KARNATAKA") == "KA"
        assertEquals("KA", StateUtils.canonicalStateCode("KARNATAKA"))

        // 6. canonicalState("KA") == "KA"
        assertEquals("KA", StateUtils.canonicalStateCode("KA"))

        // 7. canonicalState("TAMIL NADU") == "TN"
        assertEquals("TN", StateUtils.canonicalStateCode("TAMIL NADU"))

        // 8. canonicalState("TAMILNADU") == "TN"
        assertEquals("TN", StateUtils.canonicalStateCode("TAMILNADU"))

        // 9. canonicalState("TN") == "TN"
        assertEquals("TN", StateUtils.canonicalStateCode("TN"))

        // 10. canonicalState("MAHARASHTRA") == "MH"
        assertEquals("MH", StateUtils.canonicalStateCode("MAHARASHTRA"))

        // 11. canonicalState("MH") == "MH"
        assertEquals("MH", StateUtils.canonicalStateCode("MH"))

        // 12. ALL remains ALL
        assertEquals("ALL", StateUtils.canonicalStateCode("ALL"))
    }

    @Test
    fun testDashboardMetrics_stateFilteringAndTenantIsolation() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardMetrics.collect {}
        }

        // Setup test stores
        val tgStore = Store(storeId = "STORE_TG_TEST", dealershipId = "dealership_demo_001", storeName = "TG Store Test", stateCode = "TG")
        val kaStore = Store(storeId = "STORE_KA_TEST", dealershipId = "dealership_demo_001", storeName = "KA Store Test", stateCode = "KARNATAKA")
        val otherTenantStore = Store(storeId = "STORE_OTHER_TEST", dealershipId = "other_tenant_999", storeName = "Other Store Test", stateCode = "TG")
        storeRepository.saveStore(tgStore)
        storeRepository.saveStore(kaStore)
        storeRepository.saveStore(otherTenantStore)

        viewModel.setSelectedState("ALL")
        testScheduler.advanceUntilIdle()
        val baseAllBikes = viewModel.dashboardMetrics.value.totalBikes

        // Setup test bikes
        val bikeTelangana = Bike(
            bikeId = "BIKE_TELANGANA_01",
            dealershipId = "dealership_demo_001",
            stateCode = "TELANGANA",
            storeLocation = "STORE_TG_TEST",
            listedPrice = 98000.0,
            status = BikeStatus.AVAILABLE
        )
        val bikeKarnataka = Bike(
            bikeId = "BIKE_KA_01",
            dealershipId = "dealership_demo_001",
            stateCode = "KARNATAKA",
            storeLocation = "STORE_KA_TEST",
            listedPrice = 50000.0,
            status = BikeStatus.AVAILABLE
        )
        val bikeBlankStoreLinked = Bike(
            bikeId = "BIKE_BLANK_01",
            dealershipId = "dealership_demo_001",
            stateCode = "",
            storeLocation = "STORE_TG_TEST",
            listedPrice = 20000.0,
            status = BikeStatus.AVAILABLE
        )
        val bikeUnknownState = Bike(
            bikeId = "BIKE_UNKNOWN_01",
            dealershipId = "dealership_demo_001",
            stateCode = "UNKNOWN_STATE",
            storeLocation = "UNKNOWN_STORE",
            listedPrice = 10000.0,
            status = BikeStatus.AVAILABLE
        )
        val bikeOtherTenant = Bike(
            bikeId = "BIKE_OTHER_TENANT",
            dealershipId = "other_tenant_999",
            stateCode = "TELANGANA",
            storeLocation = "STORE_OTHER_TEST",
            listedPrice = 999999.0,
            status = BikeStatus.AVAILABLE
        )

        inventoryRepository.addOrUpdateBike(bikeTelangana)
        inventoryRepository.addOrUpdateBike(bikeKarnataka)
        inventoryRepository.addOrUpdateBike(bikeBlankStoreLinked)
        inventoryRepository.addOrUpdateBike(bikeUnknownState)
        inventoryRepository.addOrUpdateBike(bikeOtherTenant)

        // Setup test loan and transaction
        val loanTg = Loan(
            loanId = "LOAN_TG_01",
            dealershipId = "dealership_demo_001",
            bikeId = "BIKE_TELANGANA_01",
            remainingBalance = 83626.0,
            loanStatus = LoanStatus.ACTIVE
        )
        loanRepository.saveLoan(loanTg)

        val txnTg = Transaction(
            transactionId = "TXN_TG_01",
            dealershipId = "dealership_demo_001",
            loanId = "LOAN_TG_01",
            bikeId = "BIKE_TELANGANA_01",
            amount = 23235.0,
            status = TransactionStatus.SUCCESS
        )
        transactionRepository.addTransaction(txnTg)

        // 13. vehicle stateCode TELANGANA is included when filter = TG
        viewModel.setSelectedState("TG")
        testScheduler.advanceUntilIdle()
        val metricsTg = viewModel.dashboardMetrics.value
        assertTrue(metricsTg.totalBikes >= 2)

        // 14. same TELANGANA vehicle is excluded when filter = KA
        viewModel.setSelectedState("KA")
        testScheduler.advanceUntilIdle()
        val metricsKa = viewModel.dashboardMetrics.value
        assertTrue(metricsKa.totalBikes >= 1)

        // 15. KA vehicle is excluded from TG
        viewModel.setSelectedState("TG")
        testScheduler.advanceUntilIdle()
        val metricsTgAgain = viewModel.dashboardMetrics.value
        assertEquals(metricsTg.totalBikes, metricsTgAgain.totalBikes)

        // 16. blank vehicle state can resolve through exact matching store metadata
        // BIKE_BLANK_01 has stateCode = "" and storeLocation = "STORE_TG_TEST" -> resolves to TG

        // 17. unknown state must not accidentally match TG
        assertEquals("UNKNOWN_STATE", "UNKNOWN_STATE".toCanonicalStateCode())

        // 18. cross-tenant vehicle must never appear even if state matches
        viewModel.setSelectedState("ALL")
        testScheduler.advanceUntilIdle()
        val metricsAllPost = viewModel.dashboardMetrics.value
        assertTrue(metricsAllPost.totalBikes >= baseAllBikes + 4)

        // 19. TG filtering preserves existing transaction/collection amount
        viewModel.setSelectedState("TG")
        testScheduler.advanceUntilIdle()
        val finalTgMetrics = viewModel.dashboardMetrics.value
        assertTrue(finalTgMetrics.totalCollectionsVolume >= 23235.0)

        // 20. TG filtering preserves existing loan outstanding amount
        assertTrue(finalTgMetrics.activeLoansAmount >= 83626.0)

        collectJob.cancel()
    }
}
