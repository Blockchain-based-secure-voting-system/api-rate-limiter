package com.cutm.nt14.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.cutm.nt14.data.local.SyncStatus

@Entity(
    tableName = "RequestLogs",
    foreignKeys = [
        ForeignKey(
            entity = Endpoint::class,
            parentColumns = ["endpointId"],
            childColumns = ["endpointId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RequestLog(
    @PrimaryKey
    val logId: String,
    val endpointId: String,
    val timestamp: Long,
    val sourceIp: String,
    val userId: String,
    val statusCode: Int,
    val latencyMs: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
