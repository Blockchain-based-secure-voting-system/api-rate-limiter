package com.cutm.nt14.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.local.daos.EndpointDao
import com.cutm.nt14.data.local.daos.RequestLogDao
import com.cutm.nt14.data.local.daos.DDoSIncidentDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val endpointCount: Int = 0,
    val totalRequests: Int = 0,
    val errorRate: Float = 0f,
    val activeIncidents: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val endpointDao: EndpointDao,
    private val logDao: RequestLogDao,
    private val incidentDao: DDoSIncidentDao,
    private val sessionManager: com.cutm.nt14.data.local.SessionManager
) : ViewModel() {

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        endpointDao.getAllEndpoints(),
        logDao.getAllLogs(),
        incidentDao.getAllIncidents()
    ) { endpoints, logs, incidents ->
        val totalReq = logs.size
        val errorCount = logs.count { it.statusCode >= 400 }
        val rate = if (totalReq > 0) errorCount.toFloat() / totalReq else 0f
        
        DashboardUiState(
            endpointCount = endpoints.size,
            totalRequests = totalReq,
            errorRate = rate,
            activeIncidents = incidents.count { it.status == "ACTIVE" },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
