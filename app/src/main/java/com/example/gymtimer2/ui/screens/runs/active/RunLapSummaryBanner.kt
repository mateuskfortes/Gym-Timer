package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.metricsSince
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import kotlinx.coroutines.delay

/** Mostra por alguns segundos a distância e o pace da volta recém-marcada. */
@Composable
internal fun RunLapSummaryBanner(runId: Long, laps: List<RunLap>, modifier: Modifier = Modifier) {
    val latestLap = laps.lastOrNull()
    val metrics = latestLap?.metricsSince(laps.getOrNull(laps.lastIndex - 1))
    var isVisible by remember(runId) { mutableStateOf(false) }

    LaunchedEffect(runId, latestLap?.id) {
        val recordedAtEpochMs = latestLap?.recordedAtEpochMs
        val elapsedMs = recordedAtEpochMs?.let { System.currentTimeMillis() - it }
        if (elapsedMs == null || elapsedMs !in 0..LAP_SUMMARY_DURATION_MS) {
            isVisible = false
            return@LaunchedEffect
        }

        isVisible = true
        delay(LAP_SUMMARY_DURATION_MS - elapsedMs)
        isVisible = false
    }

    if (latestLap != null && metrics != null) {
        AnimatedVisibility(visible = isVisible, modifier, enter = fadeIn(), exit = fadeOut()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = stringResource(R.string.run_lap_summary_title, laps.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(
                            R.string.run_lap_summary_values,
                            RunFormatting.distanceLabel(metrics.distanceMeters),
                            RunFormatting.pace(metrics.paceSecondsPerKm)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    metrics.elapsedDurationMs
                        ?.takeIf { it > metrics.durationMs }
                        ?.let { elapsedMs ->
                            Text(
                                text = stringResource(
                                    R.string.run_lap_elapsed_time,
                                    RunMetrics.formatDuration(elapsedMs)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                }
            }
        }
    }
}

private const val LAP_SUMMARY_DURATION_MS = 5_000L
