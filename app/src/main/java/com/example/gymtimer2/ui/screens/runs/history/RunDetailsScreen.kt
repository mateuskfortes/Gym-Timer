package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.ui.screens.runs.components.RunMetricsRow
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.map.RunMap

/** Mostra o mapa e as métricas de uma corrida escolhida no histórico. */
@Composable
internal fun RunDetailsScreen(
    run: RunModel,
    points: List<RunPointModel>,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Text(stringResource(R.string.run_back_action), modifier = Modifier.padding(start = 6.dp))
        }
        Text(RunFormatting.displayTitle(run), style = MaterialTheme.typography.headlineSmall)
        if (run.title != null) {
            Text(
                RunFormatting.formatDate(run.startedAtEpochMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(14.dp))
        if (points.isEmpty()) {
            EmptyRunMap()
        } else {
            RunMap(
                points = points,
                followLatestPoint = false,
                useTextureView = true,
                modifier = Modifier.fillMaxWidth().height(320.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        RunMetricsRow(run)
        TextButton(onClick = onDelete) {
            Text(stringResource(R.string.run_delete_action), color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(20.dp))
    }
}
