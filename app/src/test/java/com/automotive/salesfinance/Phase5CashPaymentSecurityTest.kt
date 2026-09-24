package com.automotive.salesfinance

import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5CashPaymentSecurityTest {

    @Test
    fun testSuperAdminCannotCollectCash() {
        val superAdmin = User(uid = "super_1", role = UserRole.SUPER_ADMIN)
        assertFalse("SUPER_ADMIN should NOT be allowed to collect cash physically.", superAdmin.canCollectCashPayment())
    }

    @Test
    fun testDealershipAdminCanCollectCash() {
        val dealershipAdmin = User(uid = "dealer_admin_1", role = UserRole.DEALERSHIP_ADMIN)
        assertTrue("DEALERSHIP_ADMIN should be allowed to collect cash.", dealershipAdmin.canCollectCashPayment())
    }

    @Test
    fun testStoreManagerCanCollectCash() {
        val storeManager = User(uid = "manager_1", role = UserRole.STORE_MANAGER)
        assertTrue("STORE_MANAGER should be allowed to collect cash.", storeManager.canCollectCashPayment())
    }

    @Test
    fun testFinanceUserCanCollectCash() {
        val financeUser = User(uid = "finance_1", role = UserRole.FINANCE_USER)
        assertTrue("FINANCE_USER should be allowed to collect cash.", financeUser.canCollectCashPayment())
    }

    @Test
    fun testSalesUserCannotCollectCash() {
        val salesUser = User(uid = "sales_1", role = UserRole.SALES_USER)
        assertFalse("SALES_USER should NOT be allowed to collect cash.", salesUser.canCollectCashPayment())
    }
}
