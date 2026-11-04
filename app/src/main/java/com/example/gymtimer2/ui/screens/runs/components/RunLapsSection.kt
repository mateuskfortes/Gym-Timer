package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.metricsSince
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting

/** Exibe as diferenças de tempo e distância entre os marcos manuais. */
@Composable
internal fun RunLapsSection(laps: List<RunLap>) {
    if (laps.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Text(stringResource(R.string.run_laps_title), style = MaterialTheme.typography.titleMedium)
        laps.forEachIndexed { index, lap ->
            val metrics = lap.metricsSince(laps.getOrNull(index - 1))
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.run_lap_number, index + 1))
                    metrics.elapsedDurationMs
                        ?.takeIf { it > metrics.durationMs }
                        ?.let { elapsedMs ->
                            Text(
                                stringResource(R.string.run_lap_elapsed_time, RunMetrics.formatDuration(elapsedMs)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                }
                Text(stringResource(
                    R.string.run_lap_values,
                    RunMetrics.formatDuration(metrics.durationMs),
                    RunFormatting.distanceLabel(metrics.distanceMeters)
                ))
            }
        }
    }
}
