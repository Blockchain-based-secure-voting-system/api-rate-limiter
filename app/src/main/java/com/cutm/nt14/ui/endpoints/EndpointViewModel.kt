package com.cutm.nt14.ui.endpoints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.local.entities.Endpoint
import com.cutm.nt14.domain.repository.EndpointRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

@HiltViewModel
class EndpointViewModel @Inject constructor(
    private val repository: com.cutm.nt14.domain.repository.EndpointRepository,
    private val sessionManager: com.cutm.nt14.data.local.SessionManager
) : ViewModel() {

    val userRole = sessionManager.userRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.cutm.nt14.domain.model.UserRole.VIEWER)

    val endpoints = repository.getAllEndpoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEndpoint(name: String, url: String, method: String) {
        viewModelScope.launch {
            val endpoint = Endpoint(
                endpointId = UUID.randomUUID().toString(),
                name = name,
                baseUrl = url,
                method = method,
                status = "ACTIVE",
                ownerEmail = "admin@example.com" // Placeholder, in real app get from session
            )
            repository.addEndpoint(endpoint)
        }
    }

    fun deleteEndpoint(endpoint: Endpoint) {
        viewModelScope.launch {
            repository.deleteEndpoint(endpoint)
        }
    }
}
