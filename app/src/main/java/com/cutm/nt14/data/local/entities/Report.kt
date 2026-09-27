package com.cutm.nt14.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cutm.nt14.data.local.SyncStatus

@Entity(tableName = "Reports")
data class Report(
    @PrimaryKey
    val reportId: String,
    val period: String,
    val summary: String,
    val recommendation: String,
    val generatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
