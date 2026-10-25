package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.ui.screens.runs.components.RunMetricsRow

/** Agrupa métricas e ações num painel que pode ser recolhido sem pausar a gravação. */
@Composable
internal fun RunBottomSheet(
    run: RunModel,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().navigationBarsPadding()) {
        RunSheetHeader(run, expanded, onToggle)
        Column(
            Modifier.weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            RunMetricsRow(run)
            Spacer(Modifier.height(20.dp))
            RunControls(run.status, onPause, onResume, onFinish)
            Spacer(Modifier.height(24.dp))
        }
    }
}
