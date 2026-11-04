package com.example.gymtimer2.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.example.gymtimer2.R

@Composable
internal fun RunTurnCueCountSetting(count: Int, enabled: Boolean, onCountChanged: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val title = stringResource(R.string.run_turn_cue_count_setting)
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(title, modifier = Modifier.weight(1f))
            TextButton(
                enabled = enabled,
                onClick = { showDialog = true },
                modifier = Modifier.semantics { contentDescription = title }
            ) {
                Text(stringResource(R.string.run_turn_cue_count_value, count))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        Text(
            stringResource(R.string.run_turn_cue_count_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (showDialog) RunTurnCueCountDialog(count, onCountChanged) { showDialog = false }
}

@Composable
private fun RunTurnCueCountDialog(count: Int, onCountChanged: (Int) -> Unit, onDismiss: () -> Unit) {
    var input by remember(count) { mutableStateOf(count.toString()) }
    val parsedCount = input.toIntOrNull()?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_turn_cue_count_setting)) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.run_turn_cue_count_setting)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = parsedCount == null
                )
                if (parsedCount == null) {
                    Text(
                        stringResource(R.string.run_turn_cue_count_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedCount != null,
                onClick = {
                    parsedCount?.let(onCountChanged)
                    onDismiss()
                }
            ) { Text(stringResource(R.string.run_body_mass_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}
