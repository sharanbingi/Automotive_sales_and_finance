package com.automotive.salesfinance.model

enum class UserRole {
    SUPER_ADMIN,
    DEALERSHIP_ADMIN,
    ADMIN,
    STATE_MANAGER,
    STORE_MANAGER,
    SALES_USER,
    FINANCE_USER,
    CUSTOMER
}

data class User(
    val uid: String = "",
    val dealershipId: String = "dealership_demo_001",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.SALES_USER,
    val stateCode: String = "",
    val storeId: String = "",
    val active: Boolean = true
) {
    fun canAccessStore(targetStoreId: String): Boolean {
        if (role == UserRole.SUPER_ADMIN || role == UserRole.DEALERSHIP_ADMIN || role == UserRole.ADMIN) return true
        if (storeId == "ALL") return true
        return storeId == targetStoreId
    }

    fun canAccessState(targetStateCode: String): Boolean {
        if (role == UserRole.SUPER_ADMIN || role == UserRole.DEALERSHIP_ADMIN || role == UserRole.ADMIN) return true
        if (stateCode == "ALL") return true
        return stateCode == targetStateCode
    }

    fun canCollectCashPayment(): Boolean = role in listOf(
        UserRole.DEALERSHIP_ADMIN,
        UserRole.STORE_MANAGER,
        UserRole.FINANCE_USER
    )
}
