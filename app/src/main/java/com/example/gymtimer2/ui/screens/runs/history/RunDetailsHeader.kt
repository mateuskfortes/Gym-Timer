package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.foundation.layout.padding
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
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting

@Composable
internal fun RunDetailsHeader(
    run: RunModel,
    points: List<RunPointModel>,
    canShareMap: Boolean,
    onBack: () -> Unit,
    onShareMap: () -> Unit
) {
    TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        Text(stringResource(R.string.run_back_action), modifier = Modifier.padding(start = 6.dp))
    }
    Text(run.title ?: RunActivityFormatting.activityTypeLabel(run.activityType), style = MaterialTheme.typography.headlineSmall)
    if (run.title != null) {
        Text(
            RunActivityFormatting.activityTypeLabel(run.activityType),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Text(
        RunActivityFormatting.formatDate(run.startedAtEpochMs),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    RunDetailsMapSection(points)
    if (canShareMap) {
        TextButton(onClick = onShareMap) {
            Text(stringResource(R.string.run_share_map_action))
        }
    }
}
