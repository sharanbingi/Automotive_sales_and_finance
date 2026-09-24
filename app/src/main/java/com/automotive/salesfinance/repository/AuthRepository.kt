package com.automotive.salesfinance.repository

import android.util.Log
import com.automotive.salesfinance.BuildConfig
import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.data.TenantContext
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Collections

private fun safeLogE(tag: String, msg: String, tr: Throwable? = null) {
    try {
        if (tr != null) {
            Log.e(tag, msg, tr)
        } else {
            Log.e(tag, msg)
        }
    } catch (_: Throwable) {
        System.err.println("[$tag] $msg")
        tr?.printStackTrace()
    }
}

private fun safeLogD(tag: String, msg: String) {
    try {
        Log.d(tag, msg)
    } catch (_: Throwable) {
        println("[$tag] $msg")
    }
}

open class AuthRepository(
    userRepositoryParam: UserRepository? = null
) {
    companion object {
        @Volatile
        private var instance: AuthRepository? = null

        fun getInstance(userRepositoryParam: UserRepository? = null): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository(userRepositoryParam).also { instance = it }
            }
        }

        fun resetInstance() {
            instance = null
        }
    }

    private val clearListeners = Collections.synchronizedSet(mutableSetOf<() -> Unit>())

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isDemoMode = MutableStateFlow(true)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    val userRepository: UserRepository by lazy {
        userRepositoryParam ?: UserRepository(this)
    }

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    init {
        safeLogD("ASF_AUTH_RUNTIME", "AUTH REPOSITORY CREATED")
        safeLogD("ASF_AUTH_RUNTIME", "CLEAR LISTENER REGISTRY INITIALIZED")
        safeLogD("ASF_RUNTIME", "AUTH CREATED | initial isDemoMode = ${_isDemoMode.value}")
        val startupUser = firebaseAuth?.currentUser
        safeLogD("ASF_RUNTIME", "FIREBASE CURRENT USER AT APP START = ${if (startupUser != null) "PRESENT (${startupUser.uid})" else "ABSENT"}")
        if (BuildConfig.DEBUG) {
            try {
                val app = FirebaseApp.getInstance()
                val projId = app.options.projectId
                val appId = BuildConfig.APPLICATION_ID
                safeLogD("AuthRepository", "FirebaseApp init check -> ProjectId: $projId, ApplicationId: $appId")
            } catch (e: Throwable) {
                safeLogE("AuthRepository", "FirebaseApp init check failed: ${e.javaClass.simpleName} - ${e.message}", e)
            }
        }
        if (startupUser != null) {
            safeLogD("ASF_AUTH_RUNTIME", "STARTUP SESSION RESTORE START")
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val userProfile = userRepository.getProductionUserProfile(startupUser.uid)
                    if (userProfile != null && userProfile.active) {
                        _currentUser.value = userProfile
                        TenantContext.setCurrentUser(userProfile)
                        _isDemoMode.value = false
                        safeLogD("ASF_AUTH_RUNTIME", "STARTUP SESSION RESTORE SUCCESS")
                    } else {
                        safeLogD("ASF_AUTH_RUNTIME", "STARTUP SESSION RESTORE SKIPPED")
                    }
                } catch (e: Exception) {
                    safeLogE("ASF_AUTH_RUNTIME", "STARTUP SESSION RESTORE FAILED", e)
                }
            }
        } else {
            safeLogD("ASF_AUTH_RUNTIME", "STARTUP SESSION RESTORE SKIPPED")
        }
        try {
            firebaseAuth?.addAuthStateListener { auth ->
                val firebaseUser = auth.currentUser
                if (!_isDemoMode.value) {
                    if (firebaseUser == null) {
                        _currentUser.value = null
                        TenantContext.clear()
                    }
                }
            }
        } catch (_: Exception) {
            // Firebase Auth not initialized or offline
        }
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        if (enabled && _currentUser.value == null) {
            val user = DemoData.users.firstOrNull { it.role == UserRole.ADMIN }
            _currentUser.value = user
            TenantContext.setCurrentUser(user)
        }
    }

    open suspend fun performFirebaseSignIn(email: String, pass: String): String? {
        val auth = firebaseAuth ?: return null
        val authResult = auth.signInWithEmailAndPassword(email, pass).await()
        return authResult.user?.uid
    }

    suspend fun login(email: String, pass: String): Result<User> {
        safeLogD("ASF_RUNTIME", "LOGIN START")
        val trimmedEmail = email.trim().lowercase()
        safeLogD("ASF_RUNTIME", "LOGIN EMAIL NORMALIZED = $trimmedEmail")
        if (email.isBlank()) {
            _currentUser.value = null
            TenantContext.clear()
            return Result.failure(Exception("Email address cannot be empty"))
        }

        if (BuildConfig.DEBUG) {
            try {
                val app = FirebaseApp.getInstance()
                val projId = app.options.projectId
                val appId = BuildConfig.APPLICATION_ID
                safeLogD("AuthRepository", "Firebase Login attempt -> ProjectId: $projId, ApplicationId: $appId, Email: $trimmedEmail")
            } catch (e: Throwable) {
                safeLogE("AuthRepository", "FirebaseApp instance check failed in login: ${e.javaClass.simpleName} - ${e.message}", e)
            }
        }

        safeLogD("ASF_RUNTIME", "BEFORE signInWithEmailAndPassword")
        return try {
            val uid = performFirebaseSignIn(trimmedEmail, pass)
            safeLogD("ASF_RUNTIME", "AFTER signInWithEmailAndPassword")
            safeLogD("ASF_RUNTIME", "FIREBASE LOGIN SUCCESS")

            if (uid == null) {
                _currentUser.value = null
                TenantContext.clear()
                val msg = if (firebaseAuth == null) "Firebase Auth unavailable" else "Authentication failed: invalid user credentials."
                return Result.failure(if (firebaseAuth == null) IllegalStateException(msg) else Exception(msg))
            }
            safeLogD("ASF_RUNTIME", "AUTH UID RECEIVED = $uid")

            safeLogD("ASF_RUNTIME", "BEFORE USER PROFILE FETCH")
            val userProfile = userRepository.getProductionUserProfile(uid)

            if (userProfile == null) {
                safeLogD("ASF_RUNTIME", "USER PROFILE FETCH RESULT | exists = false")
                _currentUser.value = null
                TenantContext.clear()
                return Result.failure(Exception("User profile not found in Firestore for UID $uid"))
            }
            safeLogD("ASF_RUNTIME", "USER PROFILE FETCH RESULT | exists = true")

            if (!userProfile.active) {
                try { firebaseAuth?.signOut() } catch (_: Exception) {}
                _currentUser.value = null
                TenantContext.clear()
                return Result.failure(Exception("User account is inactive"))
            }

            safeLogD("ASF_RUNTIME", "PROFILE ROLE = ${userProfile.role}")
            safeLogD("ASF_RUNTIME", "BEFORE SET CURRENT USER")
            _currentUser.value = userProfile
            TenantContext.setCurrentUser(userProfile)
            safeLogD("ASF_RUNTIME", "CURRENT USER SET")
            safeLogD("ASF_RUNTIME", "BEFORE SET DEMO MODE FALSE")
            _isDemoMode.value = false
            safeLogD("ASF_RUNTIME", "DEMO MODE AFTER REAL LOGIN = false")
            safeLogD("ASF_RUNTIME", "LOGIN RETURN SUCCESS")
            Result.success(userProfile)
        } catch (e: Exception) {
            safeLogD("ASF_RUNTIME", "LOGIN AUTH FAILURE | exception = ${e.javaClass.simpleName} | code = ${(e as? FirebaseAuthException)?.errorCode} | message = ${e.message}")
            _currentUser.value = null
            TenantContext.clear()
            Result.failure(e)
        }
    }

    suspend fun loginDemoRole(role: UserRole): Result<User> {
        _isDemoMode.value = true
        val demoUser = DemoData.getDemoProfileForRole(role)
        _currentUser.value = demoUser
        TenantContext.setCurrentUser(demoUser)
        return Result.success(demoUser)
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            return Result.failure(Exception("Email address cannot be empty"))
        }

        if (_isDemoMode.value || firebaseAuth == null) {
            return Result.success(Unit)
        }

        return try {
            firebaseAuth?.sendPasswordResetEmail(trimmedEmail)?.await()
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            safeLogE("AuthRepository", "FirebaseAuthException in password reset: class=${e.javaClass.simpleName}, errorCode=${e.errorCode}, message=${e.message}", e)
            val mappedMsg = mapAuthErrorCodeToMessage(e.errorCode)
            val detailedMessage = if (BuildConfig.DEBUG || !_isDemoMode.value) {
                "Password reset failed: [${e.javaClass.simpleName}] - ${e.message ?: mappedMsg} (ErrorCode: ${e.errorCode})"
            } else {
                mappedMsg
            }
            Result.failure(Exception(detailedMessage))
        } catch (e: Exception) {
            safeLogE("AuthRepository", "Exception in password reset: class=${e.javaClass.simpleName}, message=${e.message}", e)
            val detailedMessage = if (BuildConfig.DEBUG || !_isDemoMode.value) {
                "Password reset failed: [${e.javaClass.simpleName}] - ${e.message ?: "Failed to send password reset email"}"
            } else {
                e.localizedMessage ?: "Failed to send password reset email"
            }
            Result.failure(Exception(detailedMessage))
        }
    }

    fun registerClearListener(listener: () -> Unit) {
        clearListeners.add(listener)
    }

    fun unregisterClearListener(listener: () -> Unit) {
        clearListeners.remove(listener)
    }

    fun clearAllRepositoryState() {
        val listeners = synchronized(clearListeners) { clearListeners.toList() }
        listeners.forEach { it.invoke() }
    }

    fun logout() {
        if (!_isDemoMode.value) {
            try {
                firebaseAuth?.signOut()
            } catch (_: Exception) {}
        }
        _currentUser.value = null
        TenantContext.clear()
        clearAllRepositoryState()
    }

    fun switchDemoRole(role: UserRole): Result<User> {
        if (!BuildConfig.DEBUG || !_isDemoMode.value) {
            return Result.failure(SecurityException("Role switching is disabled in production"))
        }
        val demoUser = DemoData.demoUserProfiles[role]
            ?: DemoData.users.find { it.role == role }
            ?: DemoData.users.first()

        _currentUser.value = demoUser
        TenantContext.setCurrentUser(demoUser)
        return Result.success(demoUser)
    }

    fun setCurrentUser(user: User) {
        _currentUser.value = user
        TenantContext.setCurrentUser(user)
    }

    private fun mapAuthErrorCodeToMessage(errorCode: String): String {
        return when (errorCode.uppercase()) {
            "ERROR_INVALID_EMAIL", "INVALID_EMAIL", "INVALID-EMAIL" -> "Invalid email address format."
            "ERROR_WRONG_PASSWORD", "WRONG_PASSWORD", "WRONG-PASSWORD" -> "Invalid password provided."
            "ERROR_INVALID_CREDENTIAL", "INVALID_CREDENTIAL", "INVALID-CREDENTIAL" -> "Invalid credentials provided."
            "ERROR_USER_NOT_FOUND", "USER_NOT_FOUND", "USER-NOT-FOUND" -> "No account found with this email address."
            "ERROR_USER_DISABLED", "USER_DISABLED", "USER-DISABLED" -> "This user account has been disabled."
            "ERROR_TOO_MANY_REQUESTS", "TOO_MANY_REQUESTS", "TOO-MANY-REQUESTS" -> "Too many unsuccessful login attempts. Please try again later."
            "ERROR_NETWORK_REQUEST_FAILED", "NETWORK_REQUEST_FAILED", "NETWORK-NETWORK-FAILED" -> "Network error. Please check your internet connection."
            else -> "Authentication error: $errorCode"
        }
    }
}
