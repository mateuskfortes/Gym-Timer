package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.PlannedRouteModel
import com.example.gymtimer2.domain.model.RunBenchmarkDistance
import com.example.gymtimer2.domain.model.RunBestEffortOverride
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunStepRateSample
import com.example.gymtimer2.domain.model.averageRunCadence
import com.example.gymtimer2.domain.model.averageRunCadenceFromEvents
import com.example.gymtimer2.ui.screens.runs.components.RunBestEffortsSection
import com.example.gymtimer2.ui.screens.runs.components.RunCadenceProfileSection
import com.example.gymtimer2.ui.screens.runs.components.RunElevationMetric
import com.example.gymtimer2.ui.screens.runs.components.RunElevationProfileSection
import com.example.gymtimer2.ui.screens.runs.components.RunLocalSegmentHistoryMetric
import com.example.gymtimer2.ui.screens.runs.components.RunMetric
import com.example.gymtimer2.ui.screens.runs.components.RunMetricsRow
import com.example.gymtimer2.ui.screens.runs.components.RunPaceProfileSection
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting

/** Reúne as métricas salvas e os detalhes de desempenho da corrida. */
@Composable
internal fun RunDetailsMetricsSection(
    run: RunModel,
    points: List<RunPointModel>,
    stepRateSamples: List<RunStepRateSample>,
    bestEffortOverrides: List<RunBestEffortOverride>,
    onSaveBestEffortOverride: (RunBestEffortOverride) -> Unit,
    onClearBestEffortOverride: (RunBenchmarkDistance) -> Unit,
    localSegmentEfforts: List<RunLocalSegmentEffort>,
    plannedRoutes: List<PlannedRouteModel>
) {
    Spacer(Modifier.height(14.dp))
    RunMetricsRow(run)
    RunBestEffortsSection(
        runId = run.id,
        points = points,
        overrides = bestEffortOverrides,
        onSaveOverride = onSaveBestEffortOverride,
        onClearOverride = onClearBestEffortOverride
    )
    RunLocalSegmentHistoryMetric(
        startedAtDurationMs = run.localSegmentStartedAtDurationMs,
        completedAtDurationMs = run.localSegmentCompletedAtDurationMs,
        efforts = localSegmentEfforts,
        plannedRoutes = plannedRoutes
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RunMetric(
            label = stringResource(R.string.run_average_speed_label),
            value = run.averageSpeedKmPerHour?.let { RunFormatting.speedLabel(it) } ?: "--",
            modifier = Modifier.weight(1f)
        )
        RunMetric(
            label = stringResource(R.string.run_average_cadence_label),
            value = (averageRunCadenceFromEvents(stepRateSamples) ?: averageRunCadence(points))
                ?.let { stringResource(R.string.run_cadence_value, it) } ?: "--",
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(8.dp))
    RunPaceProfileSection(points)
    RunCadenceProfileSection(points)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RunMetric(
            label = stringResource(R.string.run_estimated_calories_label),
            value = run.estimatedCaloriesKcal?.let {
                stringResource(R.string.run_estimated_calories_value, it)
            } ?: "--",
            modifier = Modifier.weight(1f)
        )
        RunElevationMetric(points, Modifier.weight(1f))
    }
    RunElevationProfileSection(points)
}
