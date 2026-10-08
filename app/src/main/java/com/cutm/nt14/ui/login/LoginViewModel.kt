package com.cutm.nt14.ui.login

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.cutm.nt14.data.local.SessionManager
import com.cutm.nt14.data.remote.GoogleAuthManager
import com.cutm.nt14.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    // Live real-time Firebase Auth user stream
    val realtimeFirebaseUser: StateFlow<FirebaseUser?> = authManager.realtimeFirebaseUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun getGoogleSignInIntent(activity: Activity): Intent {
        return authManager.getGoogleSignInIntent(activity)
    }

    fun handleGoogleSignInResult(intent: Intent?) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val user = authManager.handleGoogleSignInResult(intent)
                _uiState.value = LoginUiState.Success(user.email, user.displayName)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Google Sign-In failed")
            }
        }
    }

    fun signInWithCredentialManager(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val user = authManager.signInWithCredentialManager(activity)
                _uiState.value = LoginUiState.Success(user.email, user.displayName)
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                _uiState.value = LoginUiState.Idle
            } catch (e: Exception) {
                // If CredentialManager fails, fallback to launching standard GoogleSignInIntent
                try {
                    val intent = authManager.getGoogleSignInIntent(activity)
                    activity.startActivity(intent)
                } catch (fallbackEx: Exception) {
                    _uiState.value = LoginUiState.Error(e.message ?: "Google authentication failed")
                }
            }
        }
    }

    fun onSignInCancelled() {
        _uiState.value = LoginUiState.Idle
    }

    fun signInWithGoogleEmail(email: String, displayName: String = "Google User") {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val user = authManager.signInWithGoogleEmail(email, displayName)
                _uiState.value = LoginUiState.Success(user.email, user.displayName)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Sign-in failed")
            }
        }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val guestEmail = "guest@cutm.nt14.com"
            val guestName = "Guest Viewer"
            val guestToken = com.cutm.nt14.util.JwtUtils.generateLocalClientSessionToken(guestEmail, guestName, UserRole.VIEWER)
            sessionManager.saveSession(guestEmail, guestName, UserRole.VIEWER, provider = "guest", jwtToken = guestToken)
            _uiState.value = LoginUiState.Success(guestEmail, guestName)
        }
    }

    fun signOut(activity: Activity?) {
        viewModelScope.launch {
            authManager.signOut(activity)
            _uiState.value = LoginUiState.Idle
        }
    }
}
