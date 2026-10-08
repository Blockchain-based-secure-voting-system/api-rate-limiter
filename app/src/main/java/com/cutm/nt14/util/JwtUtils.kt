package com.cutm.nt14.util

import com.cutm.nt14.domain.model.UserRole
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class DecodedJwt(
    val header: Map<String, Any?>,
    val payload: Map<String, Any?>,
    val sub: String?,
    val email: String?,
    val name: String?,
    val role: String?,
    val exp: Long,
    val isExpired: Boolean,
    val issuer: String?
)

object JwtUtils {

    private const val CLIENT_SECRET = "nt14-gateway-secure-production-jwt-secret-key-2026"

    /**
     * Decodes and parses an RFC 7519 JSON Web Token without requiring network.
     * Works for both Google OpenID Connect ID Tokens and NT14 Gateway Session JWTs.
     */
    fun decodeToken(token: String?): DecodedJwt? {
        if (token.isNullOrBlank()) return null
        val parts = token.trim().split(".")
        if (parts.size < 2) return null

        return try {
            val headerJson = String(base64UrlDecode(parts[0]), StandardCharsets.UTF_8)
            val payloadJson = String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8)

            val headerObj = JSONObject(headerJson)
            val payloadObj = JSONObject(payloadJson)

            val headerMap = mutableMapOf<String, Any?>()
            headerObj.keys().forEach { k -> headerMap[k] = headerObj.opt(k) }

            val payloadMap = mutableMapOf<String, Any?>()
            payloadObj.keys().forEach { k -> payloadMap[k] = payloadObj.opt(k) }

            val sub = payloadObj.optString("sub").takeIf { it.isNotBlank() }
            val email = payloadObj.optString("email").takeIf { it.isNotBlank() }
            val name = payloadObj.optString("name").takeIf { it.isNotBlank() }
            val role = payloadObj.optString("role").takeIf { it.isNotBlank() }
            val exp = payloadObj.optLong("exp", 0L)
            val iss = payloadObj.optString("iss").takeIf { it.isNotBlank() }

            val now = System.currentTimeMillis() / 1000
            val isExpired = exp in 1 until now

            DecodedJwt(
                header = headerMap,
                payload = payloadMap,
                sub = sub,
                email = email,
                name = name,
                role = role,
                exp = exp,
                isExpired = isExpired,
                issuer = iss
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a local fallback client session JWT (HS256) when the gateway server is temporarily unreachable.
     */
    fun generateLocalClientSessionToken(
        email: String,
        displayName: String,
        role: UserRole,
        validitySeconds: Long = 86400L
    ): String {
        val now = System.currentTimeMillis() / 1000
        val exp = now + validitySeconds

        val headerJson = """{"alg":"HS256","typ":"JWT"}"""
        val payload = JSONObject().apply {
            put("sub", email)
            put("email", email)
            put("name", displayName)
            put("role", role.name)
            put("iat", now)
            put("exp", exp)
            put("iss", "nt14-android-client")
            put("provider", "google")
        }.toString()

        val headerB64 = base64UrlEncode(headerJson.toByteArray(StandardCharsets.UTF_8))
        val payloadB64 = base64UrlEncode(payload.toByteArray(StandardCharsets.UTF_8))
        val signatureB64 = signHmacSha256("$headerB64.$payloadB64", CLIENT_SECRET)

        return "$headerB64.$payloadB64.$signatureB64"
    }

    /**
     * Formats a token preview string (e.g., "eyJhbGci...x8F9a") for UI display.
     */
    fun formatTokenPreview(token: String?): String {
        if (token.isNullOrBlank()) return "No JWT Token"
        val trimmed = token.trim()
        if (trimmed.length <= 18) return trimmed
        val prefix = trimmed.take(10)
        val suffix = trimmed.takeLast(6)
        return "$prefix...$suffix"
    }

    private fun signHmacSha256(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val signed = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return base64UrlEncode(signed)
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun base64UrlDecode(str: String): ByteArray {
        return Base64.getUrlDecoder().decode(str)
    }
}
