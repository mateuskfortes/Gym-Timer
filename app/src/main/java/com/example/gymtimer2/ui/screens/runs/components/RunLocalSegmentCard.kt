package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.ui.screens.runs.RunLiveSegment
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import kotlin.math.abs

/** Mostra o progresso e a comparação da tentativa local no percurso selecionado. */
@Composable
internal fun RunLocalSegmentCard(segment: RunLiveSegment) {
    val distanceToStartMeters = segment.distanceToStartMeters
    val progress = (segment.progressMeters / segment.distanceMeters).toFloat().coerceIn(0f, 1f)
    val comparisonText = when {
        distanceToStartMeters != null -> null
        segment.isNewRecord -> stringResource(R.string.run_local_segment_new_record)
        segment.bestDurationMs == null -> stringResource(R.string.run_local_segment_first_attempt)
        segment.isCompleted -> stringResource(R.string.run_local_segment_completed)
        segment.differenceFromBestMs == null -> null
        abs(segment.differenceFromBestMs) < 1_000L -> stringResource(R.string.run_local_segment_even)
        segment.differenceFromBestMs > 0L -> stringResource(
            R.string.run_local_segment_ahead,
            RunMetrics.formatDuration(segment.differenceFromBestMs)
        )
        else -> stringResource(
            R.string.run_local_segment_behind,
            RunMetrics.formatDuration(abs(segment.differenceFromBestMs))
        )
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                stringResource(
                    if (distanceToStartMeters == null) R.string.run_local_segment_title
                    else R.string.run_local_segment_approaching
                ),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                segment.name ?: stringResource(R.string.run_local_segment_default_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (distanceToStartMeters == null) {
                Spacer(Modifier.height(8.dp))
                if (segment.elevationProfile.size >= 2) {
                    RunElevationProfileChart(
                        profile = segment.elevationProfile,
                        routeDistanceMeters = segment.distanceMeters,
                        progressMeters = segment.progressMeters,
                        description = stringResource(R.string.run_local_segment_elevation_description)
                    )
                } else {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(
                        R.string.run_local_segment_progress,
                        RunFormatting.distanceLabel(segment.progressMeters),
                        RunFormatting.distanceLabel(segment.distanceMeters)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(R.string.run_local_segment_time, RunMetrics.formatDuration(segment.elapsedDurationMs)),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    stringResource(
                        R.string.run_local_segment_start_distance,
                        RunFormatting.adaptiveDistanceLabel(distanceToStartMeters)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            segment.bestDurationMs?.let { bestDurationMs ->
                Text(
                    stringResource(R.string.run_local_segment_best, RunMetrics.formatDuration(bestDurationMs)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            comparisonText?.let { text ->
                Spacer(Modifier.height(4.dp))
                Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
