@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunWorkoutStep
import com.example.gymtimer2.domain.model.RunWorkoutTrigger
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting

@Composable
internal fun RunWorkoutCurrentStep(
    step: RunWorkoutStep,
    stepCount: Int,
    isRunning: Boolean,
    currentPaceSecondsPerKm: Double?,
    onAdvance: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.run_workout_step_progress, step.stepIndex + 1, stepCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(step.description, style = MaterialTheme.typography.titleSmall)
        if (step.repetitionCount > 1) {
            Text(stringResource(R.string.run_workout_repetition_label, step.repetition, step.repetitionCount))
        }
        Text(RunWorkoutStepGoal(step))
        step.paceTarget?.let { RunPaceGuidanceCard(it, currentPaceSecondsPerKm) }
        if (step.isRestInterval) {
            Text(stringResource(R.string.run_workout_rest_interval), style = MaterialTheme.typography.labelMedium)
        }
        if (isRunning && step.trigger == RunWorkoutTrigger.MANUAL) {
            Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.run_workout_advance))
            }
        }
    }
}

@Composable
internal fun RunWorkoutStepGoal(step: RunWorkoutStep): String = when (step.trigger) {
    RunWorkoutTrigger.DURATION -> stringResource(
        R.string.run_workout_goal_time,
        RunMetrics.formatDuration(step.durationTargetMs ?: 0L)
    )
    RunWorkoutTrigger.DISTANCE -> stringResource(
        R.string.run_workout_goal_distance,
        RunFormatting.distanceLabel(step.distanceTargetMeters ?: 0.0)
    )
    RunWorkoutTrigger.ELEVATION -> stringResource(
        R.string.run_workout_goal_elevation,
        RunFormatting.elevationLabel(step.elevationChangeTargetMeters ?: 0.0)
    )
    RunWorkoutTrigger.MANUAL -> stringResource(R.string.run_workout_goal_manual)
}
