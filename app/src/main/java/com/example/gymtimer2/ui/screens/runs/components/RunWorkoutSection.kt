package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunWorkoutStep

@Composable
internal fun RunWorkoutSection(
    title: String?,
    steps: List<RunWorkoutStep>,
    isRunning: Boolean,
    currentPaceSecondsPerKm: Double? = null,
    onAdvance: () -> Unit = {}
) {
    if (steps.isEmpty()) return
    val current = steps.firstOrNull { it.startedAtDurationMs != null && it.completedAtDurationMs == null }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title ?: stringResource(R.string.run_workout_title), style = MaterialTheme.typography.titleMedium)
            when {
                current != null -> RunWorkoutCurrentStep(
                    current, steps.size, isRunning, currentPaceSecondsPerKm, onAdvance
                )
                isRunning -> Text(stringResource(R.string.run_workout_complete), color = MaterialTheme.colorScheme.primary)
                else -> RunWorkoutHistorySteps(steps)
            }
        }
    }
}
