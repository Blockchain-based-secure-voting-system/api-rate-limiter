package com.cutm.nt14.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cutm.nt14.data.local.SyncStatus

@Entity(tableName = "Endpoints")
data class Endpoint(
    @PrimaryKey
    val endpointId: String,
    val name: String,
    val baseUrl: String,
    val method: String,
    val status: String,
    val ownerEmail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
