package com.cutm.nt14.gateway.routes

import com.cutm.nt14.gateway.core.WebSocketManager
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("EventsWebSocket")

/**
 * Real-time telemetry feed route matching Android's GatewayWebSocketClient.
 * Verifies API key matching Decision 4b / dev-local-key.
 */
fun Route.eventsWebSocket(webSocketManager: WebSocketManager) {
    webSocket("/ws/events") {
        val apiKeyQuery = call.request.queryParameters["api_key"]
        val apiKeyHeader = call.request.headers["X-API-Key"]
        val key = apiKeyQuery ?: apiKeyHeader

        if (key != "dev-local-key") {
            logger.warn("Rejected unauthorized WebSocket connection attempt.")
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized: Invalid or missing API key"))
            return@webSocket
        }

        webSocketManager.register(this)
        try {
            // Keep connection open and drain incoming frames
            incoming.consumeEach { frame ->
                logger.trace("Received frame from client: ${frame.frameType}")
            }
        } catch (e: Exception) {
            logger.debug("WebSocket session ended: ${e.message}")
        } finally {
            webSocketManager.unregister(this)
        }
    }
}
