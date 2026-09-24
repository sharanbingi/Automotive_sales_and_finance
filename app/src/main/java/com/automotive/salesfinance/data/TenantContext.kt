package com.automotive.salesfinance.data

import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TenantContext {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentDealership = MutableStateFlow<Dealership?>(null)
    val currentDealership: StateFlow<Dealership?> = _currentDealership.asStateFlow()

    private val _dealershipId = MutableStateFlow("")
    val dealershipId: StateFlow<String> = _dealershipId.asStateFlow()

    fun getDealershipId(): String = _dealershipId.value

    private val _role = MutableStateFlow<UserRole?>(UserRole.ADMIN)
    val role: StateFlow<UserRole?> = _role.asStateFlow()

    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus?>(SubscriptionStatus.ACTIVE)
    val subscriptionStatus: StateFlow<SubscriptionStatus?> = _subscriptionStatus.asStateFlow()

    private val _activeStoreId = MutableStateFlow("TG_Madhapur")
    val activeStoreId: StateFlow<String> = _activeStoreId.asStateFlow()

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
        if (user != null) {
            _dealershipId.value = user.dealershipId
            _role.value = user.role
            if (user.storeId.isNotBlank() && user.storeId != "ALL") {
                _activeStoreId.value = user.storeId
            }
        } else {
            clear()
        }
    }

    fun setDealership(dealership: Dealership?) {
        _currentDealership.value = dealership
        if (dealership != null) {
            _dealershipId.value = dealership.dealershipId
            _subscriptionStatus.value = dealership.subscriptionStatus
        }
    }

    fun setDealershipId(id: String) {
        if (id.isNotBlank()) {
            _dealershipId.value = id
        }
    }

    fun setActiveStoreId(storeId: String) {
        if (storeId.isNotBlank()) {
            _activeStoreId.value = storeId
        }
    }

    fun setSubscriptionStatus(status: SubscriptionStatus?) {
        _subscriptionStatus.value = status
    }

    fun clear() {
        _currentUser.value = null
        _currentDealership.value = null
        _dealershipId.value = ""
        _role.value = null
        _subscriptionStatus.value = null
        _activeStoreId.value = ""
    }

    fun reset() {
        _currentUser.value = null
        _currentDealership.value = null
        _dealershipId.value = ""
        _role.value = UserRole.ADMIN
        _subscriptionStatus.value = SubscriptionStatus.ACTIVE
        _activeStoreId.value = "TG_Madhapur"
    }
}
