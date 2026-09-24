package com.automotive.salesfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.FeatureAccessManager
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BillingCycle
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.Subscription
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.repository.AuditLogRepository
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.StoreRepository
import com.automotive.salesfinance.repository.SubscriptionRepository
import com.automotive.salesfinance.repository.UserRepository
import com.automotive.salesfinance.services.PaymentService
import com.automotive.salesfinance.services.RazorpayPaymentServiceImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DealershipFeatureLimits(
    val currentStores: Int = 0,
    val maxStores: Int = 1,
    val currentUsers: Int = 0,
    val maxUsers: Int = 3,
    val currentBikes: Int = 0,
    val maxBikes: Int = 100,
    val currentCustomers: Int = 0,
    val maxCustomers: Int = 500,
    val isSubscriptionActive: Boolean = true,
    val reportsEnabled: Boolean = true,
    val advancedReportsEnabled: Boolean = false,
    val financeEnabled: Boolean = true,
    val multiStoreEnabled: Boolean = false,
    val canAddStore: Boolean = true,
    val canAddUser: Boolean = true,
    val canAddBike: Boolean = true,
    val canAddCustomer: Boolean = true
)

class SubscriptionViewModel(
    private val authRepository: AuthRepository,
    private val dealershipRepository: DealershipRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val storeRepository: StoreRepository,
    private val userRepository: UserRepository,
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository,
    private val auditLogRepository: AuditLogRepository,
    private val paymentService: PaymentService = RazorpayPaymentServiceImpl()
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
    val isDemoMode: StateFlow<Boolean> = authRepository.isDemoMode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()
    val errorMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow(true)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    val plans: List<SubscriptionPlan> = SubscriptionPlan.ALL_PLANS

    private val _selectedBillingCycle = MutableStateFlow(BillingCycle.MONTHLY)
    val selectedBillingCycle: StateFlow<BillingCycle> = _selectedBillingCycle.asStateFlow()

    val currentDealership: StateFlow<Dealership?> = combine(
        authRepository.currentUser,
        dealershipRepository.dealershipsFlow
    ) { user, dealerships ->
        val dId = user?.dealershipId?.ifBlank { DemoData.DEMO_DEALERSHIP_ID } ?: DemoData.DEMO_DEALERSHIP_ID
        dealerships.find { it.dealershipId == dId } ?: dealerships.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentSubscription: StateFlow<Subscription?> = combine(
        currentDealership,
        subscriptionRepository.subscriptionsFlow
    ) { d, subs ->
        val dId = d?.dealershipId ?: DemoData.DEMO_DEALERSHIP_ID
        subs.find { it.dealershipId == dId && it.status == SubscriptionStatus.ACTIVE }
            ?: subs.find { it.dealershipId == dId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trialDaysRemaining: StateFlow<Int> = currentDealership.map { d ->
        if (d != null) dealershipRepository.getDaysRemainingInTrial(d) else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isTrialActive: StateFlow<Boolean> = currentDealership.map { d ->
        if (d != null) dealershipRepository.isTrialActive(d) else false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val featureLimits: StateFlow<DealershipFeatureLimits> = combine(
        currentDealership,
        storeRepository.storesFlow,
        userRepository.usersFlow,
        inventoryRepository.bikesFlow,
        customerRepository.customersFlow
    ) { flows ->
        val dealership = flows[0] as? Dealership
        @Suppress("UNCHECKED_CAST")
        val stores = flows[1] as List<Store>
        @Suppress("UNCHECKED_CAST")
        val users = flows[2] as List<User>
        @Suppress("UNCHECKED_CAST")
        val bikes = flows[3] as List<Bike>
        @Suppress("UNCHECKED_CAST")
        val customers = flows[4] as List<Customer>

        val dId = dealership?.dealershipId ?: DemoData.DEMO_DEALERSHIP_ID
        val plan = SubscriptionPlan.getPlanById(dealership?.subscriptionPlan ?: "STARTER")

        val dStoresCount = stores.count { it.dealershipId == dId }
        val dUsersCount = users.count { it.dealershipId == dId }
        val dBikesCount = bikes.count { it.dealershipId == dId }
        val dCustsCount = customers.count { it.dealershipId == dId }

        val active = FeatureAccessManager.isSubscriptionActive(dealership)

        DealershipFeatureLimits(
            currentStores = dStoresCount,
            maxStores = plan.maxStores,
            currentUsers = dUsersCount,
            maxUsers = plan.maxUsers,
            currentBikes = dBikesCount,
            maxBikes = plan.maxBikes,
            currentCustomers = dCustsCount,
            maxCustomers = plan.maxCustomers,
            isSubscriptionActive = active,
            reportsEnabled = plan.reportsEnabled,
            advancedReportsEnabled = plan.advancedReportsEnabled,
            financeEnabled = plan.financeEnabled,
            multiStoreEnabled = plan.multiStoreEnabled,
            canAddStore = active && FeatureAccessManager.canAddStore(dStoresCount, plan),
            canAddUser = active && FeatureAccessManager.canAddUser(dUsersCount, plan),
            canAddBike = active && FeatureAccessManager.canAddBike(dBikesCount, plan),
            canAddCustomer = active && FeatureAccessManager.canAddCustomer(dCustsCount, plan)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DealershipFeatureLimits())

    fun setBillingCycle(cycle: BillingCycle) {
        _selectedBillingCycle.value = cycle
    }

    fun calculatePrice(plan: SubscriptionPlan, cycle: BillingCycle = _selectedBillingCycle.value): Double {
        return if (cycle == BillingCycle.YEARLY) plan.yearlyPrice else plan.monthlyPrice
    }

    fun upgradePlan(
        planId: String,
        billingCycle: BillingCycle = _selectedBillingCycle.value,
        targetDealershipId: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val dId = targetDealershipId
                ?: currentDealership.value?.dealershipId
                ?: DemoData.DEMO_DEALERSHIP_ID

            val plan = SubscriptionPlan.getPlanById(planId)
            val price = calculatePrice(plan, billingCycle)

            val payRes = paymentService.processSaaSPlanSubscription(
                dealershipId = dId,
                planId = planId,
                billingCycle = billingCycle.name,
                amount = price
            )

            if (payRes.isFailure) {
                _isLoading.value = false
                _operationMessage.value = payRes.exceptionOrNull()?.message ?: "SaaS subscription payment failed"
                _isSuccess.value = false
                return@launch
            }

            val updateRes = dealershipRepository.updateSubscriptionPlan(dId, planId, SubscriptionStatus.ACTIVE)
            if (updateRes.isSuccess) {
                val subRes = subscriptionRepository.upgradePlan(dId, planId, billingCycle)
                _isLoading.value = false
                if (subRes.isSuccess) {
                    _operationMessage.value = "Plan upgraded to $planId via Razorpay successfully!"
                    _isSuccess.value = true
                    logAudit("UPGRADE_PLAN", "Subscription", subRes.getOrNull()?.subscriptionId ?: "", mapOf("planId" to planId, "amount" to price.toString()))
                    onSuccess()
                } else {
                    _operationMessage.value = subRes.exceptionOrNull()?.message ?: "Upgrade failed"
                    _isSuccess.value = false
                }
            } else {
                _isLoading.value = false
                _operationMessage.value = updateRes.exceptionOrNull()?.message ?: "Update plan failed"
                _isSuccess.value = false
            }
        }
    }

    fun renewSubscription(subscriptionId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val sub = subscriptionRepository.subscriptionsFlow.value.find { it.subscriptionId == subscriptionId }
            val dId = currentDealership.value?.dealershipId ?: DemoData.DEMO_DEALERSHIP_ID
            val planId = sub?.planId ?: "STARTER"
            val price = sub?.price ?: 2999.0

            val payRes = paymentService.processSaaSPlanSubscription(
                dealershipId = dId,
                planId = planId,
                billingCycle = sub?.billingCycle?.name ?: "MONTHLY",
                amount = price
            )

            if (payRes.isFailure) {
                _isLoading.value = false
                _operationMessage.value = payRes.exceptionOrNull()?.message ?: "Renewal payment failed"
                _isSuccess.value = false
                return@launch
            }

            val res = subscriptionRepository.renewSubscription(subscriptionId)
            _isLoading.value = false
            if (res.isSuccess) {
                dealershipRepository.updateSubscriptionPlan(dId, res.getOrNull()?.planId ?: "STARTER", SubscriptionStatus.ACTIVE)
                _operationMessage.value = "Subscription renewed successfully!"
                _isSuccess.value = true
                logAudit("RENEW_SUBSCRIPTION", "Subscription", subscriptionId, mapOf("amount" to price.toString()))
                onSuccess()
            } else {
                _operationMessage.value = res.exceptionOrNull()?.message ?: "Renewal failed"
                _isSuccess.value = false
            }
        }
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    private fun logAudit(action: String, entityType: String, entityId: String, metadata: Map<String, String>) {
        viewModelScope.launch {
            val uid = currentUser.value?.uid ?: "USER"
            val dId = currentDealership.value?.dealershipId ?: DemoData.DEMO_DEALERSHIP_ID
            auditLogRepository.logEvent(dId, uid, action, entityType, entityId, metadata)
        }
    }
}
