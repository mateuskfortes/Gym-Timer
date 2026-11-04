package com.example.gymtimer2.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunStartEndHideDistance

@Composable
internal fun RunStartEndHideDistanceSetting(
    selectedDistance: RunStartEndHideDistance,
    distanceUnit: RunDistanceUnit,
    onDistanceSelected: (RunStartEndHideDistance) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val title = stringResource(R.string.run_start_end_hide_distance_setting)

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f))
            Box {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.semantics { contentDescription = title }
                ) {
                    Text(selectedDistance.label(distanceUnit))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    RunStartEndHideDistance.entries.forEach { distance ->
                        DropdownMenuItem(
                            text = { Text(distance.label(distanceUnit)) },
                            onClick = {
                                onDistanceSelected(distance)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        Text(
            stringResource(R.string.run_start_end_hide_distance_description),
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun RunStartEndHideDistance.label(unit: RunDistanceUnit): String = when {
    this == RunStartEndHideDistance.NONE -> stringResource(R.string.run_start_end_hide_distance_none)
    unit == RunDistanceUnit.METRIC -> stringResource(
        R.string.run_start_end_hide_distance_meters,
        meters
    )
    else -> stringResource(
        R.string.run_start_end_hide_distance_miles,
        IMPERIAL_EIGHTH_MILE_LABELS[meters / METERS_PER_EIGHTH_MILE]
    )
}

private const val METERS_PER_EIGHTH_MILE = 200
private val IMPERIAL_EIGHTH_MILE_LABELS = listOf("0", "⅛", "¼", "⅜", "½", "⅝", "¾", "⅞", "1")
