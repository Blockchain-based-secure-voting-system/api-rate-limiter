/**
 * Rate Limiter Core Engine.
 *
 * COMBINATION LOGIC (Decision 4a):
 * Algorithm combination uses strict "AND" semantics:
 * For every incoming request targeting a protected route, BOTH the Token Bucket
 * (burst control) and the Sliding Window (rate ceiling) checks MUST allow the request.
 * If either limiter denies the request, the request is rejected with HTTP 429 Too Many Requests.
 *
 * Token consumption semantics:
 * - A request is allowed if and only if TokenBucket.allow() AND SlidingWindow.allow() both succeed.
 * - If either check denies the request, the client receives HTTP 429 and rate-limit headers
 *   instructing when to retry.
 */
package com.cutm.nt14.gateway.core

import com.cutm.nt14.gateway.algorithms.SlidingWindow
import com.cutm.nt14.gateway.algorithms.TokenBucket
import com.cutm.nt14.gateway.models.RateLimitRule
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.response.header
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

data class RateLimitEvaluation(
    val allowed: Boolean,
    val limit: Int,
    val remaining: Int,
    val resetSeconds: Long,
    val retryAfterSeconds: Long,
    val action: String
)

private class ClientLimiterPair(
    val tokenBucket: TokenBucket,
    val slidingWindow: SlidingWindow
) {
    val lock = Mutex()
}

class RateLimiter {
    // In-memory store of active rules per endpoint pattern
    private val rules = ConcurrentHashMap<String, RateLimitRule>()

    // In-memory limiters indexed by "$endpoint:$clientId"
    private val limiters = ConcurrentHashMap<String, ClientLimiterPair>()

    init {
        // Default seed rules
        setRule(RateLimitRule(endpointId = "/api/users", limitPerMin = 60, burstLimit = 15, action = "BLOCK"))
        setRule(RateLimitRule(endpointId = "/api/orders", limitPerMin = 40, burstLimit = 10, action = "BLOCK"))
        setRule(RateLimitRule(endpointId = "/api/products", limitPerMin = 100, burstLimit = 20, action = "ALERT"))
    }

    fun getAllRules(): List<RateLimitRule> = rules.values.toList()

    fun getRule(endpoint: String): RateLimitRule? = rules[endpoint]

    fun setRule(rule: RateLimitRule) {
        rules[rule.endpointId] = rule
        // Invalidate cached limiters for this endpoint so new rules take immediate effect
        limiters.keys.removeIf { it.startsWith("${rule.endpointId}:") }
    }

    fun removeRule(endpoint: String): Boolean {
        val removed = rules.remove(endpoint) != null
        limiters.keys.removeIf { it.startsWith("$endpoint:") }
        return removed
    }

    /**
     * Resolves the client identity from headers or remote network address.
     */
    fun resolveClientId(call: ApplicationCall): String {
        val forwarded = call.request.headers["X-Forwarded-For"]
        if (!forwarded.isNullOrBlank()) {
            return forwarded.split(",").first().trim()
        }
        val clientKey = call.request.headers["X-Client-ID"]
        if (!clientKey.isNullOrBlank()) {
            return clientKey.trim()
        }
        return call.request.origin.remoteHost
    }

    /**
     * Evaluates a request against the configured rule for the target endpoint.
     * Implements Decision 4a (AND combination semantics).
     */
    suspend fun evaluate(endpoint: String, clientId: String): RateLimitEvaluation {
        val rule = rules[endpoint] ?: RateLimitRule(
            endpointId = endpoint,
            limitPerMin = 100,
            burstLimit = 20,
            action = "ALERT"
        )

        val key = "$endpoint:$clientId"
        val pair = limiters.computeIfAbsent(key) {
            ClientLimiterPair(
                tokenBucket = TokenBucket(
                    capacity = rule.burstLimit.toDouble(),
                    refillRatePerSec = rule.limitPerMin / 60.0
                ),
                slidingWindow = SlidingWindow(
                    limit = rule.limitPerMin,
                    windowMillis = 60_000L
                )
            )
        }

        return pair.lock.withLock {
            // First check TokenBucket (burst ceiling)
            val tbResult = pair.tokenBucket.allow(1.0)
            if (!tbResult.allowed) {
                // Denied by Token Bucket
                val (_, _, swReset) = pair.slidingWindow.peek()
                return@withLock RateLimitEvaluation(
                    allowed = false,
                    limit = rule.limitPerMin,
                    remaining = 0,
                    resetSeconds = ceil(swReset).toLong().coerceAtLeast(1L),
                    retryAfterSeconds = ceil(tbResult.retryAfterSeconds).toLong().coerceAtLeast(1L),
                    action = rule.action
                )
            }

            // Next check Sliding Window (rolling minute ceiling)
            val swResult = pair.slidingWindow.allow()
            if (!swResult.allowed) {
                // Denied by Sliding Window -> refund the token consumed from token bucket
                pair.tokenBucket.allow(-1.0)
                return@withLock RateLimitEvaluation(
                    allowed = false,
                    limit = rule.limitPerMin,
                    remaining = 0,
                    resetSeconds = ceil(swResult.resetSeconds).toLong().coerceAtLeast(1L),
                    retryAfterSeconds = ceil(swResult.resetSeconds).toLong().coerceAtLeast(1L),
                    action = rule.action
                )
            }

            // Both checks PASSED (Decision 4a: AND semantics satisfied)
            val remainingTokens = tbResult.remainingTokens.toInt()
            val remainingQuota = minOf(remainingTokens, swResult.remainingRequests)

            RateLimitEvaluation(
                allowed = true,
                limit = rule.limitPerMin,
                remaining = remainingQuota.coerceAtLeast(0),
                resetSeconds = ceil(swResult.resetSeconds).toLong().coerceAtLeast(0L),
                retryAfterSeconds = 0L,
                action = rule.action
            )
        }
    }

    /**
     * Injects standard Rate Limit headers into the HTTP response.
     */
    fun appendHeaders(call: ApplicationCall, evaluation: RateLimitEvaluation) {
        call.response.header("X-RateLimit-Limit", evaluation.limit.toString())
        call.response.header("X-RateLimit-Remaining", evaluation.remaining.toString())
        call.response.header("X-RateLimit-Reset", evaluation.resetSeconds.toString())

        if (!evaluation.allowed) {
            call.response.header("Retry-After", evaluation.retryAfterSeconds.toString())
        }
    }
}
