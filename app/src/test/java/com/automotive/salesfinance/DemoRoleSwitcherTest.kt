package com.automotive.salesfinance

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.ui.navigation.NavRoutes
import com.automotive.salesfinance.viewmodel.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DemoRoleSwitcherTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setUp() {
        TenantContext.reset()
        authRepository = AuthRepository()
        authRepository.setDemoMode(true)
        authViewModel = AuthViewModel(authRepository)
    }

    @Test
    fun testDemoRoleProfilesInDemoData() {
        val expectedRoles = listOf(
            UserRole.SUPER_ADMIN,
            UserRole.DEALERSHIP_ADMIN,
            UserRole.STATE_MANAGER,
            UserRole.STORE_MANAGER,
            UserRole.SALES_USER,
            UserRole.FINANCE_USER,
            UserRole.CUSTOMER
        )

        expectedRoles.forEach { role ->
            val profile = DemoData.demoUserProfiles[role]
            assertNotNull("Demo profile for role $role should exist in DemoData", profile)
            assertEquals(role, profile?.role)
            assertEquals("dealership_demo_001", profile?.dealershipId)
            assertTrue(profile?.active == true)
        }

        assertEquals("demo_super_admin", DemoData.demoUserProfiles[UserRole.SUPER_ADMIN]?.uid)
        assertEquals("admin@automotive.com", DemoData.demoUserProfiles[UserRole.SUPER_ADMIN]?.email)

        assertEquals("demo_dealership_admin", DemoData.demoUserProfiles[UserRole.DEALERSHIP_ADMIN]?.uid)
        assertEquals("dealer.admin@automotive.com", DemoData.demoUserProfiles[UserRole.DEALERSHIP_ADMIN]?.email)

        assertEquals("demo_state_manager", DemoData.demoUserProfiles[UserRole.STATE_MANAGER]?.uid)
        assertEquals("state.manager@automotive.com", DemoData.demoUserProfiles[UserRole.STATE_MANAGER]?.email)
        assertEquals("TG", DemoData.demoUserProfiles[UserRole.STATE_MANAGER]?.stateCode)

        assertEquals("demo_store_manager", DemoData.demoUserProfiles[UserRole.STORE_MANAGER]?.uid)
        assertEquals("store.manager@automotive.com", DemoData.demoUserProfiles[UserRole.STORE_MANAGER]?.email)
        assertEquals("TG_Madhapur", DemoData.demoUserProfiles[UserRole.STORE_MANAGER]?.storeId)

        assertEquals("demo_sales_user", DemoData.demoUserProfiles[UserRole.SALES_USER]?.uid)
        assertEquals("sales@automotive.com", DemoData.demoUserProfiles[UserRole.SALES_USER]?.email)

        assertEquals("demo_finance_user", DemoData.demoUserProfiles[UserRole.FINANCE_USER]?.uid)
        assertEquals("finance@automotive.com", DemoData.demoUserProfiles[UserRole.FINANCE_USER]?.email)

        assertEquals("demo_customer_user", DemoData.demoUserProfiles[UserRole.CUSTOMER]?.uid)
        assertEquals("customer@automotive.com", DemoData.demoUserProfiles[UserRole.CUSTOMER]?.email)
    }

    @Test
    fun testDemoRoleSwitchingAcrossAll7Roles() {
        val rolesToTest = listOf(
            UserRole.SUPER_ADMIN,
            UserRole.DEALERSHIP_ADMIN,
            UserRole.STATE_MANAGER,
            UserRole.STORE_MANAGER,
            UserRole.SALES_USER,
            UserRole.FINANCE_USER,
            UserRole.CUSTOMER
        )

        rolesToTest.forEach { role ->
            var callbackRoute: String? = null
            authViewModel.switchDemoRole(role) { route ->
                callbackRoute = route
            }

            val currentUser = authRepository.currentUser.value
            assertNotNull("Current user should not be null after switching to $role", currentUser)
            assertEquals("User role should match requested role $role", role, currentUser?.role)

            // Check TenantContext updates
            assertEquals("TenantContext user should match current user", currentUser, TenantContext.currentUser.value)
            assertEquals("TenantContext role should match switched role", role, TenantContext.role.value)
            assertEquals("dealership_demo_001", TenantContext.dealershipId.value)

            val expectedStartRoute = authViewModel.getStartDestinationForRole(role)
            assertEquals("Callback start route should match expected destination for $role", expectedStartRoute, callbackRoute)
        }
    }

    @Test
    fun testProductionModeRejectionOfSwitchDemoRole() {
        // Disable demo mode
        authRepository.setDemoMode(false)

        val result = authRepository.switchDemoRole(UserRole.SUPER_ADMIN)
        assertTrue("switchDemoRole should fail when demo mode is disabled", result.isFailure)

        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)
        assertTrue("Exception should be SecurityException", exception is SecurityException)
        assertEquals("Role switching is disabled in production", exception?.message)
    }

    @Test
    fun testStartDestinationRouteCalculationPerRole() {
        assertEquals(NavRoutes.SUPER_ADMIN_DASHBOARD, authViewModel.getStartDestinationForRole(UserRole.SUPER_ADMIN))
        assertEquals(NavRoutes.DEALERSHIP_ADMIN_DASHBOARD, authViewModel.getStartDestinationForRole(UserRole.DEALERSHIP_ADMIN))
        assertEquals(NavRoutes.ADMIN_DASHBOARD, authViewModel.getStartDestinationForRole(UserRole.ADMIN))
        assertEquals(NavRoutes.STATE_MANAGER_DASHBOARD, authViewModel.getStartDestinationForRole(UserRole.STATE_MANAGER))
        assertEquals(NavRoutes.STORE_MANAGER_DASHBOARD, authViewModel.getStartDestinationForRole(UserRole.STORE_MANAGER))
        assertEquals(NavRoutes.SALES_DASHBOARD, authViewModel.getStartDestinationForRole(UserRole.SALES_USER))
        assertEquals(NavRoutes.FINANCE_DASHBOARD, authViewModel.getStartDestinationForRole(UserRole.FINANCE_USER))
        assertEquals(NavRoutes.CUSTOMER_PORTAL, authViewModel.getStartDestinationForRole(UserRole.CUSTOMER))
    }

    @Test
    fun testSessionAndTenantContextUpdatesUponRoleSwitch() {
        // Switch to Store Manager
        authViewModel.switchDemoRole(UserRole.STORE_MANAGER)
        val storeManager = authRepository.currentUser.value

        assertNotNull(storeManager)
        assertEquals("demo_store_manager", storeManager?.uid)
        assertEquals("TG_Madhapur", storeManager?.storeId)
        assertEquals("TG_Madhapur", TenantContext.activeStoreId.value)
        assertEquals(UserRole.STORE_MANAGER, TenantContext.role.value)

        // Switch to Customer
        authViewModel.switchDemoRole(UserRole.CUSTOMER)
        val customerUser = authRepository.currentUser.value

        assertNotNull(customerUser)
        assertEquals("demo_customer_user", customerUser?.uid)
        assertEquals("customer@automotive.com", customerUser?.email)
        assertEquals(UserRole.CUSTOMER, TenantContext.role.value)
    }
}
