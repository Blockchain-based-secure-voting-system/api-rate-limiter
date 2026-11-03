package com.cutm.nt14.gateway.tools

import com.cutm.nt14.gateway.db.DatabaseUrl
import com.cutm.nt14.gateway.db.GatewayDatabase

/**
 * Runs a SQL query against DATABASE_URL (after applying Flyway migrations if requested)
 * and prints a formatted table. Used to verify live PostgreSQL state from the command line.
 *
 * Usage:
 *   $env:DATABASE_URL="postgres://..."
 *   .\gradlew.bat :gateway_server:dbQuery -Psql="SELECT count(*) FROM request_logs"
 */
fun main(args: Array<String>) {
    val rawUrl = System.getenv("DATABASE_URL")
        ?: error("DATABASE_URL environment variable is required")
    val sql = args.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: """
           SELECT 'request_logs' AS table_name, count(*) AS row_count FROM request_logs
           UNION ALL SELECT 'metrics_1m', count(*) FROM metrics_1m
           UNION ALL SELECT 'clients', count(*) FROM clients
           UNION ALL SELECT 'bans', count(*) FROM bans
           UNION ALL SELECT 'rules', count(*) FROM rules
           UNION ALL SELECT 'incidents', count(*) FROM incidents
           UNION ALL SELECT 'monitored_targets', count(*) FROM monitored_targets
           UNION ALL SELECT 'probe_results', count(*) FROM probe_results
        """.trimIndent()

    GatewayDatabase.create(DatabaseUrl.parse(rawUrl)).use { db ->
        db.withConnection { conn ->
            conn.createStatement().use { st ->
                val hasRs = st.execute(sql)
                if (!hasRs) {
                    println("Updated rows: ${st.updateCount}")
                    return
                }
                st.resultSet.use { rs ->
                    val meta = rs.metaData
                    val cols = (1..meta.columnCount).map { meta.getColumnLabel(it) }
                    val rows = mutableListOf<List<String>>()
                    while (rs.next()) {
                        rows += (1..meta.columnCount).map { rs.getObject(it)?.toString() ?: "NULL" }
                    }
                    val widths = cols.indices.map { i ->
                        maxOf(cols[i].length, rows.maxOfOrNull { it[i].length } ?: 0)
                    }
                    fun fmt(r: List<String>) = r.indices.joinToString(" | ") { i -> r[i].padEnd(widths[i]) }
                    println(fmt(cols))
                    println(widths.joinToString("-+-") { "-".repeat(it) })
                    rows.forEach { println(fmt(it)) }
                    println("(${rows.size} row(s))")
                }
            }
        }
    }
}
