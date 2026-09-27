package com.cutm.nt14.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.local.SessionManager
import com.cutm.nt14.data.local.daos.AbuseEventDao
import com.cutm.nt14.data.local.daos.DDoSIncidentDao
import com.cutm.nt14.data.local.daos.EndpointDao
import com.cutm.nt14.data.local.daos.RequestLogDao
import com.cutm.nt14.data.local.entities.RequestLog
import com.cutm.nt14.data.remote.GatewayConnectionState
import com.cutm.nt14.data.remote.GatewayWebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val endpointCount: Int = 0,
    val totalRequests: Int = 0,
    val errorRate: Float = 0f,
    val activeIncidents: Int = 0,
    val isLoading: Boolean = true,
    val connectionState: GatewayConnectionState = GatewayConnectionState.DISCONNECTED,
    val connectedHost: String = "192.168.29.231:8000",
    val recentLogs: List<RequestLog> = emptyList(),
    val actionMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val endpointDao: EndpointDao,
    private val logDao: RequestLogDao,
    private val incidentDao: DDoSIncidentDao,
    private val abuseDao: AbuseEventDao,
    private val sessionManager: SessionManager,
    private val wsClient: GatewayWebSocketClient
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)

    private val dbDataFlow = combine(
        endpointDao.getAllEndpoints(),
        logDao.getAllLogs(),
        incidentDao.getAllIncidents()
    ) { endpoints, logs, incidents ->
        Triple(endpoints, logs, incidents)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        dbDataFlow,
        wsClient.connectionState,
        wsClient.connectedHost,
        _actionMessage
    ) { (endpoints, logs, incidents), connState, host, msg ->
        val totalReq = logs.size
        val errorCount = logs.count { it.statusCode >= 400 }
        val rate = if (totalReq > 0) errorCount.toFloat() / totalReq else 0f

        DashboardUiState(
            endpointCount = endpoints.size,
            totalRequests = totalReq,
            errorRate = rate,
            activeIncidents = incidents.count { it.status == "ACTIVE" },
            isLoading = false,
            connectionState = connState,
            connectedHost = host,
            recentLogs = logs.take(6),
            actionMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun reconnect() {
        wsClient.connect()
    }

    fun updateGatewayHost(newHost: String) {
        wsClient.reconnectWithHost(newHost)
    }

    fun fireTestRequest(endpoint: String = "/api/users") {
        viewModelScope.launch {
            _actionMessage.value = "Sending request to $endpoint..."
            val code = wsClient.sendTestRequest(endpoint)
            _actionMessage.value = if (code > 0) "Response: HTTP $code" else "Connection error"
        }
    }

    fun simulateAttackBurst() {
        viewModelScope.launch {
            _actionMessage.value = "Blasting 18 concurrent requests to test rate limit..."
            wsClient.sendBurstSimulation(18, "/api/users")
            _actionMessage.value = "Burst completed! Live metrics updated."
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            logDao.clearAllLogs()
            abuseDao.clearAllEvents()
            incidentDao.clearAllIncidents()
            _actionMessage.value = "All logs & incident metrics cleared!"
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
        }
    }
}
