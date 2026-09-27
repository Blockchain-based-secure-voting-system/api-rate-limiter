package com.cutm.nt14.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.cutm.nt14.data.local.SyncStatus

@Entity(
    tableName = "DDoSIncidents",
    foreignKeys = [
        ForeignKey(
            entity = Endpoint::class,
            parentColumns = ["endpointId"],
            childColumns = ["endpointId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DDoSIncident(
    @PrimaryKey
    val incidentId: String,
    val endpointId: String,
    val startTime: Long,
    val requestSpike: Int,
    val severity: String,
    val status: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
