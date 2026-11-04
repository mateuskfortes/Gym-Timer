package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.ui.screens.runs.components.RunMetricsRow
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting

/** Mantém o estado e a ação de expansão visíveis e resume a corrida no painel recolhido. */
@Composable
internal fun RunSheetHeader(
    run: RunModel,
    expanded: Boolean,
    onToggle: () -> Unit,
    gpsStatus: RunGpsStatus = RunGpsStatus.GOOD,
    currentSplitPaceSecondsPerKm: Double? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth().height(
            if (expanded) RUN_SHEET_EXPANDED_HEADER_HEIGHT_DP.dp else RUN_SHEET_COLLAPSED_HEIGHT_DP.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(width = 32.dp, height = 4.dp).background(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(2.dp)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = RunActivityFormatting.statusLabel(run.status),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!expanded && gpsStatus != RunGpsStatus.GOOD) RunGpsIndicator(gpsStatus)
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(
                        if (expanded) R.string.run_collapse_panel else R.string.run_expand_panel
                    )
                )
            }
        }
        if (!expanded) {
            val useCurrentSplitPace = run.status == RunStatus.RUNNING && run.workoutTitle == null
            RunMetricsRow(
                run = run,
                modifier = Modifier.padding(horizontal = 20.dp),
                paceSecondsPerKm = RunMetrics.compactPaceSecondsPerKm(run, currentSplitPaceSecondsPerKm),
                paceLabelResource = if (useCurrentSplitPace) {
                    R.string.run_current_split_label
                } else {
                    R.string.run_pace_label
                }
            )
        }
    }
}

/** Altura compacta que mantém estado, controle e três métricas visíveis. */
internal const val RUN_SHEET_COLLAPSED_HEIGHT_DP = 128
private const val RUN_SHEET_EXPANDED_HEADER_HEIGHT_DP = 56
