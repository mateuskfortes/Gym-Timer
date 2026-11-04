package com.example.gymtimer2.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunDistanceUnit

@Composable
internal fun RunDistanceUnitSetting(
    selectedUnit: RunDistanceUnit,
    onUnitSelected: (RunDistanceUnit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        RunDistanceUnit.METRIC to stringResource(R.string.run_distance_unit_metric),
        RunDistanceUnit.IMPERIAL to stringResource(R.string.run_distance_unit_imperial)
    )
    val title = stringResource(R.string.run_distance_unit_setting)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Box {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.semantics { contentDescription = title }
            ) {
                Text(options.first { it.first == selectedUnit }.second)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (unit, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onUnitSelected(unit)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
