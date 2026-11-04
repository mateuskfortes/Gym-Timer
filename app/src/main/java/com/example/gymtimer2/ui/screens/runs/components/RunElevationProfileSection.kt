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
import com.example.gymtimer2.domain.model.calculateRunElevationProfile
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Mostra o perfil de altitude da corrida salva quando há pontos utilizáveis. */
@Composable
internal fun RunElevationProfileSection(points: List<RunPointModel>) {
    val profile by produceState(initialValue = emptyList(), points) {
        value = withContext(Dispatchers.Default) { calculateRunElevationProfile(points) }
    }
    val routeDistanceMeters = profile.lastOrNull()?.distanceMeters
        ?.takeIf { it.isFinite() && it > 0.0 }
    if (profile.size < 2 || routeDistanceMeters == null) return

    val minimumElevation = RunFormatting.elevationLabel(profile.minOf { it.elevationMeters })
    val maximumElevation = RunFormatting.elevationLabel(profile.maxOf { it.elevationMeters })
    var selectedIndex by remember(profile) { mutableIntStateOf(-1) }
    val selectedPoint = profile.getOrNull(selectedIndex)
    val chartDescription = selectedPoint?.let {
        stringResource(
            R.string.run_elevation_profile_selected_description,
            RunFormatting.elevationLabel(it.elevationMeters),
            RunFormatting.distanceLabel(it.distanceMeters)
        )
    } ?: stringResource(R.string.run_elevation_profile_description, minimumElevation, maximumElevation)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.run_elevation_profile_title), style = MaterialTheme.typography.titleMedium)
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
                    R.string.run_elevation_profile_selected_value,
                    RunFormatting.elevationLabel(it.elevationMeters),
                    RunFormatting.distanceLabel(it.distanceMeters)
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.run_elevation_profile_min, minimumElevation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.run_elevation_profile_max, maximumElevation),
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
