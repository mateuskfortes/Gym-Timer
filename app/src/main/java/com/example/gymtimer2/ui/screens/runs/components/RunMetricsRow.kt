package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunModel

/** Exibe três métricas com unidades claras. */
@Composable
internal fun RunMetricsRow(run: RunModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RunMetric(
            label = stringResource(R.string.run_time_label),
            value = RunMetrics.formatDuration(run.durationMs),
            modifier = Modifier.weight(1f)
        )
        RunMetric(
            label = stringResource(R.string.run_distance_label),
            value = stringResource(R.string.run_distance_value, run.distanceMeters / METERS_PER_KILOMETER),
            modifier = Modifier.weight(1f)
        )
        RunMetric(
            label = stringResource(R.string.run_pace_label),
            value = RunMetrics.formatPace(run.averagePaceSecondsPerKm),
            modifier = Modifier.weight(1f)
        )
    }
}

private const val METERS_PER_KILOMETER = 1_000.0
