package com.cutm.nt14.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.cutm.nt14.data.local.SyncStatus

@Entity(
    tableName = "AbuseEvents",
    foreignKeys = [
        ForeignKey(
            entity = RequestLog::class,
            parentColumns = ["logId"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AbuseEvent(
    @PrimaryKey
    val eventId: String,
    val logId: String,
    val eventType: String,
    val riskScore: Int,
    val action: String,
    val createdAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
