package com.cutm.nt14.data.remote

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cutm.nt14.domain.repository.IncidentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DetectionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val incidentRepository: IncidentRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            incidentRepository.runDetection()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
