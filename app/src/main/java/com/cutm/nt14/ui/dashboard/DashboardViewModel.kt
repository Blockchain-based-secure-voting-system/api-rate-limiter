package com.cutm.nt14.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cutm.nt14.data.local.SessionManager
import com.cutm.nt14.data.local.daos.AbuseEventDao
import com.cutm.nt14.data.local.daos.DDoSIncidentDao
import com.cutm.nt14.data.local.daos.EndpointDao
import com.cutm.nt14.data.local.daos.RateLimitDao
import com.cutm.nt14.data.local.daos.RequestLogDao
import com.cutm.nt14.data.local.entities.RateLimit
import com.cutm.nt14.data.local.entities.RequestLog
import com.cutm.nt14.data.remote.GatewayConnectionState
import com.cutm.nt14.data.remote.GatewayWebSocketClient
import com.cutm.nt14.data.remote.model.PolyLanceAttestation
import com.cutm.nt14.data.remote.model.PolyLanceEscrow
import com.cutm.nt14.data.remote.model.PolyLanceTalent
import com.cutm.nt14.domain.detector.OptimizationResult
import com.cutm.nt14.domain.detector.RateLimitOptimizer
import com.cutm.nt14.domain.model.UserRole
import com.cutm.nt14.security.SecurityIntegrityChecker
import com.cutm.nt14.security.SecurityIntegrityReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PolyLanceTab {
    ESCROWS, ATTESTATIONS, TALENTS
}

data class PolyLanceInspectorUiState(
    val selectedTab: PolyLanceTab = PolyLanceTab.ESCROWS,
    val isLoading: Boolean = false,
    val escrows: List<PolyLanceEscrow> = emptyList(),
    val attestations: List<PolyLanceAttestation> = emptyList(),
    val talents: List<PolyLanceTalent> = emptyList(),
    val lastStatusCode: Int? = null,
    val lastLatencyMs: Long? = null,
    val rateLimitRemaining: Int? = null,
    val rateLimitLimit: Int? = null,
    val rateLimitReset: Long? = null,
    val optimizationResult: OptimizationResult? = null,
    val isBursting: Boolean = false,
    val rawJson: String? = null,
    val showJsonModal: Boolean = false
)

