package com.cutm.nt14.data.local.daos

import androidx.room.*
import com.cutm.nt14.data.local.entities.Endpoint
import kotlinx.coroutines.flow.Flow

@Dao
interface EndpointDao {
    @Query("SELECT * FROM Endpoints")
    fun getAllEndpoints(): Flow<List<Endpoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndpoint(endpoint: Endpoint)

    @Delete
    suspend fun deleteEndpoint(endpoint: Endpoint)

    @Query("SELECT * FROM Endpoints WHERE endpointId = :id")
    suspend fun getEndpointById(id: String): Endpoint?

    @Query("SELECT * FROM Endpoints WHERE syncStatus = 'PENDING'")
    suspend fun getEndpointsToSync(): List<Endpoint>

    @Query("UPDATE Endpoints SET syncStatus = 'SYNCED' WHERE endpointId IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
