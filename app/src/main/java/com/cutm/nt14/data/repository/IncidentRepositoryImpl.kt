package com.cutm.nt14.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cutm.nt14.data.local.daos.AbuseEventDao
import com.cutm.nt14.data.local.daos.DDoSIncidentDao
import com.cutm.nt14.data.local.daos.RequestLogDao
import com.cutm.nt14.data.local.entities.AbuseEvent
import com.cutm.nt14.data.local.entities.DDoSIncident
import com.cutm.nt14.domain.detector.AbuseDetector
import com.cutm.nt14.domain.detector.DDoSDetector
import com.cutm.nt14.domain.repository.IncidentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject

class IncidentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val abuseDao: AbuseEventDao,
    private val ddosDao: DDoSIncidentDao,
    private val logDao: RequestLogDao
) : IncidentRepository {

    private val abuseDetector = AbuseDetector()
    private val ddosDetector = DDoSDetector()

    override fun getAllAbuseEvents(): Flow<List<AbuseEvent>> = abuseDao.getAllEvents()
    override fun getAllDDoSIncidents(): Flow<List<DDoSIncident>> = ddosDao.getAllIncidents()

    override suspend fun saveAbuseEvent(event: AbuseEvent) = abuseDao.insertEvent(event)
    override suspend fun saveDDoSIncident(incident: DDoSIncident) = ddosDao.insertIncident(incident)

    override suspend fun runDetection() {
        val allLogs = logDao.getAllLogs().first()
        
        // Group logs by IP/User for abuse detection
        val logsByIp = allLogs.groupBy { it.sourceIp }
        logsByIp.forEach { (ip, logs) ->
            val riskScore = abuseDetector.analyze(logs)
            if (riskScore >= 70) {
                val event = AbuseEvent(
                    eventId = UUID.randomUUID().toString(),
                    logId = logs.first().logId,
                    eventType = "ABUSE_IP",
                    riskScore = riskScore,
                    action = "ALERT",
                    createdAt = System.currentTimeMillis()
                )
                saveAbuseEvent(event)
                sendAlert("API Abuse Detected", "IP $ip has a risk score of $riskScore")
            }
        }

        // Group logs by Endpoint for DDoS detection
        val logsByEndpoint = allLogs.groupBy { it.endpointId }
        logsByEndpoint.forEach { (endpointId, logs) ->
            if (ddosDetector.analyze(logs)) {
                val incident = DDoSIncident(
                    incidentId = UUID.randomUUID().toString(),
                    endpointId = endpointId,
                    startTime = System.currentTimeMillis(),
                    requestSpike = logs.size, // Simplified
                    severity = "CRITICAL",
                    status = "ACTIVE"
                )
                saveDDoSIncident(incident)
                sendAlert("DDoS Attack Detected", "Critical spike on endpoint $endpointId")
            }
        }
    }

    private fun sendAlert(title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "high_risk_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "High-Risk Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
