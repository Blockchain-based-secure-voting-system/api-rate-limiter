package com.cutm.nt14.domain.detector

import com.cutm.nt14.data.local.entities.RequestLog
import com.cutm.nt14.data.local.SyncStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DDoSDetectorTest {

    private val detector = DDoSDetector(DDoSConfig(
        rollingAverageWindowMs = 300000, // 5 min
        detectionWindowMs = 30000, // 30 sec
        spikeThresholdMultiplier = 3.0f,
        latencyIncreaseThresholdMs = 500
    ))

    @Test
    fun `test normal consistent traffic returns false`() {
        val now = System.currentTimeMillis()
        val logs = mutableListOf<RequestLog>()
        // Historic: 10 requests every 30s for 4.5 mins = 9 * 10 = 90 logs
        repeat(90) {
            logs.add(createLog(now - 40000 - it * 3000, 100))
        }
        // Recent: 10 requests in last 30s
        repeat(10) {
            logs.add(createLog(now - it * 1000, 100))
        }
        
        assertFalse(detector.analyze(logs))
    }

    @Test
    fun `test sudden request spike returns true`() {
        val now = System.currentTimeMillis()
        val logs = mutableListOf<RequestLog>()
        // Historic: 1 request every 30s
        repeat(9) {
            logs.add(createLog(now - 40000 - it * 30000, 100))
        }
        // Recent: 50 requests in 30s
        repeat(50) {
            logs.add(createLog(now - it * 100, 100))
        }
        
        assertTrue(detector.analyze(logs))
    }

    @Test
    fun `test massive latency increase returns true`() {
        val now = System.currentTimeMillis()
        val logs = mutableListOf<RequestLog>()
        // Historic: 100ms latency
        repeat(10) {
            logs.add(createLog(now - 40000 - it * 1000, 100))
        }
        // Recent: 1000ms latency
        repeat(5) {
            logs.add(createLog(now - it * 1000, 1000))
        }
        
        assertTrue(detector.analyze(logs))
    }

    private fun createLog(timestamp: Long, latencyMs: Long): RequestLog {
        return RequestLog(
            logId = "log",
            endpointId = "ep",
            timestamp = timestamp,
            sourceIp = "1.2.3.4",
            userId = "user",
            statusCode = 200,
            latencyMs = latencyMs,
            syncStatus = SyncStatus.SYNCED
        )
    }
}
