package com.cutm.nt14.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.local.BiometricHelper
import com.cutm.nt14.data.local.SessionManager
import com.cutm.nt14.data.remote.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    object Login : SplashDestination()
    object BiometricLock : SplashDestination()
    object Dashboard : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination

    private val _isOnline = MutableStateFlow<Boolean?>(null)
    val isOnline: StateFlow<Boolean?> = _isOnline

    init {
        checkStatus()
    }

    private fun checkStatus() {
        viewModelScope.launch {
            // Keep network monitor updated for diagnostics
            launch {
                networkMonitor.isOnline.collect { online ->
                    _isOnline.value = online
                }
            }

            // Check persistent session in DataStore
            val email = sessionManager.userEmail.firstOrNull()

            if (email.isNullOrBlank()) {
                _destination.value = SplashDestination.Login
            } else {
                // User has an active Google/Guest session
                if (biometricHelper.canAuthenticate()) {
                    // Device supports fingerprint or PIN -> require Biometric Lock
                    _destination.value = SplashDestination.BiometricLock
                } else {
                    // Device lacks biometric/PIN hardware -> skip gate and proceed to Dashboard
                    _destination.value = SplashDestination.Dashboard
                }
            }
        }
    }
}
