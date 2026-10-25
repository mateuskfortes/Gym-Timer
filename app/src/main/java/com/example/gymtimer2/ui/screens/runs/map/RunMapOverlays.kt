package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Posiciona mensagens, atribuição e ação de centralizar fora da área coberta pelo painel. */
@Composable
internal fun RunMapOverlays(
    waitingForLocation: Boolean,
    recenterEnabled: Boolean,
    recenterVisible: Boolean,
    bottomPadding: Dp,
    onRecenter: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        if (waitingForLocation) RunMapWaitingLocation(bottomPadding = bottomPadding)
        RunMapAttribution(bottomPadding = bottomPadding, modifier = Modifier.align(Alignment.BottomEnd))
        if (recenterEnabled && recenterVisible) {
            RunMapRecenterButton(
                onClick = onRecenter,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = bottomPadding + 56.dp)
            )
        }
    }
}
