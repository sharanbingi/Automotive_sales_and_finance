package com.automotive.salesfinance.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automotive.salesfinance.BuildConfig
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.repository.AuthRepository
import com.automotive.salesfinance.ui.navigation.NavRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

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

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
    val isDemoMode: StateFlow<Boolean> = authRepository.isDemoMode

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    val isLoading: StateFlow<Boolean> = _loginState.map { it is LoginState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val errorMessage: StateFlow<String?> = _loginState.map { (it as? LoginState.Error)?.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isSuccess: StateFlow<Boolean> = _loginState.map { it is LoginState.Success }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun login(email: String, pass: String) {
        if (email.isBlank()) {
            _loginState.value = LoginState.Error("Email address cannot be empty")
            return
        }
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = authRepository.login(email, pass)
            safeLogD("ASF_RUNTIME", "AUTH VIEWMODEL LOGIN RESULT = success:${result.isSuccess}")
            result.fold(
                onSuccess = { user ->
                    _loginState.value = LoginState.Success(user)
                },
                onFailure = { error ->
                    if (BuildConfig.DEBUG) {
                        safeLogE("AuthViewModel", "Login failure: class=${error.javaClass.simpleName}, message=${error.message}", error)
                    }
                    _loginState.value = LoginState.Error(error.message ?: "Login failed")
                }
            )
        }
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (email.isBlank()) {
            _loginState.value = LoginState.Error("Please enter email address for password reset")
            onError("Please enter email address for password reset")
            return
        }
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = authRepository.sendPasswordResetEmail(email)
            result.fold(
                onSuccess = {
                    _loginState.value = LoginState.Idle
                    onSuccess()
                },
                onFailure = { err ->
                    val msg = err.message ?: "Failed to send reset email"
                    _loginState.value = LoginState.Error(msg)
                    onError(msg)
                }
            )
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        authRepository.logout()
        _loginState.value = LoginState.Idle
        onComplete()
    }

    fun toggleDemoMode(enabled: Boolean) {
        authRepository.setDemoMode(enabled)
    }

    fun loginDemoRole(role: UserRole, onRoleSwitched: (startRoute: String) -> Unit = {}) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = authRepository.loginDemoRole(role)
            result.fold(
                onSuccess = { user ->
                    _loginState.value = LoginState.Success(user)
                    val startRoute = getStartDestinationForRole(user.role)
                    onRoleSwitched(startRoute)
                },
                onFailure = { error ->
                    _loginState.value = LoginState.Error(error.message ?: "Demo login failed")
                }
            )
        }
    }

    fun switchDemoRole(role: UserRole, onRoleSwitched: (startRoute: String) -> Unit = {}) {
        val result = authRepository.switchDemoRole(role)
        result.fold(
            onSuccess = { user ->
                _loginState.value = LoginState.Success(user)
                val startRoute = getStartDestinationForRole(user.role)
                onRoleSwitched(startRoute)
            },
            onFailure = { error ->
                _loginState.value = LoginState.Error(error.message ?: "Role switching failed")
            }
        )
    }

    fun getStartDestinationForRole(role: UserRole): String {
        return when (role) {
            UserRole.SUPER_ADMIN -> NavRoutes.SUPER_ADMIN_DASHBOARD
            UserRole.DEALERSHIP_ADMIN -> NavRoutes.DEALERSHIP_ADMIN_DASHBOARD
            UserRole.ADMIN -> NavRoutes.ADMIN_DASHBOARD
            UserRole.STATE_MANAGER -> NavRoutes.STATE_MANAGER_DASHBOARD
            UserRole.STORE_MANAGER -> NavRoutes.STORE_MANAGER_DASHBOARD
            UserRole.SALES_USER -> NavRoutes.SALES_DASHBOARD
            UserRole.FINANCE_USER -> NavRoutes.FINANCE_DASHBOARD
            UserRole.CUSTOMER -> NavRoutes.CUSTOMER_PORTAL
        }
    }
}
