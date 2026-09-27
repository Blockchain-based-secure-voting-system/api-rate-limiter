package com.cutm.nt14.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val networkMonitor: com.cutm.nt14.data.remote.NetworkMonitor
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn

    private val _isOnline = MutableStateFlow<Boolean?>(null)
    val isOnline: StateFlow<Boolean?> = _isOnline

    init {
        checkStatus()
    }

    private fun checkStatus() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
                if (online) {
                    val email = sessionManager.userEmail.firstOrNull()
                    _isLoggedIn.value = !email.isNullOrEmpty()
                }
            }
        }
    }
}
