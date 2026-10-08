package com.cutm.nt14.util

import com.cutm.nt14.domain.model.UserRole
import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class JwtUtilsTest {

    @Test
    fun testGenerateAndDecodeClientToken() {
        val email = "akpolylance@gmail.com"
        val name = "Akhil Muvva"
        val token = JwtUtils.generateLocalClientSessionToken(email, name, UserRole.ADMIN, 3600)

        assertNotNull(token)
        val decoded = JwtUtils.decodeToken(token)

        assertNotNull(decoded)
        assertEquals(email, decoded!!.email)
        assertEquals(email, decoded.sub)
        assertEquals(name, decoded.name)
        assertEquals("ADMIN", decoded.role)
        assertEquals("nt14-android-client", decoded.issuer)
        assertFalse(decoded.isExpired)
    }

    @Test
    fun testDecodeSimulatedGoogleOidcToken() {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"RS256","kid":"google-kid-123"}""".toByteArray(StandardCharsets.UTF_8))
        val exp = (System.currentTimeMillis() / 1000) + 7200
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"iss":"https://accounts.google.com","sub":"google-oidc-sub-555","email":"admin.google@gmail.com","name":"Google Admin User","exp":$exp}""".toByteArray(StandardCharsets.UTF_8)
        )
        val sig = Base64.getUrlEncoder().withoutPadding().encodeToString("google-rsa-signature".toByteArray(StandardCharsets.UTF_8))
        val token = "$header.$payload.$sig"

        val decoded = JwtUtils.decodeToken(token)
        assertNotNull(decoded)
        assertEquals("admin.google@gmail.com", decoded!!.email)
        assertEquals("google-oidc-sub-555", decoded.sub)
        assertEquals("Google Admin User", decoded.name)
        assertEquals("https://accounts.google.com", decoded.issuer)
        assertFalse(decoded.isExpired)
    }

    @Test
    fun testExpiredTokenDetection() {
        val token = JwtUtils.generateLocalClientSessionToken("user@test.com", "User", UserRole.VIEWER, -60)
        val decoded = JwtUtils.decodeToken(token)

        assertNotNull(decoded)
        assertTrue(decoded!!.isExpired)
    }

    @Test
    fun testFormatTokenPreview() {
        assertEquals("No JWT Token", JwtUtils.formatTokenPreview(null))
        assertEquals("No JWT Token", JwtUtils.formatTokenPreview(""))
        val longToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val preview = JwtUtils.formatTokenPreview(longToken)
        assertTrue(preview.startsWith("eyJhbGciOi..."))
        assertTrue(preview.endsWith("Qssw5c"))
    }
}
