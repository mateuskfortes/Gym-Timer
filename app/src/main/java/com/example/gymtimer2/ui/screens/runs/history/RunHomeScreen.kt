package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.R
import com.example.gymtimer2.ui.screens.runs.RunUiState

/** Mostra o histórico e abre a sessão atual numa tela exclusiva. */
@Composable
internal fun RunHomeScreen(
    state: RunUiState,
    onStartRun: () -> Unit,
    onOpenActiveRun: () -> Unit,
    onSelectRun: (Long) -> Unit,
    onDeleteRun: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
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
                PrimaryButton(onClick = onStartRun, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text(stringResource(R.string.run_start_action), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.run_history_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 14.dp)
            )
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
