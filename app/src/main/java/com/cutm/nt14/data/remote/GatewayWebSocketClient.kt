package com.cutm.nt14.data.remote

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cutm.nt14.BuildConfig
import com.cutm.nt14.MainActivity
import com.cutm.nt14.data.local.SessionManager
import com.cutm.nt14.data.local.SyncStatus
import com.cutm.nt14.data.local.daos.AbuseEventDao
import com.cutm.nt14.data.local.daos.DDoSIncidentDao
import com.cutm.nt14.data.local.daos.RequestLogDao
import com.cutm.nt14.data.local.entities.AbuseEvent
import com.cutm.nt14.data.local.entities.DDoSIncident
import com.cutm.nt14.data.local.entities.RequestLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import com.cutm.nt14.data.remote.model.GatewayResponse
import com.cutm.nt14.data.remote.model.PolyLanceAttestation
import com.cutm.nt14.data.remote.model.PolyLanceEscrow
import com.cutm.nt14.data.remote.model.PolyLanceTalent
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class GatewayConnectionState {
    object Idle : GatewayConnectionState()
    object WakingServer : GatewayConnectionState()
    object Connecting : GatewayConnectionState()
    object Connected : GatewayConnectionState()
    data class Failed(val code: Int? = null, val message: String = "Connection failed") : GatewayConnectionState()
}

