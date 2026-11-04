package com.example.gymtimer2.ui.screens.runs.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunCadenceProfilePoint
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.calculateRunCadenceProfile
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Mostra a cadência ao longo da distância quando há leituras locais válidas. */
@Composable
internal fun RunCadenceProfileSection(points: List<RunPointModel>) {
    val profile by produceState(initialValue = emptyList<RunCadenceProfilePoint>(), points) {
        value = withContext(Dispatchers.Default) { calculateRunCadenceProfile(points) }
    }
    val routeDistanceMeters = profile.lastOrNull()?.distanceMeters
        ?.takeIf { it.isFinite() && it > 0.0 }
        ?: return
    if (profile.size < 2) return

    var selectedIndex by remember(profile) { mutableIntStateOf(-1) }
    val lowestCadence = profile.minOf(RunCadenceProfilePoint::stepsPerMinute)
    val highestCadence = profile.maxOf(RunCadenceProfilePoint::stepsPerMinute)
    val selectedPoint = profile.getOrNull(selectedIndex)
    val chartDescription = selectedPoint?.let {
        stringResource(
            R.string.run_cadence_profile_selected_description,
            it.stepsPerMinute,
            RunFormatting.distanceLabel(it.distanceMeters, 1)
        )
    } ?: stringResource(R.string.run_cadence_profile_description, lowestCadence, highestCadence)

    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.run_cadence_profile_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        RunMetricProfileChart(
            profile = profile,
            routeDistanceMeters = routeDistanceMeters,
            selectedIndex = selectedIndex,
            lowerValueAtTop = false,
            description = chartDescription,
            onSelectPoint = { selectedIndex = it }
        )
        selectedPoint?.let {
            Text(
                stringResource(
                    R.string.run_cadence_profile_selected_value,
                    it.stepsPerMinute,
                    RunFormatting.distanceLabel(it.distanceMeters, 1)
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.run_cadence_profile_lowest, lowestCadence),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.run_cadence_profile_highest, highestCadence),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                RunFormatting.distanceLabel(0.0, 1),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                RunFormatting.distanceLabel(routeDistanceMeters, 1),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
