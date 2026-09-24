package com.automotive.salesfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.FeatureAccessManager
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReportsUiState(
    val isReportsEnabled: Boolean = true,
    val isAdvancedReportsEnabled: Boolean = false,
    val isMultiStoreEnabled: Boolean = false,
    val featureUnavailableMessage: String? = null
)

class ReportsViewModel(
    private val authRepository: AuthRepository,
    private val dealershipRepository: DealershipRepository,
    private val storeRepository: StoreRepository
) : ViewModel() {

    val currentPlan: StateFlow<SubscriptionPlan> = combine(
        authRepository.currentUser,
        dealershipRepository.dealershipsFlow
    ) { user, dealerships ->
        val dId = user?.dealershipId.orEmpty().ifBlank {
            if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
        }
        val d = dealerships.find { it.dealershipId == dId }
            ?: if (authRepository.isDemoMode.value) DemoData.dealerships.firstOrNull() else null
        SubscriptionPlan.getPlanById(d?.subscriptionPlan ?: "STARTER")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionPlan.STARTER)

    val uiState: StateFlow<ReportsUiState> = currentPlan.map { plan ->
        val reports = FeatureAccessManager.hasFeature("reports", plan)
        val advReports = FeatureAccessManager.hasAdvancedReports(plan)
        val multiStore = FeatureAccessManager.hasMultiStore(plan)

        val msg = if (!reports) {
            "Feature unavailable: Reports module is disabled on your current plan (${plan.name})"
        } else if (!advReports) {
            "Notice: Advanced Analytics requires Professional plan or higher"
        } else null

        ReportsUiState(
            isReportsEnabled = reports,
            isAdvancedReportsEnabled = advReports,
            isMultiStoreEnabled = multiStore,
            featureUnavailableMessage = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())
}
