package com.cutm.nt14.domain.detector

import com.cutm.nt14.data.local.entities.RateLimit
import com.cutm.nt14.data.local.entities.RequestLog

class RateLimitOptimizer {
    
    fun recommend(logs: List<RequestLog>, currentLimit: RateLimit?): RateLimit? {
        if (logs.isEmpty()) return null
        
        val avgLatency = logs.map { it.latencyMs }.average()
        val errorRate = logs.count { it.statusCode >= 400 }.toFloat() / logs.size
        
        // Simple logic: if error rate is high and latency is high, suggest tighter limits
        if (errorRate > 0.1 || avgLatency > 1000) {
            val newLimit = (currentLimit?.limitPerMin ?: 100) * 0.8
            return currentLimit?.copy(
                limitPerMin = newLimit.toInt(),
                updatedAt = System.currentTimeMillis()
            ) ?: RateLimit(
                ruleId = "recommended",
                endpointId = logs.first().endpointId,
                limitPerMin = 80,
                burstLimit = 100,
                action = "ALERT",
                updatedAt = System.currentTimeMillis()
            )
        }
        
        return null
    }
}
