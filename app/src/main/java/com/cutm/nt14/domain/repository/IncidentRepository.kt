package com.cutm.nt14.domain.repository

import com.cutm.nt14.data.local.entities.AbuseEvent
import com.cutm.nt14.data.local.entities.DDoSIncident
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    fun getAllAbuseEvents(): Flow<List<AbuseEvent>>
    fun getAllDDoSIncidents(): Flow<List<DDoSIncident>>
    suspend fun saveAbuseEvent(event: AbuseEvent)
    suspend fun saveDDoSIncident(incident: DDoSIncident)
    suspend fun runDetection()
}
