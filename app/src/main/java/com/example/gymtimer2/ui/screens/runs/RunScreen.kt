package com.example.gymtimer2.ui.screens.runs

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.gymtimer2.R
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.ui.screens.runs.RunGearCatalog
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunSaveDetails
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunLap
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunBestEffortOverride
import com.example.gymtimer2.domain.model.RunBenchmarkDistance
import com.example.gymtimer2.domain.model.RunCropOutcome
import com.example.gymtimer2.domain.model.RunMapBounds
import com.example.gymtimer2.domain.model.RunMapCameraSnapshot
import com.example.gymtimer2.domain.model.DEFAULT_RUN_SCREEN_DIM_TIMEOUT_MS
import com.example.gymtimer2.ui.screens.runs.active.ActiveRunScreen
import com.example.gymtimer2.ui.screens.runs.active.RunActiveScreenActions
import com.example.gymtimer2.ui.screens.runs.active.RunPreparationScreen
import com.example.gymtimer2.ui.screens.runs.active.RunPreparationScreenActions
import com.example.gymtimer2.ui.screens.runs.active.RunPreparationScreenState
import com.example.gymtimer2.ui.screens.runs.history.RunDeleteDialog
import com.example.gymtimer2.ui.screens.runs.history.RunDetailsScreen
import com.example.gymtimer2.ui.screens.runs.history.RunHomeScreen
import com.example.gymtimer2.ui.screens.runs.history.RunTrainingLogScreen
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import com.example.gymtimer2.ui.screens.runs.permissions.rememberRunStartAction
import com.example.gymtimer2.data.exporting.RunGpxDocument
import com.example.gymtimer2.data.exporting.RunFitDocument
import com.example.gymtimer2.ui.screens.runs.map.RunMapLayer
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Encaminha o estado para a tela inicial, o detalhe salvo ou a corrida em tela cheia. */
@Composable
fun RunScreen(
    state: RunUiState,
    actions: RunScreenActions,
    offRouteAlertsEnabled: Boolean = true,
    keepScreenAwakeDuringRun: Boolean = false,
    screenDimTimeoutMs: Long = DEFAULT_RUN_SCREEN_DIM_TIMEOUT_MS,
    manualLapsEnabled: Boolean = false,
    onManualLapsChanged: (Boolean) -> Unit = {},
    onOpenRunSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Preparation and recording use separate map compositions but share one selection.
    var selectedRunMapLayers by rememberSaveable { mutableStateOf(emptySet<RunMapLayer>()) }
    if (state.isActiveRunOpen && state.activeRun != null) {
        ActiveRunContent(
            state = state,
            run = state.activeRun,
            actions = actions,
            selectedMapLayers = selectedRunMapLayers,
            onSelectedMapLayersChanged = { selectedRunMapLayers = it },
            offRouteAlertsEnabled = offRouteAlertsEnabled,
            keepScreenAwake = keepScreenAwakeDuringRun,
            screenDimTimeoutMs = screenDimTimeoutMs,
            manualLapsEnabled = manualLapsEnabled,
            onOpenRunSettings = onOpenRunSettings,
            modifier = modifier,
        )
        return
    }
    if (state.isRunPreparationOpen) {
        RunPreparationContent(
            state = state,
            actions = actions,
            selectedMapLayers = selectedRunMapLayers,
            onSelectedMapLayersChanged = { selectedRunMapLayers = it },
            manualLapsEnabled = manualLapsEnabled,
            onManualLapsChanged = onManualLapsChanged,
            onOpenRunSettings = onOpenRunSettings,
            modifier = modifier
        )
        return
    }

    var pendingDeletionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var trainingLogOpen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val requestStart = rememberRunStartAction(context, actions.onPrepareRun) { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) { insets ->
        val selectedRun = state.selectedRun
        if (selectedRun != null) {
            SelectedRunDetails(
                state = state,
                run = selectedRun,
                onBack = actions.onBackFromRunDetails,
                onDelete = {
                    requestRunDeletion(selectedRun.id, state.activeRun, context, scope, snackbarHostState) {
                        pendingDeletionId = it
                    }
                },
                onUpdateSaveDetails = actions.onUpdateRunSaveDetails,
                onExportRunGpx = { uri ->
                    scope.launchRunExportResult(
                        context, snackbarHostState, R.string.run_export_gpx_success, R.string.run_export_gpx_failed
                    ) {
                        actions.onExportRunGpx(
                            uri,
                            selectedRun,
                            context.getString(RunActivityFormatting.activityTypeResource(selectedRun.activityType)),
                            state.selectedPoints
                        )
                    }
                },
                onExportRunFit = { uri ->
                    scope.launchRunExportResult(
                        context, snackbarHostState, R.string.run_export_fit_success, R.string.run_export_fit_failed
                    ) {
                        actions.onExportRunFit(uri, selectedRun, state.selectedPoints, state.selectedLaps)
                    }
                },
                onAddMedia = actions.onAddRunMedia,
                onRemoveMedia = actions.onDeleteRunMedia,
                onSetFeaturedMedia = actions.onSetFeaturedRunMedia,
                onSaveBestEffortOverride = actions.onSaveBestEffortOverride,
                onClearBestEffortOverride = actions.onClearBestEffortOverride,
                onSaveLocalSegment = actions.onSaveLocalSegment,
                onCropRun = actions.onCropRun,
                onLocalSegmentSaved = { scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.run_local_segment_saved)) } },
                gearCatalog = actions.gearCatalog,
                modifier = Modifier.padding(insets)
            )
        } else if (trainingLogOpen) {
            RunTrainingLogScreen(
                runs = state.completedRuns,
                onBack = { trainingLogOpen = false },
                onSelectRun = actions.onSelectRun,
                modifier = Modifier.padding(insets)
            )
        } else {
            RunHomeScreen(
                state = state,
                onStartRun = requestStart,
                onOpenActiveRun = actions.onOpenActiveRun,
                onOpenTrainingLog = { trainingLogOpen = true },
                onSelectRun = actions.onSelectRun,
                onDeleteRun = { runId ->
                    requestRunDeletion(runId, state.activeRun, context, scope, snackbarHostState) {
                        pendingDeletionId = it
                    }
                },
                modifier = Modifier.padding(insets)
            )
        }
    }
    state.completedRuns.firstOrNull { it.id == pendingDeletionId }?.let { run ->
        RunDeleteDialog(
            run = run,
            onConfirm = {
                pendingDeletionId = null
                actions.onDeleteRun(run.id)
            },
            onDismiss = { pendingDeletionId = null }
        )
    }
}

