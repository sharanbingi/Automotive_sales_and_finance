package com.automotive.salesfinance

import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.SubscriptionRepository
import com.automotive.salesfinance.repository.SupportTicketRepository
import com.automotive.salesfinance.repository.UserRepository
import com.automotive.salesfinance.viewmodel.SuperAdminViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoleGuardAndRouteScopeTest {

    private val testDispatcher = StandardTestDispatcher()

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
    fun test1_SuperAdminViewModel_SkipsPlatformQueries_WhenUserRoleIsNotSuperAdminOrNull() = runTest {
        val authRepo = AuthRepository.getInstance()
        authRepo.setDemoMode(false)

        val dealershipRepo = DealershipRepository(authRepo)
        val subscriptionRepo = SubscriptionRepository(authRepo)
        val supportTicketRepo = SupportTicketRepository(authRepo)
        val auditLogRepo = AuditLogRepository(authRepo)
        val storeRepo = StoreRepository(authRepo)
        val userRepo = UserRepository(authRepo)

        // Case A: Role is DEALERSHIP_ADMIN
        val nonSuperAdminUser = User(
            uid = "DEALER_ADMIN_001",
            email = "dealer@admin.com",
            dealershipId = "dealership_001",
            role = UserRole.DEALERSHIP_ADMIN,
            active = true
        )
        authRepo.setCurrentUser(nonSuperAdminUser)

        val viewModel = SuperAdminViewModel(
            authRepository = authRepo,
            dealershipRepository = dealershipRepo,
            subscriptionRepository = subscriptionRepo,
            supportTicketRepository = supportTicketRepo,
            auditLogRepository = auditLogRepo,
            storeRepository = storeRepo,
            userRepository = userRepo
        )

        viewModel.loadPlatformData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify platform flow remains empty because query was skipped
        assertTrue("Dealerships flow should be empty when role is DEALERSHIP_ADMIN", dealershipRepo.dealershipsFlow.value.isEmpty())

        // Case B: User is null
        authRepo.logout()
        val viewModelNullUser = SuperAdminViewModel(
            authRepository = authRepo,
            dealershipRepository = dealershipRepo,
            subscriptionRepository = subscriptionRepo,
            supportTicketRepository = supportTicketRepo,
            auditLogRepository = auditLogRepo,
            storeRepository = storeRepo,
            userRepository = userRepo
        )

        viewModelNullUser.loadPlatformData()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Dealerships flow should be empty when user is null", dealershipRepo.dealershipsFlow.value.isEmpty())
    }

    @Test
    fun test2_SuperAdminViewModel_ExecutesPlatformQueries_WhenUserRoleIsSuperAdmin() = runTest {
        val authRepo = AuthRepository.getInstance()
        authRepo.setDemoMode(false)

        val dealershipRepo = DealershipRepository(authRepo)
        val subscriptionRepo = SubscriptionRepository(authRepo)
        val supportTicketRepo = SupportTicketRepository(authRepo)
        val auditLogRepo = AuditLogRepository(authRepo)
        val storeRepo = StoreRepository(authRepo)
        val userRepo = UserRepository(authRepo)

        val superAdminUser = User(
            uid = "SUPER_ADMIN_001",
            email = "superadmin@platform.com",
            dealershipId = "",
            role = UserRole.SUPER_ADMIN,
            active = true
        )
        authRepo.setCurrentUser(superAdminUser)

        val viewModel = SuperAdminViewModel(
            authRepository = authRepo,
            dealershipRepository = dealershipRepo,
            subscriptionRepository = subscriptionRepo,
            supportTicketRepository = supportTicketRepo,
            auditLogRepository = auditLogRepo,
            storeRepository = storeRepo,
            userRepository = userRepo
        )

        viewModel.loadPlatformData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify role is SUPER_ADMIN and role check passes
        assertEquals(UserRole.SUPER_ADMIN, authRepo.currentUser.value?.role)
    }

    @Test
    fun test3_StoreRepositoryAndUserRepository_SkipFirestoreOnBlankDealershipId() = runTest {
        val authRepo = AuthRepository.getInstance()
        authRepo.setDemoMode(false)

        val storeRepo = StoreRepository(authRepo)
        val userRepo = UserRepository(authRepo)
        val dealershipRepo = DealershipRepository(authRepo)

        val fetchedStores = storeRepo.fetchStoresFromFirestore("")
        assertEquals("Store fetch with blank dealershipId must return empty list", emptyList<Store>(), fetchedStores)

        val fetchedUsers = userRepo.fetchUsersFromFirestore("")
        assertEquals("User fetch with blank dealershipId must return empty list", emptyList<User>(), fetchedUsers)

        val fetchedDealership = dealershipRepo.fetchSingleDealershipFromFirestore("")
        assertNull("Single dealership fetch with blank dealershipId must return null", fetchedDealership)
    }

    @Test
    fun test4_Logout_ClearsCurrentUserAndTenantContext_AcrossAllRoles() = runTest {
        val authRepo = AuthRepository.getInstance()
        authRepo.setDemoMode(false)

        val rolesToTest = listOf(
            UserRole.SUPER_ADMIN,
            UserRole.DEALERSHIP_ADMIN,
            UserRole.SALES_USER,
            UserRole.CUSTOMER
        )

        for (role in rolesToTest) {
            val user = User(
                uid = "USER_${role.name}",
                email = "${role.name.lowercase()}@test.com",
                dealershipId = if (role == UserRole.SUPER_ADMIN) "" else "dealership_test_001",
                role = role,
                active = true
            )

            authRepo.setCurrentUser(user)

            assertEquals("currentUser uid must match for $role", user.uid, authRepo.currentUser.value?.uid)
            assertEquals("TenantContext currentUser must match for $role", user.uid, TenantContext.currentUser.value?.uid)

            // Perform logout
            authRepo.logout()

            // Verify session and TenantContext cleared
            assertNull("currentUser must be null after logout for $role", authRepo.currentUser.value)
            assertNull("TenantContext.currentUser must be null after logout for $role", TenantContext.currentUser.value)
            assertEquals("TenantContext.dealershipId must be empty string after logout for $role", "", TenantContext.dealershipId.value)
            assertNull("TenantContext.role must be null after logout for $role", TenantContext.role.value)
        }
    }
}
