package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.PlannedRouteModel
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting

/** Permite escolher uma corrida salva como percurso-alvo ou remover a escolha atual. */
@Composable
internal fun RunRoutePickerDialog(
    routes: List<RunModel>,
    plannedRoutes: List<PlannedRouteModel>,
    onSetRouteFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    onSelectRoute: (Long?, Long?) -> Unit,
    onImportRoute: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_route_picker_title)) },
        text = {
            Column {
                TextButton(onClick = { onSelectRoute(null, null) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.run_route_none))
                }
                TextButton(onClick = onImportRoute, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.run_import_gpx))
                }
                if (routes.isEmpty() && plannedRoutes.isEmpty()) {
                    Text(
                        stringResource(R.string.run_route_picker_empty),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(routes, key = { "run:${it.id}" }) { run ->
                            TextButton(onClick = { onSelectRoute(run.id, null) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(RunActivityFormatting.displayTitle(run))
                                    Text(RunFormatting.distanceLabel(run.distanceMeters))
                                }
                            }
                        }
                        items(plannedRoutes, key = { "gpx:${it.id}" }) { route ->
                            Row(Modifier.fillMaxWidth()) {
                                TextButton(
                                    onClick = { onSelectRoute(null, route.id) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(route.name)
                                        Text(RunFormatting.distanceLabel(route.distanceMeters))
                                    }
                                }
                                IconButton(onClick = { onSetRouteFavorite(route.id, !route.isFavorite) }) {
                                    Icon(
                                        imageVector = if (route.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = stringResource(
                                            if (route.isFavorite) R.string.run_unfavorite_route_action
                                            else R.string.run_favorite_route_action
                                        ),
                                        tint = if (route.isFavorite) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}
