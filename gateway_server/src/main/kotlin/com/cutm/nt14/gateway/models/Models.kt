package com.cutm.nt14.gateway.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Real-time event payload matching Android's GatewayWebSocketClient byte-for-byte.
 * Note: latencyMs is annotated with @SerialName("latency_ms") because Android's
 * GatewayWebSocketClient specifically parses `json.optDouble("latency_ms", 50.0)`.
 */
@Serializable
data class GatewayEvent(
    val type: String, // "request" | "blocked_request" | "ip_blocked"
    val ip: String,
    val endpoint: String,
    val status: Int,
    @SerialName("latency_ms")
    val latencyMs: Long,
    val timestamp: Double
)

/**
 * Dynamic rate limit rule definition.
 */
@Serializable
data class RateLimitRule(
    val endpointId: String,
    val limitPerMin: Int,
    val burstLimit: Int,
    val action: String = "ALERT" // "ALERT" | "BLOCK"
)

/**
 * Request payload for POST /api/simulate control-plane route.
 */
@Serializable
data class SimulateRequest(
    val endpoint: String = "/api/users",
    val requestCount: Int = 30,
    val rps: Double = 20.0,
    val sourceIps: List<String> = listOf("192.168.1.10", "192.168.1.25", "10.0.0.5")
)

/**
 * Response payload for POST /api/simulate.
 */
@Serializable
data class SimulateResponse(
    val message: String,
    val totalTriggered: Int,
    val endpoint: String
)

/**
 * General status response payload.
 */
@Serializable
data class ApiMessage(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Protected Demo Resources
@Serializable
data class DemoUser(val id: String, val name: String, val role: String)

@Serializable
data class DemoOrder(val orderId: String, val amount: Double, val status: String)

@Serializable
data class DemoProduct(val sku: String, val name: String, val price: Double)

@Serializable
data class HealthResponse(val status: String, val subscribers: Int)
