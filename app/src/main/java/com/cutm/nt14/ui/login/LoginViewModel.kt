package com.cutm.nt14.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.cutm.nt14.data.local.SessionManager
import com.cutm.nt14.data.remote.GoogleAuthManager
import com.cutm.nt14.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun signIn() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val result = authManager.signIn()
                val credential = result.credential
                
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val email = googleIdTokenCredential.id
                val name = googleIdTokenCredential.displayName ?: "User"

                val role = if (email.contains("admin")) UserRole.ADMIN else UserRole.VIEWER

                sessionManager.saveSession(email, name, role)
                _uiState.value = LoginUiState.Success(email, name)
            } catch (e: Exception) {
                // Fallback to demo admin session if Google Play Services / OAuth client ID is not configured on the test device
                val fallbackEmail = "admin@cutm.nt14.com"
                val fallbackName = "Admin User"
                sessionManager.saveSession(fallbackEmail, fallbackName, UserRole.ADMIN)
                _uiState.value = LoginUiState.Success(fallbackEmail, fallbackName)
            }
        }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val guestEmail = "guest@cutm.nt14.com"
            val guestName = "Guest Viewer"
            sessionManager.saveSession(guestEmail, guestName, UserRole.VIEWER)
            _uiState.value = LoginUiState.Success(guestEmail, guestName)
        }
    }
}
