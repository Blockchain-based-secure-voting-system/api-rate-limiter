package com.cutm.nt14.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cutm.nt14.data.local.entities.RequestLog
import kotlinx.coroutines.flow.Flow

@Dao
interface RequestLogDao {
    @Query("SELECT * FROM RequestLogs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<RequestLog>>

    @Query("SELECT * FROM RequestLogs WHERE endpointId = :endpointId ORDER BY timestamp DESC")
    fun getLogsByEndpoint(endpointId: String): Flow<List<RequestLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: RequestLog)

    @Query("SELECT * FROM RequestLogs WHERE syncStatus = 'PENDING'")
    suspend fun getLogsToSync(): List<RequestLog>

    @Query("UPDATE RequestLogs SET syncStatus = 'SYNCED' WHERE logId IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Query("DELETE FROM RequestLogs")
    suspend fun clearAllLogs()
}
