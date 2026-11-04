package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.estimatedElevationGainMeters
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Calcula o ganho de elevação fora da thread principal e o apresenta como estimativa. */
@Composable
internal fun RunElevationMetric(points: List<RunPointModel>, modifier: Modifier = Modifier.fillMaxWidth()) {
    val gain by produceState<Double?>(initialValue = null, points) {
        value = withContext(Dispatchers.Default) { estimatedElevationGainMeters(points) }
    }
    RunMetric(
        label = stringResource(R.string.run_elevation_gain_label),
        value = gain?.let { RunFormatting.elevationLabel(it) } ?: "--",
        modifier = modifier
    )
}
