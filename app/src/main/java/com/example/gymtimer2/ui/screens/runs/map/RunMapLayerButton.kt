package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R

/** Liga camadas locais independentes ou limpa todas para voltar ao mapa padrão. */
@Composable
internal fun RunMapLayerButton(
    selectedLayers: Set<RunMapLayer>,
    gradientAvailable: Boolean,
    weeklyActivityAvailable: Boolean,
    localHistoryAvailable: Boolean,
    poiLayerEnabled: Boolean,
    onSelect: (RunMapLayer) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            shape = CircleShape,
            color = if (selectedLayers.isEmpty()) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (selectedLayers.isEmpty()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 4.dp
        ) {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.Layers, contentDescription = stringResource(R.string.run_map_layers))
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            RunMapLayerMenuItem(RunMapLayer.STANDARD, R.string.run_map_layer_standard, selectedLayers, onSelect) {
                menuExpanded = false
            }
            if (gradientAvailable) {
                RunMapLayerMenuItem(RunMapLayer.GRADIENT, R.string.run_map_layer_gradient, selectedLayers, onSelect) {
                    menuExpanded = false
                }
            }
            if (weeklyActivityAvailable) {
                RunMapLayerMenuItem(
                    RunMapLayer.WEEKLY_ACTIVITY,
                    R.string.run_map_layer_weekly_activity,
                    selectedLayers,
                    onSelect
                ) { menuExpanded = false }
            }
            if (localHistoryAvailable) {
                RunMapLayerMenuItem(
                    RunMapLayer.LOCAL_HISTORY,
                    R.string.run_map_layer_local_history,
                    selectedLayers,
                    onSelect
                ) { menuExpanded = false }
            }
            if (poiLayerEnabled) {
                RunMapLayerMenuItem(
                    RunMapLayer.OPENSTREETMAP_POI,
                    R.string.run_map_layer_pois,
                    selectedLayers,
                    onSelect
                ) { menuExpanded = false }
            }
        }
    }
}

@Composable
private fun RunMapLayerMenuItem(
    layer: RunMapLayer,
    labelResource: Int,
    selectedLayers: Set<RunMapLayer>,
    onSelect: (RunMapLayer) -> Unit,
    onDismiss: () -> Unit
) {
    val isSelected = selectedLayers.isSelected(layer)
    DropdownMenuItem(
        modifier = Modifier.semantics { selected = isSelected },
        text = { Text(stringResource(labelResource)) },
        onClick = {
            onSelect(layer)
            onDismiss()
        },
        trailingIcon = {
            if (isSelected) Icon(Icons.Default.Check, contentDescription = null)
        }
    )
}
