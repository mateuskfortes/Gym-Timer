package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunGpsStatus

/** Mostra a qualidade da leitura que o serviço recebe do GPS. */
@Composable
internal fun RunGpsIndicator(status: RunGpsStatus, modifier: Modifier = Modifier) {
    val label = when (status) {
        RunGpsStatus.SEARCHING -> R.string.run_gps_searching
        RunGpsStatus.NO_SIGNAL -> R.string.run_gps_no_signal
        RunGpsStatus.WEAK -> R.string.run_gps_weak
        RunGpsStatus.GOOD -> R.string.run_gps_good
        RunGpsStatus.DISABLED -> R.string.run_gps_disabled
    }
    Text(
        stringResource(label),
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = if (status == RunGpsStatus.GOOD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    )
}
