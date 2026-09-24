package com.automotive.salesfinance

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.FeatureAccessManager
import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.repository.UserRepository
import com.automotive.salesfinance.viewmodel.CustomerViewModel
import com.automotive.salesfinance.viewmodel.DealershipAdminViewModel
import com.automotive.salesfinance.viewmodel.FinanceViewModel
import com.automotive.salesfinance.viewmodel.InventoryViewModel
import com.automotive.salesfinance.viewmodel.ReportsViewModel
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
class Phase3QuotaAndStoreTest {

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

    private lateinit var inventoryViewModel: InventoryViewModel
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var dealershipAdminViewModel: DealershipAdminViewModel
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var reportsViewModel: ReportsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        TenantContext.reset()

        authRepository = AuthRepository()
        dealershipRepository = DealershipRepository(authRepository)
        storeRepository = StoreRepository(authRepository)
        userRepository = UserRepository(authRepository)
        inventoryRepository = InventoryRepository(authRepository)
        loanRepository = LoanRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        transactionRepository = TransactionRepository(authRepository)
        auditLogRepository = AuditLogRepository(authRepository)

        inventoryViewModel = InventoryViewModel(
            inventoryRepository = inventoryRepository,
            storeRepository = storeRepository,
            authRepository = authRepository,
            dealershipRepository = dealershipRepository
        )

        customerViewModel = CustomerViewModel(
            customerRepository = customerRepository,
            loanRepository = loanRepository,
            inventoryRepository = inventoryRepository,
            authRepository = authRepository,
            dealershipRepository = dealershipRepository,
            transactionRepository = transactionRepository
        )

        dealershipAdminViewModel = DealershipAdminViewModel(
            authRepository = authRepository,
            dealershipRepository = dealershipRepository,
            storeRepository = storeRepository,
            userRepository = userRepository,
            inventoryRepository = inventoryRepository,
            loanRepository = loanRepository,
            customerRepository = customerRepository,
            transactionRepository = transactionRepository,
            auditLogRepository = auditLogRepository
        )

        financeViewModel = FinanceViewModel(
            loanRepository = loanRepository,
            inventoryRepository = inventoryRepository,
            customerRepository = customerRepository,
            transactionRepository = transactionRepository,
            auditLogRepository = auditLogRepository,
            authRepository = authRepository,
            dealershipRepository = dealershipRepository
        )

