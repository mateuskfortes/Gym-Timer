package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunRoutePosition
import com.example.gymtimer2.domain.model.RunTurnDirection
import com.example.gymtimer2.domain.model.RunTurnGuidance
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting

@Composable
internal fun RunRouteStatusPill(
    runId: Long,
    routeKey: String?,
    routePosition: RunRoutePosition?,
    isOffRoute: Boolean,
    nextTurnGuidance: RunTurnGuidance?,
    isVisible: Boolean,
    showMuteAction: Boolean,
    onMuteClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDismissed by rememberSaveable(runId, routeKey) { mutableStateOf(false) }
    var showMuteDialog by rememberSaveable(runId, routeKey) { mutableStateOf(false) }
    val isOnRoute = routePosition != null && !isOffRoute
    RunRouteStatusStateEffect(
        isOnRoute = isOnRoute,
        isVisible = isVisible,
        hasPosition = routePosition != null,
        onRouteRestored = { isDismissed = false },
        onHideMuteDialog = { showMuteDialog = false }
    )

    if (!isVisible || isDismissed) return
    if (routeKey == null) return
    val position = routePosition ?: return

    RunRouteStatusPillContent(
        isOffRoute = isOffRoute,
        detail = runRouteStatusDetail(isOffRoute, position, nextTurnGuidance),
        showMuteAction = showMuteAction,
        onDismiss = { isDismissed = true },
        onShowMuteDialog = { showMuteDialog = true },
        modifier = modifier
    )
    RunRouteStatusMuteDialog(showMuteDialog, onMuteClick) { showMuteDialog = false }
}

@Composable
private fun RunRouteStatusStateEffect(
    isOnRoute: Boolean,
    isVisible: Boolean,
    hasPosition: Boolean,
    onRouteRestored: () -> Unit,
    onHideMuteDialog: () -> Unit
) {
    LaunchedEffect(isOnRoute, isVisible, hasPosition) {
        if (isOnRoute) onRouteRestored()
        if (!isVisible || !hasPosition || isOnRoute) onHideMuteDialog()
    }
}

@Composable
private fun RunRouteStatusMuteDialog(
    isVisible: Boolean,
    onMuteClick: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return
    RunRouteAlertMuteDialog(
        onMuteOnce = {
            onDismiss()
            onMuteClick(true)
        },
        onMuteForRun = {
            onDismiss()
            onMuteClick(false)
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun runRouteStatusDetail(
    isOffRoute: Boolean,
    routePosition: RunRoutePosition,
    guidance: RunTurnGuidance?
): String {
    if (isOffRoute) {
        return stringResource(
            R.string.run_route_status_distance_from_route,
            RunFormatting.adaptiveDistanceLabel(routePosition.distanceFromRouteMeters)
        )
    }
    return if (guidance == null) {
        stringResource(R.string.run_route_status_continue)
    } else {
        runTurnDetail(guidance)
    }
}

@Composable
private fun runTurnDetail(guidance: RunTurnGuidance): String {
    val direction = when (guidance.direction) {
        RunTurnDirection.LEFT -> stringResource(R.string.run_turn_direction_left)
        RunTurnDirection.RIGHT -> stringResource(R.string.run_turn_direction_right)
        RunTurnDirection.U_TURN -> stringResource(R.string.run_turn_direction_uturn)
    }
    val distance = if (guidance.distanceToTurnMeters <= TURN_NOW_THRESHOLD_METERS) {
        stringResource(R.string.run_turn_distance_now)
    } else {
        stringResource(R.string.run_turn_distance, RunFormatting.adaptiveDistanceLabel(guidance.distanceToTurnMeters))
    }
    return stringResource(R.string.run_turn_guidance, direction, distance)
}

@Composable
private fun RunRouteStatusPillContent(
    isOffRoute: Boolean,
    detail: String,
    showMuteAction: Boolean,
    onDismiss: () -> Unit,
    onShowMuteDialog: () -> Unit,
    modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isOffRoute) colors.errorContainer else colors.primaryContainer,
        contentColor = if (isOffRoute) colors.onErrorContainer else colors.onPrimaryContainer,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isOffRoute) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                contentDescription = null
            )
            Column(Modifier.padding(start = 8.dp).weight(1f)) {
                Text(
                    stringResource(if (isOffRoute) R.string.run_route_status_off_route else R.string.run_route_status_on_route),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isOffRoute) RunOffRouteActions(showMuteAction, onDismiss, onShowMuteDialog)
        }
    }
}

@Composable
private fun RunOffRouteActions(showMuteAction: Boolean, onDismiss: () -> Unit, onShowMuteDialog: () -> Unit) {
    IconButton(onClick = onDismiss) {
        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.run_route_status_dismiss))
    }
    if (showMuteAction) {
        IconButton(onClick = onShowMuteDialog) {
            Icon(
                Icons.Default.NotificationsOff,
                contentDescription = stringResource(R.string.run_mute_route_alerts_button)
            )
        }
    }
}

private const val TURN_NOW_THRESHOLD_METERS = 25.0
