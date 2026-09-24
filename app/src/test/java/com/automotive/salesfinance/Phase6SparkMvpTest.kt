package com.automotive.salesfinance

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.model.Vehicle
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.SubscriptionRepository
import com.automotive.salesfinance.repository.SupportTicketRepository
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.repository.UserRepository
import com.automotive.salesfinance.services.RazorpayPaymentServiceImpl
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Phase6SparkMvpTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: AuthRepository
    private lateinit var dealershipRepository: DealershipRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var userRepository: UserRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var loanRepository: LoanRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var auditLogRepository: AuditLogRepository
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var supportTicketRepository: SupportTicketRepository

    private lateinit var financeViewModel: FinanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        authRepository = AuthRepository()
        dealershipRepository = DealershipRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        userRepository = UserRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        transactionRepository = TransactionRepository(authRepository)
        auditLogRepository = AuditLogRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)

        loanRepository = LoanRepository(
            authRepository = authRepository,
            transactionRepository = transactionRepository,
            auditLogRepository = auditLogRepository,
            customerRepository = customerRepository
        )

        subscriptionRepository = SubscriptionRepository(authRepository)
        supportTicketRepository = SupportTicketRepository(authRepository)

        financeViewModel = FinanceViewModel(
            loanRepository = loanRepository,
            inventoryRepository = inventoryRepository,
            customerRepository = customerRepository,
            transactionRepository = transactionRepository,
            auditLogRepository = auditLogRepository,
            authRepository = authRepository,
            paymentService = RazorpayPaymentServiceImpl(),
            dealershipRepository = dealershipRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. Spark MVP Production Mode Payment State Handling
    @Test
    fun testProductionModePaymentHandling_disabledInProductionWithoutBackend() = runTest {
        // Switch to Live Production Mode (isDemoMode = false)
        authRepository.setDemoMode(false)
        assertFalse(authRepository.isDemoMode.value)

        var errorMessage: String? = null
        var paymentSuccess = false

        financeViewModel.processCustomerPayment(
            loanId = "LOAN_001",
            amount = 5500.0,
            purpose = "REGULAR_EMI",
            paymentMethod = "UPI_INTENT",
            onSuccess = { paymentSuccess = true },
            onError = { err -> errorMessage = err }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(paymentSuccess)
        assertEquals(PaymentState.FAILED, financeViewModel.paymentState.value)
        assertNotNull(errorMessage)
        assertTrue(
            errorMessage!!.contains("Live Payment Verification Unavailable") ||
            errorMessage!!.contains("Backend Deployment Required")
        )

        // Switch back to Demo Mode (isDemoMode = true) -> Simulated Payment Succeeds
        authRepository.setDemoMode(true)
        assertTrue(authRepository.isDemoMode.value)

        financeViewModel.processDemoPaymentSimulation(
            loanId = "LOAN_001",
            amount = 5500.0,
            purpose = "REGULAR_EMI",
            onSuccess = { paymentSuccess = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(paymentSuccess)
        assertEquals(PaymentState.SUCCESS, financeViewModel.paymentState.value)
    }

    // 2. Multi-Vehicle Configuration Adaptation (Bike-Only, Car-Only, Bike+Car)
    @Test
    fun testMultiVehicleConfigurationAdaptation() = runTest {
        // Bike-Only Dealership
        val bikeOnlyDealership = dealershipRepository.getDealershipById(DemoData.DEMO_DEALERSHIP_BIKES_ONLY)
        assertNotNull(bikeOnlyDealership)
        assertEquals(listOf(VehicleType.BIKE), bikeOnlyDealership?.supportedVehicleTypes)

        val bikeVehicles = inventoryRepository.getVehiclesForDealership(DemoData.DEMO_DEALERSHIP_BIKES_ONLY)
        assertTrue(bikeVehicles.all { it.vehicleType == VehicleType.BIKE })

        // Car-Only Dealership
        val carOnlyDealership = dealershipRepository.getDealershipById(DemoData.DEMO_DEALERSHIP_CARS_ONLY)
        assertNotNull(carOnlyDealership)
        assertEquals(listOf(VehicleType.CAR), carOnlyDealership?.supportedVehicleTypes)

        val carVehicles = inventoryRepository.getVehiclesForDealership(DemoData.DEMO_DEALERSHIP_CARS_ONLY)
        assertTrue(carVehicles.all { it.vehicleType == VehicleType.CAR })

        // Bike + Car Dealership
        val multiVehicleDealership = dealershipRepository.getDealershipById(DemoData.DEMO_DEALERSHIP_ID)
        assertNotNull(multiVehicleDealership)
        assertEquals(listOf(VehicleType.BIKE, VehicleType.CAR), multiVehicleDealership?.supportedVehicleTypes)

        val multiVehicles = inventoryRepository.getVehiclesForDealership(DemoData.DEMO_DEALERSHIP_ID)
        assertTrue(multiVehicles.any { it.vehicleType == VehicleType.BIKE })
        assertTrue(multiVehicles.any { it.vehicleType == VehicleType.CAR })
    }

    // 3. Multi-Tenant Isolation Verification across All Entities
    @Test
    fun testTenantIsolationForAllEntities() = runTest {
        val d1 = "tenant_dealership_001"
        val d2 = "tenant_dealership_002"

        // 1. Vehicle Isolation
        val v1 = Vehicle(vehicleId = "V_T1", dealershipId = d1, vehicleType = VehicleType.BIKE, chassisNumber = "VIN_T1", engineNumber = "ENG_T1", make = "Honda", model = "Shine", stateCode = "TG", storeId = "S1")
        val v2 = Vehicle(vehicleId = "V_T2", dealershipId = d2, vehicleType = VehicleType.CAR, chassisNumber = "VIN_T2", engineNumber = "ENG_T2", make = "Hyundai", model = "I20", stateCode = "KA", storeId = "S2")
        inventoryRepository.addOrUpdateVehicle(v1)
        inventoryRepository.addOrUpdateVehicle(v2)

        val d1Vehicles = inventoryRepository.getVehiclesForDealership(d1)
        assertEquals(1, d1Vehicles.size)
        assertEquals("V_T1", d1Vehicles.first().vehicleId)
        assertFalse(d1Vehicles.any { it.dealershipId == d2 })

        // 2. Customer Isolation
        val c1 = Customer(customerId = "C_T1", dealershipId = d1, fullName = "Customer T1", phone = "9876543210")
        val c2 = Customer(customerId = "C_T2", dealershipId = d2, fullName = "Customer T2", phone = "9876543211")
        customerRepository.saveCustomer(c1)
        customerRepository.saveCustomer(c2)

        val d1Customers = customerRepository.getCustomersForDealership(d1)
        assertEquals(1, d1Customers.size)
        assertEquals("C_T1", d1Customers.first().customerId)
        assertFalse(d1Customers.any { it.dealershipId == d2 })

        // 3. Loan Isolation
        val l1 = Loan(loanId = "L_T1", dealershipId = d1, customerId = "C_T1", bikeId = "V_T1", totalAmount = 80000.0, remainingBalance = 50000.0, emiAmount = 5000.0)
        val l2 = Loan(loanId = "L_T2", dealershipId = d2, customerId = "C_T2", bikeId = "V_T2", totalAmount = 500000.0, remainingBalance = 400000.0, emiAmount = 20000.0)
        loanRepository.saveLoan(l1)
        loanRepository.saveLoan(l2)

        val d1Loans = loanRepository.getLoansForDealership(d1)
        assertEquals(1, d1Loans.size)
        assertEquals("L_T1", d1Loans.first().loanId)
        assertFalse(d1Loans.any { it.dealershipId == d2 })

        // 4. Transaction Isolation
        val t1 = Transaction(transactionId = "TXN_T1", dealershipId = d1, customerId = "C_T1", loanId = "L_T1", amount = 5000.0, status = TransactionStatus.SUCCESS)
        val t2 = Transaction(transactionId = "TXN_T2", dealershipId = d2, customerId = "C_T2", loanId = "L_T2", amount = 20000.0, status = TransactionStatus.SUCCESS)
        transactionRepository.addTransaction(t1)
        transactionRepository.addTransaction(t2)

        val d1Txns = transactionRepository.getTransactionsForDealership(d1)
        assertEquals(1, d1Txns.size)
        assertEquals("TXN_T1", d1Txns.first().transactionId)
        assertFalse(d1Txns.any { it.dealershipId == d2 })

        // 5. Store Isolation
        val s1 = Store(storeId = "S_T1", dealershipId = d1, storeName = "Store T1", stateCode = "TG", city = "Hyderabad")
        val s2 = Store(storeId = "S_T2", dealershipId = d2, storeName = "Store T2", stateCode = "KA", city = "Bangalore")
        storeRepository.saveStore(s1)
        storeRepository.saveStore(s2)

        val d1Stores = storeRepository.getStoresForDealership(d1)
        assertEquals(1, d1Stores.size)
        assertEquals("S_T1", d1Stores.first().storeId)
        assertFalse(d1Stores.any { it.dealershipId == d2 })
    }

    // 4. Quota Optimization & Query Limits
    @Test
    fun testQuotaOptimizationQueryLimits() = runTest {
        // Verify all repository fetch methods run safely without throwing exceptions
        val dId = DemoData.DEMO_DEALERSHIP_ID

        val vehicles = inventoryRepository.fetchVehiclesFromFirestore(dId)
        assertNotNull(vehicles)

        val customers = customerRepository.fetchCustomersFromFirestore(dId)
        assertNotNull(customers)

        val loans = loanRepository.fetchLoansFromFirestore(dId)
        assertNotNull(loans)

        val transactions = transactionRepository.fetchTransactionsFromFirestore(dId)
        assertNotNull(transactions)

        val stores = storeRepository.fetchStoresFromFirestore(dId)
        assertNotNull(stores)

        val users = userRepository.fetchUsersFromFirestore(dId)
        assertNotNull(users)

        val dealerships = dealershipRepository.fetchDealershipsFromFirestore()
        assertNotNull(dealerships)

        val subscriptions = subscriptionRepository.fetchSubscriptionsFromFirestore()
        assertNotNull(subscriptions)

        val tickets = supportTicketRepository.fetchSupportTicketsFromFirestore()
        assertNotNull(tickets)

        val auditLogs = auditLogRepository.fetchAuditLogsFromFirestore(dId)
        assertNotNull(auditLogs)
    }

    // 5. Security Boundary Verification (Demo Role Switcher Rejection in Production Mode)
    @Test
    fun testSecurityBoundary_demoRoleSwitcherRejectionInProductionMode() = runTest {
        // Put repository in Live Production Mode
        authRepository.setDemoMode(false)
        assertFalse(authRepository.isDemoMode.value)

        // Attempt role switching in production mode -> must be rejected
        val switchResult = authRepository.switchDemoRole(UserRole.SUPER_ADMIN)
        assertTrue(switchResult.isFailure)

        val exception = switchResult.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is SecurityException)
        assertTrue(exception!!.message!!.contains("Role switching is disabled in production"))

        // Re-enable Demo Mode -> Role switching succeeds
        authRepository.setDemoMode(true)
        assertTrue(authRepository.isDemoMode.value)

        val demoSwitchResult = authRepository.switchDemoRole(UserRole.SUPER_ADMIN)
        assertTrue(demoSwitchResult.isSuccess)
        assertEquals(UserRole.SUPER_ADMIN, authRepository.currentUser.value?.role)
    }
}
