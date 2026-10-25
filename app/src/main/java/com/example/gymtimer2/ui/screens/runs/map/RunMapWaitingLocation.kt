package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R

/** Aguarda uma posição sem exibir a visão global durante a busca. */
@Composable
internal fun RunMapWaitingLocation(bottomPadding: Dp) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.run_map_waiting_location), modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
