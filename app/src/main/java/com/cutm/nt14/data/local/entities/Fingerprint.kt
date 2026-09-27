package com.cutm.nt14.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cutm.nt14.data.local.SyncStatus

@Entity(tableName = "Fingerprints")
data class Fingerprint(
    @PrimaryKey
    val fingerprintId: String,
    val userId: String,
    val ipHash: String,
    val deviceHash: String,
    val tokenHash: String,
    val riskLevel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
