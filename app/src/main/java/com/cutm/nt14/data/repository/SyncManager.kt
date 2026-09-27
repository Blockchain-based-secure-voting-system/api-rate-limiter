package com.cutm.nt14.data.repository

import android.content.Context
import androidx.work.*
import com.cutm.nt14.data.remote.SheetsSyncWorker
import com.cutm.nt14.data.remote.DetectionWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleTasks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Sync Task
        val syncRequest = PeriodicWorkRequestBuilder<SheetsSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "SheetsSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // Detection Task
        val detectionRequest = PeriodicWorkRequestBuilder<DetectionWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "TrafficDetection",
            ExistingPeriodicWorkPolicy.KEEP,
            detectionRequest
        )
    }

    fun triggerOneTimeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SheetsSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }
}
