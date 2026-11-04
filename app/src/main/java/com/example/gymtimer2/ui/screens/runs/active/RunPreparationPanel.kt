package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunMetrics
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import com.example.gymtimer2.ui.screens.runs.components.RunManualLapsSetting

/** Painel inferior com estado do GPS, métricas iniciais e ação principal. */
@Composable
internal fun RunPreparationPanel(
    gpsStatus: RunGpsStatus,
    selectedRouteTitle: String?,
    paceTarget: RunPaceTarget?,
    workoutTitle: String? = null,
    onChooseRoute: () -> Unit,
    onEditPaceTarget: () -> Unit,
    onEditWorkout: () -> Unit = {},
    onStart: () -> Unit,
    manualLapsEnabled: Boolean = false,
    onManualLapsChanged: (Boolean) -> Unit = {},
    activityType: RunActivityType = RunActivityType.RUN,
    onEditActivityType: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activityTypeLabel = RunActivityFormatting.activityTypeLabel(activityType)
    val startDescription = "${stringResource(R.string.run_preparation_start)} $activityTypeLabel"
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                Modifier.size(width = 36.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
            )
            Column(
                modifier = Modifier.weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = if (gpsStatus == RunGpsStatus.GOOD) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                        RunGpsIndicator(
                            status = gpsStatus,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                RunWorkoutSelector(workoutTitle, onEditWorkout)
                RunPreparationMetrics()
                RunPreparationPaceTarget(paceTarget, onEditPaceTarget)
                RunManualLapsSetting(manualLapsEnabled, onManualLapsChanged)
                selectedRouteTitle?.let {
                    Text(
                        stringResource(R.string.run_route_selected, it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RunActivityTypeSelector(
                    label = activityTypeLabel,
                    onClick = onEditActivityType,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onStart,
                    modifier = Modifier.size(88.dp).semantics {
                        contentDescription = startDescription
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp))
                        Text(stringResource(R.string.run_preparation_start), fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onChooseRoute, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Route, contentDescription = null)
                        Text(
                            stringResource(if (selectedRouteTitle == null) R.string.run_add_route else R.string.run_change_route),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RunPreparationMetrics() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RunPreparationMetric(
            label = stringResource(R.string.run_time_label),
            value = RunMetrics.formatDuration(0L),
            modifier = Modifier.weight(1f)
        )
        RunPreparationMetric(
            label = stringResource(R.string.run_pace_label),
            value = RunFormatting.pace(null),
            modifier = Modifier.weight(1f)
        )
        RunPreparationMetric(
            label = stringResource(R.string.run_distance_label),
            value = RunFormatting.distanceLabel(0.0),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RunPreparationPaceTarget(target: RunPaceTarget?, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.run_pace_target_label), style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = onEdit) {
            Text(
                target?.let {
                    stringResource(
                        R.string.run_pace_target_range,
                        RunFormatting.pace(it.fasterSecondsPerKm.toDouble()),
                        RunFormatting.pace(it.slowerSecondsPerKm.toDouble())
                    )
                } ?: stringResource(R.string.run_pace_target_add)
            )
        }
    }
}
