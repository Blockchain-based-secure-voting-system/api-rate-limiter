package com.cutm.nt14.domain.detector

import com.cutm.nt14.data.local.entities.RequestLog
import com.cutm.nt14.data.local.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbuseDetectorTest {

    private val detector = AbuseDetector(AbuseConfig(
        timeWindowMs = 60000,
        maxRequestsPerWindow = 100,
        failureRateThreshold = 0.5f
    ))

    @Test
    fun `test normal traffic returns zero risk`() {
        val now = System.currentTimeMillis()
        val logs = listOf(
            createLog(now - 1000, 200),
            createLog(now - 2000, 200)
        )
        val score = detector.analyze(logs)
        assertEquals(0, score)
    }

    @Test
    fun `test threshold-crossing traffic returns high risk score`() {
        val now = System.currentTimeMillis()
        val logs = mutableListOf<RequestLog>()
        // 110 requests (exceeds 100)
        repeat(110) {
            logs.add(createLog(now - it * 10, 500)) // All errors
        }
        val score = detector.analyze(logs)
        // Frequency (>100) -> 50
        // Failure Rate (1.0 > 0.5) -> 40
        // Total = 90
        assertEquals(90, score)
    }

    @Test
    fun `test empty window returns zero risk`() {
        val now = System.currentTimeMillis()
        val logs = listOf(
            createLog(now - 70000, 200) // Outside 1 min window
        )
        val score = detector.analyze(logs)
        assertEquals(0, score)
    }

    @Test
    fun `test half threshold frequency returns moderate risk`() {
        val now = System.currentTimeMillis()
        val logs = mutableListOf<RequestLog>()
        // 60 requests (exceeds 50 but not 100)
        repeat(60) {
            logs.add(createLog(now - it * 10, 200))
        }
        val score = detector.analyze(logs)
        assertEquals(25, score)
    }

    private fun createLog(timestamp: Long, statusCode: Int): RequestLog {
        return RequestLog(
            logId = "log",
            endpointId = "ep",
            timestamp = timestamp,
            sourceIp = "1.2.3.4",
            userId = "user",
            statusCode = statusCode,
            latencyMs = 100,
            syncStatus = SyncStatus.SYNCED
        )
    }
}
