package com.cutm.nt14.gateway.routes

import com.cutm.nt14.gateway.core.RateLimiter
import com.cutm.nt14.gateway.models.ApiMessage
import com.cutm.nt14.gateway.models.RateLimitRule
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Control-plane rule management routes.
 * Implements Decision 4b: Gated behind authenticate("api-key").
 */
fun Route.ruleRoutes(rateLimiter: RateLimiter) {
    authenticate("api-key") {
        route("/api/rules") {
            // GET /api/rules -> list all active rate limit rules
            get {
                call.respond(rateLimiter.getAllRules())
            }

            // POST /api/rules -> create or update a rate limit rule
            post {
                val rule = call.receive<RateLimitRule>()
                rateLimiter.setRule(rule)
                call.respond(HttpStatusCode.Created, rule)
            }

            // DELETE /api/rules/{endpoint...} -> remove rate limit rule
            delete("{endpoint...}") {
                val endpointPath = "/" + (call.parameters.getAll("endpoint")?.joinToString("/") ?: "")
                val removed = rateLimiter.removeRule(endpointPath)
                if (removed) {
                    call.respond(ApiMessage("Rule for $endpointPath removed successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiMessage("No rule found for $endpointPath"))
                }
            }
        }
    }
}
