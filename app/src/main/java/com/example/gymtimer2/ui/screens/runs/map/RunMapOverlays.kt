package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.RunMapPoi
import com.example.gymtimer2.ui.screens.runs.RunMapPoiUiState

/** Posiciona mensagens e controles do mapa fora da área coberta pelo painel. */
@Composable
internal fun RunMapOverlays(
    waitingForLocation: Boolean,
    recenterEnabled: Boolean,
    recenterVisible: Boolean,
    routeOverviewEnabled: Boolean,
    gradientAvailable: Boolean,
    weeklyHeatmapAvailable: Boolean,
    localHistoryAvailable: Boolean,
    poiLayerEnabled: Boolean,
    poiState: RunMapPoiUiState,
    selectedMapLayers: Set<RunMapLayer>,
    selectedPoi: RunMapPoi?,
    bottomPadding: Dp,
    onRecenter: () -> Unit,
    onSelectMapLayer: (RunMapLayer) -> Unit,
    onShowRouteOverview: () -> Unit
) {
    val showRecenterButton = recenterEnabled && recenterVisible
    val showLayerButton = gradientAvailable || weeklyHeatmapAvailable || localHistoryAvailable || poiLayerEnabled
    Box(Modifier.fillMaxSize()) {
        if (waitingForLocation) RunMapWaitingLocation(bottomPadding = bottomPadding)
        RunMapAttribution(bottomPadding = bottomPadding, modifier = Modifier.align(Alignment.BottomEnd))
        Column(
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = bottomPadding + 56.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showLayerButton) {
                RunMapLayerButton(
                    selectedLayers = selectedMapLayers,
                    gradientAvailable = gradientAvailable,
                    weeklyActivityAvailable = weeklyHeatmapAvailable,
                    localHistoryAvailable = localHistoryAvailable,
                    poiLayerEnabled = poiLayerEnabled,
                    onSelect = onSelectMapLayer
                )
            }
            if (routeOverviewEnabled) {
                RunMapRouteOverviewButton(onClick = onShowRouteOverview)
            }
            if (showRecenterButton) {
                RunMapRecenterButton(onClick = onRecenter)
            }
        }
        if (RunMapLayer.OPENSTREETMAP_POI in selectedMapLayers) {
            RunMapPoiStatus(
                poiState,
                selectedPoi,
                modifier = Modifier.align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = bottomPadding + 16.dp)
            )
        }
    }
}
