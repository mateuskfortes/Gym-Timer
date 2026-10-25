package com.example.gymtimer2.ui.screens.runs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.gymtimer2.ui.screens.runs.active.ActiveRunScreen
import com.example.gymtimer2.ui.screens.runs.history.RunDeleteDialog
import com.example.gymtimer2.ui.screens.runs.history.RunDetailsScreen
import com.example.gymtimer2.ui.screens.runs.history.RunHomeScreen
import com.example.gymtimer2.ui.screens.runs.permissions.rememberRunStartAction
import kotlinx.coroutines.launch

/** Encaminha o estado para a tela inicial, o detalhe salvo ou a corrida em tela cheia. */
@Composable
fun RunScreen(
    state: RunUiState,
    onStartRun: () -> Unit,
    onPauseRun: (Long) -> Unit,
    onResumeRun: (Long) -> Unit,
    onFinishRun: (Long, String?) -> Unit,
    onDiscardRun: (Long) -> Unit,
    onSelectRun: (Long) -> Unit,
    onDeleteRun: (Long) -> Unit,
    onBackFromDetails: () -> Unit,
    onOpenActiveRun: () -> Unit,
    onCloseActiveRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeRun = state.activeRun
    if (state.isActiveRunOpen && activeRun != null) {
        ActiveRunScreen(
            run = activeRun,
            points = state.activePoints,
            onPause = { onPauseRun(activeRun.id) },
            onResume = { onResumeRun(activeRun.id) },
            onFinish = { title -> onFinishRun(activeRun.id, title) },
            onDiscard = { onDiscardRun(activeRun.id) },
            onBack = onCloseActiveRun,
            modifier = modifier
        )
        return
    }

    var pendingDeletionId by rememberSaveable { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val requestStart = rememberRunStartAction(context, onStartRun) { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) { insets ->
        val selectedRun = state.selectedRun
        if (selectedRun != null) {
            RunDetailsScreen(
                run = selectedRun,
                points = state.selectedPoints,
                onBack = onBackFromDetails,
                onDelete = { pendingDeletionId = selectedRun.id },
                modifier = Modifier.padding(insets)
            )
        } else {
            RunHomeScreen(
                state = state,
                onStartRun = requestStart,
                onOpenActiveRun = onOpenActiveRun,
                onSelectRun = onSelectRun,
                onDeleteRun = { pendingDeletionId = it },
                modifier = Modifier.padding(insets)
            )
        }
    }
    state.completedRuns.firstOrNull { it.id == pendingDeletionId }?.let { run ->
        RunDeleteDialog(
            run = run,
            onConfirm = {
                pendingDeletionId = null
                onDeleteRun(run.id)
            },
            onDismiss = { pendingDeletionId = null }
        )
    }
}
