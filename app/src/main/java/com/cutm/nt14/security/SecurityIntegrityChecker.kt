package com.cutm.nt14.security

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Debug
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Report containing real-time runtime security, anti-tamper, and anti-proxy telemetry.
 */
data class SecurityIntegrityReport(
    val isRooted: Boolean,
    val isProxyDetected: Boolean,
    val isFridaDetected: Boolean,
    val isDebuggerAttached: Boolean,
    val rootIndicators: List<String>,
    val proxyIndicators: List<String>,
    val reverseEngineeringIndicators: List<String>
) {
    val isCompromised: Boolean
        get() = isRooted || isProxyDetected || isFridaDetected
}

@Singleton
class SecurityIntegrityChecker @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val suPaths = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    /**
     * Conducts a full sweep of the runtime environment for root binaries, MitM proxies,
     * Frida/Xposed hooks, and attached debuggers.
     */
    fun checkIntegrity(): SecurityIntegrityReport {
        val rootIndicators = mutableListOf<String>()
        val proxyIndicators = mutableListOf<String>()
        val reIndicators = mutableListOf<String>()

        // 1. Root / Jailbreak Detection
        for (path in suPaths) {
            try {
                if (File(path).exists()) {
                    rootIndicators.add("SU binary found at $path")
                }
            } catch (_: Exception) {}
        }

        if (Build.TAGS != null && Build.TAGS.contains("test-keys")) {
            rootIndicators.add("OS build has test-keys")
        }

        // 2. HTTP/HTTPS MitM Proxy Detection
        val httpHost = System.getProperty("http.proxyHost")
        val httpPort = System.getProperty("http.proxyPort")
        if (!httpHost.isNullOrBlank()) {
            proxyIndicators.add("HTTP proxy configured: $httpHost:$httpPort")
        }

        val httpsHost = System.getProperty("https.proxyHost")
        val httpsPort = System.getProperty("https.proxyPort")
        if (!httpsHost.isNullOrBlank()) {
            proxyIndicators.add("HTTPS proxy configured: $httpsHost:$httpsPort")
        }

        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val defaultProxy = cm?.defaultProxy
            if (defaultProxy != null && !defaultProxy.host.isNullOrBlank()) {
                proxyIndicators.add("System default proxy active: ${defaultProxy.host}:${defaultProxy.port}")
            }
        } catch (_: Exception) {}

        // 3. Reverse Engineering & Hooking Detection (Frida, Xposed, Debugger)
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            reIndicators.add("Active debugger attached to process")
        }

        // Check if Frida server default port is actively listening on localhost
        if (isPortListening("127.0.0.1", 27042)) {
            reIndicators.add("Frida server port 27042 open on localhost")
        }

        // Inspect memory maps for injected hooking libraries
        try {
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists()) {
                BufferedReader(FileReader(mapsFile)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val lower = line?.lowercase() ?: continue
                        if (lower.contains("frida") || lower.contains("gadget")) {
                            reIndicators.add("Frida instrumentation library mapped in process")
                            break
                        }
                        if (lower.contains("xposed") || lower.contains("edxposed") || lower.contains("substrate")) {
                            reIndicators.add("Xposed/Substrate hooking framework mapped in process")
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return SecurityIntegrityReport(
            isRooted = rootIndicators.isNotEmpty(),
            isProxyDetected = proxyIndicators.isNotEmpty(),
            isFridaDetected = reIndicators.any { it.contains("Frida", ignoreCase = true) || it.contains("Xposed", ignoreCase = true) },
            isDebuggerAttached = reIndicators.any { it.contains("debugger", ignoreCase = true) },
            rootIndicators = rootIndicators,
            proxyIndicators = proxyIndicators,
            reverseEngineeringIndicators = reIndicators
        )
    }

    private fun isPortListening(host: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 120)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
