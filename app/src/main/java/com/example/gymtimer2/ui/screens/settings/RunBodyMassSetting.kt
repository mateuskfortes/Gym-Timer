package com.example.gymtimer2.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.parseRunBodyMassKg

@Composable
internal fun RunBodyMassSetting(bodyMassKg: Double?, onBodyMassKgChanged: (Double?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val label = stringResource(R.string.run_body_mass_setting)
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f))
            TextButton(
                onClick = { showDialog = true },
                modifier = Modifier.semantics { contentDescription = label }
            ) {
                Text(bodyMassKg?.let { stringResource(R.string.run_body_mass_value, it) }
                    ?: stringResource(R.string.run_body_mass_unset))
            }
        }
        Text(
            stringResource(R.string.run_body_mass_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (showDialog) RunBodyMassDialog(bodyMassKg, onBodyMassKgChanged) { showDialog = false }
}

@Composable
private fun RunBodyMassDialog(
    bodyMassKg: Double?,
    onBodyMassKgChanged: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember(bodyMassKg) { mutableStateOf(bodyMassKg?.toString().orEmpty()) }
    val parsedBodyMassKg = parseRunBodyMassKg(input)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_body_mass_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.run_body_mass_input_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = input.isNotBlank() && parsedBodyMassKg == null
                )
                if (input.isNotBlank() && parsedBodyMassKg == null) {
                    Text(
                        stringResource(R.string.run_body_mass_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedBodyMassKg != null,
                onClick = {
                    parsedBodyMassKg?.let(onBodyMassKgChanged)
                    onDismiss()
                }
            ) { Text(stringResource(R.string.run_body_mass_save)) }
        },
        dismissButton = {
            Row {
                if (bodyMassKg != null) {
                    TextButton(onClick = {
                        onBodyMassKgChanged(null)
                        onDismiss()
                    }) { Text(stringResource(R.string.run_body_mass_remove)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
            }
        }
    )
}
