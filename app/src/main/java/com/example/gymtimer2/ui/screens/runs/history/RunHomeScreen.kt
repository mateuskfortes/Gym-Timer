package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.buildRunTrainingLogWeeks
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.screens.runs.RunUiState
import com.example.gymtimer2.ui.screens.runs.components.RunBestEffortRecordsSection

/** Mostra o histórico e abre a sessão atual numa tela exclusiva. */
@Composable
internal fun RunHomeScreen(
    state: RunUiState,
    onStartRun: () -> Unit,
    onOpenActiveRun: () -> Unit,
    onOpenTrainingLog: () -> Unit,
    onSelectRun: (Long) -> Unit,
    onDeleteRun: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val trainingLogWeeks = remember(state.completedRuns) {
        buildRunTrainingLogWeeks(state.completedRuns)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val activeRun = state.activeRun
        if (activeRun != null) {
            item {
                ActiveRunCard(run = activeRun, onOpen = onOpenActiveRun)
            }
        } else {
            item {
                PrimaryButton(onClick = onStartRun, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text(
                        stringResource(R.string.run_start_action),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        trainingLogWeeks.firstOrNull()?.let { currentWeek ->
            item {
                RunTrainingLogPreview(currentWeek, onOpenTrainingLog)
            }
        }
        item {
            Text(
                text = stringResource(R.string.run_history_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
        if (state.bestEffortRecords.annual.isNotEmpty()) {
            item {
                RunBestEffortRecordsSection(
                    records = state.bestEffortRecords,
                    onSelectRun = onSelectRun
                )
            }
        }
        if (state.completedRuns.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.run_history_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(state.completedRuns, key = { it.id }) { run ->
            RunHistoryCard(
                run = run,
                onClick = { onSelectRun(run.id) },
                onDelete = { onDeleteRun(run.id) },
                // A chave estável permite ao LazyColumn animar a remoção e os cartões vizinhos.
                modifier = Modifier.animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = tween(HISTORY_ITEM_FADE_DURATION_MS),
                    placementSpec = tween(HISTORY_ITEM_MOVE_DURATION_MS)
                )
            )
        }
    }
}

private const val HISTORY_ITEM_FADE_DURATION_MS = 180
private const val HISTORY_ITEM_MOVE_DURATION_MS = 280
