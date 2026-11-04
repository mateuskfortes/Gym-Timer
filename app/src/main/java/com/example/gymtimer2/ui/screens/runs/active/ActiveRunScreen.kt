package com.example.gymtimer2.ui.screens.runs.active

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.ui.screens.runs.RunGearCatalog
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.DEFAULT_RUN_SCREEN_DIM_TIMEOUT_MS
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunHeatmapCell
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.RunMapBounds
import com.example.gymtimer2.domain.model.RunMapCameraSnapshot
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.RunRoutePosition
import com.example.gymtimer2.domain.model.RunTurnGuidance
import com.example.gymtimer2.domain.model.RunWorkoutStep
import com.example.gymtimer2.ui.screens.runs.RunLiveSegment
import com.example.gymtimer2.ui.screens.runs.RunStreetReturnState
import com.example.gymtimer2.ui.screens.runs.RunPlannedRouteGuidanceState
import com.example.gymtimer2.ui.screens.runs.RunMapPoiUiState
import com.example.gymtimer2.ui.screens.runs.RunUiState
import com.example.gymtimer2.ui.screens.runs.shouldShowRouteAlert
import com.example.gymtimer2.domain.model.hasRecordedBacktrack
import com.example.gymtimer2.domain.model.routeBackToRunStart
import com.example.gymtimer2.ui.screens.runs.map.RunMap
import com.example.gymtimer2.ui.screens.runs.map.RunMapFullscreenButton
import com.example.gymtimer2.ui.screens.runs.map.RunMapLayer
import com.example.gymtimer2.ui.screens.runs.map.RunMapSettingsButton
import kotlinx.coroutines.launch

internal data class RunActiveScreenActions(
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onFinish: (RunSaveDetails) -> Unit,
    val onDiscard: () -> Unit,
    val onBack: () -> Unit,
    val onMuteRouteAlerts: (Boolean) -> Unit = {},
    val onOpenRunSettings: () -> Unit = {},
    val onCalculateStreetReturn: () -> Unit = {},
    val onClearStreetReturn: () -> Unit = {},
    val onCalculatePlannedRouteGuidance: () -> Unit = {},
    val onLap: (() -> Unit)? = {},
    val onAdvanceWorkout: () -> Unit = {},
    val onMapPoiViewportChanged: (RunMapBounds?) -> Unit = {},
    val onCameraSnapshotChanged: (RunMapCameraSnapshot) -> Unit = {},
    val selectedMapLayers: Set<RunMapLayer>? = null,
    val onSelectedMapLayersChanged: ((Set<RunMapLayer>) -> Unit)? = null,
    val gearCatalog: RunGearCatalog = RunGearCatalog()
)

