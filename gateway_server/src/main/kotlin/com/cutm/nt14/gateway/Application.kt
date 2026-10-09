package com.cutm.nt14.gateway

import com.cutm.nt14.gateway.core.JwtService
import com.cutm.nt14.gateway.core.RateLimitEvaluation
import com.cutm.nt14.gateway.core.RateLimiter
import com.cutm.nt14.gateway.core.WebSocketManager
import com.cutm.nt14.gateway.models.ApiMessage
import com.cutm.nt14.gateway.models.GatewayEvent
import com.cutm.nt14.gateway.models.HealthResponse
import com.cutm.nt14.gateway.routes.authRoutes
import com.cutm.nt14.gateway.routes.demoRoutes
import com.cutm.nt14.gateway.routes.eventsWebSocket
import com.cutm.nt14.gateway.routes.ruleRoutes
import com.cutm.nt14.gateway.routes.simulateRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.Principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration

data class ApiKeyPrincipal(val key: String) : Principal

fun main() {
    val logger = LoggerFactory.getLogger("GatewayServer")
    logger.info("Starting NT14 Rate Limiter Gateway on port 8000 (bind 0.0.0.0)...")

    embeddedServer(Netty, port = 8000, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val rateLimiter = RateLimiter()
    val webSocketManager = WebSocketManager()
    val jwtService = JwtService()

    // 1. Content Negotiation (JSON)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    // 2. WebSockets (hardened frame size to 64KB)
    install(WebSockets) {
        maxFrameSize = 65536L
        masking = false
    }

    // 3. Call Logging
    install(CallLogging)

    // 4. Control-plane Authentication Plugin
    install(Authentication) {
        provider("api-key") {
            authenticate { context ->
                val expectedApiKey = System.getenv("GATEWAY_API_KEY") ?: "dev-local-key"
                val apiKeyHeader = context.call.request.headers["X-API-Key"]
                val apiKeyQuery = context.call.request.queryParameters["api_key"]
                val key = apiKeyHeader ?: apiKeyQuery

                if (key == expectedApiKey) {
                    context.principal(ApiKeyPrincipal(expectedApiKey))
                } else {
                    context.challenge("api-key", AuthenticationFailedCause.InvalidCredentials) { challenge, call ->
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiMessage("Unauthorized: Invalid or missing API Key. Provide via X-API-Key header.")
                        )
                        challenge.complete()
                    }
                }
            }
        }
    }

    // 5. Rate Limiting Gateway Interceptor (intercepting /api/* demo routes)
    intercept(ApplicationCallPipeline.Plugins) {
        val path = call.request.path()

        // Apply rate limiting to demo API routes, excluding control-plane and auth endpoints
        if (path.startsWith("/api/") && !path.startsWith("/api/rules") && !path.startsWith("/api/simulate") && !path.startsWith("/api/auth")) {
            val startTime = System.currentTimeMillis()

            // Resolve identity from JWT Bearer token if present
            val authHeader = call.request.headers["Authorization"]
            val bearerToken = authHeader?.removePrefix("Bearer ")?.trim()
                ?: call.request.queryParameters["token"]

            val jwtClaims = bearerToken?.let { jwtService.verifyToken(it) }
            val clientId = if (jwtClaims != null) "user:${jwtClaims.email}" else rateLimiter.resolveClientId(call)

            // Evaluate rate limit: ADMIN role with valid JWT gets privileged headroom
            val evaluation = if (jwtClaims?.role == "ADMIN") {
                RateLimitEvaluation(
                    allowed = true,
                    limit = 1000,
                    remaining = 999,
                    resetSeconds = 60,
                    retryAfterSeconds = 0,
                    action = "ALLOW"
                )
            } else {
                rateLimiter.evaluate(path, clientId)
            }

            rateLimiter.appendHeaders(call, evaluation)

            if (jwtClaims != null) {
                call.response.header("X-Authenticated-User", jwtClaims.email)
                call.response.header("X-Authenticated-Role", jwtClaims.role)
            }

            if (!evaluation.allowed) {
                val latency = System.currentTimeMillis() - startTime
                val eventType = if (evaluation.action == "BLOCK") "ip_blocked" else "blocked_request"

                webSocketManager.broadcast(
                    GatewayEvent(
                        type = eventType,
                        ip = clientId,
                        endpoint = path,
                        status = 429,
                        latencyMs = latency.coerceAtLeast(1L),
                        timestamp = System.currentTimeMillis() / 1000.0
                    )
                )

                call.respond(
                    HttpStatusCode.TooManyRequests,
                    ApiMessage("Rate limit exceeded for $clientId. Action: ${evaluation.action}. Retry after ${evaluation.retryAfterSeconds}s")
                )
                finish()
                return@intercept
            }

            // Proceed to the endpoint handler
            proceed()

            // After execution: emit telemetry for successful call
            val latency = System.currentTimeMillis() - startTime
            val status = call.response.status()?.value ?: 200

            webSocketManager.broadcast(
                GatewayEvent(
                    type = "request",
                    ip = clientId,
                    endpoint = path,
                    status = status,
                    latencyMs = latency.coerceAtLeast(1L),
                    timestamp = System.currentTimeMillis() / 1000.0
                )
            )
            return@intercept
        }

        proceed()
    }

    // 6. Routing Assembly
    routing {
        get("/") {
            call.respond(ApiMessage("NT14 Rate Limiter Gateway (Kotlin/Ktor) running on port 8000"))
        }

        get("/health") {
            call.respond(HealthResponse("UP", webSocketManager.activeSubscriberCount()))
        }

        authRoutes(jwtService)
        demoRoutes()
        ruleRoutes(rateLimiter)
        simulateRoutes(rateLimiter, webSocketManager)
        eventsWebSocket(webSocketManager)
    }
}