@Composable
private fun ActiveRunContent(
    state: RunUiState,
    run: RunModel,
    selectedMapLayers: Set<RunMapLayer>,
    onSelectedMapLayersChanged: (Set<RunMapLayer>) -> Unit,
    offRouteAlertsEnabled: Boolean,
    keepScreenAwake: Boolean,
    screenDimTimeoutMs: Long,
    manualLapsEnabled: Boolean,
    onOpenRunSettings: () -> Unit,
    actions: RunScreenActions,
    modifier: Modifier,
) {
    val activeRunState = state.copy(
        activeRun = run,
        nextTurnGuidance = state.nextTurnGuidance.takeUnless { state.isOffPlannedRoute },
        upcomingTurnGuidances = state.upcomingTurnGuidances.takeUnless { state.isOffPlannedRoute }.orEmpty()
    )
    ActiveRunScreen(
        state = activeRunState,
        actions = RunActiveScreenActions(
            onMuteRouteAlerts = { untilReturn -> actions.onMuteRouteAlerts(run.id, untilReturn) },
            onOpenRunSettings = onOpenRunSettings,
            onPause = { actions.onPauseRun(run.id) },
            onResume = { actions.onResumeRun(run.id) },
            onFinish = { details -> actions.onFinishRun(run.id, details) },
            onDiscard = { actions.onDiscardRun(run.id) },
            onBack = actions.onCloseActiveRun,
            onCalculateStreetReturn = actions.onCalculateStreetReturn,
            onClearStreetReturn = actions.onClearStreetReturn,
            onCalculatePlannedRouteGuidance = {
                actions.onCalculatePlannedRouteGuidance(state.plannedRoutePoints)
            },
            onLap = if (manualLapsEnabled) ({ actions.onMarkLap(run.id) }) else null,
            onAdvanceWorkout = { actions.onAdvanceWorkout(run.id) },
            onMapPoiViewportChanged = actions.onMapPoiViewportChanged,
            onCameraSnapshotChanged = actions.onSaveActiveRunMapCamera,
            selectedMapLayers = selectedMapLayers,
            onSelectedMapLayersChanged = onSelectedMapLayersChanged,
            gearCatalog = actions.gearCatalog
        ),
        keepScreenAwake = keepScreenAwake,
        screenDimTimeoutMs = screenDimTimeoutMs,
        offRouteAlertsEnabled = offRouteAlertsEnabled,
        modifier = modifier
    )
}

