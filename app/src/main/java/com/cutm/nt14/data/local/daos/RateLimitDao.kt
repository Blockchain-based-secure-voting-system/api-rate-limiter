package com.cutm.nt14.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cutm.nt14.data.local.entities.RateLimit
import kotlinx.coroutines.flow.Flow

@Dao
interface RateLimitDao {
    @Query("SELECT * FROM RateLimits")
    fun getAllRules(): Flow<List<RateLimit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RateLimit)

    @Query("DELETE FROM RateLimits WHERE ruleId = :ruleId")
    suspend fun deleteRuleById(ruleId: String)

    @Query("SELECT * FROM RateLimits WHERE syncStatus = 'PENDING'")
    suspend fun getRulesToSync(): List<RateLimit>

    @Query("UPDATE RateLimits SET syncStatus = 'SYNCED' WHERE ruleId IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
