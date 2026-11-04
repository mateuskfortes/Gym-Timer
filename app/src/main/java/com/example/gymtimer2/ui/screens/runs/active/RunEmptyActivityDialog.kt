package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R
import com.example.gymtimer2.ui.components.PrimaryButton

/** Oferece retomar ou iniciar a confirmação de descarte quando a sessão não registrou distância. */
@Composable
internal fun RunEmptyActivityDialog(onResume: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onResume,
        title = { Text(stringResource(R.string.run_empty_activity_title)) },
        text = { Text(stringResource(R.string.run_empty_activity_message)) },
        confirmButton = {
            PrimaryButton(onClick = onResume) {
                Text(stringResource(R.string.run_resume_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.run_discard_action))
            }
        }
    )
}