        reportsViewModel = ReportsViewModel(
            authRepository = authRepository,
            dealershipRepository = dealershipRepository,
            storeRepository = storeRepository
        )
    }

    @After
    fun tearDown() {
        TenantContext.reset()
        Dispatchers.resetMain()
    }

    @Test
    fun dynamicStoreLoading_loadsStoresForActiveDealershipId() = runTest {
        val activeDealershipId = DemoData.DEMO_DEALERSHIP_ID
        val stores = storeRepository.getStoresForDealership(activeDealershipId)

        assertTrue(stores.isNotEmpty())
        assertTrue(stores.all { it.dealershipId == activeDealershipId })
    }

    @Test
    fun crossDealershipStores_doNotAppear() = runTest {
        val otherStore = Store(
            storeId = "STORE_OTHER_001",
            dealershipId = "dealership_other_999",
            storeName = "Other Dealership Store",
            stateCode = "KA",
            city = "Bangalore"
        )
        storeRepository.saveStore(otherStore)

        val storesForDemo = storeRepository.getStoresForDealership(DemoData.DEMO_DEALERSHIP_ID)

        assertFalse(storesForDemo.any { it.dealershipId == "dealership_other_999" })
        assertFalse(storesForDemo.any { it.storeId == "STORE_OTHER_001" })
    }

    @Test
    fun quotaEnforcement_inventoryViewModel_canAddBike() = runTest {
        val testDealership = Dealership(
            dealershipId = "dealership_quota_test",
            name = "Quota Test Dealership",
            subscriptionPlan = "STARTER",
            subscriptionStatus = SubscriptionStatus.ACTIVE
        )
        dealershipRepository.saveDealership(testDealership)

        val user = User(
            uid = "USER_QUOTA_TEST",
            dealershipId = "dealership_quota_test",
            name = "Quota Tester",
            email = "quota@test.com",
            role = UserRole.DEALERSHIP_ADMIN
        )
        authRepository.setCurrentUser(user)

        // Fill 100 bikes (STARTER maxBikes = 100)
        val starterPlan = SubscriptionPlan.STARTER
        for (i in 1..starterPlan.maxBikes) {
            val b = Bike(
                bikeId = "BIKE_QUOTA_$i",
                dealershipId = "dealership_quota_test",
                chassisNumber = "CHASSIS_Q_$i",
                engineNumber = "ENGINE_Q_$i",
                make = "Honda",
                model = "Activa",
                year = 2024,
                costPrice = 80000.0,
                listedPrice = 90000.0,
                stateCode = "TG",
                storeLocation = "TG_Madhapur",
                status = BikeStatus.AVAILABLE
            )
            inventoryRepository.addOrUpdateBike(b)
        }

        // Verify count is 100 and canAddBike(100, STARTER) is false
        assertFalse(FeatureAccessManager.canAddBike(starterPlan.maxBikes, starterPlan))

        // Attempt to add 101st bike
        val overflowBike = Bike(
            bikeId = "NEW",
            dealershipId = "dealership_quota_test",
            chassisNumber = "CHASSIS_OVERFLOW",
            engineNumber = "ENGINE_OVERFLOW",
            make = "TVS",
            model = "Jupiter",
            year = 2024,
            costPrice = 75000.0,
            listedPrice = 85000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur",
            status = BikeStatus.AVAILABLE
        )

        var errorMsg: String? = null
        inventoryViewModel.addOrUpdateBike(overflowBike, onError = { errorMsg = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(errorMsg)
        assertTrue(errorMsg!!.contains("Plan limit reached"))
    }

    @Test
    fun quotaEnforcement_customerViewModel_canAddCustomer() = runTest {
        val testDealership = Dealership(
            dealershipId = "dealership_cust_quota",
            name = "Cust Quota Dealership",
            subscriptionPlan = "STARTER",
            subscriptionStatus = SubscriptionStatus.ACTIVE
        )
        dealershipRepository.saveDealership(testDealership)

        val user = User(
            uid = "USER_CUST_TEST",
            dealershipId = "dealership_cust_quota",
            name = "Cust Admin",
            email = "cust@test.com",
            role = UserRole.DEALERSHIP_ADMIN
        )
        authRepository.setCurrentUser(user)

        val starterPlan = SubscriptionPlan.STARTER
        for (i in 1..starterPlan.maxCustomers) {
            val phone = "91${i.toString().padStart(8, '0')}"
            val c = Customer(
                customerId = "CUST_Q_$i",
                dealershipId = "dealership_cust_quota",
                fullName = "Cust $i",
                phone = phone,
                email = "cust$i@test.com",
                address = "Address",
                city = "Hyderabad",
                state = "Telangana"
            )
            customerRepository.saveCustomer(c)
        }

        assertFalse(FeatureAccessManager.canAddCustomer(starterPlan.maxCustomers, starterPlan))

        // Attempt to add 501st customer
        var secondError: String? = null
        customerViewModel.addCustomer(
            fullName = "Overflow Customer",
            phone = "9999999999",
            email = "overflow@test.com",
            address = "Address Overflow",
            city = "Hyderabad",
            state = "Telangana",
            onError = { secondError = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(secondError)
        assertTrue(secondError!!.contains("Plan limit reached"))
    }

    @Test
    fun quotaEnforcement_dealershipAdminViewModel_canAddStoreAndUser() = runTest {
        val testDealership = Dealership(
            dealershipId = "dealership_admin_quota",
            name = "Admin Quota Dealership",
            subscriptionPlan = "STARTER", // maxStores = 1, maxUsers = 3
            subscriptionStatus = SubscriptionStatus.ACTIVE
        )
        dealershipRepository.saveDealership(testDealership)

        val adminUser = User(
            uid = "USER_ADMIN_QUOTA_1",
            dealershipId = "dealership_admin_quota",
            name = "Admin User 1",
            email = "admin1@quota.com",
            role = UserRole.DEALERSHIP_ADMIN
        )
        userRepository.saveUser(adminUser)
        authRepository.setCurrentUser(adminUser)

        dealershipAdminViewModel.setSelectedDealershipId("dealership_admin_quota")
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Store quota check (STARTER allows max 1 store)
        val store1 = Store(storeId = "STORE_01", dealershipId = "dealership_admin_quota", storeName = "Store 1", stateCode = "TG", city = "Hyd")
        storeRepository.saveStore(store1)

        val starterPlan = SubscriptionPlan.STARTER
        assertFalse(FeatureAccessManager.canAddStore(1, starterPlan))

        val store2 = Store(storeId = "", storeName = "Store 2", stateCode = "TG", city = "Hyd")
        dealershipAdminViewModel.saveStore(store2)
        testDispatcher.scheduler.advanceUntilIdle()

        val storeError = dealershipAdminViewModel.errorMessage.value
        assertNotNull(storeError)
        assertTrue(storeError!!.contains("Plan limit reached"))

        // 2. User quota check (STARTER allows max 3 users)
        val user2 = User(uid = "USER_ADMIN_QUOTA_2", dealershipId = "dealership_admin_quota", name = "User 2", email = "u2@quota.com", role = UserRole.SALES_USER)
        val user3 = User(uid = "USER_ADMIN_QUOTA_3", dealershipId = "dealership_admin_quota", name = "User 3", email = "u3@quota.com", role = UserRole.SALES_USER)
        userRepository.saveUser(user2)
        userRepository.saveUser(user3)

        assertFalse(FeatureAccessManager.canAddUser(3, starterPlan))

        val user4 = User(uid = "", name = "User 4", email = "user4@quota.com", role = UserRole.SALES_USER)
        dealershipAdminViewModel.saveUser(user4)
        testDispatcher.scheduler.advanceUntilIdle()

        val userError = dealershipAdminViewModel.errorMessage.value
        assertNotNull(userError)
        assertTrue(userError!!.contains("Plan limit reached"))
    }

    @Test
    fun featureEntitlementChecks_finance_reports_multiStore() {
        val starter = SubscriptionPlan.STARTER
        val pro = SubscriptionPlan.PROFESSIONAL

        // Starter plan entitlements
        assertTrue(FeatureAccessManager.hasFinance(starter))
        assertTrue(FeatureAccessManager.hasFeature("reports", starter))
        assertFalse(FeatureAccessManager.hasAdvancedReports(starter))
        assertFalse(FeatureAccessManager.hasMultiStore(starter))

        // Professional plan entitlements
        assertTrue(FeatureAccessManager.hasFinance(pro))
        assertTrue(FeatureAccessManager.hasFeature("reports", pro))
        assertTrue(FeatureAccessManager.hasAdvancedReports(pro))
        assertTrue(FeatureAccessManager.hasMultiStore(pro))
    }

    @Test
    fun readOnlyBehavior_expiredOrSuspendedSubscriptions() = runTest {
        val expiredDealership = Dealership(
            dealershipId = "dealership_expired_001",
            name = "Expired Dealership",
            subscriptionPlan = "STARTER",
            subscriptionStatus = SubscriptionStatus.EXPIRED
        )
        dealershipRepository.saveDealership(expiredDealership)
        TenantContext.setDealership(expiredDealership)

        val user = User(
            uid = "USER_EXPIRED",
            dealershipId = "dealership_expired_001",
            name = "Expired Admin",
            email = "expired@test.com",
            role = UserRole.DEALERSHIP_ADMIN
        )
        authRepository.setCurrentUser(user)

        // Verify active status is false
        assertFalse(FeatureAccessManager.isSubscriptionActive(expiredDealership))
        assertFalse(FeatureAccessManager.isSubscriptionActive(SubscriptionStatus.EXPIRED))
        assertFalse(FeatureAccessManager.isSubscriptionActive(SubscriptionStatus.SUSPENDED))

        // Attempt bike write -> should be rejected
        var bikeError: String? = null
        val bike = Bike(
            bikeId = "NEW",
            dealershipId = "dealership_expired_001",
            chassisNumber = "CHEXP123",
            engineNumber = "ENGEXP123",
            make = "Honda",
            model = "Shine",
            year = 2024,
            costPrice = 70000.0,
            listedPrice = 80000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur"
        )
        inventoryViewModel.addOrUpdateBike(bike, onError = { bikeError = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(bikeError)
        assertTrue(bikeError!!.contains("Subscription inactive"))

        // Attempt customer write -> should be rejected
        var custError: String? = null
        customerViewModel.addCustomer("Exp Cust", "9111111111", "exp@test.com", "Addr", "City", "State", onError = { custError = it })
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(custError)
        assertTrue(custError!!.contains("Subscription inactive"))
    }

    @Test
    fun roleRestrictionsCombinedWithPlanLimits() = runTest {
        val user = User(
            uid = "STORE_MGR_001",
            dealershipId = DemoData.DEMO_DEALERSHIP_ID,
            name = "Store Manager Hyd",
            email = "mgr@hyd.com",
            role = UserRole.STORE_MANAGER,
            storeId = "TG_Hyderabad",
            stateCode = "TG"
        )
        authRepository.setCurrentUser(user)

        // Check store filtering in InventoryViewModel
        val stores = inventoryViewModel.availableStores.value
        assertTrue(stores.all { it.storeId == "TG_Hyderabad" })

        // Check plan limits also apply
        val starterPlan = SubscriptionPlan.STARTER.copy(maxBikes = 0)
        assertFalse(FeatureAccessManager.canAddBike(0, starterPlan))
    }
}
