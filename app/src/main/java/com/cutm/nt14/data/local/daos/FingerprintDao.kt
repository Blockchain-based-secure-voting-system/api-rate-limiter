package com.cutm.nt14.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cutm.nt14.data.local.entities.Fingerprint
import kotlinx.coroutines.flow.Flow

@Dao
interface FingerprintDao {
    @Query("SELECT * FROM Fingerprints")
    fun getAllFingerprints(): Flow<List<Fingerprint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFingerprint(fingerprint: Fingerprint)
}
