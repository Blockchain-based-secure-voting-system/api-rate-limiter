package com.cutm.nt14.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.cutm.nt14.data.local.SyncStatus

@Entity(
    tableName = "RateLimits",
    foreignKeys = [
        ForeignKey(
            entity = Endpoint::class,
            parentColumns = ["endpointId"],
            childColumns = ["endpointId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RateLimit(
    @PrimaryKey
    val ruleId: String,
    val endpointId: String,
    val limitPerMin: Int,
    val burstLimit: Int,
    val action: String,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
