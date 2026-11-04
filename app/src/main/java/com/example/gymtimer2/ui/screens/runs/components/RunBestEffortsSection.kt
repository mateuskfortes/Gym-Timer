package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunBestEffort
import com.example.gymtimer2.domain.model.RunBestEffortOverride
import com.example.gymtimer2.domain.model.RunBestEfforts
import com.example.gymtimer2.domain.model.RunBenchmarkDistance
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting

/** Mostra as marcas da corrida e permite corrigir ou ocultar cada esforço. */
@Composable
internal fun RunBestEffortsSection(
    runId: Long,
    points: List<RunPointModel>,
    overrides: List<RunBestEffortOverride>,
    onSaveOverride: (RunBestEffortOverride) -> Unit,
    onClearOverride: (RunBenchmarkDistance) -> Unit
) {
    val efforts = remember(points) { RunBestEfforts.calculate(points) }
    val overridesByDistance = remember(overrides) { overrides.associateBy(RunBestEffortOverride::distance) }
    var editingEffort by remember { mutableStateOf<RunBestEffort?>(null) }
    if (efforts.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.run_best_efforts_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.run_best_efforts_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        efforts.forEach { effort ->
            val override = overridesByDistance[effort.distance]
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(RunActivityFormatting.bestEffortDistanceResource(effort.distance)),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (override?.isRemoved == true) {
                    Text(
                        stringResource(R.string.run_best_effort_removed),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val durationMs = override?.durationMs ?: effort.durationMs
                    Text(RunMetrics.formatDuration(durationMs))
                    Text(
                        RunFormatting.pace(durationMs / effort.distance.distanceMeters),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                TextButton(onClick = { editingEffort = effort }) {
                    Text(stringResource(R.string.run_best_effort_edit_action))
                }
            }
        }
    }

    editingEffort?.let { effort ->
        RunBestEffortEditDialog(
            runId = runId,
            effort = effort,
            currentOverride = overridesByDistance[effort.distance],
            onSave = { durationMs ->
                onSaveOverride(RunBestEffortOverride(runId, effort.distance, durationMs))
                editingEffort = null
            },
            onRemove = {
                onSaveOverride(RunBestEffortOverride(runId, effort.distance, isRemoved = true))
                editingEffort = null
            },
            onClear = {
                onClearOverride(effort.distance)
                editingEffort = null
            },
            onDismiss = { editingEffort = null }
        )
    }
}
