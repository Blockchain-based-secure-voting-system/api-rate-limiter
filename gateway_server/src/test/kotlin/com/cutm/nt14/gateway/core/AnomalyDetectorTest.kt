package com.cutm.nt14.gateway.core

import org.junit.Assert.*
import org.junit.Test

class AnomalyDetectorTest {

    @Test
    fun testExcessiveFailuresTriggersAutoBan() {
        val detector = AnomalyDetector(failureThreshold = 5, burstVolumeThreshold = 50, banDurationSeconds = 60)
        val client = "attacker_1"

        // 4 failures -> no ban yet
        repeat(4) {
            val ban = detector.recordAndInspect(client, "/api/users", 401)
            assertNull("Should not ban before threshold", ban)
            assertNull(detector.checkBan(client))
        }

        // 5th failure -> triggers ban
        val triggeredBan = detector.recordAndInspect(client, "/api/users", 401)
        assertNotNull("Should trigger auto-ban on 5th failure", triggeredBan)
        assertTrue(triggeredBan!!.reason.contains("Excessive failure rate"))
        assertTrue(triggeredBan.bannedUntil > System.currentTimeMillis())

        // Subsequent requests should see the active ban
        val activeBan = detector.checkBan(client)
        assertNotNull(activeBan)
        assertEquals(triggeredBan.reason, activeBan!!.reason)
    }

    @Test
    fun testBurstVolumeSpikeTriggersAutoBan() {
        val detector = AnomalyDetector(failureThreshold = 50, burstVolumeThreshold = 10, banDurationSeconds = 60)
        val client = "flooder_1"

        // 9 rapid requests -> no ban yet
        repeat(9) {
            val ban = detector.recordAndInspect(client, "/api/orders", 200)
            assertNull(ban)
        }

        // 10th request in rapid succession -> triggers flood ban
        val triggeredBan = detector.recordAndInspect(client, "/api/orders", 200)
        assertNotNull("Should trigger auto-ban on flood threshold", triggeredBan)
        assertTrue(triggeredBan!!.reason.contains("DDoS flood spike"))
    }

    @Test
    fun testUnbanClientLiftsBan() {
        val detector = AnomalyDetector(failureThreshold = 3, banDurationSeconds = 120)
        val client = "temp_banned"

        repeat(3) {
            detector.recordAndInspect(client, "/api/data", 500)
        }
        assertNotNull(detector.checkBan(client))

        val unbanned = detector.unbanClient(client)
        assertTrue(unbanned)
        assertNull(detector.checkBan(client))
    }
}
