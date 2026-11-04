package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R

@Composable
internal fun RunRouteFarDistanceDialog(
    onUseRoute: () -> Unit,
    onSwitchRoute: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onUseRoute,
        title = { Text(stringResource(R.string.run_route_far_title)) },
        text = { Text(stringResource(R.string.run_route_far_message)) },
        confirmButton = {
            TextButton(onClick = onUseRoute) {
                Text(stringResource(R.string.run_route_far_use))
            }
        },
        dismissButton = {
            TextButton(onClick = onSwitchRoute) {
                Text(stringResource(R.string.run_route_far_switch))
            }
        }
    )
}
