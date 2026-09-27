package com.cutm.nt14.domain.detector

import com.cutm.nt14.data.local.entities.RequestLog
import java.util.*

data class AbuseConfig(
    val timeWindowMs: Long = 60000, // 1 minute
    val maxRequestsPerWindow: Int = 100,
    val failureRateThreshold: Float = 0.5f,
    val riskScoreThreshold: Int = 70
)

class AbuseDetector(private val config: AbuseConfig = AbuseConfig()) {

    fun analyze(logs: List<RequestLog>): Int {
        if (logs.isEmpty()) return 0
        
        val now = System.currentTimeMillis()
        val windowLogs = logs.filter { now - it.timestamp <= config.timeWindowMs }
        
        if (windowLogs.isEmpty()) return 0
        
        val requestCount = windowLogs.size
        val failedCount = windowLogs.count { it.statusCode >= 400 }
        val failureRate = if (requestCount > 0) failedCount.toFloat() / requestCount else 0f
        
        var riskScore = 0
        
        // Factor 1: High frequency
        if (requestCount > config.maxRequestsPerWindow) {
            riskScore += 50
        } else if (requestCount > config.maxRequestsPerWindow / 2) {
            riskScore += 25
        }
        
        // Factor 2: High failure rate
        if (failureRate > config.failureRateThreshold) {
            riskScore += 40
        } else if (failureRate > config.failureRateThreshold / 2) {
            riskScore += 20
        }
        
        return riskScore.coerceAtMost(100)
    }
}
