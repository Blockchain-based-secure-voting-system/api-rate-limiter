package com.cutm.nt14.data.remote

import android.util.Log
import com.cutm.nt14.BuildConfig
import com.cutm.nt14.data.local.entities.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRealtimeDatabaseManager @Inject constructor() {
    private val tag = "FirebaseRTDB"

    private val dbInstance: FirebaseDatabase by lazy {
        val url = BuildConfig.FIREBASE_RTDB_URL.ifBlank {
            "https://api-ratelimiter-default-rtdb.asia-southeast1.firebasedatabase.app/"
        }
        runCatching {
            FirebaseDatabase.getInstance(url).apply {
                runCatching { setPersistenceEnabled(true) }
            }
        }.getOrElse {
            FirebaseDatabase.getInstance()
        }
    }

    suspend fun syncEndpoint(endpoint: Endpoint) {
        try {
            dbInstance.getReference("endpoints").child(endpoint.endpointId).setValue(endpoint).await()
            Log.d(tag, "Synced endpoint ${endpoint.endpointId} to Firebase RTDB")
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync endpoint to Firebase RTDB: ${e.message}")
        }
    }

    suspend fun deleteEndpoint(endpointId: String) {
        try {
            dbInstance.getReference("endpoints").child(endpointId).removeValue().await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete endpoint in Firebase RTDB: ${e.message}")
        }
    }

    suspend fun syncRequestLog(log: RequestLog) {
        try {
            dbInstance.getReference("request_logs").child(log.logId).setValue(log).await()
            Log.d(tag, "Synced log ${log.logId} to Firebase RTDB")
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync request log to Firebase RTDB: ${e.message}")
        }
    }

    suspend fun syncAbuseEvent(event: AbuseEvent) {
        try {
            dbInstance.getReference("abuse_events").child(event.eventId).setValue(event).await()
            Log.d(tag, "Synced abuse event ${event.eventId} to Firebase RTDB")
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync abuse event to Firebase RTDB: ${e.message}")
        }
    }

    suspend fun syncDDoSIncident(incident: DDoSIncident) {
        try {
            dbInstance.getReference("incidents").child(incident.incidentId).setValue(incident).await()
            Log.d(tag, "Synced incident ${incident.incidentId} to Firebase RTDB")
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync incident to Firebase RTDB: ${e.message}")
        }
    }

    suspend fun syncRateLimit(rule: RateLimit) {
        try {
            dbInstance.getReference("rate_limits").child(rule.ruleId).setValue(rule).await()
            Log.d(tag, "Synced rate limit ${rule.ruleId} to Firebase RTDB")
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync rate limit to Firebase RTDB: ${e.message}")
        }
    }
}
