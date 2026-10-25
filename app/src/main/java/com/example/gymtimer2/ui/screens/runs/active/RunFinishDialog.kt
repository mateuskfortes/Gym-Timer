package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunTitle
import com.example.gymtimer2.ui.components.AppTextField
import com.example.gymtimer2.ui.components.PrimaryButton

/** Oferece título opcional, salvamento e descarte confirmado sem alterar a sessão ao cancelar. */
@Composable
internal fun RunFinishDialog(onConfirm: (String?) -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    if (confirmDiscard) {
        RunDiscardDialog(onConfirm = onDiscard, onDismiss = { confirmDiscard = false })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_finish_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.run_finish_message))
                AppTextField(
                    value = title,
                    onValueChange = { title = it.take(RunTitle.MAX_LENGTH) },
                    label = { Text(stringResource(R.string.run_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    outlined = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    )
                )
                Text(
                    stringResource(R.string.run_title_character_count, title.length, RunTitle.MAX_LENGTH),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { confirmDiscard = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text(stringResource(R.string.run_discard_action))
                }
            }
        },
        confirmButton = {
            PrimaryButton(onClick = { onConfirm(RunTitle.normalize(title)) }) {
                Text(stringResource(R.string.run_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}
