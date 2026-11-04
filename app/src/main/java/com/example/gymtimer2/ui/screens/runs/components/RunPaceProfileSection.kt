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
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.calculateRunPaceProfile
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Mostra o gráfico de pace da corrida usando somente os pontos GPS locais. */
@Composable
internal fun RunPaceProfileSection(points: List<RunPointModel>) {
    val profile by produceState(initialValue = emptyList(), points) {
        value = withContext(Dispatchers.Default) { calculateRunPaceProfile(points) }
    }
    val routeDistanceMeters = profile.lastOrNull()?.distanceMeters
        ?.takeIf { it.isFinite() && it > 0.0 }
        ?: return
    if (profile.size < 2) return

    var selectedIndex by remember(profile) { mutableIntStateOf(-1) }
    val fastestPace = RunFormatting.pace(profile.minOf { it.secondsPerKm })
    val slowestPace = RunFormatting.pace(profile.maxOf { it.secondsPerKm })
    val selectedPoint = profile.getOrNull(selectedIndex)
    val chartDescription = selectedPoint?.let {
        stringResource(
            R.string.run_pace_profile_selected_description,
            RunFormatting.pace(it.secondsPerKm),
            RunFormatting.distanceLabel(it.distanceMeters)
        )
    } ?: stringResource(R.string.run_pace_profile_description, fastestPace, slowestPace)

    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.run_pace_profile_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        RunMetricProfileChart(
            profile = profile,
            routeDistanceMeters = routeDistanceMeters,
            selectedIndex = selectedIndex,
            lowerValueAtTop = true,
            description = chartDescription,
            onSelectPoint = { selectedIndex = it }
        )
        selectedPoint?.let {
            Text(
                stringResource(
                    R.string.run_pace_profile_selected_value,
                    RunFormatting.pace(it.secondsPerKm),
                    RunFormatting.distanceLabel(it.distanceMeters)
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.run_pace_profile_fastest, fastestPace),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.run_pace_profile_slowest, slowestPace),
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
