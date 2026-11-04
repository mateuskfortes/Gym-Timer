package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R

@Composable
internal fun RunRouteAlertMuteDialog(
    onMuteOnce: () -> Unit,
    onMuteForRun: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_mute_route_alerts_title)) },
        text = { Text(stringResource(R.string.run_mute_route_alerts_message)) },
        confirmButton = {
            TextButton(onClick = onMuteOnce) {
                Text(stringResource(R.string.run_mute_route_alerts_once))
            }
        },
        dismissButton = {
            TextButton(onClick = onMuteForRun) {
                Text(stringResource(R.string.run_mute_route_alerts_activity))
            }
        }
    )
}
