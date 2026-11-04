package com.example.gymtimer2.ui.screens.runs.tracking

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.gymtimer2.MainActivity
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import com.example.gymtimer2.ui.screens.runs.notifications.RUN_NOTIFICATION_CHANNEL_ID
import com.example.gymtimer2.ui.screens.runs.notifications.RUN_ROUTE_ALERT_NOTIFICATION_CHANNEL_ID

/** Monta a notificação da corrida com métricas e controle de pausa. */
internal class RunNotificationFactory(
    private val context: Context,
    private val distanceUnit: () -> RunDistanceUnit
) {
    fun build(
        messageResource: Int,
        runId: Long?,
        run: RunModel?,
        elapsedDurationMs: Long,
        showNoGps: Boolean = false
    ): Notification {
        val isRunning = messageResource == R.string.run_notification_running
        val displayedMessageResource = if (isRunning && showNoGps) {
            R.string.run_notification_no_gps
        } else {
            messageResource
        }
        val builder = NotificationCompat.Builder(context, RUN_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(notificationTitle(isRunning, elapsedDurationMs))
            .setContentText(notificationText(displayedMessageResource, isRunning, run, elapsedDurationMs))
            .setSubText(run?.let { context.getString(RunActivityFormatting.activityTypeResource(it.activityType)) })
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(openRunIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(System.currentTimeMillis() - elapsedDurationMs)
            .setUsesChronometer(isRunning)
            .setShowWhen(isRunning)

        runId?.let { builder.addAction(controlAction(isRunning, it)) }
        return builder.build()
    }

    fun buildRouteAlert(): Notification = NotificationCompat.Builder(context, RUN_ROUTE_ALERT_NOTIFICATION_CHANNEL_ID)
        .setContentTitle(context.getString(R.string.run_off_route_notification_title))
        .setContentText(context.getString(R.string.run_off_route_notification_message))
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setContentIntent(openRunIntent())
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    private fun notificationTitle(isRunning: Boolean, elapsedDurationMs: Long): String =
        if (isRunning) {
            context.getString(R.string.run_notification_title)
        } else {
            context.getString(
                R.string.run_notification_title_elapsed,
                context.getString(R.string.run_notification_title),
                RunMetrics.formatDuration(elapsedDurationMs)
            )
        }

    private fun notificationText(
        messageResource: Int,
        isRunning: Boolean,
        run: RunModel?,
        elapsedDurationMs: Long
    ): String {
        if (run == null) return context.getString(messageResource)

        val unit = distanceUnit()
        val distance = RunFormatting.distanceLabel(context, run.distanceMeters, unit)
        val metric = if (isRunning) {
            RunMetrics.formatPace(
                RunMetrics.averagePaceSecondsPerKm(elapsedDurationMs, run.distanceMeters),
                unit
            )
        } else {
            RunMetrics.formatDuration(elapsedDurationMs)
        }
        return context.getString(R.string.run_notification_details, context.getString(messageResource), distance, metric)
    }

    private fun controlAction(isRunning: Boolean, runId: Long): NotificationCompat.Action {
        val command = if (isRunning) RunTrackingService.ACTION_PAUSE else RunTrackingService.ACTION_RESUME
        val labelResource = if (isRunning) {
            R.string.run_notification_action_pause
        } else {
            R.string.run_notification_action_resume
        }
        val iconResource = if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val pendingIntent = PendingIntent.getService(
            context,
            command.hashCode() xor runId.hashCode(),
            Intent(context, RunTrackingService::class.java)
                .setAction(command)
                .putExtra(RunTrackingService.EXTRA_RUN_ID, runId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(iconResource, context.getString(labelResource), pendingIntent).build()
    }

    private fun openRunIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
