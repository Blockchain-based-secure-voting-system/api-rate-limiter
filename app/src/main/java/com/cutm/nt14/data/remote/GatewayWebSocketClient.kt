package com.cutm.nt14.data.remote

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
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
import okhttp3.*
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class GatewayConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
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
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow(GatewayConnectionState.DISCONNECTED)
    val connectionState: StateFlow<GatewayConnectionState> = _connectionState.asStateFlow()

    private val _connectedHost = MutableStateFlow("192.168.29.231:8000")
    val connectedHost: StateFlow<String> = _connectedHost.asStateFlow()

    fun connect() {
        if (_connectionState.value == GatewayConnectionState.CONNECTED ||
            _connectionState.value == GatewayConnectionState.CONNECTING) {
            return
        }

        scope.launch {
            val configuredHost = sessionManager.gatewayHost.first()
            attemptConnect(configuredHost)
        }
    }

    fun reconnectWithHost(newHost: String) {
        scope.launch {
            sessionManager.saveGatewayHost(newHost)
            disconnect()
            delay(500)
            attemptConnect(newHost)
        }
    }

    private suspend fun attemptConnect(host: String) {
        _connectionState.value = GatewayConnectionState.CONNECTING
        _connectedHost.value = host

        val wsUrl = "ws://$host/ws/events?api_key=dev-local-key"
        Log.i(tag, "Connecting to live gateway: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(tag, "Successfully connected to live gateway at $host")
                _connectionState.value = GatewayConnectionState.CONNECTED
                reconnectJob?.cancel()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleLiveEvent(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "WebSocket failure on $host: ${t.message}")
                this@GatewayWebSocketClient.webSocket = null
                _connectionState.value = GatewayConnectionState.DISCONNECTED
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(tag, "WebSocket closed ($code): $reason")
                this@GatewayWebSocketClient.webSocket = null
                _connectionState.value = GatewayConnectionState.DISCONNECTED
            }
        })
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(4000)
            if (_connectionState.value == GatewayConnectionState.DISCONNECTED) {
                Log.i(tag, "Attempting auto-reconnect...")
                val current = _connectedHost.value
                attemptConnect(current)
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = GatewayConnectionState.DISCONNECTED
    }

    /**
     * Sends an actual live HTTP request to the Gateway Server.
     * The gateway will process it, rate-limit if necessary, and broadcast
     * the event via WebSocket back to this app in real time!
     */
    suspend fun sendTestRequest(endpoint: String = "/api/users"): Int = withContext(Dispatchers.IO) {
        val host = _connectedHost.value
        val url = "http://$host$endpoint"
        try {
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                resp.code
            }
        } catch (e: Exception) {
            Log.e(tag, "Test request failed: ${e.message}")
            -1
        }
    }

    /**
     * Fires a burst of concurrent requests to trigger rate limiting and DDoS alarms
     * against the live gateway server.
     */
    suspend fun sendBurstSimulation(count: Int = 18, endpoint: String = "/api/users") = withContext(Dispatchers.IO) {
        val host = _connectedHost.value
        val url = "http://$host$endpoint"
        val jobs = (1..count).map {
            async {
                try {
                    val req = Request.Builder().url(url).get().build()
                    client.newCall(req).execute().use { it.code }
                } catch (e: Exception) {
                    -1
                }
            }
        }
        jobs.awaitAll()
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
                        title = "🚨 Security Alert: Rate Limit Exceeded",
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
