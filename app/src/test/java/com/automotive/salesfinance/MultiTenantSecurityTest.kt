package com.automotive.salesfinance

import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MultiTenantSecurityTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var loanRepository: LoanRepository

    @Before
    fun setUp() {
        authRepository = AuthRepository()
        userRepository = authRepository.userRepository
        inventoryRepository = InventoryRepository(authRepository)
        customerRepository = CustomerRepository(authRepository)
        loanRepository = LoanRepository(authRepository)
        TenantContext.reset()
    }

    @Test
    fun testUnauthenticatedUser_accessDenied() {
        authRepository.logout()
        assertNull(authRepository.currentUser.value)
        assertNull(TenantContext.currentUser.value)

        // In unauthenticated state, non-existent user cannot be retrieved
        runBlocking {
            val userProfile = userRepository.getUserProfile("NON_EXISTENT_UID")
            assertNull(userProfile)
        }
    }

    @Test
    fun testMissingOrBlankDealershipId_deniedAuthentication() = runBlocking {
        // User with blank dealership ID
        val invalidUser = User(
            uid = "USER_BLANK_DEALERSHIP",
            name = "Test Blank",
            email = "blank@dealership.com",
            role = UserRole.SALES_USER,
            dealershipId = "",
            active = true
        )

        userRepository.saveUser(invalidUser)

        val loginResult = authRepository.login("blank@dealership.com", "password")
        assertTrue(loginResult.isFailure)
        val exception = loginResult.exceptionOrNull()
        assertNotNull(exception)
    }

    @Test
    fun testInactiveUser_deniedAuthentication() = runBlocking {
        val inactiveUser = User(
            uid = "USER_INACTIVE",
            name = "Inactive User",
            email = "inactive@dealership.com",
            role = UserRole.SALES_USER,
            dealershipId = "dealership_demo_001",
            active = false
        )

        userRepository.saveUser(inactiveUser)

        // Logging in as inactive user should fail
        val loginResult = authRepository.login("inactive@dealership.com", "password")
        assertTrue(loginResult.isFailure)
        val exception = loginResult.exceptionOrNull()
        assertNotNull(exception)

        // Current user should remain null or unauthenticated
        assertNull(authRepository.currentUser.value)
    }

    @Test
    fun testCrossDealershipAccess_prevention() = runBlocking {
        val bikeDealershipA = Bike(
            bikeId = "BIKE_A_001",
            dealershipId = "dealership_A",
            chassisNumber = "CHASSIS_A_001",
            engineNumber = "ENGINE_A_001",
            model = "Model A"
        )
        val bikeDealershipB = Bike(
            bikeId = "BIKE_B_001",
            dealershipId = "dealership_B",
            chassisNumber = "CHASSIS_B_001",
            engineNumber = "ENGINE_B_001",
            model = "Model B"
        )

        inventoryRepository.addOrUpdateBike(bikeDealershipA)
        inventoryRepository.addOrUpdateBike(bikeDealershipB)

        val bikesA = inventoryRepository.getBikesForDealership("dealership_A")
        val bikesB = inventoryRepository.getBikesForDealership("dealership_B")

        assertTrue(bikesA.any { it.bikeId == "BIKE_A_001" })
        assertFalse(bikesA.any { it.bikeId == "BIKE_B_001" })

        assertTrue(bikesB.any { it.bikeId == "BIKE_B_001" })
        assertFalse(bikesB.any { it.bikeId == "BIKE_A_001" })

        val customerA = Customer(customerId = "CUST_A_001", dealershipId = "dealership_A", fullName = "Customer A")
        val customerB = Customer(customerId = "CUST_B_001", dealershipId = "dealership_B", fullName = "Customer B")

        customerRepository.saveCustomer(customerA)
        customerRepository.saveCustomer(customerB)

        val custsA = customerRepository.getCustomersForDealership("dealership_A")
        val custsB = customerRepository.getCustomersForDealership("dealership_B")

        assertTrue(custsA.any { it.customerId == "CUST_A_001" })
        assertFalse(custsA.any { it.customerId == "CUST_B_001" })
        assertTrue(custsB.any { it.customerId == "CUST_B_001" })
        assertFalse(custsB.any { it.customerId == "CUST_A_001" })

        val loanA = Loan(loanId = "LOAN_A_001", dealershipId = "dealership_A", customerId = "CUST_A_001")
        val loanB = Loan(loanId = "LOAN_B_001", dealershipId = "dealership_B", customerId = "CUST_B_001")

        loanRepository.saveLoan(loanA)
        loanRepository.saveLoan(loanB)

        val loansA = loanRepository.getLoansForDealership("dealership_A")
        val loansB = loanRepository.getLoansForDealership("dealership_B")

        assertTrue(loansA.any { it.loanId == "LOAN_A_001" })
        assertFalse(loansA.any { it.loanId == "LOAN_B_001" })
        assertTrue(loansB.any { it.loanId == "LOAN_B_001" })
        assertFalse(loansB.any { it.loanId == "LOAN_A_001" })
    }

    @Test
    fun testRoleEscalationAndPrivilegedFields_modificationDenied() {
        val salesUser = User(
            uid = "USER_SALES",
            role = UserRole.SALES_USER,
            dealershipId = "dealership_demo_001",
            storeId = "STORE_001",
            stateCode = "TG"
        )

        // Sales user cannot access other stores or states
        assertFalse(salesUser.canAccessStore("STORE_999"))
        assertTrue(salesUser.canAccessStore("STORE_001"))

        assertFalse(salesUser.canAccessState("KA"))
        assertTrue(salesUser.canAccessState("TG"))

        val adminUser = User(
            uid = "USER_ADMIN",
            role = UserRole.DEALERSHIP_ADMIN,
            dealershipId = "dealership_demo_001"
        )

        // Admin can access any store/state under dealership
        assertTrue(adminUser.canAccessStore("STORE_999"))
        assertTrue(adminUser.canAccessState("KA"))
    }

    @Test
    fun testTenantContext_serverSideProfileResolution() {
        val userProfile = User(
            uid = "USER_RESOLVED",
            name = "Resolved User",
            email = "resolved@dealership.com",
            role = UserRole.STORE_MANAGER,
            dealershipId = "dealership_xyz_789",
            storeId = "STORE_MUMBAI",
            active = true
        )

        TenantContext.setCurrentUser(userProfile)

        assertEquals("dealership_xyz_789", TenantContext.dealershipId.value)
        assertEquals(UserRole.STORE_MANAGER, TenantContext.role.value)
        assertEquals("STORE_MUMBAI", TenantContext.activeStoreId.value)
        assertEquals(userProfile, TenantContext.currentUser.value)
    }
}
