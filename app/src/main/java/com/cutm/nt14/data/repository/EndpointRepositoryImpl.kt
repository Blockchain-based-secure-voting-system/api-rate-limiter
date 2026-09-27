package com.cutm.nt14.data.repository

import com.cutm.nt14.data.local.daos.EndpointDao
import com.cutm.nt14.data.local.entities.Endpoint
import com.cutm.nt14.domain.repository.EndpointRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EndpointRepositoryImpl @Inject constructor(
    private val dao: EndpointDao
) : EndpointRepository {
    override fun getAllEndpoints(): Flow<List<Endpoint>> = dao.getAllEndpoints()

    override suspend fun addEndpoint(endpoint: Endpoint) {
        dao.insertEndpoint(endpoint)
    }

    override suspend fun deleteEndpoint(endpoint: Endpoint) {
        dao.deleteEndpoint(endpoint)
    }

    override suspend fun getEndpointById(id: String): Endpoint? {
        return dao.getEndpointById(id)
    }
}