@Composable
private fun RunPreparationContent(
    state: RunUiState,
    actions: RunScreenActions,
    selectedMapLayers: Set<RunMapLayer>,
    onSelectedMapLayersChanged: (Set<RunMapLayer>) -> Unit,
    manualLapsEnabled: Boolean,
    onManualLapsChanged: (Boolean) -> Unit,
    onOpenRunSettings: () -> Unit,
    modifier: Modifier
) {
    RunPreparationScreen(
        screenState = RunPreparationScreenState(
            completedRuns = state.completedRuns,
            plannedRoutes = state.plannedRoutes,
            selectedRouteRunId = state.plannedRouteRunId,
            selectedRouteId = state.plannedRouteId,
            plannedPoints = state.plannedRoutePoints,
            manualLapsEnabled = manualLapsEnabled,
            paceTarget = state.selectedPaceTarget,
            workoutPlan = state.selectedWorkoutPlan,
            workoutTemplates = state.workoutTemplates,
            selectedWorkoutTemplateId = state.selectedWorkoutTemplateId,
            routeImportFailed = state.routeImportFailed,
            mapPoiState = state.mapPoiState,
            historyHeatmapCells = state.historyHeatmapCells,
            selectedMapLayers = selectedMapLayers
        ),
        screenActions = RunPreparationScreenActions(
            onManualLapsChanged = onManualLapsChanged,
            onSetRouteFavorite = actions.onSetPlannedRouteFavorite,
            onSetPaceTarget = actions.onSetPaceTarget,
            onSetWorkoutPlan = actions.onSetWorkoutPlan,
            onSaveWorkoutTemplate = actions.onSaveWorkoutTemplate,
            onDeleteWorkoutTemplate = actions.onDeleteWorkoutTemplate,
            onSelectRoute = actions.onSelectPlannedRoute,
            onImportRoute = actions.onImportRoute,
            onDismissImportError = actions.onDismissRouteImportError,
            onStart = actions.onBeginRun,
            onBack = actions.onCloseRunPreparation,
            onOpenSettings = onOpenRunSettings,
            onMapPoiViewportChanged = actions.onMapPoiViewportChanged,
            onSelectedMapLayersChanged = onSelectedMapLayersChanged
        ),
        modifier = modifier
    )
}

@Composable
private fun SelectedRunDetails(
    state: RunUiState,
    run: RunModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUpdateSaveDetails: (Long, RunSaveDetails) -> Unit,
    onAddMedia: (Long, List<Uri>) -> Unit,
    onRemoveMedia: (Long) -> Unit,
    onSetFeaturedMedia: (Long, Long) -> Unit,
    onSaveBestEffortOverride: (RunBestEffortOverride) -> Unit,
    onClearBestEffortOverride: (Long, RunBenchmarkDistance) -> Unit,
    onSaveLocalSegment: suspend (Long, String, Int, Double, Double) -> Boolean,
    onCropRun: suspend (Long, Long, Long) -> RunCropOutcome,
    onLocalSegmentSaved: () -> Unit,
    onExportRunGpx: (Uri) -> Unit,
    onExportRunFit: (Uri) -> Unit,
    gearCatalog: RunGearCatalog,
    modifier: Modifier
) {
    RunDetailsScreen(
        run = run,
        points = state.selectedPoints,
        privacyZones = state.privacyZones,
        localSegmentEfforts = state.selectedLocalSegmentEfforts,
        plannedRoutes = state.plannedRoutes,
        stepRateSamples = state.selectedStepRateSamples,
        laps = state.selectedLaps,
        workoutSteps = state.selectedWorkoutSteps,
        bestEffortOverrides = state.selectedBestEffortOverrides,
        media = state.selectedRunMedia,
        onBack = onBack,
        onDelete = onDelete,
        onUpdateSaveDetails = { onUpdateSaveDetails(run.id, it) },
        onAddMedia = { onAddMedia(run.id, it) },
        onRemoveMedia = onRemoveMedia,
        onSetFeaturedMedia = { onSetFeaturedMedia(run.id, it) },
        onSaveBestEffortOverride = onSaveBestEffortOverride,
        onClearBestEffortOverride = { onClearBestEffortOverride(run.id, it) },
        onSaveLocalSegment = onSaveLocalSegment,
        onCropRun = { startPointId, endPointId -> onCropRun(run.id, startPointId, endPointId) },
        isRouteInUse = state.activeRun?.plannedRouteRunId == run.id,
        onExportRunGpx = onExportRunGpx,
        onExportRunFit = onExportRunFit,
        onLocalSegmentSaved = onLocalSegmentSaved,
        gearCatalog = gearCatalog,
        modifier = modifier
    )
}

internal fun shouldShowRouteAlert(state: RunUiState, alertsEnabled: Boolean): Boolean =
    alertsEnabled && state.isOffPlannedRoute && state.activeRun?.routeAlertsMuted != true &&
        !state.isRouteAlertsMutedUntilReturn

private fun requestRunDeletion(
    runId: Long,
    activeRun: RunModel?,
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onRequestDeletion: (Long) -> Unit
) {
    if (activeRun?.plannedRouteRunId == runId) {
        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.run_route_in_use)) }
    } else {
        onRequestDeletion(runId)
    }
}

private fun CoroutineScope.launchRunExportResult(
    context: Context,
    snackbarHostState: SnackbarHostState,
    successMessage: Int,
    failureMessage: Int,
    export: suspend () -> Unit
) {
    launch {
        val resultMessage = try {
            export()
            successMessage
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            failureMessage
        } catch (_: SecurityException) {
            failureMessage
        } catch (_: IllegalArgumentException) {
            failureMessage
        }
        snackbarHostState.showSnackbar(context.getString(resultMessage))
    }
}
