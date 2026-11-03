package com.cutm.nt14.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GatewayEndpointResolverTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private suspend fun awaitResolution(resolver: GatewayEndpointResolver, expectedUrl: String? = null) {
        delay(20)
        var attempts = 0
        while (attempts < 150) {
            val state = resolver.state.value
            val settled = state is ResolverState.Connected || state is ResolverState.Unreachable
            val urlMatches = expectedUrl == null || resolver.currentEndpoint.value == expectedUrl
            if (settled && urlMatches) break
            delay(20)
            attempts++
        }
    }

    @Test
    fun testProbeEndpoint_validNt14Gateway_returnsTrue() = runBlocking {
        // Mock a real NT14 /health response with status: UP and metrics object
        val validHealthJson = """
            {
                "status": "UP",
                "subscribers": 2,
                "uptimeMs": 45000,
                "metrics": {
                    "totalRequests": 1420,
                    "allowedRequests": 1390,
                    "throttledRequests": 30,
                    "activeBans": 1
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(validHealthJson))

        val serverUrl = mockWebServer.url("").toString().trimEnd('/')
        val resolver = GatewayEndpointResolver(null, null)

        val result = resolver.probeEndpoint(serverUrl, 2000L)
        assertTrue("Probe must accept valid NT14 gateway with status UP + metrics", result)
    }

    @Test
    fun testProbeEndpoint_nonNt14Server_returnsFalse() = runBlocking {
        // 1. Port returns arbitrary JSON without metrics
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"UP","message":"Hello from random service"}""")
        )

        val serverUrl = mockWebServer.url("").toString().trimEnd('/')
        val resolver = GatewayEndpointResolver(null, null)

        val result1 = resolver.probeEndpoint(serverUrl, 2000L)
        assertFalse("Probe must reject servers without metrics object", result1)

        // 2. Port returns HTML (e.g., Apache / Nginx / Router login page)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("<html><body>Welcome to Localhost</body></html>")
        )

        val result2 = resolver.probeEndpoint(serverUrl, 2000L)
        assertFalse("Probe must reject non-JSON HTML payloads", result2)

        // 3. Port returns HTTP 404 or 500 error
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        val result3 = resolver.probeEndpoint(serverUrl, 2000L)
        assertFalse("Probe must reject 404 responses", result3)
    }

    @Test
    fun testResolverPicksLocalWhenHealthAnswers() = runBlocking {
        val resolver = GatewayEndpointResolver(null, null)
        resolver.isEmulatorProvider = { false }

        // Local 127.0.0.1:8000 answers UP + metrics
        resolver.probeOverride = { url, _ ->
            url.contains("127.0.0.1")
        }

        resolver.resolve(force = true)
        awaitResolution(resolver)

        assertEquals("http://127.0.0.1:8000", resolver.currentEndpoint.value)
        assertEquals(EndpointType.LOCAL, resolver.endpointType.value)
        assertTrue(resolver.state.value is ResolverState.Connected)
    }

    @Test
    fun testResolverFallsBackToCloudWhenLocalFails() = runBlocking {
        val resolver = GatewayEndpointResolver(null, null)
        resolver.isEmulatorProvider = { false }

        // Local fails, cloud succeeds
        resolver.probeOverride = { url, _ ->
            url.contains("onrender.com")
        }

        resolver.resolve(force = true)
        awaitResolution(resolver)

        assertEquals(GatewayEndpointResolver.CLOUD_DEFAULT_URL, resolver.currentEndpoint.value)
        assertEquals(EndpointType.CLOUD, resolver.endpointType.value)
        assertTrue(resolver.state.value is ResolverState.Connected)
    }

    @Test
    fun testResolverIgnoresNonNt14ServerOnPort8000() = runBlocking {
        val resolver = GatewayEndpointResolver(null, null)
        resolver.isEmulatorProvider = { false }

        // Local port 8000 answers but probe rejects it as non-NT14; Cloud succeeds
        resolver.probeOverride = { url, _ ->
            if (url.contains("127.0.0.1") || url.contains("8000")) {
                false // Simulated rejection: not UP + metrics
            } else {
                url.contains("onrender.com")
            }
        }

        resolver.resolve(force = true)
        awaitResolution(resolver)

        // Must reject port 8000 and select Cloud
        assertEquals(GatewayEndpointResolver.CLOUD_DEFAULT_URL, resolver.currentEndpoint.value)
        assertEquals(EndpointType.CLOUD, resolver.endpointType.value)
    }

    @Test
    fun testResolverReResolvesAfterFailureReport() = runBlocking {
        val resolver = GatewayEndpointResolver(null, null)
        resolver.isEmulatorProvider = { false }

        var localOnline = true
        resolver.probeOverride = { url, _ ->
            if (url.contains("127.0.0.1")) localOnline else url.contains("onrender.com")
        }

        // Step 1: Initial resolution selects local
        resolver.resolve(force = true)
        awaitResolution(resolver)
        assertEquals("http://127.0.0.1:8000", resolver.currentEndpoint.value)
        assertEquals(EndpointType.LOCAL, resolver.endpointType.value)

        // Step 2: Local goes offline, WebSocket fails and calls reportFailure()
        localOnline = false
        resolver.reportFailure("127.0.0.1:8000")
        awaitResolution(resolver, GatewayEndpointResolver.CLOUD_DEFAULT_URL)

        // Step 3: Must automatically failover to Cloud!
        assertEquals(GatewayEndpointResolver.CLOUD_DEFAULT_URL, resolver.currentEndpoint.value)
        assertEquals(EndpointType.CLOUD, resolver.endpointType.value)
    }

    @Test
    fun testManualModeDirectOverride() = runBlocking {
        val resolver = GatewayEndpointResolver(null, null)
        resolver.selectionModeProvider = { "MANUAL" }
        resolver.manualHostProvider = { "192.168.1.55:8000" }

        resolver.probeOverride = { url, _ ->
            url.contains("192.168.1.55")
        }

        resolver.resolve(force = true)
        awaitResolution(resolver)

        assertEquals("http://192.168.1.55:8000", resolver.currentEndpoint.value)
        assertEquals(EndpointType.LOCAL, resolver.endpointType.value)
    }
}
