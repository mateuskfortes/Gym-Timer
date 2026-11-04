package com.example.gymtimer2.ui.screens.runs.tracking

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/** Reinicia o serviço de corrida usando a sessão local gravada antes da falha. */
class RunCrashRecoveryJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        val runId = params.extras.getLong(RunCrashRecovery.EXTRA_RUN_ID, INVALID_RUN_ID)
        if (runId == INVALID_RUN_ID || RunTrackingService.isTracking(runId)) return false

        return try {
            ContextCompat.startForegroundService(
                this,
                Intent(this, RunTrackingService::class.java)
                    .setAction(RunTrackingService.ACTION_RECOVER)
                    .putExtra(RunTrackingService.EXTRA_RUN_ID, runId)
            )
            false
        } catch (error: SecurityException) {
            Log.e(TAG, "Location permission changed before the run service restarted.", error)
            false
        } catch (error: IllegalStateException) {
            Log.e(TAG, "Could not restart the interrupted run service.", error)
            false
        }
    }

    override fun onStopJob(params: JobParameters): Boolean = false

    private companion object {
        const val TAG = "RunCrashRecovery"
        const val INVALID_RUN_ID = -1L
    }
}
