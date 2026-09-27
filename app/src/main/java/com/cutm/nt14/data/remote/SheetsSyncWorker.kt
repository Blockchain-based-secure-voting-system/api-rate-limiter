package com.cutm.nt14.data.remote

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cutm.nt14.data.local.daos.EndpointDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SheetsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val endpointDao: EndpointDao,
    private val requestLogDao: com.cutm.nt14.data.local.daos.RequestLogDao,
    private val rateLimitDao: com.cutm.nt14.data.local.daos.RateLimitDao,
    private val sheetsDataSource: SheetsDataSource
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            syncEndpoints()
            syncLogs()
            syncRules()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun syncEndpoints() {
        val pending = endpointDao.getEndpointsToSync()
        if (pending.isNotEmpty()) {
            sheetsDataSource.pushEndpoints(pending)
            endpointDao.markAsSynced(pending.map { it.endpointId })
        }
    }

    private suspend fun syncLogs() {
        val pending = requestLogDao.getLogsToSync()
        if (pending.isNotEmpty()) {
            sheetsDataSource.pushLogs(pending)
            requestLogDao.markAsSynced(pending.map { it.logId })
        }
    }

    private suspend fun syncRules() {
        val pending = rateLimitDao.getRulesToSync()
        if (pending.isNotEmpty()) {
            sheetsDataSource.pushRateLimits(pending)
            rateLimitDao.markAsSynced(pending.map { it.ruleId })
        }
    }
}
