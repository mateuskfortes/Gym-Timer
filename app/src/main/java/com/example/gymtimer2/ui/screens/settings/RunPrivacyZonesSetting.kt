package com.example.gymtimer2.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunPrivacyZone
import com.example.gymtimer2.ui.screens.runs.map.RunMap
import org.maplibre.android.geometry.LatLng

@Composable
internal fun RunPrivacyZonesSetting(
    zones: List<RunPrivacyZone>,
    distanceUnit: RunDistanceUnit,
    onSave: (RunPrivacyZone) -> Unit,
    onDelete: (Long) -> Unit
) {
    var managerVisible by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<RunPrivacyZone?>(null) }
    var deletingZone by remember { mutableStateOf<RunPrivacyZone?>(null) }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.run_privacy_zones_setting), Modifier.weight(1f))
            TextButton(onClick = { managerVisible = true }) {
                Text(stringResource(R.string.run_privacy_zones_manage, zones.size))
            }
        }
        Text(
            stringResource(R.string.run_privacy_zones_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (managerVisible && editingZone == null && deletingZone == null) {
        PrivacyZonesDialog(
            zones = zones,
            distanceUnit = distanceUnit,
            onDismiss = { managerVisible = false },
            onAdd = { editingZone = newPrivacyZone() },
            onEdit = { editingZone = it },
            onDelete = { deletingZone = it }
        )
    }
    editingZone?.let { zone ->
        RunPrivacyZoneEditorDialog(
            zone = zone,
            distanceUnit = distanceUnit,
            onDismiss = { editingZone = null },
            onSave = {
                onSave(it)
                editingZone = null
            }
        )
    }
    deletingZone?.let { zone ->
        AlertDialog(
            onDismissRequest = { deletingZone = null },
            title = { Text(stringResource(R.string.run_privacy_zone_delete_title)) },
            text = { Text(stringResource(R.string.run_privacy_zone_delete_message, zone.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(zone.id)
                    deletingZone = null
                }) { Text(stringResource(R.string.run_privacy_zone_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingZone = null }) {
                    Text(stringResource(R.string.run_cancel_action))
                }
            }
        )
    }
}

@Composable
private fun PrivacyZonesDialog(
    zones: List<RunPrivacyZone>,
    distanceUnit: RunDistanceUnit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (RunPrivacyZone) -> Unit,
    onDelete: (RunPrivacyZone) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_privacy_zones_title)) },
        text = {
            Column(
                Modifier.heightIn(max = PRIVACY_ZONE_DIALOG_HEIGHT_DP.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                zones.forEach { zone ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                Modifier.weight(1f).clickable { onEdit(zone) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(zone.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    zone.radiusMeters.radiusLabel(distanceUnit),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { onDelete(zone) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.run_privacy_zone_delete_action, zone.name)
                                )
                            }
                        }
                    }
                }
                if (zones.isEmpty()) {
                    Text(
                        stringResource(R.string.run_privacy_zones_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.run_privacy_zone_add_action))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.run_privacy_zones_close_action)) }
        }
    )
}

@Composable
private fun RunPrivacyZoneEditorDialog(
    zone: RunPrivacyZone,
    distanceUnit: RunDistanceUnit,
    onDismiss: () -> Unit,
    onSave: (RunPrivacyZone) -> Unit
) {
    var name by remember(zone.id) { mutableStateOf(zone.name) }
    var radiusMeters by remember(zone.id) { mutableStateOf(zone.radiusMeters) }
    var visibleCenter by remember(zone.id) {
        mutableStateOf(if (zone.id == 0L) null else LatLng(zone.latitude, zone.longitude))
    }
    var selectedCenter by remember(zone.id) {
        mutableStateOf(if (zone.id == 0L) null else LatLng(zone.latitude, zone.longitude))
    }
    var radiusMenuExpanded by remember { mutableStateOf(false) }
    val cameraStart = selectedCenter ?: visibleCenter
    val canSave = name.isNotBlank() && selectedCenter != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (zone.id == 0L) R.string.run_privacy_zone_add_title
                    else R.string.run_privacy_zone_edit_title
                )
            )
        },
        text = {
            Column(
                Modifier.heightIn(max = PRIVACY_ZONE_EDITOR_HEIGHT_DP.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it.take(RunPrivacyZone.MAX_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.run_privacy_zone_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    TextButton(onClick = { radiusMenuExpanded = true }) {
                        Text(stringResource(R.string.run_privacy_zone_radius_label, radiusMeters.radiusLabel(distanceUnit)))
                    }
                    DropdownMenu(
                        expanded = radiusMenuExpanded,
                        onDismissRequest = { radiusMenuExpanded = false }
                    ) {
                        RunPrivacyZone.RADIUS_OPTIONS_METERS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.radiusLabel(distanceUnit)) },
                                onClick = {
                                    radiusMeters = option
                                    radiusMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Box {
                    RunMap(
                        points = emptyList(),
                        followLatestPoint = false,
                        modifier = Modifier.fillMaxWidth().height(PRIVACY_ZONE_MAP_HEIGHT_DP.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        initialCameraLocation = cameraStart,
                        onCameraCenterChanged = { visibleCenter = it }
                    )
                    Icon(
                        Icons.Default.Place,
                        contentDescription = stringResource(R.string.run_privacy_zone_map_center),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center).size(34.dp)
                    )
                }
                Button(
                    onClick = { selectedCenter = visibleCenter },
                    enabled = visibleCenter != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.run_privacy_zone_use_map_center))
                }
                Text(
                    if (selectedCenter == null) {
                        stringResource(R.string.run_privacy_zone_center_required)
                    } else {
                        stringResource(R.string.run_privacy_zone_center_selected)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val center = checkNotNull(selectedCenter)
                    onSave(zone.copy(
                        name = name.trim(),
                        latitude = center.latitude,
                        longitude = center.longitude,
                        radiusMeters = radiusMeters
                    ))
                }
            ) { Text(stringResource(R.string.run_privacy_zone_save_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.run_cancel_action)) }
        }
    )
}

private fun newPrivacyZone() = RunPrivacyZone(
    name = "",
    latitude = 0.0,
    longitude = 0.0,
    radiusMeters = RunPrivacyZone.RADIUS_OPTIONS_METERS.first()
)

@Composable
private fun Int.radiusLabel(distanceUnit: RunDistanceUnit): String = if (distanceUnit == RunDistanceUnit.METRIC) {
    stringResource(R.string.run_privacy_zone_radius_meters, this)
} else {
    stringResource(R.string.run_privacy_zone_radius_miles, IMPERIAL_RADIUS_LABELS[this / METERS_PER_EIGHTH_MILE])
}

private const val METERS_PER_EIGHTH_MILE = 200
private const val PRIVACY_ZONE_DIALOG_HEIGHT_DP = 360
private const val PRIVACY_ZONE_EDITOR_HEIGHT_DP = 520
private const val PRIVACY_ZONE_MAP_HEIGHT_DP = 200
private val IMPERIAL_RADIUS_LABELS = listOf("0", "⅛", "¼", "⅜", "½", "⅝", "¾", "⅞", "1")
