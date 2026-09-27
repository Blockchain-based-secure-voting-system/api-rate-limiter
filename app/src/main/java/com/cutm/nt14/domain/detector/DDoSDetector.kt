package com.cutm.nt14.domain.detector

import com.cutm.nt14.data.local.entities.RequestLog

data class DDoSConfig(
    val rollingAverageWindowMs: Long = 300000, // 5 minutes
    val detectionWindowMs: Long = 30000, // 30 seconds
    val spikeThresholdMultiplier: Float = 3.0f,
    val latencyIncreaseThresholdMs: Long = 500
)

class DDoSDetector(private val config: DDoSConfig = DDoSConfig()) {

    fun analyze(logs: List<RequestLog>): Boolean {
        if (logs.isEmpty()) return false
        
        val now = System.currentTimeMillis()
        val recentLogs = logs.filter { now - it.timestamp <= config.detectionWindowMs }
        val historicLogs = logs.filter { 
            val diff = now - it.timestamp
            diff > config.detectionWindowMs && diff <= config.rollingAverageWindowMs 
        }
        
        if (recentLogs.isEmpty()) return false
        
        val recentRate = recentLogs.size.toFloat() / config.detectionWindowMs
        val historicRate = if (historicLogs.isNotEmpty()) {
            historicLogs.size.toFloat() / (config.rollingAverageWindowMs - config.detectionWindowMs)
        } else {
            0f
        }
        
        // Spike detection
        if (historicRate > 0 && recentRate > historicRate * config.spikeThresholdMultiplier) {
            return true
        }
        
        // Latency increase detection
        val recentAvgLatency = recentLogs.map { it.latencyMs }.average()
        val historicAvgLatency = if (historicLogs.isNotEmpty()) historicLogs.map { it.latencyMs }.average() else 0.0
        
        if (historicAvgLatency > 0 && recentAvgLatency > historicAvgLatency + config.latencyIncreaseThresholdMs) {
            return true
        }
        
        return false
    }
}
