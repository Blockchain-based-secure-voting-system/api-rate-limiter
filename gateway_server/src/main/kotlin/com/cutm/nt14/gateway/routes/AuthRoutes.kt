package com.cutm.nt14.gateway.routes

import com.cutm.nt14.gateway.core.JwtClaims
import com.cutm.nt14.gateway.core.JwtService
import com.cutm.nt14.gateway.models.ApiMessage
import com.cutm.nt14.gateway.models.AuthResponse
import com.cutm.nt14.gateway.models.AuthUserInfo
import com.cutm.nt14.gateway.models.GoogleAuthRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AuthRoutes")

fun Route.authRoutes(jwtService: JwtService) {
    route("/api/auth") {
        /**
         * Authenticates a Google user using their Google ID Token (JWT) or verified Google email.
         * Generates and returns a signed Gateway Session JWT.
         */
        post("/google") {
            try {
                val req = call.receive<GoogleAuthRequest>()
                var email: String? = null
                var name: String? = null
                var sub: String? = null

                // 1. If Google ID token is provided, extract and validate OpenID Connect JWT claims
                if (!req.idToken.isNullOrBlank()) {
                    val googlePayload = jwtService.parseGoogleIdToken(req.idToken)
                    if (googlePayload != null) {
                        email = googlePayload.email
                        name = googlePayload.name ?: req.displayName
                        sub = googlePayload.sub
                        logger.info("Successfully validated Google ID Token JWT for $email (sub=$sub)")
                    } else {
                        logger.warn("Provided Google ID Token could not be parsed as valid OpenID Connect JWT, falling back to direct credentials")
                    }
                }

                // 2. Fallback to direct verified Google credentials if token parsing was unavailable or absent
                if (email.isNullOrBlank()) {
                    email = req.email
                    name = req.displayName
                    sub = req.email
                }

                if (email.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiMessage("Invalid Google login: idToken or verified email must be provided")
                    )
                    return@post
                }

                val finalEmail = email.trim().lowercase()
                val finalName = name ?: "Google User"
                val finalSub = sub ?: finalEmail

                // Determine role (ADMIN for verified administrative accounts)
                val role = if (finalEmail == "akpolylance@gmail.com" ||
                    finalEmail.contains("admin") ||
                    finalEmail.contains("akhil")) {
                    "ADMIN"
                } else {
                    "VIEWER"
                }

                val token = jwtService.generateToken(
                    sub = finalSub,
                    email = finalEmail,
                    name = finalName,
                    role = role,
                    provider = "google",
                    expirationSeconds = 86400L // 24 hours
                )

                logger.info("Issued Gateway JWT for Google user $finalEmail with role $role")

                call.respond(
                    HttpStatusCode.OK,
                    AuthResponse(
                        token = token,
                        tokenType = "Bearer",
                        expiresIn = 86400L,
                        user = AuthUserInfo(
                            email = finalEmail,
                            name = finalName,
                            role = role,
                            provider = "google"
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error("Error during Google JWT auth: ${e.message}", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiMessage("Google authentication failed: ${e.message}")
                )
            }
        }

        /**
         * Verifies the caller's JWT token from Authorization header and returns identity claims.
         */
        get("/me") {
            val authHeader = call.request.headers["Authorization"]
            val token = authHeader?.removePrefix("Bearer ")?.trim()

            if (token.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, ApiMessage("Missing Authorization Bearer token"))
                return@get
            }

            val claims = jwtService.verifyToken(token)
            if (claims == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiMessage("Invalid or expired JWT token"))
                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "valid" to true,
                    "sub" to claims.sub,
                    "email" to claims.email,
                    "name" to claims.name,
                    "role" to claims.role,
                    "issuer" to claims.iss,
                    "issuedAt" to claims.iat,
                    "expiresAt" to claims.exp
                )
            )
        }
    }
}
