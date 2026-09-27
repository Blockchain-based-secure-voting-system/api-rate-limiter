package com.cutm.nt14.ui.biometric

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.local.BiometricAuthResult
import com.cutm.nt14.data.local.BiometricHelper
import com.cutm.nt14.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BiometricLockUiState {
    object Idle : BiometricLockUiState()
    object Prompting : BiometricLockUiState()
    object Success : BiometricLockUiState()
    data class Error(val message: String) : BiometricLockUiState()
}

@HiltViewModel
class BiometricLockViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    val userEmail: StateFlow<String?> = sessionManager.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow<BiometricLockUiState>(BiometricLockUiState.Idle)
    val uiState: StateFlow<BiometricLockUiState> = _uiState

    fun authenticate(activity: FragmentActivity) {
        _uiState.value = BiometricLockUiState.Prompting
        biometricHelper.showPrompt(activity) { result ->
            when (result) {
                is BiometricAuthResult.Success -> {
                    _uiState.value = BiometricLockUiState.Success
                }
                is BiometricAuthResult.Error -> {
                    _uiState.value = BiometricLockUiState.Error(result.errString)
                }
                is BiometricAuthResult.Failed -> {
                    _uiState.value = BiometricLockUiState.Error("Biometric not recognized. Please try again.")
                }
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            sessionManager.clearSession()
            onSignedOut()
        }
    }
}
