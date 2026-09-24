package com.automotive.salesfinance.repository

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.Customer
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CustomerRepository(private val authRepository: AuthRepository) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _customersFlow = MutableStateFlow<List<Customer>>(
        if (authRepository.isDemoMode.value) DemoData.customers else emptyList()
    )
    val customersFlow: StateFlow<List<Customer>> = _customersFlow.asStateFlow()

    fun clearInMemoryState() {
        _customersFlow.value = emptyList()
    }

    init {
        authRepository.registerClearListener {
            clearInMemoryState()
        }
        CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch {
            authRepository.isDemoMode.collect { isDemo ->
                if (!isDemo) {
                    _customersFlow.value = _customersFlow.value.filterNot { c ->
                        DemoData.customers.any { it.customerId == c.customerId }
                    }
                } else if (_customersFlow.value.isEmpty()) {
                    _customersFlow.value = DemoData.customers
                }
            }
        }
    }

    fun getCustomers(): List<Customer> = _customersFlow.value

    fun getCustomersForDealership(dealershipId: String): List<Customer> {
        if (dealershipId.isBlank()) {
            return if (authRepository.isDemoMode.value) {
                _customersFlow.value.filter { it.dealershipId == DemoData.DEMO_DEALERSHIP_ID }
            } else {
                emptyList()
            }
        }
        return _customersFlow.value.filter { it.dealershipId == dealershipId }
    }

    fun getCustomerById(customerId: String): Customer? {
        return _customersFlow.value.find { it.customerId == customerId }
    }

    fun getCustomerByPhone(phone: String): Customer? {
        return _customersFlow.value.find { it.phone == phone.trim() }
    }

    suspend fun fetchCustomersFromFirestore(dealershipId: String = ""): List<Customer> {
        if (authRepository.isDemoMode.value) {
            return if (dealershipId.isBlank()) _customersFlow.value else getCustomersForDealership(dealershipId)
        }
        if (firestore == null) {
            _customersFlow.value = _customersFlow.value.filterNot { c -> DemoData.customers.any { it.customerId == c.customerId } }
            return if (dealershipId.isBlank()) _customersFlow.value else getCustomersForDealership(dealershipId)
        }
        return try {
            val fetched = if (dealershipId.isBlank()) {
                val snapshot = firestore?.collectionGroup("customers")?.limit(200)?.get()?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(Customer::class.java) } catch (_: Exception) { null }
                } ?: emptyList()
            } else {
                val snapshot = firestore?.collection("dealerships")
                    ?.document(dealershipId)
                    ?.collection("customers")
                    ?.limit(100)
                    ?.get()
                    ?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(Customer::class.java) } catch (_: Exception) { null }
                } ?: emptyList()
            }

            if (dealershipId.isBlank()) {
                _customersFlow.value = fetched
            } else {
                val current = _customersFlow.value.filterNot { it.dealershipId == dealershipId || DemoData.customers.any { demo -> demo.customerId == it.customerId } }.toMutableList()
                current.addAll(fetched)
                _customersFlow.value = current
            }
            fetched
        } catch (e: Exception) {
            val liveOnly = _customersFlow.value.filterNot { c -> DemoData.customers.any { it.customerId == c.customerId } }
            _customersFlow.value = liveOnly
            if (dealershipId.isBlank()) liveOnly else liveOnly.filter { it.dealershipId == dealershipId }
        }
    }

    suspend fun saveCustomer(customer: Customer): Result<Unit> {
        if (customer.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }
        val preparedCustomer = if (customer.dealershipId.isBlank()) customer.copy(dealershipId = DemoData.DEMO_DEALERSHIP_ID) else customer
        val current = _customersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.customerId == preparedCustomer.customerId }
        if (index >= 0) {
            current[index] = preparedCustomer
        } else {
            current.add(preparedCustomer)
        }
        _customersFlow.value = current

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                firestore?.collection("dealerships")
                    ?.document(preparedCustomer.dealershipId)
                    ?.collection("customers")
                    ?.document(preparedCustomer.customerId)
                    ?.set(preparedCustomer)
                    ?.await()
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }
}
