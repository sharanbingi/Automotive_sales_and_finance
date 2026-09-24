package com.automotive.salesfinance.repository

import android.util.Log
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.Store
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun safeLogD(tag: String, msg: String) {
    try {
        Log.d(tag, msg)
    } catch (_: Throwable) {
        println("[$tag] $msg")
    }
}

class StoreRepository(private val authRepository: AuthRepository) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _storesFlow = MutableStateFlow<List<Store>>(
        if (authRepository.isDemoMode.value) DemoData.stores else emptyList()
    )
    val storesFlow: StateFlow<List<Store>> = _storesFlow.asStateFlow()

    fun clearInMemoryState() {
        _storesFlow.value = emptyList()
    }

    init {
        authRepository.registerClearListener {
            clearInMemoryState()
        }
        CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch {
            authRepository.isDemoMode.collect { isDemo ->
                if (!isDemo) {
                    _storesFlow.value = _storesFlow.value.filterNot { st ->
                        DemoData.stores.any { it.storeId == st.storeId }
                    }
                } else if (_storesFlow.value.isEmpty()) {
                    _storesFlow.value = DemoData.stores
                }
            }
        }
    }

    fun getStores(): List<Store> {
        return _storesFlow.value
    }

    fun getStoresForDealership(dealershipId: String): List<Store> {
        if (dealershipId.isBlank()) {
            return if (authRepository.isDemoMode.value) {
                _storesFlow.value.filter { it.dealershipId == DemoData.DEMO_DEALERSHIP_ID }
            } else {
                emptyList()
            }
        }
        return _storesFlow.value.filter { it.dealershipId == dealershipId }
    }

    fun getStoresByState(stateCode: String): List<Store> {
        if (stateCode == "ALL") return _storesFlow.value
        return _storesFlow.value.filter { it.stateCode == stateCode }
    }

    fun getStoreById(storeId: String): Store? {
        return _storesFlow.value.find { it.storeId == storeId }
    }

    suspend fun fetchStoresFromFirestore(dealershipId: String = ""): List<Store> {
        if (dealershipId.isBlank()) {
            return emptyList()
        }
        if (authRepository.isDemoMode.value) {
            return getStoresForDealership(dealershipId)
        }
        if (firestore == null) {
            _storesFlow.value = _storesFlow.value.filterNot { st -> DemoData.stores.any { it.storeId == st.storeId } }
            return getStoresForDealership(dealershipId)
        }
        return try {
            val fetched = if (dealershipId.isBlank()) {
                val snapshot = firestore?.collectionGroup("stores")?.limit(100)?.get()?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Store::class.java)
                    } catch (e: Exception) {
                        safeLogD("StoreRepository", "STORE DOC DESERIALIZATION FAILED | docId = ${doc.id} | exception = ${e.javaClass.simpleName} | message = ${e.message}")
                        null
                    }
                } ?: emptyList()
            } else {
                val snapshot = firestore?.collection("dealerships")
                    ?.document(dealershipId)
                    ?.collection("stores")
                    ?.limit(50)
                    ?.get()
                    ?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Store::class.java)
                    } catch (e: Exception) {
                        safeLogD("StoreRepository", "STORE DOC DESERIALIZATION FAILED | docId = ${doc.id} | exception = ${e.javaClass.simpleName} | message = ${e.message}")
                        null
                    }
                } ?: emptyList()
            }

            if (dealershipId.isBlank()) {
                _storesFlow.value = fetched
            } else {
                val current = _storesFlow.value.filterNot { it.dealershipId == dealershipId || DemoData.stores.any { demo -> demo.storeId == it.storeId } }.toMutableList()
                current.addAll(fetched)
                _storesFlow.value = current
            }
            fetched
        } catch (e: Exception) {
            safeLogD("StoreRepository", "STORE FETCH FAILED | exception = ${e.javaClass.simpleName} | code = ${(e as? FirebaseFirestoreException)?.code} | message = ${e.message}")
            val liveOnly = _storesFlow.value.filterNot { st -> DemoData.stores.any { it.storeId == st.storeId } }
            _storesFlow.value = liveOnly
            if (dealershipId.isBlank()) liveOnly else liveOnly.filter { it.dealershipId == dealershipId }
        }
    }

    suspend fun saveStore(store: Store): Result<Unit> {
        if (store.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }

        val effectiveDealershipId = store.dealershipId.ifBlank {
            TenantContext.getDealershipId().ifBlank { DemoData.DEMO_DEALERSHIP_ID }
        }

        val generatedStoreId = store.storeId.ifBlank {
            "${store.stateCode.trim().uppercase()}_${store.city.trim().replace("\\s+".toRegex(), "")}_${System.currentTimeMillis() % 1000}"
        }
        val preparedStore = store.copy(
            dealershipId = effectiveDealershipId,
            storeId = generatedStoreId
        )

        val current = _storesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.storeId == preparedStore.storeId }
        if (index >= 0) {
            current[index] = preparedStore
        } else {
            current.add(preparedStore)
        }
        _storesFlow.value = current

        if (authRepository.isDemoMode.value) {
            return Result.success(Unit)
        }

        if (firestore != null) {
            return try {
                firestore?.collection("dealerships")
                    ?.document(preparedStore.dealershipId)
                    ?.collection("stores")
                    ?.document(preparedStore.storeId)
                    ?.set(preparedStore)
                    ?.await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        return Result.success(Unit)
    }
}
