@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunBestEffort
import com.example.gymtimer2.domain.model.RunBestEffortDuration
import com.example.gymtimer2.domain.model.RunBestEffortOverride
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting

/** Dialog para corrigir, remover ou restaurar o tempo GPS de uma marca. */
@Composable
internal fun RunBestEffortEditDialog(
    runId: Long,
    effort: RunBestEffort,
    currentOverride: RunBestEffortOverride?,
    onSave: (Long) -> Unit,
    onRemove: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember(runId, effort.distance, currentOverride) {
        mutableStateOf(RunMetrics.formatDuration(currentOverride?.durationMs ?: effort.durationMs))
    }
    val durationMs = remember(value) { RunBestEffortDuration.parse(value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.run_best_effort_edit_title,
                    stringResource(RunActivityFormatting.bestEffortDistanceResource(effort.distance))
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.run_best_effort_time_input)) },
                    placeholder = { Text(stringResource(R.string.run_best_effort_time_hint)) },
                    supportingText = {
                        if (durationMs == null) {
                            Text(
                                stringResource(R.string.run_best_effort_time_invalid),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    isError = durationMs == null,
                    singleLine = true
                )
                if (currentOverride?.isRemoved != true) {
                    TextButton(onClick = onRemove, modifier = Modifier.padding(top = 4.dp)) {
                        Text(stringResource(R.string.run_best_effort_remove_action))
                    }
                }
                if (currentOverride != null) {
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.run_best_effort_restore_action))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { durationMs?.let(onSave) }, enabled = durationMs != null) {
                Text(stringResource(R.string.run_best_effort_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.run_best_effort_cancel_action))
            }
        }
    )
}
