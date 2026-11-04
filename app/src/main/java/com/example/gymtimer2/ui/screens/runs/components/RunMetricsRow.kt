package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting

/** Exibe três métricas com unidades claras. */
@Composable
internal fun RunMetricsRow(
    run: RunModel,
    modifier: Modifier = Modifier,
    paceSecondsPerKm: Double? = run.averagePaceSecondsPerKm,
    paceLabelResource: Int = R.string.run_pace_label
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RunMetric(
            label = stringResource(
                if (run.usesElapsedTimeForMetrics) R.string.run_elapsed_time_label else R.string.run_time_label
            ),
            value = RunMetrics.formatDuration(run.performanceDurationMs),
            modifier = Modifier.weight(1f)
        )
        RunMetric(
            label = stringResource(R.string.run_distance_label),
            value = RunFormatting.distanceLabel(run.distanceMeters),
            modifier = Modifier.weight(1f)
        )
        RunMetric(
            label = stringResource(paceLabelResource),
            value = RunFormatting.pace(paceSecondsPerKm),
            modifier = Modifier.weight(1f)
        )
    }
}
