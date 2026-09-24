package com.automotive.salesfinance.data

import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SessionManager(
    private val authRepository: AuthRepository,
    coroutineScope: CoroutineScope? = null
) {

    private val scope = coroutineScope ?: CoroutineScope(Dispatchers.Default)

    val currentUser: StateFlow<User?> = TenantContext.currentUser
    val currentDealership: StateFlow<Dealership?> = TenantContext.currentDealership
    val dealershipId: StateFlow<String> = TenantContext.dealershipId
    val role: StateFlow<UserRole?> = TenantContext.role
    val subscriptionStatus: StateFlow<SubscriptionStatus?> = TenantContext.subscriptionStatus
    val activeStoreId: StateFlow<String> = TenantContext.activeStoreId

    init {
        authRepository.currentUser.onEach { user ->
            TenantContext.setCurrentUser(user)
        }.launchIn(scope)
    }

    fun updateActiveStore(storeId: String) {
        TenantContext.setActiveStoreId(storeId)
    }

    fun updateDealershipContext(dealership: Dealership) {
        TenantContext.setDealership(dealership)
    }

    fun setTenantDealershipId(id: String) {
        TenantContext.setDealershipId(id)
    }

    fun clearSession() {
        authRepository.logout()
    }
}
