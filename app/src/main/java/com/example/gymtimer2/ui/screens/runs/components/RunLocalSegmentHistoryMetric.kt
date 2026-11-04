package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.PlannedRouteModel
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunMetrics

/** Mostra as durações das tentativas locais no detalhe da corrida. */
@Composable
internal fun RunLocalSegmentHistoryMetric(
    startedAtDurationMs: Long?,
    completedAtDurationMs: Long?,
    efforts: List<RunLocalSegmentEffort>,
    plannedRoutes: List<PlannedRouteModel>
) {
    if (startedAtDurationMs != null && completedAtDurationMs != null) {
        RunMetric(
            label = stringResource(R.string.run_local_segment_history_duration),
            value = RunMetrics.formatDuration((completedAtDurationMs - startedAtDurationMs).coerceAtLeast(0L)),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }

    efforts.forEach { effort ->
        val durationMs = effort.durationMs ?: return@forEach
        val routeName = plannedRoutes.firstOrNull { it.id == effort.routeId }?.name
        RunMetric(
            label = routeName?.let {
                stringResource(R.string.run_local_segment_history_named_duration, it)
            } ?: stringResource(R.string.run_local_segment_history_duration),
            value = RunMetrics.formatDuration(durationMs),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }
}
