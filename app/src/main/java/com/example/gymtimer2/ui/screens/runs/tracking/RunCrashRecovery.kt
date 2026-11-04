package com.example.gymtimer2.ui.screens.runs.tracking

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PersistableBundle
import android.os.SystemClock
import androidx.core.content.edit
import androidx.core.content.ContextCompat

/** Agenda a retomada rápida quando uma falha encerra o processo durante uma corrida. */
internal object RunCrashRecovery {
    private const val JOB_ID = 2303
    const val EXTRA_RUN_ID = "run_id"
    private const val PREFERENCES_NAME = "run_crash_recovery"
    private const val LAST_SCHEDULED_AT_KEY = "last_scheduled_at_elapsed_ms"
    private const val RETRY_INTERVAL_MS = 60_000L

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        synchronized(this) {
            if (installed) return
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler() ?: return
            val appContext = context.applicationContext
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                try {
                    scheduleRecovery(appContext)
                } catch (_: Exception) {
                    // A falha original deve seguir para o handler do sistema.
                } finally {
                    previousHandler.uncaughtException(thread, error)
                }
            }
            installed = true
        }
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun scheduleRecovery(context: Context) {
        val runId = RunTrackingState.crashRecoverableRunId ?: return
        if (!hasBackgroundLocationPermission(context)) return

        val nowElapsedMs = SystemClock.elapsedRealtime()
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val lastScheduledAtMs = preferences.getLong(LAST_SCHEDULED_AT_KEY, 0L)
        if (lastScheduledAtMs > 0L && nowElapsedMs >= lastScheduledAtMs &&
            nowElapsedMs - lastScheduledAtMs < RETRY_INTERVAL_MS
        ) return

        val extras = PersistableBundle().apply { putLong(EXTRA_RUN_ID, runId) }
        val component = ComponentName(context, RunCrashRecoveryJobService::class.java)
        val builder = JobInfo.Builder(JOB_ID, component)
            .setPersisted(false)
            .setExtras(extras)

        val supportsExpeditedJobs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        if (supportsExpeditedJobs) {
            builder.setExpedited(true)
        } else {
            builder.setMinimumLatency(0L)
            builder.setOverrideDeadline(5_000L)
        }

        try {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val result = scheduler.schedule(builder.build())
            if (shouldFallbackToRegularJob(result, supportsExpeditedJobs)) {
                scheduler.schedule(
                    JobInfo.Builder(JOB_ID, component)
                        .setPersisted(false)
                        .setExtras(extras)
                        .setMinimumLatency(0L)
                        .setOverrideDeadline(5_000L)
                        .build()
                )
            }
        } finally {
            // Record failed attempts too, so repeated crashes cannot loop through JobScheduler.
            preferences.edit(commit = true) { putLong(LAST_SCHEDULED_AT_KEY, nowElapsedMs) }
        }
    }

    internal fun shouldFallbackToRegularJob(result: Int, supportsExpeditedJobs: Boolean): Boolean =
        supportsExpeditedJobs && result == JobScheduler.RESULT_FAILURE
}
