package com.example.gymtimer2.ui.screens.runs.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.RunModel

internal const val RUN_NOTIFICATION_ID = 2301
internal const val RUN_ROUTE_ALERT_NOTIFICATION_ID = 2302

internal class RunTrackingNotifications(
    private val context: Context,
    distanceUnit: () -> RunDistanceUnit,
    private val currentRunId: () -> Long?,
    private val currentRun: () -> RunModel?,
    private val currentDurationMs: () -> Long
) {
    private val factory = RunNotificationFactory(context, distanceUnit)
    private var showNoGps = false

    fun build(messageId: Int, runId: Long? = currentRunId(), run: RunModel? = currentRun()): Notification =
        factory.build(messageId, runId, run, currentDurationMs(), showNoGps)

    fun update(messageId: Int) {
        context.getSystemService(NotificationManager::class.java).notify(
            RUN_NOTIFICATION_ID,
            build(messageId)
        )
    }

    fun resetNoGpsState() {
        showNoGps = false
    }

    fun updateGpsStatus(status: RunGpsStatus) {
        setNoGps(status == RunGpsStatus.NO_SIGNAL || status == RunGpsStatus.DISABLED)
    }

    fun onGpsSignalRestored() {
        if (showNoGps) setNoGps(false)
    }

    fun showRouteAlert(runId: Long) {
        if (runId != currentRunId() || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        try {
            NotificationManagerCompat.from(context).notify(
                RUN_ROUTE_ALERT_NOTIFICATION_ID,
                factory.buildRouteAlert()
            )
        } catch (_: SecurityException) {
            // Permission may change after the notification checks.
        }
    }

    fun clearRouteAlert() {
        NotificationManagerCompat.from(context).cancel(RUN_ROUTE_ALERT_NOTIFICATION_ID)
    }

    private fun setNoGps(value: Boolean) {
        if (showNoGps == value) return
        showNoGps = value
        update(R.string.run_notification_running)
    }
}
