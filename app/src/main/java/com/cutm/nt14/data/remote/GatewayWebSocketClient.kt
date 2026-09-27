package com.cutm.nt14.data.remote

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.cutm.nt14.MainActivity
import com.cutm.nt14.data.local.SyncStatus
import com.cutm.nt14.data.local.daos.AbuseEventDao
import com.cutm.nt14.data.local.daos.DDoSIncidentDao
import com.cutm.nt14.data.local.daos.RequestLogDao
import com.cutm.nt14.data.local.entities.AbuseEvent
import com.cutm.nt14.data.local.entities.DDoSIncident
import com.cutm.nt14.data.local.entities.RequestLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GatewayWebSocketClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logDao: RequestLogDao,
    private val abuseDao: AbuseEventDao,
    private val ddosDao: DDoSIncidentDao,
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Default connection URL for Android Emulator -> Host machine server
    private val serverWsUrl = "ws://10.0.2.2:8000/ws/events?api_key=dev-local-key"

    fun connect() {
        if (webSocket != null) return

        val request = Request.Builder()
            .url(serverWsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Connected to real-time gateway server
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleLiveEvent(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@GatewayWebSocketClient.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@GatewayWebSocketClient.webSocket = null
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "App disconnected")
        webSocket = null
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

                if (type == "ip_blocked" || type == "blocked_request") {
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
                        title = "🚨 Security Alert: IP Blocked",
                        body = "IP $ip breached rate limit on $endpoint ($type)"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
