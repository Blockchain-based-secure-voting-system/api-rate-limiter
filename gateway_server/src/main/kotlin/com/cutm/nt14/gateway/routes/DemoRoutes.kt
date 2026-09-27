package com.cutm.nt14.gateway.routes

import com.cutm.nt14.gateway.models.*
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

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

        // PolyLance Sovereign Protocol Endpoints
        route("/polylance") {
            get("/escrows") {
                val escrows = listOf(
                    PolyLanceEscrow("esc_0x1a8f", "0x3F9a...b210", "0x78Ce...4a91", 450.0, "FUNDED_IN_ESCROW"),
                    PolyLanceEscrow("esc_0x2b4c", "0x91Ad...004e", "0x53B9...1ef2", 1200.0, "MILESTONE_PENDING"),
                    PolyLanceEscrow("esc_0x9e12", "0x22Fa...89ac", "0x67Df...55a0", 320.0, "SETTLED_RELEASED")
                )
                call.respond(escrows)
            }

            get("/attestations") {
                val attestations = listOf(
                    PolyLanceAttestation("att_881", "akhilmuvva", "Smart Contract Security & Polygon Architecture", "SBT_#1042"),
                    PolyLanceAttestation("att_882", "balram-taddi", "Cross-Chain Security & Interoperability", "SBT_#1043"),
                    PolyLanceAttestation("att_883", "sunny-pasumarthi", "Web3 UI/UX & High-Performance Frontend", "SBT_#1044")
                )
                call.respond(attestations)
            }

            get("/talents") {
                val talents = listOf(
                    PolyLanceTalent("tal_01", "Akhil Muvva", "Protocol Architect & Solidity Lead", 5.0),
                    PolyLanceTalent("tal_02", "Balram Taddi", "Chief Security Officer & Cryptographer", 4.9),
                    PolyLanceTalent("tal_03", "Sunny Pasumarthi", "Lead Web3 Frontend Engineer", 4.9)
                )
                call.respond(talents)
            }
        }
    }
}
