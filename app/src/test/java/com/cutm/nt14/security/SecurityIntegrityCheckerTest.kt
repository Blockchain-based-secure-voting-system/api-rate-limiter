package com.cutm.nt14.security

import org.junit.Assert.*
import org.junit.Test

class SecurityIntegrityCheckerTest {

    @Test
    fun testSecurityIntegrityReport_cleanState() {
        val report = SecurityIntegrityReport(
            isRooted = false,
            isProxyDetected = false,
            isFridaDetected = false,
            isDebuggerAttached = false,
            rootIndicators = emptyList(),
            proxyIndicators = emptyList(),
            reverseEngineeringIndicators = emptyList()
        )

        assertFalse(report.isCompromised)
        assertFalse(report.isRooted)
        assertFalse(report.isProxyDetected)
        assertFalse(report.isFridaDetected)
    }

    @Test
    fun testSecurityIntegrityReport_proxyDetected_isCompromised() {
        val report = SecurityIntegrityReport(
            isRooted = false,
            isProxyDetected = true,
            isFridaDetected = false,
            isDebuggerAttached = false,
            rootIndicators = emptyList(),
            proxyIndicators = listOf("HTTP proxy configured: 127.0.0.1:8080"),
            reverseEngineeringIndicators = emptyList()
        )

        assertTrue(report.isCompromised)
        assertTrue(report.isProxyDetected)
        assertEquals(1, report.proxyIndicators.size)
    }

    @Test
    fun testSecurityIntegrityReport_rootDetected_isCompromised() {
        val report = SecurityIntegrityReport(
            isRooted = true,
            isProxyDetected = false,
            isFridaDetected = false,
            isDebuggerAttached = false,
            rootIndicators = listOf("SU binary found at /system/bin/su"),
            proxyIndicators = emptyList(),
            reverseEngineeringIndicators = emptyList()
        )

        assertTrue(report.isCompromised)
        assertTrue(report.isRooted)
    }

    @Test
    fun testSecurityIntegrityReport_fridaDetected_isCompromised() {
        val report = SecurityIntegrityReport(
            isRooted = false,
            isProxyDetected = false,
            isFridaDetected = true,
            isDebuggerAttached = false,
            rootIndicators = emptyList(),
            proxyIndicators = emptyList(),
            reverseEngineeringIndicators = listOf("Frida server port 27042 open on localhost")
        )

        assertTrue(report.isCompromised)
        assertTrue(report.isFridaDetected)
    }
}
