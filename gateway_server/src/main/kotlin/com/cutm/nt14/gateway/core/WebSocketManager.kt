package com.cutm.nt14.gateway.core

import com.cutm.nt14.gateway.models.GatewayEvent
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages active WebSocket subscriber sessions and broadcasts real-time telemetry events.
 * Directly matches the JSON schema expected by Android's GatewayWebSocketClient.
 */
class WebSocketManager {
    private val logger = LoggerFactory.getLogger(WebSocketManager::class.java)
    private val sessions = Collections.newSetFromMap(ConcurrentHashMap<DefaultWebSocketServerSession, Boolean>())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun register(session: DefaultWebSocketServerSession) {
        sessions.add(session)
        logger.info("New WebSocket client connected. Active subscribers: ${sessions.size}")
    }

    fun unregister(session: DefaultWebSocketServerSession) {
        sessions.remove(session)
        logger.info("WebSocket client disconnected. Active subscribers: ${sessions.size}")
    }

    fun activeSubscriberCount(): Int = sessions.size

    fun broadcast(event: GatewayEvent) {
        val jsonPayload = json.encodeToString(event)
        val frame = Frame.Text(jsonPayload)

        scope.launch {
            val deadSessions = mutableListOf<DefaultWebSocketServerSession>()

            for (session in sessions) {
                try {
                    session.send(frame)
                } catch (e: ClosedSendChannelException) {
                    deadSessions.add(session)
                } catch (e: Exception) {
                    logger.warn("Failed to send WebSocket event to session: ${e.message}")
                    deadSessions.add(session)
                }
            }

            if (deadSessions.isNotEmpty()) {
                sessions.removeAll(deadSessions.toSet())
                logger.debug("Cleaned up ${deadSessions.size} closed WebSocket sessions.")
            }
        }
    }
}
