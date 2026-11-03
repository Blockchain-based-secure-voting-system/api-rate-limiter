package com.cutm.nt14.data.repository

import com.cutm.nt14.data.local.daos.EndpointDao
import com.cutm.nt14.data.local.entities.Endpoint
import com.cutm.nt14.data.remote.FirebaseRealtimeDatabaseManager
import com.cutm.nt14.domain.repository.EndpointRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EndpointRepositoryImpl @Inject constructor(
    private val dao: EndpointDao,
    private val firebaseRtdb: FirebaseRealtimeDatabaseManager
) : EndpointRepository {
    override fun getAllEndpoints(): Flow<List<Endpoint>> = dao.getAllEndpoints()

    override suspend fun addEndpoint(endpoint: Endpoint) {
        dao.insertEndpoint(endpoint)
        firebaseRtdb.syncEndpoint(endpoint)
    }

    override suspend fun deleteEndpoint(endpoint: Endpoint) {
        dao.deleteEndpoint(endpoint)
        firebaseRtdb.deleteEndpoint(endpoint.endpointId)
    }

    override suspend fun getEndpointById(id: String): Endpoint? {
        return dao.getEndpointById(id)
    }
}
