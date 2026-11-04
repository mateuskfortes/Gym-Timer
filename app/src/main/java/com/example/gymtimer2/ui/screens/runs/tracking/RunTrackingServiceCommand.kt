package com.example.gymtimer2.ui.screens.runs.tracking

import android.content.Intent
import com.example.gymtimer2.domain.model.RunActivityTag
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunSaveDetails

/** Valores validados que o serviço recebe por um intent do app ou da notificação. */
internal data class RunTrackingServiceCommand(
    val action: String,
    val runId: Long,
    val saveDetails: RunSaveDetails
) {
    val startsForeground: Boolean
        get() = action == RunTrackingService.ACTION_START ||
            action == RunTrackingService.ACTION_RESUME ||
            action == RunTrackingService.ACTION_RECOVER

    val isRecovery: Boolean
        get() = action == RunTrackingService.ACTION_RECOVER

    val startMode: Int
        get() = if (action == RunTrackingService.ACTION_FINISH ||
            action == RunTrackingService.ACTION_DISCARD ||
            action == RunTrackingService.ACTION_PAUSE
        ) android.app.Service.START_NOT_STICKY else android.app.Service.START_STICKY

    companion object {
        private const val INVALID_RUN_ID = -1L

        fun from(intent: Intent): RunTrackingServiceCommand? {
            val action = intent.action ?: return null
            val runId = intent.getLongExtra(RunTrackingService.EXTRA_RUN_ID, INVALID_RUN_ID)
            if (runId == INVALID_RUN_ID) return null
            return RunTrackingServiceCommand(action, runId, intent.runSaveDetails())
        }
    }
}

private fun Intent.runSaveDetails(): RunSaveDetails = RunSaveDetails(
    title = getStringExtra(RunTrackingService.EXTRA_TITLE),
    description = getStringExtra(RunTrackingService.EXTRA_DESCRIPTION),
    gearName = getStringExtra(RunTrackingService.EXTRA_GEAR_NAME),
    gearId = getLongExtra(RunTrackingService.EXTRA_GEAR_ID, -1L).takeIf { it > 0 },
    privateNotes = getStringExtra(RunTrackingService.EXTRA_PRIVATE_NOTES),
    isCommute = getBooleanExtra(RunTrackingService.EXTRA_IS_COMMUTE, false),
    perceivedExertion = getIntExtra(RunTrackingService.EXTRA_PERCEIVED_EXERTION, -1)
        .takeIf { it in RunSaveDetails.MIN_PERCEIVED_EXERTION..RunSaveDetails.MAX_PERCEIVED_EXERTION },
    activityType = getStringExtra(RunTrackingService.EXTRA_ACTIVITY_TYPE)?.let { activityTypeName ->
        RunActivityType.entries.firstOrNull { it.name == activityTypeName }
    },
    activityTags = RunActivityTag.parse(getStringExtra(RunTrackingService.EXTRA_ACTIVITY_TAGS))
)
