package com.cutm.nt14.gateway

import com.cutm.nt14.gateway.core.JwtService
import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class JwtServiceTest {

    private val jwtService = JwtService("test-secret-key-1234567890-test-secret-key")

    @Test
    fun testGenerateAndVerifyToken() {
        val token = jwtService.generateToken(
            sub = "123456789",
            email = "akpolylance@gmail.com",
            name = "Akhil Muvva",
            role = "ADMIN",
            provider = "google",
            expirationSeconds = 3600
        )

        assertNotNull(token)
        val parts = token.split(".")
        assertEquals(3, parts.size)

        val claims = jwtService.verifyToken(token)
        assertNotNull(claims)
        assertEquals("123456789", claims!!.sub)
        assertEquals("akpolylance@gmail.com", claims.email)
        assertEquals("Akhil Muvva", claims.name)
        assertEquals("ADMIN", claims.role)
        assertEquals("nt14-gateway", claims.iss)
        assertEquals("google", claims.provider)
    }

    @Test
    fun testTamperedTokenRejected() {
        val token = jwtService.generateToken(
            sub = "user1",
            email = "user@test.com",
            name = "User",
            role = "VIEWER"
        )

        val parts = token.split(".")
        // Tamper with payload (change VIEWER to ADMIN)
        val tamperedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"sub":"user1","email":"user@test.com","name":"User","role":"ADMIN","iat":1000,"exp":9999999999,"iss":"nt14-gateway","provider":"google"}""".toByteArray(StandardCharsets.UTF_8)
        )
        val tamperedToken = "${parts[0]}.$tamperedPayload.${parts[2]}"

        val claims = jwtService.verifyToken(tamperedToken)
        assertNull("Tampered token must be rejected", claims)
    }

    @Test
    fun testExpiredTokenRejected() {
        val expiredToken = jwtService.generateToken(
            sub = "user1",
            email = "user@test.com",
            name = "User",
            role = "VIEWER",
            expirationSeconds = -10 // expired 10s ago
        )

        val claims = jwtService.verifyToken(expiredToken)
        assertNull("Expired token must be rejected", claims)
    }

    @Test
    fun testParseGoogleIdToken() {
        // Construct mock Google ID token (OIDC JWT)
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"RS256","kid":"google-kid-1"}""".toByteArray(StandardCharsets.UTF_8))
        val exp = (System.currentTimeMillis() / 1000) + 3600
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"iss":"https://accounts.google.com","sub":"google-sub-999","email":"google.user@gmail.com","name":"Google Tester","exp":$exp,"aud":"test-client-id"}""".toByteArray(StandardCharsets.UTF_8)
        )
        val sig = Base64.getUrlEncoder().withoutPadding().encodeToString("mock-sig".toByteArray(StandardCharsets.UTF_8))
        val mockGoogleJwt = "$header.$payload.$sig"

        val parsed = jwtService.parseGoogleIdToken(mockGoogleJwt)
        assertNotNull(parsed)
        assertEquals("google-sub-999", parsed!!.sub)
        assertEquals("google.user@gmail.com", parsed.email)
        assertEquals("Google Tester", parsed.name)
    }
}
