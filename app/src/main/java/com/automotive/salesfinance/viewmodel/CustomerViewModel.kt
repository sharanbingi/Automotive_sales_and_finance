package com.automotive.salesfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.FeatureAccessManager
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.repository.CustomerRepository
import com.automotive.salesfinance.repository.DealershipRepository
import com.automotive.salesfinance.repository.InventoryRepository
import com.automotive.salesfinance.repository.LoanRepository
import com.automotive.salesfinance.repository.TransactionRepository
import com.automotive.salesfinance.utils.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val loanRepository: LoanRepository,
    private val inventoryRepository: InventoryRepository,
    val authRepository: AuthRepository,
    private val dealershipRepository: DealershipRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val isDemoMode: StateFlow<Boolean> = authRepository.isDemoMode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedState = MutableStateFlow("ALL")
    val selectedState: StateFlow<String> = _selectedState.asStateFlow()

    private val _selectedCity = MutableStateFlow("ALL")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()
    val errorMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow(true)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    val allCustomers: StateFlow<List<Customer>> = customerRepository.customersFlow

    val filteredCustomers: StateFlow<List<Customer>> = combine(
        customerRepository.customersFlow,
        authRepository.currentUser,
        _searchQuery,
        _selectedState,
        _selectedCity
    ) { customers, user, query, stateFilter, cityFilter ->
        val dId = user?.dealershipId.orEmpty()
        val q = query.trim().lowercase()
        customers.filter { cust ->
            // Filter hierarchy: dealershipId -> state -> city -> query
            val matchesDealership = dId.isBlank() || cust.dealershipId == dId
            val matchesQuery = q.isEmpty() ||
                cust.fullName.lowercase().contains(q) ||
                cust.phone.contains(q) ||
                cust.email.lowercase().contains(q) ||
                cust.customerId.lowercase().contains(q)

            val matchesState = stateFilter == "ALL" || cust.state.equals(stateFilter, ignoreCase = true)
            val matchesCity = cityFilter == "ALL" || cust.city.equals(cityFilter, ignoreCase = true)

            matchesDealership && matchesQuery && matchesState && matchesCity
        }.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedState(state: String) {
        _selectedState.value = state
        _selectedCity.value = "ALL"
    }

    fun setSelectedCity(city: String) {
        _selectedCity.value = city
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    fun getCustomerById(customerId: String): Customer? {
        return customerRepository.getCustomerById(customerId)
    }

    fun getLoansForCustomer(customerId: String): List<Loan> {
        return loanRepository.getLoansByCustomer(customerId)
    }

    fun getBikesForCustomer(customerId: String): List<Bike> {
        val loans = getLoansForCustomer(customerId)
        return loans.mapNotNull { inventoryRepository.getBikeById(it.bikeId) }
    }

    fun getTransactionsForCustomer(customerId: String): List<Transaction> {
        return transactionRepository.getTransactionsByCustomer(customerId)
    }

    fun validateCustomer(
        fullName: String,
        phone: String,
        email: String,
        address: String,
        city: String,
        state: String
    ): String? {
        if (fullName.isBlank()) {
            return "Full Name is required"
        }
        if (!ValidationUtils.isValidPhoneNumber(phone)) {
            return "Invalid Phone Number (Must be a valid 10-digit Indian mobile number)"
        }
        if (!ValidationUtils.isValidEmail(email)) {
            return "Invalid Email Address format"
        }
        if (address.isBlank()) {
            return "Street Address is required"
        }
        if (city.isBlank()) {
            return "City is required"
        }
        if (state.isBlank()) {
            return "State is required"
        }
        return null
    }

    fun addCustomer(
        fullName: String,
        phone: String,
        email: String,
        address: String,
        city: String,
        state: String,
        onSuccess: (Customer) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = authRepository.currentUser.value
        val dId = user?.dealershipId.orEmpty().ifBlank {
            if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else ""
        }
        val dealership = dealershipRepository.dealershipsFlow.value.find { it.dealershipId == dId }
            ?: if (dId == DemoData.DEMO_DEALERSHIP_ID && authRepository.isDemoMode.value) DemoData.dealerships.firstOrNull() else null
        val plan = SubscriptionPlan.getPlanById(dealership?.subscriptionPlan ?: "STARTER")

        if (!FeatureAccessManager.isSubscriptionActive(dealership)) {
            val err = "Subscription inactive: account is in read-only mode"
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        val currentCustomerCount = customerRepository.customersFlow.value.count {
            dId.isBlank() || it.dealershipId == dId
        }
        if (!FeatureAccessManager.canAddCustomer(currentCustomerCount, plan)) {
            val err = "Plan limit reached: your current plan allows up to ${plan.maxCustomers} customers"
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        val validationErr = validateCustomer(fullName, phone, email, address, city, state)
        if (validationErr != null) {
            _operationMessage.value = validationErr
            _isSuccess.value = false
            onError(validationErr)
            return
        }

        val existing = customerRepository.getCustomerByPhone(phone)
        if (existing != null) {
            val err = "Customer with phone number $phone already exists (${existing.fullName})"
            _operationMessage.value = err
            _isSuccess.value = false
            onError(err)
            return
        }

        val newCust = Customer(
            customerId = "CUST_${System.currentTimeMillis()}",
            dealershipId = dId,
            fullName = fullName.trim(),
            phone = phone.trim(),
            email = email.trim(),
            address = address.trim(),
            city = city.trim(),
            state = state.trim(),
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _isLoading.value = true
            val result = customerRepository.saveCustomer(newCust)
            _isLoading.value = false

            result.fold(
                onSuccess = {
                    _operationMessage.value = "Customer ${newCust.fullName} created successfully"
                    _isSuccess.value = true
                    onSuccess(newCust)
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to save customer"
                    _operationMessage.value = msg
                    _isSuccess.value = false
                    onError(msg)
                }
            )
        }
    }
}
