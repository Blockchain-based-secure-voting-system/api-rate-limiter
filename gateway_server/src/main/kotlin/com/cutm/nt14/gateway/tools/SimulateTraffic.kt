package com.cutm.nt14.gateway.tools

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Standalone CLI Traffic Generator & Live Telemetry Inspector.
 *
 * Supported modes:
 * - normal: Simulates gentle baseline client traffic within limits.
 * - attack / burst: Blasts concurrent requests to exceed quota and trigger 429 Too Many Requests.
 * - listen: Connects to ws://localhost:8000/ws/events?api_key=dev-local-key and prints raw telemetry events.
 * - all: Runs a listener in the background, fires normal traffic, then triggers a burst.
 */
fun main(args: Array<String>) = runBlocking {
    val logger = LoggerFactory.getLogger("SimulateTraffic")
    val mode = args.getOrNull(0) ?: "all"
    val baseUrl = "http://127.0.0.1:8000"
    val wsUrl = "ws://127.0.0.1:8000/ws/events?api_key=dev-local-key"

    println("================================================================")
    println(" NT14 RATE LIMITER — CLI TEST RUNNER & SIMULATOR")
    println(" Mode: $mode | Target: $baseUrl")
    println("================================================================")

    val client = HttpClient(CIO) {
        install(WebSockets)
    }

    try {
        when (mode.lowercase()) {
            "listen" -> runListener(client, wsUrl)
            "normal" -> runNormalTraffic(client, baseUrl, count = 5)
            "attack", "burst" -> runBurstTraffic(client, baseUrl, burstCount = 20)
            "all" -> {
                // Launch background WebSocket listener
                val listenerJob = launch {
                    try {
                        client.webSocket(wsUrl) {
                            println("[WS-CLIENT] Connected to real-time telemetry stream.")
                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    println("[WS-EVENT] ${frame.readText()}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("[WS-CLIENT] Stream closed: ${e.message}")
                    }
                }

                delay(500) // Allow WebSocket handshake to complete

                println("\n>>> Step 1: Sending Normal Requests (within quota) <<<")
                runNormalTraffic(client, baseUrl, count = 4)

                delay(1000)

                println("\n>>> Step 2: Triggering High-Frequency Burst (exceeding burst limit) <<<")
                runBurstTraffic(client, baseUrl, burstCount = 18)

                delay(2000)
                listenerJob.cancel()
            }
            else -> {
                println("Unknown mode '$mode'. Valid options: normal | burst | listen | all")
            }
        }
    } finally {
        client.close()
        println("\nSimulation complete.")
    }
}

suspend fun runListener(client: HttpClient, wsUrl: String) {
    println("Connecting to telemetry WebSocket: $wsUrl ...")
    client.webSocket(wsUrl) {
        println("Connected. Listening for live Gateway events (Ctrl+C to stop)...")
        for (frame in incoming) {
            if (frame is Frame.Text) {
                println("[TELEMETRY-EVENT] ${frame.readText()}")
            }
        }
    }
}

suspend fun runNormalTraffic(client: HttpClient, baseUrl: String, count: Int) {
    val endpoint = "/api/users"
    val clientIp = "192.168.1.50"

    repeat(count) { i ->
        val start = System.currentTimeMillis()
        val response = client.get("$baseUrl$endpoint") {
            header("X-Forwarded-For", clientIp)
        }
        val latency = System.currentTimeMillis() - start

        val limit = response.headers["X-RateLimit-Limit"] ?: "N/A"
        val remaining = response.headers["X-RateLimit-Remaining"] ?: "N/A"
        val reset = response.headers["X-RateLimit-Reset"] ?: "N/A"

        println(
            "[REQ #${i + 1}] GET $endpoint -> ${response.status} in ${latency}ms | " +
            "Limit: $limit, Remaining: $remaining, Reset: ${reset}s"
        )
        delay(300)
    }
}

suspend fun runBurstTraffic(client: HttpClient, baseUrl: String, burstCount: Int) = runBlocking {
    val endpoint = "/api/users"
    val victimIp = "10.0.0.99" // Same client IP to exhaust burst limit

    println("Firing $burstCount rapid concurrent requests from IP $victimIp ...")

    val jobs = (1..burstCount).map { i ->
        async {
            val response = client.get("$baseUrl$endpoint") {
                header("X-Forwarded-For", victimIp)
            }
            val limit = response.headers["X-RateLimit-Limit"] ?: "N/A"
            val remaining = response.headers["X-RateLimit-Remaining"] ?: "N/A"
            val retryAfter = response.headers["Retry-After"] ?: "None"

            val statusStr = if (response.status == HttpStatusCode.TooManyRequests) {
                "429 TOO MANY REQUESTS (Throttled!)"
            } else {
                "${response.status} (Allowed)"
            }

            println("[BURST #$i] Status: $statusStr | Remaining: $remaining | Retry-After: $retryAfter")
            response.status
        }
    }

    val results = jobs.awaitAll()
    val allowedCount = results.count { it == HttpStatusCode.OK }
    val throttledCount = results.count { it == HttpStatusCode.TooManyRequests }

    println("\nBurst Summary:")
    println("  Total:     $burstCount")
    println("  Allowed:   $allowedCount")
    println("  Throttled: $throttledCount")
}
