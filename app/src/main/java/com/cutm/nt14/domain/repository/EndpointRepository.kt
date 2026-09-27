package com.cutm.nt14.domain.repository

import com.cutm.nt14.data.local.entities.Endpoint
import kotlinx.coroutines.flow.Flow

interface EndpointRepository {
    fun getAllEndpoints(): Flow<List<Endpoint>>
    suspend fun addEndpoint(endpoint: Endpoint)
    suspend fun deleteEndpoint(endpoint: Endpoint)
    suspend fun getEndpointById(id: String): Endpoint?
}
