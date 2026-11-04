@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.domain.model.RunWorkoutStep

@Composable
internal fun RunWorkoutHistorySteps(steps: List<RunWorkoutStep>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEach { step ->
            Column(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (step.repetitionCount > 1) {
                            stringResource(
                                R.string.run_workout_history_repetition,
                                step.stepIndex + 1,
                                step.description,
                                step.repetition,
                                step.repetitionCount
                            )
                        } else "${step.stepIndex + 1}. ${step.description}",
                        modifier = Modifier.weight(1f)
                    )
                    Text(workoutStepResult(step), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    RunWorkoutStepGoal(step),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                step.paceTarget?.let { target ->
                    Text(
                        stringResource(
                            R.string.run_workout_step_pace_target,
                            RunFormatting.pace(target.fasterSecondsPerKm.toDouble()),
                            RunFormatting.pace(target.slowerSecondsPerKm.toDouble())
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun workoutStepResult(step: RunWorkoutStep): String {
    val duration = if (step.completedAtDurationMs != null && step.startedAtDurationMs != null) {
        RunMetrics.formatDuration(step.completedAtDurationMs - step.startedAtDurationMs)
    } else {
        stringResource(R.string.run_workout_in_progress)
    }
    val distance = if (step.completedAtDistanceMeters != null && step.startedAtDistanceMeters != null) {
        RunFormatting.distanceLabel(step.completedAtDistanceMeters - step.startedAtDistanceMeters)
    } else null
    return listOfNotNull(duration, distance).joinToString(" · ")
}
