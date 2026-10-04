package com.cutm.nt14.domain.detector

import com.cutm.nt14.data.local.entities.RateLimit
import com.cutm.nt14.data.local.entities.RequestLog

data class OptimizationResult(
    val endpointId: String,
    val totalAnalyzed: Int,
    val errorRate: Float,
    val avgLatencyMs: Double,
    val currentLimitPerMin: Int,
    val currentBurstLimit: Int,
    val recommendedLimitPerMin: Int,
    val recommendedBurstLimit: Int,
    val rationale: String,
    val recommendedRule: RateLimit
)

class RateLimitOptimizer {

    fun recommend(logs: List<RequestLog>, currentLimit: RateLimit?): RateLimit? {
        if (logs.isEmpty()) return null
        val targetEndpoint = logs.first().endpointId
        val endpointLogs = logs.filter { it.endpointId == targetEndpoint }
        val result = optimize(targetEndpoint, endpointLogs, currentLimit)
        return if (result.recommendedLimitPerMin != result.currentLimitPerMin) {
            result.recommendedRule
        } else {
            null
        }
    }

    fun optimize(
        endpointId: String,
        logs: List<RequestLog>,
        currentLimit: RateLimit?
    ): OptimizationResult {
        val endpointLogs = if (endpointId.isNotBlank()) logs.filter { it.endpointId == endpointId } else logs
        val total = endpointLogs.size
        val currLimit = currentLimit?.limitPerMin ?: 20
        val currBurst = currentLimit?.burstLimit ?: 5

        if (total == 0) {
            return OptimizationResult(
                endpointId = endpointId,
                totalAnalyzed = 0,
                errorRate = 0f,
                avgLatencyMs = 0.0,
                currentLimitPerMin = currLimit,
                currentBurstLimit = currBurst,
                recommendedLimitPerMin = currLimit,
                recommendedBurstLimit = currBurst,
                rationale = "No live traffic logged yet for $endpointId. Baseline policy maintained.",
                recommendedRule = currentLimit ?: RateLimit(
                    ruleId = "opt_" + System.currentTimeMillis().toString().takeLast(6),
                    endpointId = endpointId,
                    limitPerMin = currLimit,
                    burstLimit = currBurst,
                    action = "BLOCK",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        val avgLatency = endpointLogs.map { it.latencyMs }.average()
        val errorCount = endpointLogs.count { it.statusCode >= 400 || it.statusCode == 429 }
        val errorRate = errorCount.toFloat() / total

        // Optimization heuristics for PolyLance web3 protocol protection:
        // If throttle/error rate is > 10% or latency exceeds 400ms:
        val (recLimit, recBurst, rationale) = when {
            errorRate >= 0.25 -> {
                val newLimit = (currLimit * 0.75).toInt().coerceAtLeast(5)
                val newBurst = (currBurst * 0.75).toInt().coerceAtLeast(2)
                Triple(
                    newLimit,
                    newBurst,
                    "High 429 throttle rate (${(errorRate * 100).toInt()}%). Tightening limit to $newLimit req/min with burst $newBurst to shield smart contracts from congestion."
                )
            }
            errorRate >= 0.10 -> {
                val newLimit = (currLimit * 0.85).toInt().coerceAtLeast(5)
                val newBurst = (currBurst * 0.85).toInt().coerceAtLeast(2)
                Triple(
                    newLimit,
                    newBurst,
                    "Moderate rate limit spikes detected (${(errorRate * 100).toInt()}%). Recommending calibrated ceiling of $newLimit req/min."
                )
            }
            avgLatency > 500.0 -> {
                val newLimit = (currLimit * 0.8).toInt().coerceAtLeast(5)
                val newBurst = (currBurst * 0.8).toInt().coerceAtLeast(2)
                Triple(
                    newLimit,
                    newBurst,
                    "Elevated latency (${avgLatency.toInt()}ms). Scaling down to $newLimit req/min to reduce gateway backend queue latency."
                )
            }
            else -> {
                Triple(
                    currLimit,
                    currBurst,
                    "Protocol operating smoothly (0% errors, ${avgLatency.toInt()}ms avg latency). Current policy is optimal."
                )
            }
        }

        val updatedRule = (currentLimit?.copy(
            limitPerMin = recLimit,
            burstLimit = recBurst,
            action = "BLOCK",
            updatedAt = System.currentTimeMillis()
        )) ?: RateLimit(
            ruleId = "opt_" + System.currentTimeMillis().toString().takeLast(6),
            endpointId = endpointId,
            limitPerMin = recLimit,
            burstLimit = recBurst,
            action = "BLOCK",
            updatedAt = System.currentTimeMillis()
        )

        return OptimizationResult(
            endpointId = endpointId,
            totalAnalyzed = total,
            errorRate = errorRate,
            avgLatencyMs = avgLatency,
            currentLimitPerMin = currLimit,
            currentBurstLimit = currBurst,
            recommendedLimitPerMin = recLimit,
            recommendedBurstLimit = recBurst,
            rationale = rationale,
            recommendedRule = updatedRule
        )
    }
}
