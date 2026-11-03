package com.cutm.nt14.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.cutm.nt14.data.local.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class EndpointType {
    LOCAL,
    CLOUD
}

sealed class ResolverState {
    data class Probing(val candidate: String) : ResolverState()
    data class WakingCloud(val url: String) : ResolverState()
    data class Connected(val url: String, val type: EndpointType) : ResolverState()
    data class Unreachable(val lastError: String) : ResolverState()
}

data class GatewayCandidate(
    val host: String,
    val type: EndpointType,
    val isEmulatorOnly: Boolean = false
)

@Singleton
class GatewayEndpointResolver private constructor(
    private val context: Context?,
    private val sessionManager: SessionManager?,
    @Suppress("UNUSED_PARAMETER") marker: Any?
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ) : this(context, sessionManager, null)

    constructor(
        context: Context? = null,
        sessionManager: SessionManager? = null,
        isTest: Boolean = true
    ) : this(context, sessionManager, Unit)
    private val tag = "EndpointResolver"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CLOUD_DEFAULT_URL = "https://api-rate-limiter-gateway-qgfy.onrender.com"
        const val LOCAL_USB_HOST = "127.0.0.1:8000"
        const val EMULATOR_HOST = "10.0.2.2:8000"
        const val LOCAL_PROBE_TIMEOUT_MS = 1500L
        const val CLOUD_PROBE_TIMEOUT_MS = 60000L
    }

    private val _state = MutableStateFlow<ResolverState>(ResolverState.Probing("Init"))
    val state: StateFlow<ResolverState> = _state.asStateFlow()

    private val _currentEndpoint = MutableStateFlow(CLOUD_DEFAULT_URL)
    val currentEndpoint: StateFlow<String> = _currentEndpoint.asStateFlow()

    private val _endpointType = MutableStateFlow(EndpointType.CLOUD)
    val endpointType: StateFlow<EndpointType> = _endpointType.asStateFlow()

    // Test overrides for emulator detection, HTTP probing, and preferences
    var isEmulatorProvider: () -> Boolean = { isEmulatorDevice() }
    var probeOverride: (suspend (url: String, timeoutMs: Long) -> Boolean)? = null
    var selectionModeProvider: suspend () -> String = {
        try { sessionManager?.selectionMode?.first() ?: "AUTO" } catch (e: Exception) { "AUTO" }
    }
    var lanHostProvider: suspend () -> String = {
        try { sessionManager?.lanHost?.first() ?: "" } catch (e: Exception) { "" }
    }
    var manualHostProvider: suspend () -> String = {
        try { sessionManager?.manualHost?.first() ?: "" } catch (e: Exception) { "" }
    }
    var onSaveResolvedHost: suspend (String) -> Unit = { host ->
        try { sessionManager?.saveResolvedHost(host) } catch (e: Exception) { }
    }

    private var activeResolveJob: Job? = null
    private var localPollJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        if (marker == null) {
            scope.launch {
                try {
                    val rawSaved = sessionManager?.resolvedHost?.first() ?: CLOUD_DEFAULT_URL
                    val savedResolved = if (rawSaved.contains("api-rate-limiter-gateway-n0ab.onrender.com")) CLOUD_DEFAULT_URL else rawSaved
                    _currentEndpoint.value = savedResolved
                    _endpointType.value = if (isLocalHost(savedResolved)) EndpointType.LOCAL else EndpointType.CLOUD
                    _state.value = ResolverState.Connected(savedResolved, _endpointType.value)
                } catch (e: Exception) {
                    _currentEndpoint.value = CLOUD_DEFAULT_URL
                    _endpointType.value = EndpointType.CLOUD
                    _state.value = ResolverState.Connected(CLOUD_DEFAULT_URL, EndpointType.CLOUD)
                }

                registerNetworkMonitor()
                startPeriodicLocalCheck()
                resolve(force = true)
            }
        }
    }

    fun isLocalHost(urlOrHost: String): Boolean {
        val clean = cleanHost(urlOrHost)
        return clean.startsWith("127.0.0.1") ||
                clean.startsWith("localhost") ||
                clean.startsWith("10.0.2.2") ||
                clean.startsWith("192.168.") ||
                clean.startsWith("10.") ||
                clean.startsWith("172.16.") ||
                clean.startsWith("172.17.") ||
                clean.startsWith("172.18.") ||
                clean.startsWith("172.19.") ||
                clean.startsWith("172.20.") ||
                clean.startsWith("172.21.") ||
                clean.startsWith("172.22.") ||
                clean.startsWith("172.23.") ||
                clean.startsWith("172.24.") ||
                clean.startsWith("172.25.") ||
                clean.startsWith("172.26.") ||
                clean.startsWith("172.27.") ||
                clean.startsWith("172.28.") ||
                clean.startsWith("172.29.") ||
                clean.startsWith("172.30.") ||
                clean.startsWith("172.31.")
    }

    fun cleanHost(raw: String): String {
        return raw.removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("ws://")
            .removePrefix("wss://")
            .trim()
            .trimEnd('/')
    }

    fun normalizeUrl(raw: String): String {
        val clean = cleanHost(raw)
        return if (isLocalHost(clean)) {
            "http://$clean"
        } else {
            if (raw.startsWith("http://")) raw.trimEnd('/') else "https://$clean"
        }
    }

    suspend fun getCandidateList(): List<GatewayCandidate> {
        val candidates = mutableListOf<GatewayCandidate>()

        // 1. Local USB via adb reverse
        candidates.add(GatewayCandidate(LOCAL_USB_HOST, EndpointType.LOCAL))

        // 2. Emulator loopback (only on emulators)
        if (isEmulatorProvider()) {
            candidates.add(GatewayCandidate(EMULATOR_HOST, EndpointType.LOCAL, isEmulatorOnly = true))
        }

        // 3. Saved LAN host from Host dialog (if present)
        val lan = lanHostProvider().trim()
        if (lan.isNotBlank() && cleanHost(lan) != LOCAL_USB_HOST && cleanHost(lan) != EMULATOR_HOST) {
            candidates.add(GatewayCandidate(lan, EndpointType.LOCAL))
        }

        // 4. Cloud gateway fallback
        candidates.add(GatewayCandidate(CLOUD_DEFAULT_URL, EndpointType.CLOUD))

        return candidates
    }

    fun resolve(force: Boolean = false) {
        activeResolveJob?.cancel()
        activeResolveJob = scope.launch {
            val mode = selectionModeProvider()
            if (mode.equals("MANUAL", ignoreCase = true)) {
                val manual = manualHostProvider().trim()
                val normalized = normalizeUrl(if (manual.isNotBlank()) manual else LOCAL_USB_HOST)
                val type = if (isLocalHost(normalized)) EndpointType.LOCAL else EndpointType.CLOUD
                _state.value = ResolverState.Probing(normalized)
                val ok = probeEndpoint(normalized, if (type == EndpointType.LOCAL) LOCAL_PROBE_TIMEOUT_MS else CLOUD_PROBE_TIMEOUT_MS)
                if (ok) {
                    applyEndpoint(normalized, type)
                } else {
                    _state.value = ResolverState.Unreachable("Manual gateway at $normalized unreachable")
                }
                return@launch
            }

            val candidates = getCandidateList()
            for (candidate in candidates) {
                val normalized = normalizeUrl(candidate.host)
                if (candidate.type == EndpointType.LOCAL) {
                    _state.value = ResolverState.Probing(candidate.host)
                    val ok = probeEndpoint(normalized, LOCAL_PROBE_TIMEOUT_MS)
                    if (ok) {
                        Log.i(tag, "Selected LOCAL gateway: $normalized")
                        applyEndpoint(normalized, EndpointType.LOCAL)
                        return@launch
                    }
                } else {
                    // Cloud fallback with wake retries up to 60s
                    _state.value = ResolverState.WakingCloud(normalized)
                    Log.i(tag, "Local gateways unavailable. Probing Cloud gateway: $normalized...")
                    val ok = probeCloudWithRetries(normalized, CLOUD_PROBE_TIMEOUT_MS)
                    if (ok) {
                        Log.i(tag, "Selected CLOUD gateway: $normalized")
                        applyEndpoint(normalized, EndpointType.CLOUD)
                        return@launch
                    }
                }
            }

            _state.value = ResolverState.Unreachable("All gateway endpoints unreachable")
        }
    }

    private suspend fun applyEndpoint(url: String, type: EndpointType) {
        _currentEndpoint.value = url
        _endpointType.value = type
        _state.value = ResolverState.Connected(url, type)
        onSaveResolvedHost(url)
    }

    fun reportFailure(failedUrl: String) {
        val current = _currentEndpoint.value
        if (cleanHost(failedUrl) == cleanHost(current)) {
            Log.w(tag, "Reported failure on current active endpoint: $failedUrl. Triggering re-resolution failover...")
            resolve(force = true)
        }
    }

    suspend fun probeEndpoint(url: String, timeoutMs: Long): Boolean {
        probeOverride?.let { return it(url, timeoutMs) }

        val healthUrl = "${url.trimEnd('/')}/health"
        val client = OkHttpClient.Builder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()

        return try {
            val req = Request.Builder().url(healthUrl).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return false
                val json = JSONObject(body)
                val status = json.optString("status")
                val hasMetrics = json.has("metrics") && !json.isNull("metrics")
                status.equals("UP", ignoreCase = true) && hasMetrics
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun probeCloudWithRetries(url: String, totalTimeoutMs: Long): Boolean {
        probeOverride?.let { return it(url, totalTimeoutMs) }

        val startTime = System.currentTimeMillis()
        var attempt = 0
        while (System.currentTimeMillis() - startTime < totalTimeoutMs) {
            attempt++
            val ok = probeEndpoint(url, 10000L) // 10s per try
            if (ok) return true
            delay(2500L)
        }
        return false
    }

    private fun registerNetworkMonitor() {
        try {
            val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.i(tag, "Network available. Re-checking gateway endpoints...")
                    resolve()
                }

                override fun onLost(network: Network) {
                    Log.i(tag, "Network lost. Re-checking gateway endpoints...")
                    resolve()
                }
            }
            cm.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.w(tag, "Failed to register network monitor: ${e.message}")
        }
    }

    private fun startPeriodicLocalCheck() {
        localPollJob?.cancel()
        localPollJob = scope.launch {
            while (isActive) {
                delay(20000L) // check every 20s
                // If currently connected to cloud and in AUTO mode, check if a local gateway has appeared
                val mode = selectionModeProvider()
                if (mode.equals("AUTO", ignoreCase = true) && _endpointType.value == EndpointType.CLOUD) {
                    val localCandidates = getCandidateList().filter { it.type == EndpointType.LOCAL }
                    for (cand in localCandidates) {
                        val norm = normalizeUrl(cand.host)
                        val ok = probeEndpoint(norm, LOCAL_PROBE_TIMEOUT_MS)
                        if (ok) {
                            Log.i(tag, "Local gateway appeared at $norm! Switching from CLOUD to LOCAL.")
                            applyEndpoint(norm, EndpointType.LOCAL)
                            break
                        }
                    }
                }
            }
        }
    }

    private fun isEmulatorDevice(): Boolean {
        val fp = Build.FINGERPRINT ?: return false
        val model = Build.MODEL ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        val hardware = Build.HARDWARE ?: ""
        val product = Build.PRODUCT ?: ""
        return (fp.startsWith("generic")
                || fp.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || manufacturer.contains("Genymotion")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || product.contains("sdk_gphone")
                || product.contains("google_sdk")
                || product.contains("sdk")
                || product.contains("sdk_x86")
                || product.contains("vbox86p")
                || product.contains("emulator"))
    }
}
