package com.example.gymtimer2.ui.screens.runs.history

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.ui.screens.runs.map.RunMap

/** Mostra a rota salva ou o estado vazio quando a corrida não tem pontos GPS. */
@Composable
internal fun RunDetailsMapSection(points: List<RunPointModel>, height: Dp = 320.dp) {
    if (points.isEmpty()) {
        EmptyRunMap()
    } else {
        RunMap(
            points = points,
            followLatestPoint = false,
            useTextureView = true,
            modifier = Modifier.fillMaxWidth().height(height)
        )
    }
}