@Singleton
class GatewayWebSocketClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logDao: RequestLogDao,
    private val abuseDao: AbuseEventDao,
    private val ddosDao: DDoSIncidentDao,
    private val sessionManager: SessionManager
) {
    private val tag = "GatewayWS"

    // OkHttpClient with 60s timeout for Render cold start and 20s ping interval for proxy keep-alive
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null
    private var reconnectDelayMs = 1000L

    private val _connectionState = MutableStateFlow<GatewayConnectionState>(GatewayConnectionState.Idle)
    val connectionState: StateFlow<GatewayConnectionState> = _connectionState.asStateFlow()

    private val _connectedHost = MutableStateFlow("polylance-fv-1-45wy.onrender.com")
    val connectedHost: StateFlow<String> = _connectedHost.asStateFlow()

    fun buildHttpUrl(host: String, path: String): String {
        val cleanHost = host.removePrefix("http://").removePrefix("https://").removePrefix("ws://").removePrefix("wss://").trim().trimEnd('/')
        val isSecure = cleanHost.contains("onrender.com") || cleanHost.contains("cloud") || host.startsWith("https://") || host.startsWith("wss://")
        val scheme = if (isSecure) "https" else "http"
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$scheme://$cleanHost$cleanPath"
    }

    fun buildWsUrl(host: String, path: String): String {
        val cleanHost = host.removePrefix("ws://").removePrefix("wss://").removePrefix("http://").removePrefix("https://").trim().trimEnd('/')
        val isSecure = cleanHost.contains("onrender.com") || cleanHost.contains("cloud") || host.startsWith("wss://") || host.startsWith("https://")
        val scheme = if (isSecure) "wss" else "ws"
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$scheme://$cleanHost$cleanPath?api_key=dev-local-key"
    }

    private suspend fun attachAuthHeaders(builder: Request.Builder): Request.Builder {
        val jwt = sessionManager.userJwtToken.first()
        if (!jwt.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $jwt")
        }
        return builder
    }

    fun connect() {
        if (_connectionState.value is GatewayConnectionState.Connected ||
            _connectionState.value is GatewayConnectionState.Connecting ||
            _connectionState.value is GatewayConnectionState.WakingServer) {
            return
        }

        scope.launch {
            val configuredHost = sessionManager.gatewayHost.first()
            attemptConnect(configuredHost)
        }
    }

    fun reconnect() {
        scope.launch {
            reconnectJob?.cancel()
            reconnectDelayMs = 1000L
            disconnect()
            val configuredHost = sessionManager.gatewayHost.first()
            attemptConnect(configuredHost)
        }
    }

    fun reconnectWithHost(newHost: String) {
        scope.launch {
            sessionManager.saveGatewayHost(newHost)
            reconnect()
        }
    }

    private suspend fun wakeServer(host: String): Boolean {
        _connectionState.value = GatewayConnectionState.WakingServer
        val healthUrl = buildHttpUrl(host, "/health")
        Log.i(tag, "Waking gateway server via $healthUrl...")
        return try {
            val req = Request.Builder().url(healthUrl).get().build()
            client.newCall(req).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(tag, "Wake server probe completed with note: ${e.message}")
            false
        }
    }

    private suspend fun attemptConnect(host: String) {
        // Step 1: Wake sleeping server instance (e.g. Render free tier cold start)
        wakeServer(host)

        // Step 2: Establish WebSocket stream
        _connectionState.value = GatewayConnectionState.Connecting
        _connectedHost.value = host

        val wsUrl = buildWsUrl(host, "/ws/events")
        Log.i(tag, "Opening WebSocket event stream at $wsUrl")

        val reqBuilder = Request.Builder().url(wsUrl)
        attachAuthHeaders(reqBuilder)
        val request = reqBuilder.build()

        webSocket?.cancel()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(tag, "Successfully connected to live gateway at $host")
                _connectionState.value = GatewayConnectionState.Connected
                reconnectDelayMs = 1000L
                reconnectJob?.cancel()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleLiveEvent(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val code = response?.code
                val userMsg = when {
                    code == 404 -> "Endpoint not found"
                    code == 401 || code == 403 -> "Session expired or unauthorized"
                    code != null && code >= 500 -> "Server error, retrying"
                    t is SocketTimeoutException -> "Connection timed out"
                    else -> "Connection failed: ${t.message ?: "Server unreachable"}"
                }
                Log.w(tag, "WebSocket failure on $host [HTTP ${code ?: 0}]: ${t.message}")
                this@GatewayWebSocketClient.webSocket = null
                _connectionState.value = GatewayConnectionState.Failed(code, userMsg)
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(tag, "WebSocket closed ($code): $reason")
                this@GatewayWebSocketClient.webSocket = null
                _connectionState.value = GatewayConnectionState.Idle
            }
        })
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectDelayMs)
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30000L) // Exponential backoff: 1s, 2s, 4s... max 30s
            val current = _connectedHost.value
            attemptConnect(current)
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.cancel()
        webSocket = null
        _connectionState.value = GatewayConnectionState.Idle
    }

    private fun mapHttpError(code: Int): String {
        return when (code) {
            404 -> "Endpoint not found"
            401 -> "Session expired"
            403 -> "Access unauthorized"
            429 -> "Rate limit reached, throttling"
            in 500..599 -> "Server error, retrying"
            else -> "Gateway error (HTTP $code)"
        }
    }

    suspend fun sendTestRequest(endpoint: String = "/api/polylance/escrows"): Int = withContext(Dispatchers.IO) {
        val host = _connectedHost.value
        val url = buildHttpUrl(host, endpoint)
        try {
            val req = attachAuthHeaders(Request.Builder().url(url).get()).build()
            client.newCall(req).execute().use { resp ->
                resp.code
            }
        } catch (e: Exception) {
            Log.e(tag, "Test request failed: ${e.message}")
            -1
        }
    }

    suspend fun sendBurstSimulation(count: Int = 18, endpoint: String = "/api/polylance/escrows") = withContext(Dispatchers.IO) {
        val host = _connectedHost.value
        val url = buildHttpUrl(host, endpoint)
        val jobs = (1..count).map {
            async {
                try {
                    val req = attachAuthHeaders(Request.Builder().url(url).get()).build()
                    client.newCall(req).execute().use { it.code }
                } catch (e: Exception) {
                    -1
                }
            }
        }
        jobs.awaitAll()
    }

    suspend fun fetchPolyLanceEscrows(): GatewayResponse<List<PolyLanceEscrow>> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val host = _connectedHost.value
        val url = buildHttpUrl(host, "/api/polylance/escrows")
        try {
            val req = attachAuthHeaders(Request.Builder().url(url).get()).build()
            client.newCall(req).execute().use { resp ->
                val latency = System.currentTimeMillis() - start
                val limit = resp.header("X-RateLimit-Limit")?.toIntOrNull()
                val remaining = resp.header("X-RateLimit-Remaining")?.toIntOrNull()
                val reset = resp.header("X-RateLimit-Reset")?.toLongOrNull()
                val bodyStr = resp.body?.string().orEmpty()

                if (resp.isSuccessful) {
                    val list = mutableListOf<PolyLanceEscrow>()
                    val arr = JSONArray(bodyStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            PolyLanceEscrow(
                                escrowId = obj.optString("escrowId"),
                                client = obj.optString("client"),
                                freelancer = obj.optString("freelancer"),
                                amountPol = obj.optDouble("amountPol", 0.0),
                                status = obj.optString("status"),
                                title = obj.optString("title"),
                                token = obj.optString("token", "POL"),
                                contractAddress = obj.optString("contractAddress")
                            )
                        )
                    }
                    GatewayResponse(
                        statusCode = resp.code,
                        data = list,
                        rawJson = bodyStr,
                        rateLimitRemaining = remaining,
                        rateLimitLimit = limit,
                        rateLimitReset = reset,
                        latencyMs = latency
                    )
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(tag, "fetchPolyLanceEscrows HTTP ${resp.code}: $bodyStr")
                    }
                    GatewayResponse(
                        statusCode = resp.code,
                        data = null,
                        rawJson = bodyStr,
                        rateLimitRemaining = remaining,
                        rateLimitLimit = limit,
                        rateLimitReset = reset,
                        latencyMs = latency,
                        errorMessage = mapHttpError(resp.code)
                    )
                }
            }
        } catch (e: Exception) {
            val userMsg = if (e is SocketTimeoutException) "Connection timed out" else "Network unreachable"
            if (BuildConfig.DEBUG) {
                Log.d(tag, "fetchPolyLanceEscrows exception: ${e.message}")
            }
            GatewayResponse(
                statusCode = -1,
                data = null,
                rawJson = null,
                rateLimitRemaining = null,
                rateLimitLimit = null,
                rateLimitReset = null,
                latencyMs = System.currentTimeMillis() - start,
                errorMessage = userMsg
            )
        }
    }

    suspend fun fetchPolyLanceAttestations(): GatewayResponse<List<PolyLanceAttestation>> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val host = _connectedHost.value
        val url = buildHttpUrl(host, "/api/polylance/attestations")
        try {
            val req = attachAuthHeaders(Request.Builder().url(url).get()).build()
            client.newCall(req).execute().use { resp ->
                val latency = System.currentTimeMillis() - start
                val limit = resp.header("X-RateLimit-Limit")?.toIntOrNull()
                val remaining = resp.header("X-RateLimit-Remaining")?.toIntOrNull()
                val reset = resp.header("X-RateLimit-Reset")?.toLongOrNull()
                val bodyStr = resp.body?.string().orEmpty()

                if (resp.isSuccessful) {
                    val list = mutableListOf<PolyLanceAttestation>()
                    val arr = JSONArray(bodyStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            PolyLanceAttestation(
                                attestationId = obj.optString("attestationId"),
                                developerGithub = obj.optString("developerGithub"),
                                skillAttestation = obj.optString("skillAttestation"),
                                soulboundTokenId = obj.optString("soulboundTokenId")
                            )
                        )
                    }
                    GatewayResponse(
                        statusCode = resp.code,
                        data = list,
                        rawJson = bodyStr,
                        rateLimitRemaining = remaining,
                        rateLimitLimit = limit,
                        rateLimitReset = reset,
                        latencyMs = latency
                    )
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(tag, "fetchPolyLanceAttestations HTTP ${resp.code}: $bodyStr")
                    }
                    GatewayResponse(
                        statusCode = resp.code,
                        data = null,
                        rawJson = bodyStr,
                        rateLimitRemaining = remaining,
                        rateLimitLimit = limit,
                        rateLimitReset = reset,
                        latencyMs = latency,
                        errorMessage = mapHttpError(resp.code)
                    )
                }
            }
        } catch (e: Exception) {
            val userMsg = if (e is SocketTimeoutException) "Connection timed out" else "Network unreachable"
            if (BuildConfig.DEBUG) {
                Log.d(tag, "fetchPolyLanceAttestations exception: ${e.message}")
            }
            GatewayResponse(
                statusCode = -1,
                data = null,
                rawJson = null,
                rateLimitRemaining = null,
                rateLimitLimit = null,
                rateLimitReset = null,
                latencyMs = System.currentTimeMillis() - start,
                errorMessage = userMsg
            )
        }
    }

    suspend fun fetchPolyLanceTalents(): GatewayResponse<List<PolyLanceTalent>> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val host = _connectedHost.value
        val url = buildHttpUrl(host, "/api/polylance/talents")
        try {
            val req = attachAuthHeaders(Request.Builder().url(url).get()).build()
            client.newCall(req).execute().use { resp ->
                val latency = System.currentTimeMillis() - start
                val limit = resp.header("X-RateLimit-Limit")?.toIntOrNull()
                val remaining = resp.header("X-RateLimit-Remaining")?.toIntOrNull()
                val reset = resp.header("X-RateLimit-Reset")?.toLongOrNull()
                val bodyStr = resp.body?.string().orEmpty()

                if (resp.isSuccessful) {
                    val list = mutableListOf<PolyLanceTalent>()
                    val arr = JSONArray(bodyStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            PolyLanceTalent(
                                talentId = obj.optString("talentId"),
                                name = obj.optString("name"),
                                specialization = obj.optString("specialization"),
                                rating = obj.optDouble("rating", 5.0)
                            )
                        )
                    }
                    GatewayResponse(
                        statusCode = resp.code,
                        data = list,
                        rawJson = bodyStr,
                        rateLimitRemaining = remaining,
                        rateLimitLimit = limit,
                        rateLimitReset = reset,
                        latencyMs = latency
                    )
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(tag, "fetchPolyLanceTalents HTTP ${resp.code}: $bodyStr")
                    }
                    GatewayResponse(
                        statusCode = resp.code,
                        data = null,
                        rawJson = bodyStr,
                        rateLimitRemaining = remaining,
                        rateLimitLimit = limit,
                        rateLimitReset = reset,
                        latencyMs = latency,
                        errorMessage = mapHttpError(resp.code)
                    )
                }
            }
        } catch (e: Exception) {
            val userMsg = if (e is SocketTimeoutException) "Connection timed out" else "Network unreachable"
            if (BuildConfig.DEBUG) {
                Log.d(tag, "fetchPolyLanceTalents exception: ${e.message}")
            }
            GatewayResponse(
                statusCode = -1,
                data = null,
                rawJson = null,
                rateLimitRemaining = null,
                rateLimitLimit = null,
                rateLimitReset = null,
                latencyMs = System.currentTimeMillis() - start,
                errorMessage = userMsg
            )
        }
    }

    suspend fun createPolyLanceEscrow(
        clientAddr: String = "0x75972bcc03026544287eb7418bd8ae53583c23ce",
        freelancerAddr: String = "0x5bab2a6561cb2dedfc95fae5cfd0779b5ab782a6",
        amountPol: Double = 500.0
    ): GatewayResponse<PolyLanceEscrow> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val host = _connectedHost.value
        val url = buildHttpUrl(host, "/api/polylance/escrows")
        try {
            val jsonBody = JSONObject().apply {
                put("client", clientAddr)
                put("freelancer", freelancerAddr)
                put("amountPol", amountPol)
            }.toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toRequestBody(mediaType)
            val req = attachAuthHeaders(Request.Builder().url(url).post(body)).build()
            client.newCall(req).execute().use { resp ->
                val latency = System.currentTimeMillis() - start
                val limit = resp.header("X-RateLimit-Limit")?.toIntOrNull()
                val remaining = resp.header("X-RateLimit-Remaining")?.toIntOrNull()
                val reset = resp.header("X-RateLimit-Reset")?.toLongOrNull()
                val bodyStr = resp.body?.string().orEmpty()

                if (resp.isSuccessful) {
                    val obj = JSONObject(bodyStr)
                    val escrow = PolyLanceEscrow(
                        escrowId = obj.optString("escrowId"),
                        client = obj.optString("client"),
                        freelancer = obj.optString("freelancer"),
                        amountPol = obj.optDouble("amountPol", amountPol),
                        status = obj.optString("status"),
                        title = obj.optString("title", "Test Escrow"),
                        token = obj.optString("token", "POL"),
                        contractAddress = obj.optString("contractAddress")
                    )
                    GatewayResponse(
                        statusCode = resp.code,
                        data = escrow,
                        rawJson = bodyStr,
                        rateLimitRemaining = remaining,
                        rateLimitLimit = limit,
                        rateLimitReset = reset,
                        latencyMs = latency
                    )
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(tag, "createPolyLanceEscrow HTTP ${resp.code}: $bodyStr")
                    }
                    GatewayResponse(
                        statusCode = resp.code,
                        data = null,
                        rawJson = bodyStr,
                        rateLimitRemaining = remaining,
                        rateLimitLimit = limit,
                        rateLimitReset = reset,
                        latencyMs = latency,
                        errorMessage = mapHttpError(resp.code)
                    )
                }
            }
        } catch (e: Exception) {
            val userMsg = if (e is SocketTimeoutException) "Connection timed out" else "Network unreachable"
            if (BuildConfig.DEBUG) {
                Log.d(tag, "createPolyLanceEscrow exception: ${e.message}")
            }
            GatewayResponse(
                statusCode = -1,
                data = null,
                rawJson = null,
                rateLimitRemaining = null,
                rateLimitLimit = null,
                rateLimitReset = null,
                latencyMs = System.currentTimeMillis() - start,
                errorMessage = userMsg
            )
        }
    }

    private fun handleLiveEvent(jsonStr: String) {
        scope.launch {
            try {
                val json = JSONObject(jsonStr)
                val type = json.optString("type", "request")
                val ip = json.optString("ip", "unknown")
                val endpoint = json.optString("endpoint", "/api/unknown")
                val status = json.optInt("status", 200)
                val latency = json.optDouble("latency_ms", 50.0).toLong()
                val timestamp = (json.optDouble("timestamp", System.currentTimeMillis() / 1000.0) * 1000).toLong()

                val logId = "log_" + UUID.randomUUID().toString().take(8)
                val log = RequestLog(
                    logId = logId,
                    endpointId = endpoint,
                    timestamp = timestamp,
                    sourceIp = ip,
                    userId = "ip_" + ip.replace(".", "_"),
                    statusCode = status,
                    latencyMs = latency,
                    syncStatus = SyncStatus.SYNCED
                )
                logDao.insertLog(log)

                if (type == "ip_blocked" || type == "blocked_request" || status == 429) {
                    val eventId = "ae_" + UUID.randomUUID().toString().take(8)
                    val abuseEvent = AbuseEvent(
                        eventId = eventId,
                        logId = logId,
                        eventType = "DDoS / Rate Limit Breach",
                        riskScore = 95,
                        action = "BLOCK_IP",
                        createdAt = timestamp,
                        syncStatus = SyncStatus.SYNCED
                    )
                    abuseDao.insertEvent(abuseEvent)

                    val incidentId = "ddos_" + UUID.randomUUID().toString().take(8)
                    val incident = DDoSIncident(
                        incidentId = incidentId,
                        endpointId = endpoint,
                        startTime = timestamp,
                        requestSpike = 500,
                        severity = "CRITICAL",
                        status = "ACTIVE",
                        syncStatus = SyncStatus.SYNCED
                    )
                    ddosDao.insertIncident(incident)

                    showNotification(
                        title = "Security Alert: Rate Limit Exceeded",
                        body = "IP $ip received HTTP 429 on $endpoint ($type)"
                    )
                }
            } catch (e: Exception) {
                Log.e(tag, "Error parsing live event: ${e.message}")
            }
        }
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "high_risk_alerts"

        val channel = NotificationChannel(
            channelId,
            "High-Risk Security Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for live DDoS attacks and IP blocks"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
