package com.cutm.nt14.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cutm.nt14.data.local.entities.AbuseEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface AbuseEventDao {
    @Query("SELECT * FROM AbuseEvents ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<AbuseEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AbuseEvent)
}
