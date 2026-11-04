package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunSplit
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.calculateRunSplits
import com.example.gymtimer2.domain.model.currentRunSplitPaceSecondsPerKm
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.ui.screens.runs.format.LocalRunDistanceUnit
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lista parciais completas e, na sessão ativa, o pace da parcial atual. */
@Composable
internal fun RunSplitsSection(
    points: List<RunPointModel>,
    activeRun: RunModel? = null,
    onShareSplits: ((List<RunSplit>) -> Unit)? = null
) {
    RunSplitsSection(
        rememberRunSplitsUiState(points, activeRun, LocalRunDistanceUnit.current),
        onShareSplits
    )
}

@Composable
internal fun RunSplitsSection(state: RunSplitsUiState, onShareSplits: ((List<RunSplit>) -> Unit)? = null) {
    val splits = state.completedSplits
    val currentPace = state.currentSplitPaceSecondsPerKm
    val unitLabel = RunFormatting.distanceUnitLabel()
    if (splits.isEmpty() && currentPace == null) return
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        if (splits.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.run_splits_title, unitLabel),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                if (onShareSplits != null) {
                    IconButton(
                        onClick = { onShareSplits(splits) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.run_share_splits_action)
                        )
                    }
                }
            }
            Text(stringResource(R.string.run_splits_estimate), style = MaterialTheme.typography.bodySmall)
        }
        splits.forEach { split ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.run_split_distance, split.number, unitLabel))
                Text(RunMetrics.formatDuration(split.durationMs))
            }
        }
        currentPace?.let { pace ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.run_current_split_label))
                Text(RunFormatting.pace(pace))
            }
        }
    }
}

/** Produz as parciais uma vez para a seção e o cabeçalho minimizado usarem os mesmos dados. */
@Composable
internal fun rememberRunSplitsUiState(
    points: List<RunPointModel>,
    activeRun: RunModel?,
    distanceUnit: RunDistanceUnit = LocalRunDistanceUnit.current
): RunSplitsUiState {
    val splits by produceState(initialValue = emptyList(), points, distanceUnit) {
        value = emptyList()
        value = withContext(Dispatchers.Default) { calculateRunSplits(points, distanceUnit) }
    }
    val currentPace = remember(activeRun?.durationMs, activeRun?.distanceMeters, splits, distanceUnit) {
        activeRun?.let { currentRunSplitPaceSecondsPerKm(it, splits, distanceUnit) }
    }
    return RunSplitsUiState(splits, currentPace)
}

/** Evita calcular parciais automáticas quando o treino substitui essa métrica. */
@Composable
internal fun rememberRunSplitsUiStateForRun(
    run: RunModel,
    points: List<RunPointModel>,
    activeRun: RunModel?
): RunSplitsUiState? = if (RunMetrics.shouldShowAutomaticSplits(run)) {
    rememberRunSplitsUiState(points, activeRun)
} else {
    null
}

/** Dados calculados para exibir parciais e o pace da parcial atual. */
internal data class RunSplitsUiState(
    val completedSplits: List<RunSplit>,
    val currentSplitPaceSecondsPerKm: Double?
)
