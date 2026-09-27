package com.cutm.nt14.gateway.routes

import com.cutm.nt14.gateway.models.DemoOrder
import com.cutm.nt14.gateway.models.DemoProduct
import com.cutm.nt14.gateway.models.DemoUser
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
    }
}