/** Mostra a corrida em tela cheia e mantém os controles num painel inferior expansível. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActiveRunScreen(
    state: RunUiState,
    actions: RunActiveScreenActions,
    keepScreenAwake: Boolean = false,
    screenDimTimeoutMs: Long = DEFAULT_RUN_SCREEN_DIM_TIMEOUT_MS,
    offRouteAlertsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val run = state.activeRun ?: return
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Expanded,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val scope = rememberCoroutineScope()
    var confirmFinish by rememberSaveable(run.id) { mutableStateOf(false) }
    var returningToStart by rememberSaveable(run.id) { mutableStateOf(false) }
    var mapFullscreen by rememberSaveable(run.id) { mutableStateOf(false) }
    var sheetWasExpandedBeforeFullscreen by rememberSaveable(run.id) { mutableStateOf(true) }
    val returnRoutePoints = returnRoutePoints(state.activePoints, returningToStart, state.streetReturnState)
    val canReturnToStart = remember(state.activePoints) { hasRecordedBacktrack(state.activePoints) }
    val canCalculateStreetRoute = canCalculateStreetRoute(state.gpsStatus, state.activePoints)
    var sheetHeightPx by remember { mutableIntStateOf(0) }
    val expanded = sheetState.currentValue == SheetValue.Expanded
    val peekHeight = RUN_SHEET_COLLAPSED_HEIGHT_DP.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val sheetHeight = with(LocalDensity.current) { sheetHeightPx.toDp() }
    val mapBottomPadding = if (expanded) maxOf(sheetHeight, peekHeight) else peekHeight
    val activeSegmentIds = runStatsLiveSegmentIds(state.localSegments)
    val previousActiveSegmentIds = remember(run.id) { mutableStateOf(emptySet<String>()) }
    RunActiveSegmentSheetEffect(activeSegmentIds, mapFullscreen, sheetState, previousActiveSegmentIds)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.7f
    var userInteractionVersion by remember(run.id) { mutableIntStateOf(0) }
    RunActiveInteractionEffects(actions.onBack, keepScreenAwake, screenDimTimeoutMs, userInteractionVersion) { userInteractionVersion++ }
    BottomSheetScaffold(
        modifier = modifier.fillMaxSize().trackUserInteraction(run.id) { userInteractionVersion++ },
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetDragHandle = null,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            RunActiveBottomSheet(
                state = RunActiveBottomSheetState(
                    run = run,
                    points = state.activePoints,
                    laps = state.activeLaps,
                    workoutSteps = state.activeWorkoutSteps,
                    gpsStatus = state.gpsStatus,
                    currentPaceSecondsPerKm = state.currentPaceSecondsPerKm,
                    currentCadenceStepsPerMinute = state.currentCadenceStepsPerMinute,
                    currentSpeedKmPerHour = state.currentSpeedKmPerHour,
                    turnGuidances = state.upcomingTurnGuidances,
                    localSegments = state.localSegments,
                    returningToStart = returningToStart,
                    canReturnToStart = canReturnToStart,
                    canCalculateStreetRoute = canCalculateStreetRoute,
                    streetReturnState = state.streetReturnState,
                    plannedRouteGuidanceState = state.plannedRouteGuidance,
                    hasPlannedRoute = state.plannedRouteKey != null
                ),
                actions = RunActiveBottomSheetActions(
                    onLap = actions.onLap,
                    onAdvanceWorkout = actions.onAdvanceWorkout,
                    onStartRecordedReturn = {
                        actions.onClearStreetReturn()
                        returningToStart = true
                    },
                    onCalculateStreetReturn = {
                        returningToStart = false
                        actions.onCalculateStreetReturn()
                    },
                    onStopReturn = {
                        returningToStart = false
                        actions.onClearStreetReturn()
                    },
                    onCalculatePlannedRouteGuidance = actions.onCalculatePlannedRouteGuidance,
                    onToggle = {
                        scope.launch { toggleRunSheet(sheetState, expanded) }
                    },
                    onPause = actions.onPause,
                    onResume = actions.onResume,
                    onFinish = { confirmFinish = true }
                ),
                expanded = expanded,
                modifier = Modifier
                    .trackUserInteraction(run.id) { userInteractionVersion++ }
                    .heightIn(max = maxSheetHeight)
                    .onSizeChanged { sheetHeightPx = it.height }
            )
        }
    ) { _ ->
        RunActiveMapLayer(
            state = state,
            actions = actions,
            returnRoutePoints = returnRoutePoints,
            bottomPadding = mapBottomPadding,
            mapFullscreen = mapFullscreen,
            offRouteAlertsEnabled = offRouteAlertsEnabled,
            onToggleMapFullscreen = {
                val showFullscreenMap = !mapFullscreen
                if (showFullscreenMap) {
                    sheetWasExpandedBeforeFullscreen = sheetState.currentValue == SheetValue.Expanded
                }
                mapFullscreen = showFullscreenMap
                scope.launch {
                    toggleRunMapSheet(sheetState, showFullscreenMap, sheetWasExpandedBeforeFullscreen)
                }
            }
        )
    }
    RunActiveFinishOverlay(
        visible = confirmFinish,
        gearCatalog = actions.gearCatalog,
        isEmptyActivity = !(run.distanceMeters > 0.0),
        onConfirm = {
            confirmFinish = false
            actions.onFinish(it)
        },
        onDiscard = {
            confirmFinish = false
            actions.onDiscard()
        },
        onDismiss = { confirmFinish = false }
    )
}

@Composable
private fun RunActiveMapLayer(
    state: RunUiState,
    actions: RunActiveScreenActions,
    returnRoutePoints: List<RoutePoint>,
    bottomPadding: Dp,
    mapFullscreen: Boolean,
    offRouteAlertsEnabled: Boolean,
    onToggleMapFullscreen: () -> Unit
) {
    val run = state.activeRun ?: return
    Box(Modifier.fillMaxSize()) {
        RunActiveMap(
            runId = run.id,
            points = state.activePoints,
            weeklyHeatmapCells = state.weeklyHeatmapCells,
            historyHeatmapCells = state.historyHeatmapCells,
            plannedPoints = state.plannedRoutePoints,
            plannedRouteKey = state.plannedRouteKey,
            plannedRoutePosition = state.plannedRoutePosition,
            returnRoutePoints = returnRoutePoints,
            runStatus = run.status,
            gpsStatus = state.gpsStatus,
            isOffPlannedRoute = state.isOffPlannedRoute,
            nextTurnGuidance = state.nextTurnGuidance,
            followLatestPoint = run.status == RunStatus.RUNNING,
            bottomPadding = bottomPadding,
            showOffRouteAlert = shouldShowRouteAlert(state, offRouteAlertsEnabled),
            onMuteRouteAlerts = actions.onMuteRouteAlerts,
            onOpenRunSettings = actions.onOpenRunSettings,
            mapFullscreen = mapFullscreen,
            onToggleMapFullscreen = onToggleMapFullscreen,
            onBack = actions.onBack,
            poiState = state.mapPoiState,
            onMapPoiViewportChanged = actions.onMapPoiViewportChanged,
            initialCameraSnapshot = state.activeMapCameraSnapshot,
            onCameraSnapshotChanged = actions.onCameraSnapshotChanged,
            selectedMapLayers = actions.selectedMapLayers,
            onSelectedMapLayersChanged = actions.onSelectedMapLayersChanged
        )
        RunLapSummaryBanner(
            runId = run.id,
            laps = state.activeLaps,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RunActiveSegmentSheetEffect(
    activeSegmentIds: Set<String>,
    mapFullscreen: Boolean,
    sheetState: SheetState,
    previousActiveSegmentIds: MutableState<Set<String>>
) {
    LaunchedEffect(activeSegmentIds, mapFullscreen) {
        if (shouldExpandRunSheetForNewSegment(
                previousSegmentIds = previousActiveSegmentIds.value,
                activeSegmentIds = activeSegmentIds,
                isMapFullscreen = mapFullscreen,
                isSheetExpanded = sheetState.currentValue == SheetValue.Expanded
            )
        ) {
            sheetState.expand()
        }
        previousActiveSegmentIds.value = activeSegmentIds
    }
}

@Composable
private fun RunActiveFinishOverlay(
    visible: Boolean,
    gearCatalog: RunGearCatalog,
    isEmptyActivity: Boolean,
    onConfirm: (RunSaveDetails) -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    RunFinishDialog(
        onConfirm = onConfirm,
        onDiscard = onDiscard,
        onDismiss = onDismiss,
        gearCatalog = gearCatalog,
        isEmptyActivity = isEmptyActivity
    )
}

private data class RunActiveBottomSheetState(
    val run: RunModel,
    val points: List<RunPointModel>,
    val laps: List<RunLap>,
    val workoutSteps: List<RunWorkoutStep>,
    val gpsStatus: RunGpsStatus,
    val currentPaceSecondsPerKm: Double?,
    val currentCadenceStepsPerMinute: Int?,
    val currentSpeedKmPerHour: Double?,
    val turnGuidances: List<RunTurnGuidance>,
    val localSegments: List<RunLiveSegment>,
    val returningToStart: Boolean,
    val canReturnToStart: Boolean,
    val canCalculateStreetRoute: Boolean,
    val streetReturnState: RunStreetReturnState,
    val plannedRouteGuidanceState: RunPlannedRouteGuidanceState,
    val hasPlannedRoute: Boolean
)

private data class RunActiveBottomSheetActions(
    val onLap: (() -> Unit)?,
    val onAdvanceWorkout: () -> Unit,
    val onStartRecordedReturn: () -> Unit,
    val onCalculateStreetReturn: () -> Unit,
    val onStopReturn: () -> Unit,
    val onCalculatePlannedRouteGuidance: () -> Unit,
    val onToggle: () -> Unit,
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onFinish: () -> Unit
)

@Composable
private fun RunActiveBottomSheet(
    state: RunActiveBottomSheetState,
    actions: RunActiveBottomSheetActions,
    expanded: Boolean,
    modifier: Modifier
) {
    RunBottomSheet(
        run = state.run,
        points = state.points,
        laps = state.laps,
        workoutSteps = state.workoutSteps,
        gpsStatus = state.gpsStatus,
        currentPaceSecondsPerKm = state.currentPaceSecondsPerKm,
        currentCadenceStepsPerMinute = state.currentCadenceStepsPerMinute,
        currentSpeedKmPerHour = state.currentSpeedKmPerHour,
        turnGuidances = state.turnGuidances,
        localSegments = state.localSegments,
        returningToStart = state.returningToStart,
        canReturnToStart = state.canReturnToStart,
        canCalculateStreetRoute = state.canCalculateStreetRoute,
        streetReturnState = state.streetReturnState,
        plannedRouteGuidanceState = state.plannedRouteGuidanceState,
        hasPlannedRoute = state.hasPlannedRoute,
        onLap = actions.onLap,
        onAdvanceWorkout = actions.onAdvanceWorkout,
        onStartRecordedReturn = actions.onStartRecordedReturn,
        onCalculateStreetReturn = actions.onCalculateStreetReturn,
        onStopReturn = actions.onStopReturn,
        onCalculatePlannedRouteGuidance = actions.onCalculatePlannedRouteGuidance,
        expanded = expanded,
        onToggle = actions.onToggle,
        onPause = actions.onPause,
        onResume = actions.onResume,
        onFinish = actions.onFinish,
        modifier = modifier
    )
}

@Composable
private fun RunActiveInteractionEffects(
    onBack: () -> Unit,
    keepScreenAwake: Boolean,
    screenDimTimeoutMs: Long,
    userInteractionVersion: Int,
    onInteraction: () -> Unit
) {
    BackHandler(onBack = onBack)
    RunScreenBrightnessEffect(keepScreenAwake, screenDimTimeoutMs, userInteractionVersion, onInteraction)
}

@Composable
private fun RunActiveMap(
    runId: Long,
    points: List<RunPointModel>,
    weeklyHeatmapCells: List<RunHeatmapCell>,
    historyHeatmapCells: List<RunHeatmapCell>,
    plannedPoints: List<RoutePoint>,
    plannedRouteKey: String?,
    plannedRoutePosition: RunRoutePosition?,
    returnRoutePoints: List<RoutePoint>,
    runStatus: RunStatus,
    gpsStatus: RunGpsStatus,
    isOffPlannedRoute: Boolean,
    nextTurnGuidance: RunTurnGuidance?,
    followLatestPoint: Boolean,
    bottomPadding: Dp,
    showOffRouteAlert: Boolean,
    onMuteRouteAlerts: (Boolean) -> Unit,
    onOpenRunSettings: () -> Unit,
    mapFullscreen: Boolean,
    onToggleMapFullscreen: () -> Unit,
    onBack: () -> Unit,
    poiState: RunMapPoiUiState,
    onMapPoiViewportChanged: (RunMapBounds?) -> Unit,
    initialCameraSnapshot: RunMapCameraSnapshot?,
    onCameraSnapshotChanged: (RunMapCameraSnapshot) -> Unit,
    selectedMapLayers: Set<RunMapLayer>?,
    onSelectedMapLayersChanged: ((Set<RunMapLayer>) -> Unit)?
) {
    val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
        RunMap(
            points = points,
            weeklyHeatmapCells = weeklyHeatmapCells,
            historyHeatmapCells = historyHeatmapCells,
            plannedPoints = plannedPoints,
            plannedRouteKey = plannedRouteKey,
            returnRoutePoints = returnRoutePoints,
            poiState = poiState,
            onPoiViewportChanged = onMapPoiViewportChanged,
            followLatestPoint = followLatestPoint,
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
            bottomPadding = if (mapFullscreen) navigationInset else bottomPadding,
            showRecenterControl = true,
            showRouteOverviewControl = true,
            selectedLayers = selectedMapLayers,
            onSelectedLayersChanged = onSelectedMapLayersChanged,
            cameraSnapshotRunId = runId,
            initialCameraSnapshot = initialCameraSnapshot,
            onCameraSnapshotChanged = onCameraSnapshotChanged
        )
        RunMapFullscreenButton(
            isFullscreen = mapFullscreen,
            onClick = onToggleMapFullscreen,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
        )
        RunMapSettingsButton(
            onClick = onOpenRunSettings,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 72.dp)
        )
        RunBackButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(16.dp))
        RunRouteStatusPill(
            runId = runId,
            routeKey = plannedRouteKey,
            routePosition = plannedRoutePosition,
            isOffRoute = isOffPlannedRoute,
            nextTurnGuidance = nextTurnGuidance,
            isVisible = shouldShowRunRouteStatus(
                isRunning = runStatus == RunStatus.RUNNING,
                hasGoodGps = gpsStatus == RunGpsStatus.GOOD,
                isOffRoute = isOffPlannedRoute,
                showOffRouteAlert = showOffRouteAlert
            ),
            showMuteAction = showOffRouteAlert,
            onMuteClick = onMuteRouteAlerts,
            modifier = Modifier.align(Alignment.TopCenter)
                .statusBarsPadding().padding(start = 72.dp, top = 76.dp, end = 72.dp)
        )
    }
}

internal fun shouldShowRunRouteStatus(
    isRunning: Boolean,
    hasGoodGps: Boolean,
    isOffRoute: Boolean,
    showOffRouteAlert: Boolean
): Boolean = isRunning && hasGoodGps && (!isOffRoute || showOffRouteAlert)

private fun returnRoutePoints(
    points: List<RunPointModel>,
    returningToStart: Boolean,
    streetReturnState: RunStreetReturnState
): List<RoutePoint> = streetReturnState.route?.points
    ?: if (returningToStart) routeBackToRunStart(points) else emptyList()

private fun canCalculateStreetRoute(gpsStatus: RunGpsStatus, points: List<RunPointModel>): Boolean =
    gpsStatus == RunGpsStatus.GOOD && points.count { !it.isFiltered } >= 2

private fun PointerEvent.hasNewTouch(): Boolean = changes.any { it.pressed && !it.previousPressed }

private fun Modifier.trackUserInteraction(key: Long, onInteraction: () -> Unit): Modifier = pointerInput(key) {
    awaitPointerEventScope {
        while (true) {
            if (awaitPointerEvent(PointerEventPass.Initial).hasNewTouch()) onInteraction()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private suspend fun toggleRunSheet(sheetState: SheetState, expanded: Boolean) {
    if (expanded) sheetState.partialExpand() else sheetState.expand()
}

@OptIn(ExperimentalMaterial3Api::class)
private suspend fun toggleRunMapSheet(
    sheetState: SheetState,
    showFullscreenMap: Boolean,
    wasExpanded: Boolean
) {
    if (showFullscreenMap) sheetState.hide()
    else if (wasExpanded) {
        sheetState.expand()
    } else {
        sheetState.partialExpand()
    }
}
