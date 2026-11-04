package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMapPoi
import com.example.gymtimer2.ui.screens.runs.RunMapPoiUiState

@Composable
internal fun RunMapPoiStatus(
    state: RunMapPoiUiState,
    selectedPoi: RunMapPoi?,
    modifier: Modifier = Modifier
) {
    val message = when {
        state.needsZoom -> R.string.run_map_poi_zoom_in
        state.isLoading -> R.string.run_map_poi_loading
        state.hasError -> R.string.run_map_poi_failed
        state.hasSearched && state.points.isEmpty() -> R.string.run_map_poi_empty
        else -> null
    }
    if (selectedPoi == null && message == null) return

    Surface(
        modifier = modifier.widthIn(max = 300.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp
    ) {
        Column(
            Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            selectedPoi?.let { RunMapPoiDetails(it) }
            message?.let {
                Text(stringResource(it), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
