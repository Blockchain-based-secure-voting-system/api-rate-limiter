package com.cutm.nt14.ui.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.remote.model.IncidentDto
import com.cutm.nt14.data.remote.model.TargetRequestDto
import com.cutm.nt14.data.remote.model.TargetStatusDto
import com.cutm.nt14.data.repository.GatewayRepository
import com.cutm.nt14.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonitoringViewModel @Inject constructor(
    private val repository: GatewayRepository
) : ViewModel() {

    val targets: StateFlow<List<TargetStatusDto>> = repository.targets
    val incidents: StateFlow<List<IncidentDto>> = repository.incidents
    val userRole: StateFlow<UserRole> = repository.userRole
    val lastDataSyncMs: StateFlow<Long> = repository.lastDataSyncMs
    val persistenceMode: StateFlow<String> = repository.persistenceMode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchTargetsStatus()
            _isLoading.value = false
        }
    }

    fun probeNow(id: Long) {
        viewModelScope.launch {
            repository.probeTargetNow(id)
        }
    }

    fun addTarget(name: String, url: String, method: String, expectedStatus: Int, intervalSec: Int) {
        viewModelScope.launch {
            repository.createTarget(
                TargetRequestDto(
                    name = name,
                    url = url,
                    method = method,
                    expectedStatus = expectedStatus,
                    intervalSec = intervalSec
                )
            )
        }
    }

    fun deleteTarget(id: Long) {
        viewModelScope.launch {
            repository.deleteTarget(id)
        }
    }
}
