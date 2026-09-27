package com.cutm.nt14.ui.login

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val email: String, val name: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
