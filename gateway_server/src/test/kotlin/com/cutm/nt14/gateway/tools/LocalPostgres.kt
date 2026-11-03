package com.cutm.nt14.gateway.tools

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres

/**
 * Starts a real PostgreSQL server (via embedded-postgres native binary) for local verification.
 * Usage:
 *   .\gradlew.bat :gateway_server:localPostgres
 */
fun main() {
    val port = System.getenv("PG_PORT")?.toIntOrNull() ?: 15432
    val pg = EmbeddedPostgres.builder().setPort(port).start()
    val url = "postgres://postgres:postgres@127.0.0.1:${pg.port}/postgres"
    println("EMBEDDED_POSTGRES_READY $url")
    Runtime.getRuntime().addShutdownHook(Thread { runCatching { pg.close() } })
    Thread.currentThread().join()
}
