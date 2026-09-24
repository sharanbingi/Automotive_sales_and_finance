package com.automotive.salesfinance.repository

import android.util.Log
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.User
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

open class UserRepository(private val authRepository: AuthRepository) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _usersFlow = MutableStateFlow<List<User>>(
        if (authRepository.isDemoMode.value) DemoData.users else emptyList()
    )
    val usersFlow: StateFlow<List<User>> = _usersFlow.asStateFlow()

    fun clearInMemoryState() {
        _usersFlow.value = emptyList()
    }

    init {
        authRepository.registerClearListener {
            clearInMemoryState()
        }
        CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch {
            authRepository.isDemoMode.collect { isDemo ->
                if (!isDemo) {
                    _usersFlow.value = _usersFlow.value.filterNot { u ->
                        DemoData.users.any { it.uid == u.uid }
                    }
                } else if (_usersFlow.value.isEmpty()) {
                    _usersFlow.value = DemoData.users
                }
            }
        }
    }

    open suspend fun getProductionUserProfile(uid: String): User? {
        if (uid.isBlank()) return null
        safeLogD("ASF_RUNTIME", "PRODUCTION PROFILE GET START")
        safeLogD("ASF_RUNTIME", "PROFILE FULL PATH = users/$uid")
        val fs = firestore
        if (fs == null) {
            safeLogD("ASF_RUNTIME", "PROFILE GET FAILED | firestore is null")
            return null
        }

        return try {
            val docRef = fs.collection("users").document(uid)
            val snapshot = docRef.get().await()
            safeLogD("ASF_RUNTIME", "PROFILE SNAPSHOT EXISTS = ${snapshot.exists()}")
            if (snapshot.exists()) {
                safeLogD("ASF_RUNTIME", "PROFILE DESERIALIZATION START")
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    safeLogD("ASF_RUNTIME", "PROFILE DESERIALIZATION SUCCESS | user = ${user.name}, role = ${user.role}")
                    val currentList = _usersFlow.value.toMutableList().filterNot { u -> DemoData.users.any { it.uid == u.uid } }.toMutableList()
                    val index = currentList.indexOfFirst { it.uid == user.uid }
                    if (index >= 0) currentList[index] = user else currentList.add(user)
                    _usersFlow.value = currentList
                }
                user
            } else {
                null
            }
        } catch (e: Exception) {
            safeLogD("ASF_RUNTIME", "PROFILE GET FAILED | exception = ${e.javaClass.simpleName} | code = ${(e as? FirebaseFirestoreException)?.code} | message = ${e.message}")
            throw e
        }
    }

    open suspend fun getUserProfile(uid: String): User? {
        if (uid.isBlank()) return null
        if (!authRepository.isDemoMode.value) {
            _usersFlow.value = _usersFlow.value.filterNot { u -> DemoData.users.any { it.uid == u.uid } }
        }
        if (authRepository.isDemoMode.value || firestore == null) {
            return _usersFlow.value.find { it.uid == uid }
        }
        return try {
            getProductionUserProfile(uid)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getUser(uid: String): User? {
        return getUserProfile(uid)
    }

    fun getUsersForDealership(dealershipId: String): List<User> {
        if (dealershipId.isBlank()) {
            return if (authRepository.isDemoMode.value) {
                _usersFlow.value.filter { it.dealershipId == DemoData.DEMO_DEALERSHIP_ID }
            } else {
                emptyList()
            }
        }
        return _usersFlow.value.filter { it.dealershipId == dealershipId }
    }

    fun getUsersForStore(dealershipId: String, storeId: String): List<User> {
        val users = getUsersForDealership(dealershipId)
        if (storeId == "ALL") return users
        return users.filter { it.storeId == storeId || it.storeId == "ALL" }
    }

    fun getUsersForStore(storeId: String): List<User> {
        return getUsersForStore(if (authRepository.isDemoMode.value) DemoData.DEMO_DEALERSHIP_ID else "", storeId)
    }

    suspend fun fetchUsersFromFirestore(dealershipId: String = ""): List<User> {
        if (dealershipId.isBlank()) {
            return emptyList()
        }
        if (authRepository.isDemoMode.value) {
            return getUsersForDealership(dealershipId)
        }
        if (firestore == null) {
            _usersFlow.value = _usersFlow.value.filterNot { u -> DemoData.users.any { it.uid == u.uid } }
            return getUsersForDealership(dealershipId)
        }
        return try {
            val fetched = if (dealershipId.isBlank()) {
                val snapshot = firestore?.collection("users")?.limit(100)?.get()?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(User::class.java)
                    } catch (e: Exception) {
                        safeLogD("UserRepository", "USER DOC DESERIALIZATION FAILED | docId = ${doc.id} | exception = ${e.javaClass.simpleName} | message = ${e.message}")
                        null
                    }
                } ?: emptyList()
            } else {
                val snapshot = firestore?.collection("users")
                    ?.whereEqualTo("dealershipId", dealershipId)
                    ?.limit(100)
                    ?.get()
                    ?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(User::class.java)
                    } catch (e: Exception) {
                        safeLogD("UserRepository", "USER DOC DESERIALIZATION FAILED | docId = ${doc.id} | exception = ${e.javaClass.simpleName} | message = ${e.message}")
                        null
                    }
                } ?: emptyList()
            }

            if (dealershipId.isBlank()) {
                _usersFlow.value = fetched
            } else {
                val current = _usersFlow.value.filterNot { it.dealershipId == dealershipId || DemoData.users.any { demo -> demo.uid == it.uid } }.toMutableList()
                current.addAll(fetched)
                _usersFlow.value = current
            }
            fetched
        } catch (e: Exception) {
            safeLogD("UserRepository", "USER FETCH FAILED | exception = ${e.javaClass.simpleName} | code = ${(e as? FirebaseFirestoreException)?.code} | message = ${e.message}")
            val liveOnly = _usersFlow.value.filterNot { u -> DemoData.users.any { it.uid == u.uid } }
            _usersFlow.value = liveOnly
            if (dealershipId.isBlank()) liveOnly else liveOnly.filter { it.dealershipId == dealershipId }
        }
    }

    suspend fun saveUser(user: User): Result<Unit> {
        if (user.dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }
        val currentList = _usersFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.uid == user.uid }
        if (index >= 0) {
            currentList[index] = user
        } else {
            currentList.add(user)
        }

        if (!authRepository.isDemoMode.value && firestore != null) {
            return try {
                firestore?.collection("users")?.document(user.uid)?.set(user)?.await()
                _usersFlow.value = currentList
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            _usersFlow.value = currentList
            return Result.success(Unit)
        }
    }
}
