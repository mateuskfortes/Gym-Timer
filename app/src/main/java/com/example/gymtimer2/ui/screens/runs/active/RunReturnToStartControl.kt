@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.components.SecondaryButton

@Composable
internal fun RunReturnToStartControl(
    isReturning: Boolean,
    hasRecordedPath: Boolean,
    canCalculateStreetRoute: Boolean,
    isLoading: Boolean,
    hasError: Boolean,
    streetRouteDistanceMeters: Double?,
    onStartRecorded: () -> Unit,
    onCalculateStreet: () -> Unit,
    onStop: () -> Unit
) {
    var showConfirmation by rememberSaveable { mutableStateOf(false) }

    if (isLoading || isReturning) {
        ReturnStatusCard(isLoading, streetRouteDistanceMeters, onStop)
    } else {
        ReturnStartAction(hasError) { showConfirmation = true }
    }

    if (showConfirmation) {
        ReturnConfirmationDialog(
            hasRecordedPath = hasRecordedPath,
            canCalculateStreetRoute = canCalculateStreetRoute,
            onDismiss = { showConfirmation = false },
            onStartRecorded = {
                showConfirmation = false
                onStartRecorded()
            },
            onCalculateStreet = {
                showConfirmation = false
                onCalculateStreet()
            }
        )
    }
}

@Composable
private fun ReturnStartAction(hasError: Boolean, onClick: () -> Unit) {
    Column {
        if (hasError) {
            Text(
                stringResource(R.string.run_return_street_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        SecondaryButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.run_return_start_action))
        }
    }
}

@Composable
private fun ReturnConfirmationDialog(
    hasRecordedPath: Boolean,
    canCalculateStreetRoute: Boolean,
    onDismiss: () -> Unit,
    onStartRecorded: () -> Unit,
    onCalculateStreet: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_return_dialog_title)) },
        text = { ReturnDisclosure(hasRecordedPath, canCalculateStreetRoute) },
        confirmButton = {
            ReturnConfirmationActions(
                hasRecordedPath,
                canCalculateStreetRoute,
                onDismiss,
                onStartRecorded,
                onCalculateStreet
            )
        }
    )
}

@Composable
private fun ReturnDisclosure(hasRecordedPath: Boolean, canCalculateStreetRoute: Boolean) {
    Column {
        Text(
            stringResource(
                if (hasRecordedPath || canCalculateStreetRoute) R.string.run_return_dialog_message
                else R.string.run_return_no_points_message
            )
        )
        if (hasRecordedPath && !canCalculateStreetRoute) {
            Text(
                stringResource(R.string.run_return_street_unavailable),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ReturnConfirmationActions(
    hasRecordedPath: Boolean,
    canCalculateStreetRoute: Boolean,
    onDismiss: () -> Unit,
    onStartRecorded: () -> Unit,
    onCalculateStreet: () -> Unit
) {
    Column(horizontalAlignment = Alignment.End) {
        if (hasRecordedPath) {
            TextButton(onClick = onStartRecorded) {
                Text(stringResource(R.string.run_return_recorded_action))
            }
        }
        if (canCalculateStreetRoute) {
            TextButton(onClick = onCalculateStreet) {
                Text(stringResource(R.string.run_return_street_action))
            }
        }
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.cancel_button))
        }
    }
}

@Composable
private fun ReturnStatusCard(
    isLoading: Boolean,
    streetRouteDistanceMeters: Double?,
    onStop: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                stringResource(
                    when {
                        isLoading -> R.string.run_return_calculating_title
                        streetRouteDistanceMeters != null -> R.string.run_return_street_active_title
                        else -> R.string.run_return_active_title
                    }
                ),
                style = MaterialTheme.typography.titleSmall
            )
            val description = when {
                isLoading -> stringResource(R.string.run_return_calculating_description)
                streetRouteDistanceMeters != null -> stringResource(
                    R.string.run_return_street_active_description,
                    RunFormatting.distanceLabel(streetRouteDistanceMeters)
                )
                else -> stringResource(R.string.run_return_active_description)
            }
            Text(description, style = MaterialTheme.typography.bodySmall)
            SecondaryButton(onClick = onStop) {
                Text(stringResource(if (isLoading) R.string.run_return_cancel_action else R.string.run_return_stop_action))
            }
        }
    }
}
