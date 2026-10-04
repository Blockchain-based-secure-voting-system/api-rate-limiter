package com.cutm.nt14.gateway.routes

import com.cutm.nt14.gateway.core.PolyLanceUpstream
import com.cutm.nt14.gateway.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

private val logger = LoggerFactory.getLogger("DemoRoutes")
private val upstream = PolyLanceUpstream()
private val dynamicLocalEscrows = CopyOnWriteArrayList<PolyLanceEscrow>()

fun Route.demoRoutes() {
    route("/api") {
        get("/users") {
            val users = listOf(
                DemoUser("usr_01", "Alice Chen", "ADMIN"),
                DemoUser("usr_02", "Bob Smith", "ANALYST"),
                DemoUser("usr_03", "Charlie Davis", "VIEWER")
            )
            call.respond(users)
        }

        get("/orders") {
            val orders = listOf(
                DemoOrder("ord_101", 249.99, "CONFIRMED"),
                DemoOrder("ord_102", 49.50, "SHIPPED"),
                DemoOrder("ord_103", 1120.00, "PROCESSING")
            )
            call.respond(orders)
        }

        get("/products") {
            val products = listOf(
                DemoProduct("sku_phone", "Smart Sensor Phone", 699.99),
                DemoProduct("sku_watch", "Optimizer Smartwatch", 199.99),
                DemoProduct("sku_tablet", "Data Gateway Tablet", 449.00)
            )
            call.respond(products)
        }

        // Real-Time PolyLance Sovereign Protocol Endpoints
        route("/polylance") {
            get("/escrows") {
                try {
                    val liveEscrows = upstream.fetchEscrows()
                    // Combine locally posted test escrows with live production PolyLance escrows
                    val combined = dynamicLocalEscrows.toList() + liveEscrows
                    call.respond(combined)
                } catch (e: Exception) {
                    logger.error("Failed to fetch live escrows from PolyLance upstream: ${e.message}", e)
                    call.respond(
                        HttpStatusCode.BadGateway,
                        ApiMessage("PolyLance upstream error: ${e.message}")
                    )
                }
            }

            post("/escrows") {
                val req = try {
                    call.receive<CreateEscrowRequest>()
                } catch (e: Exception) {
                    CreateEscrowRequest()
                }
                val newId = "0x" + UUID.randomUUID().toString().replace("-", "").take(12)
                val newEscrow = PolyLanceEscrow(
                    escrowId = newId,
                    title = "Test Escrow via NT14 Gateway",
                    client = req.client.ifBlank { "0xb30F2eFBCEBC529d946e05C9ccE0f1ffFB7e1aB1" },
                    freelancer = req.freelancer.ifBlank { "0xB8aa0398B91A150B041DA819bc954Bb356e009Dd" },
                    amountPol = if (req.amountPol > 0) req.amountPol else 500.0,
                    token = "POL",
                    status = "Funded",
                    contractAddress = "0x" + UUID.randomUUID().toString().replace("-", "").take(40),
                    createdAt = System.currentTimeMillis()
                )
                dynamicLocalEscrows.add(0, newEscrow)
                call.respond(HttpStatusCode.Created, newEscrow)
            }

            get("/attestations") {
                try {
                    val liveAttestations = upstream.fetchAttestations()
                    call.respond(liveAttestations)
                } catch (e: Exception) {
                    logger.error("Failed to fetch live attestations from PolyLance upstream: ${e.message}", e)
                    call.respond(
                        HttpStatusCode.BadGateway,
                        ApiMessage("PolyLance upstream error: ${e.message}")
                    )
                }
            }

            get("/talents") {
                try {
                    val liveTalents = upstream.fetchTalents()
                    call.respond(liveTalents)
                } catch (e: Exception) {
                    logger.error("Failed to fetch live talents from PolyLance upstream: ${e.message}", e)
                    call.respond(
                        HttpStatusCode.BadGateway,
                        ApiMessage("PolyLance upstream error: ${e.message}")
                    )
                }
            }
        }
    }
}
