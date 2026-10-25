package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.ui.components.PrimaryButton
import com.example.gymtimer2.ui.components.SecondaryButton
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunStatus

/** Mostra os controles que correspondem ao estado atual da corrida. */
@Composable
internal fun RunControls(
    status: RunStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit
) {
    when (status) {
        RunStatus.RUNNING -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(onClick = onPause, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Pause, contentDescription = null)
                Text(stringResource(R.string.run_pause_action), modifier = Modifier.padding(start = 6.dp))
            }
            SecondaryButton(onClick = onFinish, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Text(stringResource(R.string.run_finish_action), modifier = Modifier.padding(start = 6.dp))
            }
        }
        RunStatus.PAUSED, RunStatus.INTERRUPTED -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(onClick = onResume, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.run_resume_action), modifier = Modifier.padding(start = 6.dp))
            }
            SecondaryButton(onClick = onFinish, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Text(stringResource(R.string.run_finish_action), modifier = Modifier.padding(start = 6.dp))
            }
        }
        RunStatus.STARTING -> Text(stringResource(R.string.run_starting_message))
        RunStatus.COMPLETED -> Unit
    }
}
