package com.cutm.nt14.ui.login

import android.app.Activity
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.local.SessionManager
import com.cutm.nt14.data.remote.FirebaseAuthManager
import com.cutm.nt14.data.remote.FirebaseAuthResult
import com.cutm.nt14.data.remote.GatewayAuthApi
import com.cutm.nt14.data.remote.GoogleAuthException
import com.cutm.nt14.data.remote.GoogleAuthManager
import com.cutm.nt14.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val firebaseAuthManager: FirebaseAuthManager,
    private val authApi: GatewayAuthApi,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    /**
     * Firebase Email/Password Sign In
     */
    fun signInWithFirebaseEmail(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            when (val result = firebaseAuthManager.signInWithEmail(email, pass)) {
                is FirebaseAuthResult.Success -> {
                    _uiState.value = LoginUiState.Success(result.email, result.name)
                }
                is FirebaseAuthResult.Error -> {
                    _uiState.value = LoginUiState.Error(result.message)
                }
            }
        }
    }

    /**
     * Firebase Email/Password Sign Up
     */
    fun signUpWithFirebaseEmail(email: String, pass: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            when (val result = firebaseAuthManager.signUpWithEmail(email, pass, displayName)) {
                is FirebaseAuthResult.Success -> {
                    _uiState.value = LoginUiState.Success(result.email, result.name)
                }
                is FirebaseAuthResult.Error -> {
                    _uiState.value = LoginUiState.Error(result.message)
                }
            }
        }
    }

    fun signInWithCredentialManager(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authManager.signIn(activity).fold(
                onSuccess = {
                    val email = sessionManager.userEmail.first() ?: ""
                    _uiState.value = LoginUiState.Success(email, email.substringBefore("@"))
                },
                onFailure = { e ->
                    if (e is GoogleAuthException.Cancelled || e is GetCredentialCancellationException) {
                        _uiState.value = LoginUiState.Idle
                    } else {
                        _uiState.value = LoginUiState.Error(e.message ?: "Google authentication failed")
                    }
                }
            )
        }
    }

    fun onSignInCancelled() {
        _uiState.value = LoginUiState.Idle
    }

    fun signInAsAdmin() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val adminEmail = "akpolylance@gmail.com"
            try {
                val response = authApi.loginAsDevAdmin()
                sessionManager.saveSession(
                    email = response.email,
                    name = "Admin",
                    role = UserRole.ADMIN,
                    provider = "direct",
                    jwtToken = response.token
                )
                _uiState.value = LoginUiState.Success(response.email, "Admin")
            } catch (e: Exception) {
                sessionManager.saveSession(
                    email = adminEmail,
                    name = "Admin",
                    role = UserRole.ADMIN,
                    provider = "direct",
                    jwtToken = "dev-admin-token"
                )
                _uiState.value = LoginUiState.Success(adminEmail, "Admin")
            }
        }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val guestEmail = "guest@cutm.nt14.com"
            try {
                val response = authApi.loginAsGuest()
                sessionManager.saveSession(
                    email = response.email,
                    name = "Guest Viewer",
                    role = UserRole.VIEWER,
                    provider = "guest",
                    jwtToken = response.token
                )
                _uiState.value = LoginUiState.Success(response.email, "Guest Viewer")
            } catch (e: Exception) {
                sessionManager.saveSession(
                    email = guestEmail,
                    name = "Guest Viewer",
                    role = UserRole.VIEWER,
                    provider = "guest",
                    jwtToken = "dev-guest-token"
                )
                _uiState.value = LoginUiState.Success(guestEmail, "Guest Viewer")
            }
        }
    }

    fun signOut(activity: Activity?) {
        viewModelScope.launch {
            firebaseAuthManager.signOut()
            authManager.signOut(activity)
            _uiState.value = LoginUiState.Idle
        }
    }
}
