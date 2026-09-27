package com.cutm.nt14.gateway.routes

import com.cutm.nt14.gateway.core.RateLimiter
import com.cutm.nt14.gateway.core.WebSocketManager
import com.cutm.nt14.gateway.models.GatewayEvent
import com.cutm.nt14.gateway.models.SimulateRequest
import com.cutm.nt14.gateway.models.SimulateResponse
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Control-plane traffic simulation routes.
 * Implements Decision 4b: Gated behind authenticate("api-key").
 */
fun Route.simulateRoutes(
    rateLimiter: RateLimiter,
    webSocketManager: WebSocketManager,
    simulationScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    authenticate("api-key") {
        post("/api/simulate") {
            val req = call.receive<SimulateRequest>()

            // Launch simulation asynchronously to simulate realistic traffic flow
            simulationScope.launch {
                val delayMs = if (req.rps > 0) (1000.0 / req.rps).toLong() else 50L
                val ips = if (req.sourceIps.isNotEmpty()) req.sourceIps else listOf("127.0.0.1")

                repeat(req.requestCount) {
                    val ip = ips[Random.nextInt(ips.size)]
                    val startTime = System.currentTimeMillis()
                    val eval = rateLimiter.evaluate(req.endpoint, ip)
                    val latency = Random.nextLong(20, 80)
                    val timestampSeconds = (System.currentTimeMillis() / 1000.0)

                    val status = if (eval.allowed) 200 else 429
                    val eventType = when {
                        !eval.allowed && eval.action == "BLOCK" -> "ip_blocked"
                        !eval.allowed -> "blocked_request"
                        else -> "request"
                    }

                    val event = GatewayEvent(
                        type = eventType,
                        ip = ip,
                        endpoint = req.endpoint,
                        status = status,
                        latencyMs = latency,
                        timestamp = timestampSeconds
                    )
                    webSocketManager.broadcast(event)

                    if (delayMs > 0) {
                        delay(delayMs)
                    }
                }
            }

            call.respond(
                SimulateResponse(
                    message = "Simulated traffic generation started (${req.requestCount} requests on ${req.endpoint} at ~${req.rps} RPS)",
                    totalTriggered = req.requestCount,
                    endpoint = req.endpoint
                )
            )
        }
    }
}
