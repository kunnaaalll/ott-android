package com.ott.platform.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ott.platform.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.ott.platform.core.analytics.AnalyticsTracker
import com.ott.platform.core.crash.CrashReporter
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    _authState.value = AuthState.Authenticated(user.uid)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            analyticsTracker.logEvent("login_started")
            val result = authRepository.signInWithEmail(email, pass)
            if (result.isSuccess) {
                val user = result.getOrNull()
                user?.let {
                    userRepository.updateLastLogin(it.uid)
                    analyticsTracker.setUserId(it.uid)
                    crashReporter.setUserId(it.uid)
                }
                analyticsTracker.logEvent("login_success")
            } else {
                val exception = result.exceptionOrNull()
                exception?.let { crashReporter.logException(it) }
                analyticsTracker.logEvent("login_failed")
                _authState.value = AuthState.Error(mapAuthError(exception))
            }
        }
    }

    fun register(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            analyticsTracker.logEvent("registration_started")
            val result = authRepository.registerWithEmail(email, pass)
            if (result.isSuccess) {
                val user = result.getOrNull()
                user?.let {
                    val newUser = User(
                        id = it.uid,
                        email = it.email ?: email,
                        name = it.displayName ?: "",
                        photoUrl = it.photoUrl?.toString() ?: ""
                    )
                    userRepository.createOrUpdateUser(newUser)
                    analyticsTracker.setUserId(it.uid)
                    crashReporter.setUserId(it.uid)
                }
                analyticsTracker.logEvent("registration_success")
            } else {
                val exception = result.exceptionOrNull()
                exception?.let { crashReporter.logException(it) }
                analyticsTracker.logEvent("registration_failed")
                _authState.value = AuthState.Error(mapAuthError(exception))
            }
        }
    }
    
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            analyticsTracker.logEvent("google_login_started")
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                val user = result.getOrNull()
                user?.let {
                    val newUser = User(
                        id = it.uid,
                        email = it.email ?: "",
                        name = it.displayName ?: "",
                        photoUrl = it.photoUrl?.toString() ?: ""
                    )
                    userRepository.createOrUpdateUser(newUser)
                    userRepository.updateLastLogin(it.uid)
                    analyticsTracker.setUserId(it.uid)
                    crashReporter.setUserId(it.uid)
                }
                analyticsTracker.logEvent("google_login_success")
            } else {
                val exception = result.exceptionOrNull()
                exception?.let { crashReporter.logException(it) }
                analyticsTracker.logEvent("google_login_failed")
                _authState.value = AuthState.Error(mapAuthError(exception))
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            analyticsTracker.logEvent("logout")
        }
    }
    
    private fun mapAuthError(exception: Throwable?): String {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> "Email or password is incorrect."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            is FirebaseNetworkException -> "Check your internet connection and try again."
            is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
            else -> exception?.message ?: "Authentication failed"
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val uid: String) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
