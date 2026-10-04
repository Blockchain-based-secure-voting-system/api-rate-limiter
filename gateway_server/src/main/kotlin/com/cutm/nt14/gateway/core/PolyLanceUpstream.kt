package com.cutm.nt14.gateway.core

import com.cutm.nt14.gateway.models.PolyLanceAttestation
import com.cutm.nt14.gateway.models.PolyLanceEscrow
import com.cutm.nt14.gateway.models.PolyLanceTalent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory

class PolyLanceUpstreamException(message: String) : Exception(message)

/**
 * Live client for the production PolyLance Zenith backend.
 *
 * Every call goes to the real PolyLance API (the same backend polylance.codes uses).
 * There is NO mock fallback: if the upstream is down, callers get an exception and
 * the gateway returns HTTP 502 so failures are visible instead of hidden.
 *
 * Upstream base URL can be overridden with the POLYLANCE_API_BASE environment variable.
 */
class PolyLanceUpstream(
    private val baseUrl: String = System.getenv("POLYLANCE_API_BASE")
        ?: "https://polylance-fv-1-45wy.onrender.com"
) {
    private val logger = LoggerFactory.getLogger(PolyLanceUpstream::class.java)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            // Render free-tier instances can cold-start; allow time for that.
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 20_000
        }
    }

    private suspend fun fetchJson(path: String): JsonElement {
        val url = "$baseUrl$path"
        val resp = try {
            client.get(url)
        } catch (e: Exception) {
            logger.warn("PolyLance upstream unreachable: $url -> ${e.message}")
            throw PolyLanceUpstreamException("PolyLance upstream unreachable: ${e.message}")
        }
        val body = resp.bodyAsText()
        if (!resp.status.isSuccess()) {
            throw PolyLanceUpstreamException("PolyLance upstream returned HTTP ${resp.status.value} for $path")
        }
        return json.parseToJsonElement(body)
    }

    /** Live escrow jobs from PolyLance `/api/jobs`. */
    suspend fun fetchEscrows(): List<PolyLanceEscrow> {
        val root = fetchJson("/api/jobs").jsonObject
        val jobs = root["jobs"] as? JsonArray ?: return emptyList()
        return jobs.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val token = o.str("paymentTokenSymbol") ?: "POL"
            // USDC jobs carry the value in amountUsdc; native-token jobs in amountEth
            val amount = if (token.equals("USDC", true)) {
                o.str("amountUsdc")?.toDoubleOrNull()
            } else {
                o.str("amountEth")?.toDoubleOrNull()
            } ?: 0.0
            PolyLanceEscrow(
                escrowId = o.str("id") ?: return@mapNotNull null,
                title = o.str("title")?.trim().orEmpty(),
                client = o.str("client").orEmpty(),
                freelancer = o.str("freelancer").orEmpty(),
                amountPol = amount,
                token = token,
                status = o.str("status") ?: "Unknown",
                contractAddress = o.str("contractAddress"),
                createdAt = o.str("createdAt")?.toLongOrNull()
            )
        }.sortedByDescending { it.createdAt ?: 0L }
    }

    private suspend fun fetchProfiles(): List<JsonObject> {
        val root = fetchJson("/api/sync").jsonObject
        val profiles = root["profiles"] as? JsonObject ?: return emptyList()
        return profiles.values.mapNotNull { it as? JsonObject }
    }

    /** GitHub-verified skill attestations derived from live PolyLance profiles. */
    suspend fun fetchAttestations(): List<PolyLanceAttestation> {
        return fetchProfiles()
            .filter { it.str("githubVerified") == "true" && !it.str("githubUsername").isNullOrBlank() }
            .map { p ->
                val tier = p.str("reputationTier") ?: "UNRATED"
                val category = p.str("primaryCategory") ?: "general"
                val score = p.str("primaryScore") ?: "0"
                PolyLanceAttestation(
                    attestationId = p.str("address").orEmpty(),
                    developerGithub = p.str("githubUsername").orEmpty(),
                    skillAttestation = "$tier $category (score $score)",
                    soulboundTokenId = "${p.str("reputationSbtCount") ?: "0"} SBT"
                )
            }
    }

    /** Talent directory from live PolyLance profiles, highest score first. */
    suspend fun fetchTalents(): List<PolyLanceTalent> {
        return fetchProfiles()
            .filter { it.str("role") != "client" }
            .map { p ->
                val skills = (p["skills"] as? JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    ?.take(3)
                    .orEmpty()
                val title = p.str("title")?.takeIf { it.isNotBlank() }
                    ?: p.str("primaryCategory")?.replaceFirstChar { it.uppercase() }
                    ?: "Freelancer"
                PolyLanceTalent(
                    talentId = p.str("address").orEmpty(),
                    name = p.str("displayName")?.takeIf { it.isNotBlank() }
                        ?: p.str("githubUsername")
                        ?: p.str("address").orEmpty(),
                    specialization = if (skills.isEmpty()) title else "$title - ${skills.joinToString(", ")}",
                    rating = p.str("primaryScore")?.toDoubleOrNull() ?: 0.0
                )
            }
            .sortedByDescending { it.rating }
    }

    private fun JsonObject.str(key: String): String? {
        val v = this[key] ?: return null
        if (v is JsonNull) return null
        return (v as? JsonPrimitive)?.contentOrNull
    }
}
