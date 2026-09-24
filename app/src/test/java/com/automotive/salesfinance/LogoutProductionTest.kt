import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogoutProductionTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AuthRepository.resetInstance()
        TenantContext.clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        AuthRepository.resetInstance()
        TenantContext.clear()
    }

    @Test
    fun testProductionLogoutClearsSessionAndResetsRepositoryStateFlows() = runTest {
        val authRepo = AuthRepository.getInstance()
        authRepo.setDemoMode(false)

        val userRepo = UserRepository(authRepo)
        val dealershipRepo = DealershipRepository(authRepo)
        val inventoryRepo = InventoryRepository(authRepo)
        val customerRepo = CustomerRepository(authRepo)
        val loanRepo = LoanRepository(authRepo)
        val storeRepo = StoreRepository(authRepo)
        val subscriptionRepo = SubscriptionRepository(authRepo)
        val supportTicketRepo = SupportTicketRepository(authRepo)
        val transactionRepo = TransactionRepository(authRepo)
        val auditLogRepo = AuditLogRepository(authRepo)

        // Establish production session for Dealership Alpha
        val prodUserAlpha = User(
            uid = "SUPER_ADMIN_ALPHA",
            email = "superadmin@platform.com",
            dealershipId = "dealership_alpha",
            role = UserRole.SUPER_ADMIN,
            storeId = "STORE_ALPHA_1",
            active = true
        )
        authRepo.setCurrentUser(prodUserAlpha)

        // Add test data to verify clearing
        storeRepo.saveStore(
            Store(
                storeId = "STORE_ALPHA_1",
                dealershipId = "dealership_alpha",
                storeName = "Alpha Main Store",
                stateCode = "KA",
                city = "Bangalore",
                address = "123 Alpha St"
            )
        )

        // Verify active production session
        assertEquals("SUPER_ADMIN_ALPHA", authRepo.currentUser.value?.uid)
        assertEquals("dealership_alpha", TenantContext.dealershipId.value)
        assertEquals(UserRole.SUPER_ADMIN, TenantContext.role.value)
        assertFalse("isDemoMode must be false in production", authRepo.isDemoMode.value)
        assertEquals(1, storeRepo.storesFlow.value.size)

        // Perform logout
        authRepo.logout()

        // Assert session & TenantContext cleared
        assertNull("currentUser must be null after logout", authRepo.currentUser.value)
        assertNull("TenantContext currentUser must be null", TenantContext.currentUser.value)
        assertEquals("TenantContext dealershipId must be empty", "", TenantContext.dealershipId.value)
        assertNull("TenantContext role must be null", TenantContext.role.value)

        // Assert isDemoMode remains false
        assertFalse("isDemoMode must remain false after production logout", authRepo.isDemoMode.value)

        // Assert repository state flows reset to emptyList()
        assertTrue("userRepo usersFlow must be empty after logout", userRepo.usersFlow.value.isEmpty())
        assertTrue("dealershipRepo dealershipsFlow must be empty after logout", dealershipRepo.dealershipsFlow.value.isEmpty())
        assertTrue("inventoryRepo vehiclesFlow must be empty after logout", inventoryRepo.vehiclesFlow.value.isEmpty())
        assertTrue("inventoryRepo bikesFlow must be empty after logout", inventoryRepo.bikesFlow.value.isEmpty())
        assertTrue("customerRepo customersFlow must be empty after logout", customerRepo.customersFlow.value.isEmpty())
        assertTrue("loanRepo loansFlow must be empty after logout", loanRepo.loansFlow.value.isEmpty())
        assertTrue("storeRepo storesFlow must be empty after logout", storeRepo.storesFlow.value.isEmpty())
        assertTrue("subscriptionRepo subscriptionsFlow must be empty after logout", subscriptionRepo.subscriptionsFlow.value.isEmpty())
        assertTrue("supportTicketRepo supportTicketsFlow must be empty after logout", supportTicketRepo.supportTicketsFlow.value.isEmpty())
        assertTrue("transactionRepo transactionsFlow must be empty after logout", transactionRepo.transactionsFlow.value.isEmpty())
        assertTrue("auditLogRepo auditLogsFlow must be empty after logout", auditLogRepo.auditLogsFlow.value.isEmpty())
    }

    @Test
    fun testProductionLogoutDoesNotActivateDemoModeOrLoadDemoData() = runTest {
        val authRepo = AuthRepository.getInstance()
        authRepo.setDemoMode(false)

        val dealershipRepo = DealershipRepository(authRepo)
        val userRepo = UserRepository(authRepo)

        val prodUser = User(
            uid = "PROD_SA_001",
            email = "sa@automotive.com",
            dealershipId = "",
            role = UserRole.SUPER_ADMIN,
            active = true
        )
        authRepo.setCurrentUser(prodUser)

        authRepo.logout()

        assertFalse("Logout in production must not switch to demo mode", authRepo.isDemoMode.value)
        assertNull("CurrentUser must be null", authRepo.currentUser.value)

        // Confirm DemoData was NOT loaded
        assertTrue("Dealerships flow must be empty", dealershipRepo.dealershipsFlow.value.isEmpty())
        assertTrue("Users flow must be empty", userRepo.usersFlow.value.isEmpty())
    }

    @Test
    fun testSubsequentLoginEstablishesCompletelyDifferentTenantSession() = runTest {
        val authRepo = AuthRepository.getInstance()
        authRepo.setDemoMode(false)

        val storeRepo = StoreRepository(authRepo)

        // Session 1: Dealership Alpha
        val userAlpha = User(
            uid = "USER_ALPHA",
            email = "admin@alpha.com",
            dealershipId = "dealership_alpha",
            role = UserRole.DEALERSHIP_ADMIN,
            storeId = "STORE_ALPHA_1",
            active = true
        )
        authRepo.setCurrentUser(userAlpha)
        storeRepo.saveStore(
            Store(
                storeId = "STORE_ALPHA_1",
                dealershipId = "dealership_alpha",
                storeName = "Alpha Store",
                stateCode = "MH",
                city = "Mumbai",
                address = "Alpha Road"
            )
        )

        assertEquals("dealership_alpha", TenantContext.dealershipId.value)
        assertEquals(1, storeRepo.storesFlow.value.size)

        // Logout Session 1
        authRepo.logout()

        assertNull(authRepo.currentUser.value)
        assertEquals("", TenantContext.dealershipId.value)
        assertTrue(storeRepo.storesFlow.value.isEmpty())

        // Session 2: Dealership Beta
        val userBeta = User(
            uid = "USER_BETA",
            email = "admin@beta.com",
            dealershipId = "dealership_beta",
            role = UserRole.DEALERSHIP_ADMIN,
            storeId = "STORE_BETA_1",
            active = true
        )
        authRepo.setCurrentUser(userBeta)
        storeRepo.saveStore(
            Store(
                storeId = "STORE_BETA_1",
                dealershipId = "dealership_beta",
                storeName = "Beta Store",
                stateCode = "DL",
                city = "Delhi",
                address = "Beta Road"
            )
        )

        assertEquals("USER_BETA", authRepo.currentUser.value?.uid)
        assertEquals("dealership_beta", TenantContext.dealershipId.value)
        assertEquals(UserRole.DEALERSHIP_ADMIN, TenantContext.role.value)
        assertEquals("STORE_BETA_1", TenantContext.activeStoreId.value)

        // Verify state only reflects Dealership Beta
        val currentStores = storeRepo.storesFlow.value
        assertEquals(1, currentStores.size)
        assertEquals("STORE_BETA_1", currentStores.first().storeId)
        assertEquals("dealership_beta", currentStores.first().dealershipId)
        assertFalse("Must not contain Alpha stores", currentStores.any { it.dealershipId == "dealership_alpha" })
    }
}
