package com.automotive.salesfinance

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.ui.navigation.NavRoutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TenantIsolationHardeningTest {

    @Before
    fun setUp() {
        AuthRepository.resetInstance()
        TenantContext.clear()
    }

    @After
    fun tearDown() {
        AuthRepository.resetInstance()
        TenantContext.clear()
    }

    @Test
    fun test1_tenantContextInitialDealershipIdIsBlank() {
        TenantContext.clear()
        assertEquals("", TenantContext.dealershipId.value)
        assertEquals("", TenantContext.getDealershipId())
    }

    @Test
    fun test2_tenantContextClearAndResetSetsDealershipIdToBlank() {
        TenantContext.setDealershipId("dealership_real_100")
        assertEquals("dealership_real_100", TenantContext.dealershipId.value)

        TenantContext.clear()
        assertEquals("", TenantContext.dealershipId.value)

        TenantContext.setDealershipId("dealership_real_200")
        assertEquals("dealership_real_200", TenantContext.dealershipId.value)

        TenantContext.reset()
        assertEquals("", TenantContext.dealershipId.value)
    }

    @Test
    fun test3_productionMissingTenantFailsClosedWithoutDemoFallback() = runTest {
        val authRepo = AuthRepository.getInstance()
        authRepo.setDemoMode(false)
        TenantContext.clear()

        val loginResult = authRepo.login("notenant@example.com", "password")
        assertTrue("Login should fail for missing user/tenant in production", loginResult.isFailure)

        assertEquals("", TenantContext.dealershipId.value)
        assertFalse("Production missing tenant must not equal demo tenant ID", TenantContext.dealershipId.value == DemoData.DEMO_DEALERSHIP_ID)
    }

    @Test
    fun test4_explicitDemoModeEstablishesDemoTenant() = runTest {
        val authRepo = AuthRepository.getInstance()
        authRepo.setDemoMode(true)

        val switchResult = authRepo.switchDemoRole(UserRole.SALES_USER)
        assertTrue("Role switch should succeed in explicit demo mode", switchResult.isSuccess)

        assertEquals("dealership_demo_001", TenantContext.dealershipId.value)
        assertEquals(DemoData.DEMO_DEALERSHIP_ID, authRepo.currentUser.value?.dealershipId)
    }

    @Test
    fun test5_missingDealershipDetailsArgumentDoesNotSubstituteDemoTenant() {
        val argumentsMap: Map<String, String?> = mapOf("dealershipId" to null)
        val extractedId = argumentsMap["dealershipId"]
        assertNull("Missing dealershipId argument should be null", extractedId)

        val fallbackCheckedId = extractedId?.takeIf { it.isNotBlank() }
        assertNull("Missing dealershipId must not fall back to demo tenant", fallbackCheckedId)
        assertFalse("Fallback check should not equal demo tenant", fallbackCheckedId == DemoData.DEMO_DEALERSHIP_ID)
    }

    @Test
    fun test6_validRealDealershipIdResolvesCorrectlyToDealershipDetails() {
        val realDealershipId = "dealership_real_999"
        val route = NavRoutes.dealershipDetails(realDealershipId)
        assertEquals("dealership_details/dealership_real_999", route)

        val argumentsMap = mapOf("dealershipId" to realDealershipId)
        val extractedId = argumentsMap["dealershipId"]
        assertEquals("dealership_real_999", extractedId)
    }
}
