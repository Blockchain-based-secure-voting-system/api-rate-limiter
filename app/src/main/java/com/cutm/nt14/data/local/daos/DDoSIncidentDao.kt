package com.cutm.nt14.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cutm.nt14.data.local.entities.DDoSIncident
import kotlinx.coroutines.flow.Flow

@Dao
interface DDoSIncidentDao {
    @Query("SELECT * FROM DDoSIncidents ORDER BY startTime DESC")
    fun getAllIncidents(): Flow<List<DDoSIncident>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: DDoSIncident)
}