data class DashboardUiState(
    val endpointCount: Int = 0,
    val totalRequests: Int = 0,
    val errorRate: Float = 0f,
    val activeIncidents: Int = 0,
    val isLoading: Boolean = true,
    val connectionState: GatewayConnectionState = GatewayConnectionState.DISCONNECTED,
    val connectedHost: String = "10.0.2.2:8000",
    val recentLogs: List<RequestLog> = emptyList(),
    val actionMessage: String? = null,
    val userEmail: String? = null,
    val userName: String? = null,
    val userRole: UserRole = UserRole.VIEWER,
    val securityReport: SecurityIntegrityReport? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val endpointDao: EndpointDao,
    private val logDao: RequestLogDao,
    private val incidentDao: DDoSIncidentDao,
    private val abuseDao: AbuseEventDao,
    private val rateLimitDao: RateLimitDao,
    private val sessionManager: SessionManager,
    private val authManager: com.cutm.nt14.data.remote.GoogleAuthManager,
    private val wsClient: GatewayWebSocketClient,
    private val securityChecker: SecurityIntegrityChecker
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    private val _polyLanceState = MutableStateFlow(PolyLanceInspectorUiState())
    val polyLanceState: StateFlow<PolyLanceInspectorUiState> = _polyLanceState.asStateFlow()

    val userEmail: StateFlow<String?> = sessionManager.userEmail.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    val userName: StateFlow<String?> = sessionManager.userName.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    val userPhotoUrl: StateFlow<String?> = sessionManager.userPhotoUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val userRole: StateFlow<UserRole> = sessionManager.userRole.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserRole.VIEWER
    )

    private val optimizer = RateLimitOptimizer()

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
        sessionManager.userRole,
        _actionMessage
    ) { (endpoints, logs, incidents), connState, host, role, msg ->
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
            actionMessage = msg,
            userRole = role,
            securityReport = securityChecker.checkIntegrity()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun rescanSecurityIntegrity() {
        val report = securityChecker.checkIntegrity()
        _actionMessage.value = if (report.isCompromised) {
            "Security Alert: Root, Proxy or Frida detected!"
        } else {
            "Zero-Trust Security Scan: Verified Clean"
        }
    }

    fun toggleRole() {
        viewModelScope.launch {
            val current = sessionManager.userRole.first()
            val next = if (current == UserRole.ADMIN) UserRole.VIEWER else UserRole.ADMIN
            val email = sessionManager.userEmail.first() ?: "user@google.com"
            val name = sessionManager.userName.first() ?: "Google User"
            sessionManager.saveSession(email = email, name = name, role = next)
            _actionMessage.value = "Switched to ${next.name} Mode"
        }
    }

    init {
        // Auto-fetch initial live escrows on launch
        fetchActivePolyLanceData()
    }

    fun reconnect() {
        wsClient.connect()
    }

    fun updateGatewayHost(newHost: String) {
        wsClient.reconnectWithHost(newHost)
    }

    fun selectPolyLanceTab(tab: PolyLanceTab) {
        _polyLanceState.update { it.copy(selectedTab = tab) }
        fetchActivePolyLanceData()
    }

    fun toggleJsonModal(show: Boolean) {
        _polyLanceState.update { it.copy(showJsonModal = show) }
    }

    fun fetchActivePolyLanceData() {
        viewModelScope.launch {
            _polyLanceState.update { it.copy(isLoading = true) }
            val tab = _polyLanceState.value.selectedTab

            when (tab) {
                PolyLanceTab.ESCROWS -> {
                    val resp = wsClient.fetchPolyLanceEscrows()
                    _polyLanceState.update {
                        it.copy(
                            isLoading = false,
                            escrows = resp.data ?: it.escrows,
                            lastStatusCode = resp.statusCode,
                            lastLatencyMs = resp.latencyMs,
                            rateLimitRemaining = resp.rateLimitRemaining,
                            rateLimitLimit = resp.rateLimitLimit,
                            rateLimitReset = resp.rateLimitReset,
                            rawJson = resp.rawJson
                        )
                    }
                    if (resp.errorMessage != null) {
                        _actionMessage.value = "PolyLance Escrows fetch: ${resp.errorMessage}"
                    } else {
                        _actionMessage.value = "Fetched ${resp.data?.size ?: 0} live escrows in ${resp.latencyMs}ms"
                    }
                }
                PolyLanceTab.ATTESTATIONS -> {
                    val resp = wsClient.fetchPolyLanceAttestations()
                    _polyLanceState.update {
                        it.copy(
                            isLoading = false,
                            attestations = resp.data ?: it.attestations,
                            lastStatusCode = resp.statusCode,
                            lastLatencyMs = resp.latencyMs,
                            rateLimitRemaining = resp.rateLimitRemaining,
                            rateLimitLimit = resp.rateLimitLimit,
                            rateLimitReset = resp.rateLimitReset,
                            rawJson = resp.rawJson
                        )
                    }
                    if (resp.errorMessage != null) {
                        _actionMessage.value = "PolyLance Attestations fetch: ${resp.errorMessage}"
                    } else {
                        _actionMessage.value = "Fetched ${resp.data?.size ?: 0} live skill attestations in ${resp.latencyMs}ms"
                    }
                }
                PolyLanceTab.TALENTS -> {
                    val resp = wsClient.fetchPolyLanceTalents()
                    _polyLanceState.update {
                        it.copy(
                            isLoading = false,
                            talents = resp.data ?: it.talents,
                            lastStatusCode = resp.statusCode,
                            lastLatencyMs = resp.latencyMs,
                            rateLimitRemaining = resp.rateLimitRemaining,
                            rateLimitLimit = resp.rateLimitLimit,
                            rateLimitReset = resp.rateLimitReset,
                            rawJson = resp.rawJson
                        )
                    }
                    if (resp.errorMessage != null) {
                        _actionMessage.value = "PolyLance Talents fetch: ${resp.errorMessage}"
                    } else {
                        _actionMessage.value = "Fetched ${resp.data?.size ?: 0} verified talents in ${resp.latencyMs}ms"
                    }
                }
            }
        }
    }

    fun createTestEscrow(amountPol: Double = 500.0) {
        viewModelScope.launch {
            _polyLanceState.update { it.copy(isLoading = true) }
            _actionMessage.value = "Deploying live test escrow for $amountPol POL..."
            val resp = wsClient.createPolyLanceEscrow(amountPol = amountPol)
            _polyLanceState.update {
                it.copy(
                    isLoading = false,
                    lastStatusCode = resp.statusCode,
                    lastLatencyMs = resp.latencyMs,
                    rateLimitRemaining = resp.rateLimitRemaining,
                    rateLimitLimit = resp.rateLimitLimit,
                    rateLimitReset = resp.rateLimitReset,
                    rawJson = resp.rawJson
                )
            }

            if (resp.data != null) {
                _polyLanceState.update { it.copy(escrows = listOf(resp.data) + it.escrows) }
                _actionMessage.value = "Escrow ${resp.data.escrowId} created live ($amountPol POL)!"
            } else {
                _actionMessage.value = "Escrow creation response: HTTP ${resp.statusCode}"
            }
        }
    }

    fun simulateAttackBurst() {
        val targetEndpoint = when (_polyLanceState.value.selectedTab) {
            PolyLanceTab.ESCROWS -> "/api/polylance/escrows"
            PolyLanceTab.ATTESTATIONS -> "/api/polylance/attestations"
            PolyLanceTab.TALENTS -> "/api/polylance/talents"
        }
        viewModelScope.launch {
            _polyLanceState.update { it.copy(isBursting = true) }
            _actionMessage.value = "Blasting 18 concurrent requests at $targetEndpoint..."
            wsClient.sendBurstSimulation(18, targetEndpoint)
            _polyLanceState.update { it.copy(isBursting = false) }
            _actionMessage.value = "Burst completed! Live metrics & 429 rate limit recorded."
            fetchActivePolyLanceData()
            runRateLimitOptimizerOnPolyLance()
        }
    }

    fun runRateLimitOptimizerOnPolyLance() {
        viewModelScope.launch {
            val targetEndpoint = when (_polyLanceState.value.selectedTab) {
                PolyLanceTab.ESCROWS -> "/api/polylance/escrows"
                PolyLanceTab.ATTESTATIONS -> "/api/polylance/attestations"
                PolyLanceTab.TALENTS -> "/api/polylance/talents"
            }
            val logs = logDao.getLogsByEndpoint(targetEndpoint).first()
            val rules = rateLimitDao.getAllRules().first()
            val currentRule = rules.find { it.endpointId == targetEndpoint }
            val optResult = optimizer.optimize(targetEndpoint, logs, currentRule)
            _polyLanceState.update { it.copy(optimizationResult = optResult) }
            _actionMessage.value = "Optimizer evaluated ${optResult.totalAnalyzed} logs on $targetEndpoint"
        }
    }

    fun applyOptimizedLimit() {
        viewModelScope.launch {
            val recRule = _polyLanceState.value.optimizationResult?.recommendedRule ?: return@launch
            rateLimitDao.insertRule(recRule)
            _actionMessage.value = "Applied optimized rate limit: ${recRule.limitPerMin} req/min (Burst: ${recRule.burstLimit})"
            _polyLanceState.update { it.copy(optimizationResult = null) }
        }
    }

    fun dismissOptimization() {
        _polyLanceState.update { it.copy(optimizationResult = null) }
    }

    fun fireTestRequest(endpoint: String = "/api/polylance/escrows") {
        viewModelScope.launch {
            _actionMessage.value = "Sending request to $endpoint..."
            val code = wsClient.sendTestRequest(endpoint)
            _actionMessage.value = if (code > 0) "Response: HTTP $code" else "Connection error"
            fetchActivePolyLanceData()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            logDao.clearAllLogs()
            abuseDao.clearAllEvents()
            incidentDao.clearAllIncidents()
            _polyLanceState.update { it.copy(optimizationResult = null) }
            _actionMessage.value = "All logs & incident metrics cleared!"
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun logout(activity: android.app.Activity? = null) {
        viewModelScope.launch {
            authManager.signOut(activity)
        }
    }
}
