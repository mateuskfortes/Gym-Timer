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
import com.example.gymtimer2.domain.model.RunLapMetrics
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting

/** Exibe tempo, distância e pace acumulados desde a última volta manual. */
@Composable
internal fun RunCurrentLapMetrics(metrics: RunLapMetrics) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.run_current_lap_title), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunMetric(
                label = stringResource(R.string.run_current_lap_time_label),
                value = RunMetrics.formatDuration(metrics.durationMs),
                modifier = Modifier.weight(1f)
            )
            RunMetric(
                label = stringResource(R.string.run_distance_label),
                value = RunFormatting.distanceLabel(metrics.distanceMeters),
                modifier = Modifier.weight(1f)
            )
            RunMetric(
                label = stringResource(R.string.run_current_lap_pace_label),
                value = RunFormatting.pace(metrics.paceSecondsPerKm),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
