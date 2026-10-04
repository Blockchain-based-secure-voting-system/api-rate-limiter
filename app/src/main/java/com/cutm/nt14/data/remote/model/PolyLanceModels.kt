package com.cutm.nt14.data.remote.model

/**
 * PolyLance Web3 Protocol data models for real-time mobile inspection.
 */
data class PolyLanceEscrow(
    val escrowId: String,
    val client: String,
    val freelancer: String,
    val amountPol: Double = 0.0,
    val status: String,
    val title: String = "",
    val token: String = "POL",
    val contractAddress: String? = null
)

data class PolyLanceAttestation(
    val attestationId: String,
    val developerGithub: String,
    val skillAttestation: String,
    val soulboundTokenId: String
)

data class PolyLanceTalent(
    val talentId: String,
    val name: String,
    val specialization: String,
    val rating: Double
)

data class GatewayResponse<T>(
    val statusCode: Int,
    val data: T?,
    val rawJson: String?,
    val rateLimitRemaining: Int?,
    val rateLimitLimit: Int?,
    val rateLimitReset: Long?,
    val latencyMs: Long,
    val errorMessage: String? = null
)
