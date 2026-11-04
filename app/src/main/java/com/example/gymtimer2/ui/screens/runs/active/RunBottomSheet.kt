package com.example.gymtimer2.ui.screens.runs.active

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.RunWorkoutStep
import com.example.gymtimer2.ui.screens.runs.RunLiveSegment
import com.example.gymtimer2.ui.screens.runs.RunPlannedRouteGuidanceState
import com.example.gymtimer2.ui.screens.runs.RunStreetReturnState
import com.example.gymtimer2.domain.model.RunTurnGuidance
import com.example.gymtimer2.ui.components.SecondaryButton
import androidx.compose.material3.Text
import com.example.gymtimer2.R
import com.example.gymtimer2.ui.screens.runs.components.RunSplitsSection
import com.example.gymtimer2.ui.screens.runs.components.RunLapsSection
import com.example.gymtimer2.ui.screens.runs.components.RunPaceGuidanceCard
import com.example.gymtimer2.ui.screens.runs.components.RunTurnGuidanceCard
import com.example.gymtimer2.ui.screens.runs.components.RunStreetGuidanceCard
import com.example.gymtimer2.ui.screens.runs.components.RunLocalSegmentCard
import com.example.gymtimer2.ui.screens.runs.components.rememberRunSplitsUiStateForRun

/** Agrupa métricas e ações num painel que pode ser recolhido sem pausar a gravação. */
@Composable
internal fun RunBottomSheet(
    run: RunModel,
    points: List<RunPointModel>,
    laps: List<RunLap>,
    workoutSteps: List<RunWorkoutStep>,
    gpsStatus: RunGpsStatus,
    currentPaceSecondsPerKm: Double?,
    currentCadenceStepsPerMinute: Int?,
    currentSpeedKmPerHour: Double?,
    turnGuidances: List<RunTurnGuidance>,
    localSegments: List<RunLiveSegment>,
    returningToStart: Boolean,
    canReturnToStart: Boolean,
    canCalculateStreetRoute: Boolean,
    streetReturnState: RunStreetReturnState,
    plannedRouteGuidanceState: RunPlannedRouteGuidanceState,
    hasPlannedRoute: Boolean,
    onCalculatePlannedRouteGuidance: () -> Unit,
    onLap: (() -> Unit)?,
    onAdvanceWorkout: () -> Unit,
    onStartRecordedReturn: () -> Unit,
    onCalculateStreetReturn: () -> Unit,
    onStopReturn: () -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeRun = run.takeIf { it.status == RunStatus.RUNNING }
    val splitsUiState = rememberRunSplitsUiStateForRun(run, points, activeRun)
    val (approachingSegments, liveSegments) = localSegments.partition { it.distanceToStartMeters != null }
    Column(modifier.fillMaxWidth().navigationBarsPadding()) {
        RunSheetHeader(
            run = run,
            expanded = expanded,
            onToggle = onToggle,
            gpsStatus = gpsStatus,
            currentSplitPaceSecondsPerKm = splitsUiState?.currentSplitPaceSecondsPerKm
        )
        Column(
            Modifier.weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            if (run.status == RunStatus.RUNNING) {
                RunGpsIndicator(gpsStatus)
                Spacer(Modifier.height(12.dp))
                RunRouteGuidance(
                    streetReturnState = streetReturnState,
                    plannedRouteGuidanceState = plannedRouteGuidanceState,
                    turnGuidances = turnGuidances,
                    hasPlannedRoute = hasPlannedRoute,
                    onCalculatePlannedRouteGuidance = onCalculatePlannedRouteGuidance
                )
            }
            approachingSegments.forEach { segment ->
                RunLocalSegmentCard(segment)
                Spacer(Modifier.height(12.dp))
            }
            RunStatsPager(
                run = run,
                points = points,
                laps = laps,
                workoutSteps = workoutSteps,
                liveSegments = liveSegments,
                onAdvanceWorkout = onAdvanceWorkout,
                showLapPage = onLap != null || laps.isNotEmpty(),
                currentPaceSecondsPerKm = currentPaceSecondsPerKm,
                currentCadenceStepsPerMinute = currentCadenceStepsPerMinute,
                currentSpeedKmPerHour = currentSpeedKmPerHour
            )
            if (run.status == RunStatus.RUNNING) {
                run.paceTarget?.let { target ->
                    Spacer(Modifier.height(8.dp))
                    RunPaceGuidanceCard(target, currentPaceSecondsPerKm)
                }
            }
            Spacer(Modifier.height(20.dp))
            if (run.status != RunStatus.STARTING && run.status != RunStatus.COMPLETED) {
                RunReturnToStartControl(
                    isReturning = returningToStart || streetReturnState.route != null,
                    hasRecordedPath = canReturnToStart,
                    canCalculateStreetRoute = canCalculateStreetRoute,
                    isLoading = streetReturnState.isLoading,
                    hasError = streetReturnState.hasError,
                    streetRouteDistanceMeters = streetReturnState.route?.distanceMeters,
                    onStartRecorded = onStartRecordedReturn,
                    onCalculateStreet = onCalculateStreetReturn,
                    onStop = onStopReturn
                )
                Spacer(Modifier.height(8.dp))
            }
            RunControls(run.status, onPause, onResume, onFinish)
            RunLapAction(run.status, onLap)
            RunLapsSection(laps)
            splitsUiState?.let { RunSplitsSection(it) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RunRouteGuidance(
    streetReturnState: RunStreetReturnState,
    plannedRouteGuidanceState: RunPlannedRouteGuidanceState,
    turnGuidances: List<RunTurnGuidance>,
    hasPlannedRoute: Boolean,
    onCalculatePlannedRouteGuidance: () -> Unit
) {
    if (streetReturnState.route != null) {
        streetReturnState.nextGuidance?.let {
            RunStreetGuidanceCard(it)
            Spacer(Modifier.height(12.dp))
        }
    } else if (plannedRouteGuidanceState.route != null) {
        plannedRouteGuidanceState.nextGuidance?.let {
            RunStreetGuidanceCard(it)
            Spacer(Modifier.height(12.dp))
        }
    } else {
        turnGuidances.forEachIndexed { index, guidance ->
            RunTurnGuidanceCard(guidance, isNext = index == 0)
            Spacer(Modifier.height(12.dp))
        }
    }
    if (streetReturnState.route == null && hasPlannedRoute) {
        RunPlannedRouteGuidanceControl(
            isVisible = true,
            state = plannedRouteGuidanceState,
            onRequest = onCalculatePlannedRouteGuidance
        )
    }
}

@Composable
private fun RunLapAction(status: RunStatus, onLap: (() -> Unit)?) {
    if (status != RunStatus.RUNNING || onLap == null) return
    Spacer(Modifier.height(8.dp))
    SecondaryButton(onClick = onLap, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.run_lap_action))
    }
}
