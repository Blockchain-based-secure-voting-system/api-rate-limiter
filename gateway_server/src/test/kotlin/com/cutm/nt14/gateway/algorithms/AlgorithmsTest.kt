package com.cutm.nt14.gateway.algorithms

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AlgorithmsTest {

    @Test
    fun testTokenBucketEnforcesBurstAndRefills() = runBlocking {
        // Capacity 3, refill rate 10 tokens per second
        val bucket = TokenBucket(capacity = 3.0, refillRatePerSec = 10.0)

        // 3 initial requests should succeed
        val r1 = bucket.allow(1.0)
        assertTrue(r1.allowed)
        assertEquals(2.0, r1.remainingTokens, 0.1)

        val r2 = bucket.allow(1.0)
        assertTrue(r2.allowed)
        assertEquals(1.0, r2.remainingTokens, 0.1)

        val r3 = bucket.allow(1.0)
        assertTrue(r3.allowed)
        assertEquals(0.0, r3.remainingTokens, 0.1)

        // 4th request without wait should be rejected
        val r4 = bucket.allow(1.0)
        assertFalse(r4.allowed)
        assertTrue(r4.retryAfterSeconds > 0)

        // Wait 150ms -> should refill ~1.5 tokens
        Thread.sleep(150)
        val r5 = bucket.allow(1.0)
        assertTrue("Expected token to refill after sleep", r5.allowed)
    }

    @Test
    fun testSlidingWindowEnforcesLimitWithinWindow() = runBlocking {
        // Limit 3 requests in a 1-second window (1000ms)
        val window = SlidingWindow(limit = 3, windowMillis = 1000L)

        val r1 = window.allow()
        assertTrue(r1.allowed)
        assertEquals(2, r1.remainingRequests)

        val r2 = window.allow()
        assertTrue(r2.allowed)
        assertEquals(1, r2.remainingRequests)

        val r3 = window.allow()
        assertTrue(r3.allowed)
        assertEquals(0, r3.remainingRequests)

        // 4th request in the same window should be rejected
        val r4 = window.allow()
        assertFalse(r4.allowed)
        assertEquals(0, r4.remainingRequests)
        assertTrue(r4.resetSeconds > 0)

        // Wait for window to expire
        Thread.sleep(1050)
        val r5 = window.allow()
        assertTrue("Expected sliding window quota to clear", r5.allowed)
        assertEquals(2, r5.remainingRequests)
    }
}
