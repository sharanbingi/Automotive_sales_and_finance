package com.automotive.salesfinance

import com.automotive.salesfinance.data.AppContainer
import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.viewmodel.AuthViewModel
import com.automotive.salesfinance.viewmodel.CustomerViewModel
import com.automotive.salesfinance.viewmodel.DashboardViewModel
import com.automotive.salesfinance.viewmodel.FinanceViewModel
import com.automotive.salesfinance.viewmodel.InventoryViewModel
import com.automotive.salesfinance.viewmodel.TransactionViewModel
import com.automotive.salesfinance.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase2ArchitectureTest {

    private lateinit var appContainer: AppContainer
    private lateinit var viewModelFactory: ViewModelFactory

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        appContainer = AppContainer()
        viewModelFactory = ViewModelFactory(appContainer)
        TenantContext.reset()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSingletonSharing_appContainerAndViewModelFactory() {
        // AppContainer singletons check
        val authRepo = appContainer.authRepository
        val userRepo = appContainer.userRepository
        val inventoryRepo = appContainer.inventoryRepository
        val customerRepo = appContainer.customerRepository
        val loanRepo = appContainer.loanRepository
        val transactionRepo = appContainer.transactionRepository

        assertSame(authRepo, appContainer.authRepository)
        assertSame(userRepo, appContainer.userRepository)
        assertSame(inventoryRepo, appContainer.inventoryRepository)
        assertSame(customerRepo, appContainer.customerRepository)
        assertSame(loanRepo, appContainer.loanRepository)
        assertSame(transactionRepo, appContainer.transactionRepository)

        // ViewModels created via ViewModelFactory share the exact same repositories
        val authVm = viewModelFactory.create(AuthViewModel::class.java)
        val dashboardVm = viewModelFactory.create(DashboardViewModel::class.java)
        val inventoryVm = viewModelFactory.create(InventoryViewModel::class.java)
        val financeVm = viewModelFactory.create(FinanceViewModel::class.java)
        val customerVm = viewModelFactory.create(CustomerViewModel::class.java)
        val transactionVm = viewModelFactory.create(TransactionViewModel::class.java)

        assertNotNull(dashboardVm)
        assertSame(authRepo.currentUser.value, authVm.currentUser.value)
        assertSame(inventoryRepo.bikesFlow.value, inventoryVm.allBikes.value)
        assertSame(customerRepo.customersFlow.value, customerVm.allCustomers.value)
        assertSame(loanRepo.loansFlow.value, financeVm.loansFlow.value)
        assertSame(transactionRepo.transactionsFlow.value, transactionVm.allTransactions.value)
    }

    @Test
    fun testProductionFallbackRejection_liveModeRejectsBlankDealershipId() = runBlocking {
        val authRepo = appContainer.authRepository
        authRepo.setDemoMode(false) // Live Production Mode

        val inventoryRepo = appContainer.inventoryRepository
        val customerRepo = appContainer.customerRepository
        val loanRepo = appContainer.loanRepository
        val storeRepo = appContainer.storeRepository
        val transactionRepo = appContainer.transactionRepository
        val userRepo = appContainer.userRepository
        val dealershipRepo = appContainer.dealershipRepository

        // 1. Inventory write rejection
        val blankBike = Bike(bikeId = "BIKE_BLANK", dealershipId = "")
        val bikeResult = inventoryRepo.addOrUpdateBike(blankBike)
        assertTrue(bikeResult.isFailure)
        assertTrue(bikeResult.exceptionOrNull() is IllegalArgumentException)

        // 2. Customer write rejection
        val blankCustomer = Customer(customerId = "CUST_BLANK", dealershipId = "")
        val custResult = customerRepo.saveCustomer(blankCustomer)
        assertTrue(custResult.isFailure)
        assertTrue(custResult.exceptionOrNull() is IllegalArgumentException)

        // 3. Loan write rejection
        val blankLoan = Loan(loanId = "LOAN_BLANK", dealershipId = "")
        val loanResult = loanRepo.saveLoan(blankLoan)
        assertTrue(loanResult.isFailure)
        assertTrue(loanResult.exceptionOrNull() is IllegalArgumentException)

        // 4. Store write rejection
        val blankStore = Store(storeId = "STORE_BLANK", dealershipId = "")
        val storeResult = storeRepo.saveStore(blankStore)
        assertTrue(storeResult.isFailure)
        assertTrue(storeResult.exceptionOrNull() is IllegalArgumentException)

        // 5. Transaction write rejection
        val blankTxn = Transaction(transactionId = "TXN_BLANK", dealershipId = "")
        val txnResult = transactionRepo.addTransaction(blankTxn)
        assertTrue(txnResult.isFailure)
        assertTrue(txnResult.exceptionOrNull() is IllegalArgumentException)

        // 6. User write rejection
        val blankUser = User(uid = "USER_BLANK", dealershipId = "")
        val userResult = userRepo.saveUser(blankUser)
        assertTrue(userResult.isFailure)
        assertTrue(userResult.exceptionOrNull() is IllegalArgumentException)

        // 7. Reads with blank dealershipId in live mode return empty/null (no demo fallback)
        assertTrue(inventoryRepo.getBikesForDealership("").isEmpty())
        assertTrue(customerRepo.getCustomersForDealership("").isEmpty())
        assertTrue(loanRepo.getLoansForDealership("").isEmpty())
        assertTrue(storeRepo.getStoresForDealership("").isEmpty())
        assertTrue(transactionRepo.getTransactionsForDealership("").isEmpty())
        assertTrue(userRepo.getUsersForDealership("").isEmpty())
        assertNull(dealershipRepo.getDealershipById(""))
    }

    @Test
    fun testInMemoryTenantScopingHierarchy() = runBlocking {
        val inventoryRepo = appContainer.inventoryRepository
        val userRepo = appContainer.userRepository

        // Add test bikes for dealership_A and dealership_B
        val bikeA1 = Bike(bikeId = "BIKE_A1", dealershipId = "dealership_A", stateCode = "TG", storeLocation = "STORE_TG_1")
        val bikeA2 = Bike(bikeId = "BIKE_A2", dealershipId = "dealership_A", stateCode = "KA", storeLocation = "STORE_KA_1")
        val bikeB1 = Bike(bikeId = "BIKE_B1", dealershipId = "dealership_B", stateCode = "TG", storeLocation = "STORE_TG_1")

        inventoryRepo.addOrUpdateBike(bikeA1)
        inventoryRepo.addOrUpdateBike(bikeA2)
        inventoryRepo.addOrUpdateBike(bikeB1)

        // Test InventoryRepository getBikesForStore filters dealershipId FIRST
        val bikesAInTg1 = inventoryRepo.getBikesForStore("dealership_A", "STORE_TG_1")
        assertTrue(bikesAInTg1.any { it.bikeId == "BIKE_A1" })
        assertFalse(bikesAInTg1.any { it.bikeId == "BIKE_B1" }) // bikeB1 is in same storeLocation but different dealership

        // Test InventoryRepository getBikesForState filters dealershipId FIRST
        val bikesAInTg = inventoryRepo.getBikesForState("dealership_A", "TG")
        assertTrue(bikesAInTg.any { it.bikeId == "BIKE_A1" })
        assertFalse(bikesAInTg.any { it.bikeId == "BIKE_B1" })

        // Test UserRepository getUsersForStore filters dealershipId FIRST
        val userA1 = User(uid = "U_A1", dealershipId = "dealership_A", storeId = "STORE_1")
        val userB1 = User(uid = "U_B1", dealershipId = "dealership_B", storeId = "STORE_1")
        userRepo.saveUser(userA1)
        userRepo.saveUser(userB1)

        val usersAStore1 = userRepo.getUsersForStore("dealership_A", "STORE_1")
        assertTrue(usersAStore1.any { it.uid == "U_A1" })
        assertFalse(usersAStore1.any { it.uid == "U_B1" })

        // Test ViewModel scoping: when user belongs to dealership_A, ViewModels exclude dealership_B items
        val authRepo = appContainer.authRepository
        val userA = User(uid = "USER_A", email = "userA@dealership.com", dealershipId = "dealership_A", role = UserRole.ADMIN, active = true)
        authRepo.setCurrentUser(userA)

        val inventoryVm = viewModelFactory.create(InventoryViewModel::class.java)
        val filteredBikes = inventoryVm.filteredBikes.first { it.isNotEmpty() }
        assertTrue(filteredBikes.any { it.dealershipId == "dealership_A" })
        assertFalse(filteredBikes.any { it.dealershipId == "dealership_B" })
    }

    @Test
    fun testLogoutContextClearance() {
        val authRepo = appContainer.authRepository

        // Set active session user
        val user = User(
            uid = "USER_LOGOUT_TEST",
            email = "logout@dealership.com",
            dealershipId = "dealership_xyz",
            role = UserRole.DEALERSHIP_ADMIN,
            storeId = "STORE_XYZ_1",
            active = true
        )
        authRepo.setCurrentUser(user)

        // Verify TenantContext is populated
        assertEquals("dealership_xyz", TenantContext.dealershipId.value)
        assertEquals(UserRole.DEALERSHIP_ADMIN, TenantContext.role.value)
        assertEquals("STORE_XYZ_1", TenantContext.activeStoreId.value)
        assertEquals(user, TenantContext.currentUser.value)

        // Perform logout
        authRepo.logout()

        // Verify TenantContext.clear() reset all context fields to null or empty
        assertNull(authRepo.currentUser.value)
        assertNull(TenantContext.currentUser.value)
        assertNull(TenantContext.currentDealership.value)
        assertEquals("", TenantContext.dealershipId.value)
        assertNull(TenantContext.role.value)
        assertEquals("", TenantContext.activeStoreId.value)
    }
}
