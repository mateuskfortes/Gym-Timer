package com.example.gymtimer2.ui.screens.runs

import android.os.SystemClock
import com.example.gymtimer2.data.preferences.AppSettingsRepository
import com.example.gymtimer2.data.repository.PlannedRouteImportException
import com.example.gymtimer2.data.repository.PlannedRouteRepository
import com.example.gymtimer2.data.repository.RunGearRepository
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.RunBestEffortRecords
import com.example.gymtimer2.domain.model.RunBestEfforts
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPrivacyZone
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunWorkoutPlan
import com.example.gymtimer2.domain.model.RunWorkoutTemplate
import com.example.gymtimer2.domain.model.RunTurnInstruction
import com.example.gymtimer2.domain.model.RunElevationProfilePoint
import com.example.gymtimer2.domain.model.RunRouteProgressTracker
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.calculateRunElevationProfile
import com.example.gymtimer2.domain.model.currentPaceSecondsPerKm
import com.example.gymtimer2.domain.model.currentRunStepRate
import com.example.gymtimer2.domain.model.inferRunTurnInstructions
import com.example.gymtimer2.domain.model.isOffRunRoutePosition
import com.example.gymtimer2.domain.model.nextRunStreetGuidance
import com.example.gymtimer2.domain.model.runGpsStatus
import com.example.gymtimer2.domain.model.upcomingRunTurnGuidances
import com.example.gymtimer2.ui.screens.runs.tracking.RunTrackingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

internal data class RunUiStateSelections(
    val activeRunOpen: StateFlow<Boolean>,
    val runPreparationOpen: StateFlow<Boolean>,
    val selectedPlannedRouteRunId: StateFlow<Long?>,
    val selectedPlannedRouteId: StateFlow<Long?>,
    val selectedPaceTarget: StateFlow<RunPaceTarget?>,
    val selectedWorkoutPlan: StateFlow<RunWorkoutPlan?>,
    val selectedWorkoutTemplateId: StateFlow<Long?>,
    val routeImportFailure: StateFlow<PlannedRouteImportException?>,
    val streetReturnState: StateFlow<RunStreetReturnState>,
    val plannedRouteGuidance: StateFlow<RunPlannedRouteGuidanceState>,
    val selectedRunId: StateFlow<Long?>
)

internal data class RunUiStateObserverSources(
    val scope: CoroutineScope,
    val repository: RunRepository,
    val plannedRouteRepository: PlannedRouteRepository,
    val runGearRepository: RunGearRepository,
    val appSettingsRepository: AppSettingsRepository?,
    val privacyZones: StateFlow<List<RunPrivacyZone>>,
    val mapPoiController: RunMapPoiController,
    val workoutTemplates: StateFlow<List<RunWorkoutTemplate>>,
    val selections: RunUiStateSelections
)

@OptIn(ExperimentalCoroutinesApi::class)
internal class RunUiStateObserver(sources: RunUiStateObserverSources) {
    private val scope = sources.scope
    private val repository = sources.repository
    private val plannedRouteRepository = sources.plannedRouteRepository
    private val runGearRepository = sources.runGearRepository
    private val appSettingsRepository = sources.appSettingsRepository
    private val privacyZones = sources.privacyZones
    private val mapPoiController = sources.mapPoiController
    private val workoutTemplates = sources.workoutTemplates
    private val activeRunOpen = sources.selections.activeRunOpen
    private val runPreparationOpen = sources.selections.runPreparationOpen
    private val selectedPlannedRouteRunId = sources.selections.selectedPlannedRouteRunId
    private val selectedPlannedRouteId = sources.selections.selectedPlannedRouteId
    private val selectedPaceTarget = sources.selections.selectedPaceTarget
    private val selectedWorkoutPlan = sources.selections.selectedWorkoutPlan
    private val selectedWorkoutTemplateId = sources.selections.selectedWorkoutTemplateId
    private val routeImportFailure = sources.selections.routeImportFailure
    private val streetReturnState = sources.selections.streetReturnState
    private val plannedRouteGuidance = sources.selections.plannedRouteGuidance
    private val selectedRunId = sources.selections.selectedRunId

    private val activePoints = repository.activeRun.flatMapLatest { run ->
        run?.let { repository.observePoints(it.id) } ?: flowOf(emptyList())
    }
    private val selectedRun = selectedRunId.flatMapLatest { runId ->
        runId?.let(repository::observeRun) ?: flowOf(null)
    }
    private val selectedPoints = selectedRunId.flatMapLatest { runId ->
        runId?.let(repository::observePoints) ?: flowOf(emptyList())
    }
    private val selectedBestEffortOverrides = selectedRunId.flatMapLatest { runId ->
        runId?.let(repository.bestEffortRepository::observeOverrides) ?: flowOf(emptyList())
    }
    private val bestEffortRecords = combine(
        repository.completedRuns,
        repository.bestEffortRepository.observeAllOverrides()
    ) { runs, overrides -> runs to overrides }
        .distinctUntilChanged()
        .mapLatest { (runs, overrides) ->
            if (runs.isEmpty()) return@mapLatest RunBestEffortRecords()
            val overridesByRun = overrides.groupBy { it.runId }
            runs.fold(RunBestEffortRecords()) { records, run ->
                val points = withContext(Dispatchers.IO) {
                    repository.bestEffortRepository.getCompletedPoints(run.id)
                }
                withContext(Dispatchers.Default) {
                    val runRecords = RunBestEfforts.calculateRecords(
                        listOf(run),
                        points,
                        overridesByRun[run.id].orEmpty()
                    )
                    RunBestEfforts.mergeRecords(records, runRecords)
                }
            }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), RunBestEffortRecords())
    private val weeklyHeatmapCells = flow {
        while (true) {
            emit(repository.observeWeeklyHeatmapCells(System.currentTimeMillis() - WEEK_IN_MILLISECONDS, Long.MAX_VALUE))
            delay(HEATMAP_REFRESH_INTERVAL_MS)
        }
    }.flatMapLatest { it }
    // Recompute only when completed IDs change, not on every GPS write during a run.
    private val historyHeatmapCells = repository.completedRuns
        .map { runs -> runs.map(RunModel::id) }
        .distinctUntilChanged()
        .mapLatest { repository.getCompletedRunHeatmapCells() }
    private val selectedStepRateSamples = selectedRunId.flatMapLatest { runId ->
        runId?.let(repository::observeStepRateSamples) ?: flowOf(emptyList())
    }
    private val selectedRunMedia = selectedRunId.flatMapLatest { runId ->
        runId?.let(repository::observeRunMedia) ?: flowOf(emptyList())
    }
    private val selectedLocalSegmentEfforts = selectedRunId.flatMapLatest { runId ->
        runId?.let(repository::observeLocalSegmentEfforts) ?: flowOf(emptyList())
    }
    private val selectedRoute = combine(
        repository.activeRun,
        selectedPlannedRouteRunId,
        selectedPlannedRouteId
    ) { run, selectedRunId, selectedRouteId ->
        if (run != null) SelectedRunRoute(run.plannedRouteRunId, run.plannedRouteId)
        else SelectedRunRoute(selectedRunId, selectedRouteId)
    }.distinctUntilChanged().flatMapLatest(::observeSelectedRoute).map { (selection, points) ->
        val (turns, elevationProfile) = withContext(Dispatchers.Default) {
            inferRunTurnInstructions(points) to calculateRunElevationProfile(points)
        }
        SelectedRunRouteData(selection, points, turns, elevationProfile)
    }
    private val importedRoutes = plannedRouteRepository.routes
    private val localSegmentStarts = plannedRouteRepository.localSegmentStarts
    private val activeLocalSegmentEfforts = repository.activeRun.flatMapLatest { run ->
        run?.let { repository.observeLocalSegmentEfforts(it.id) } ?: flowOf(emptyList())
    }
    private val activeLocalSegmentRoutes = activeLocalSegmentEfforts
        .map { efforts -> efforts.map(RunLocalSegmentEffort::routeId).distinct() }
        .distinctUntilChanged()
        .mapLatest { routeIds ->
            routeIds.associateWith { routeId ->
                val points = plannedRouteRepository.observePoints(routeId).first()
                val routePoints: List<RoutePoint> = points
                RunLocalSegmentRouteData(
                    points = routePoints,
                    elevationProfile = withContext(Dispatchers.Default) {
                        calculateRunElevationProfile(routePoints)
                    }
                )
            }
        }
    private val localSegmentUiData = combine(
        activeLocalSegmentEfforts,
        activeLocalSegmentRoutes,
        localSegmentStarts,
        repository.completedLocalSegmentEfforts
    ) { efforts, routes, starts, completedEfforts ->
        RunLocalSegmentUiData(efforts, routes, starts, completedEfforts)
    }
    private val liveSegmentsEnabled = appSettingsRepository?.settings
        ?.map { it.runLiveSegmentsEnabled }
        ?: flowOf(true)
    private val turnCueLimit = appSettingsRepository?.settings
        ?.map { if (it.runDisplayAllTurnCues) Int.MAX_VALUE else it.runUpcomingTurnCueCount }
        ?: flowOf(1)
    private val activeMapCameraSnapshot = appSettingsRepository?.runMapCameraSnapshot ?: flowOf(null)
    private val plannedRouteProgressTracker = RunRouteProgressTracker()
    private val streetReturnProgressTracker = RunRouteProgressTracker()
    private val plannedStreetGuidanceProgressTracker = RunRouteProgressTracker()

    private fun observeSelectedRoute(
        selection: SelectedRunRoute
    ): Flow<Pair<SelectedRunRoute, List<RoutePoint>>> = when {
        selection.runId != null -> repository.observePoints(selection.runId).map { points ->
            val routePoints: List<RoutePoint> = points
            selection to routePoints
        }
        selection.plannedRouteId != null -> plannedRouteRepository.observePoints(selection.plannedRouteId).map { points ->
            val routePoints: List<RoutePoint> = points
            selection to routePoints
        }
        else -> flowOf(selection to emptyList<RoutePoint>())
    }
    private val activeLaps = repository.activeRun.flatMapLatest { run ->
        run?.let { repository.observeLaps(it.id) } ?: flowOf(emptyList())
    }
    private val selectedLaps = selectedRunId.flatMapLatest { runId ->
        runId?.let(repository::observeLaps) ?: flowOf(emptyList())
    }
    private val activeWorkoutSteps = repository.activeRun.flatMapLatest { run ->
        run?.let { repository.observeWorkoutSteps(it.id) } ?: flowOf(emptyList())
    }
    private val selectedWorkoutSteps = selectedRunId.flatMapLatest { runId ->
        runId?.let(repository::observeWorkoutSteps) ?: flowOf(emptyList())
    }
    private val gpsClock = flow {
        while (true) {
            emit(SystemClock.elapsedRealtime())
            delay(2_000)
        }
    }

    /** Junta histórico, rota e leituras atuais de GPS e cadência. */
    val uiState = combine(
        repository.activeRun,
        activePoints,
        repository.completedRuns,
        selectedRun,
        selectedPoints
    ) { activeRun, route, history, detail, detailRoute ->
        RunUiState(
            activeRun = activeRun,
            activePoints = route,
            completedRuns = history,
            selectedRun = detail,
            selectedPoints = detailRoute
        )
    }.combine(activeLaps) { state, laps -> state.copy(activeLaps = laps) }
    .combine(weeklyHeatmapCells) { state, cells -> state.copy(weeklyHeatmapCells = cells) }
    .combine(historyHeatmapCells) { state, cells -> state.copy(historyHeatmapCells = cells) }
    .combine(selectedLaps) { state, laps -> state.copy(selectedLaps = laps) }
    .combine(selectedStepRateSamples) { state, samples -> state.copy(selectedStepRateSamples = samples) }
    .combine(selectedRunMedia) { state, media -> state.copy(selectedRunMedia = media) }
    .combine(selectedLocalSegmentEfforts) { state, efforts -> state.copy(selectedLocalSegmentEfforts = efforts) }
    .combine(selectedBestEffortOverrides) { state, overrides ->
        state.copy(selectedBestEffortOverrides = overrides)
    }
    .combine(activeWorkoutSteps) { state, steps -> state.copy(activeWorkoutSteps = steps) }
    .combine(selectedWorkoutSteps) { state, steps -> state.copy(selectedWorkoutSteps = steps) }
    .combine(importedRoutes) { state, routes -> state.copy(plannedRoutes = routes) }
    .combine(selectedPaceTarget) { state, target -> state.copy(selectedPaceTarget = target) }
    .combine(selectedWorkoutPlan) { state, plan -> state.copy(selectedWorkoutPlan = plan) }
    .combine(workoutTemplates) { state, templates -> state.copy(workoutTemplates = templates) }
    .combine(selectedWorkoutTemplateId) { state, id -> state.copy(selectedWorkoutTemplateId = id) }
    .combine(routeImportFailure) { state, failure -> state.copy(routeImportFailed = failure != null) }
    .combine(streetReturnState) { state, routeState ->
        val run = state.activeRun
        val route = routeState.route
        val position = if (run != null && route != null) {
            streetReturnProgressTracker.update(
                activeRunId = run.id,
                selectedRouteKey = "return",
                point = state.activePoints.lastOrNull { !it.isFiltered },
                route = route.points
            )
        } else {
            streetReturnProgressTracker.reset()
            null
        }
        val nextGuidance = route?.let { streetRoute ->
            position?.takeUnless(::isOffRunRoutePosition)?.let {
                nextRunStreetGuidance(streetRoute.instructions, it.progressMeters)
            }
        }
        state.copy(streetReturnState = routeState.copy(nextGuidance = nextGuidance))
    }
    .combine(combine(RunTrackingState.gpsReading, RunTrackingState.stepRateReading, gpsClock) { gps, cadence, now -> Triple(gps, cadence, now) }) { state, (reading, cadence, now) ->
        val runId = state.activeRun?.id ?: -1
        state.copy(
            gpsStatus = runGpsStatus(reading, runId, now),
            currentPaceSecondsPerKm = currentPaceSecondsPerKm(reading, runId, now),
            currentCadenceStepsPerMinute = currentRunStepRate(cadence, runId, now)
        )
    }
    .combine(RunTrackingState.routeAlertMute) { state, mute ->
        state.copy(
            isRouteAlertsMutedUntilReturn = mute?.runId == state.activeRun?.id &&
                mute?.mutedUntilReturn == true
        )
    }
    .combine(combine(selectedRoute, localSegmentUiData, liveSegmentsEnabled, turnCueLimit) {
        route, data, segmentsEnabled, turnCueLimit ->
        RunRouteDisplayData(route, data, segmentsEnabled, turnCueLimit)
    }) { state, routeData ->
        val selectedRoute = routeData.selectedRoute
        val segmentData = routeData.segmentData
        val localSegments = if (routeData.liveSegmentsEnabled) state.activeRun?.let { run ->
            RunLiveSegmentPresenter.build(
                run = run,
                selectedRoute = selectedRoute.points,
                selectedElevationProfile = selectedRoute.elevationProfile,
                completedRuns = state.completedRuns,
                plannedRoutes = state.plannedRoutes,
                segmentData = segmentData,
                latestPoint = state.activePoints.lastOrNull { !it.isFiltered },
                canShowApproach = run.status == RunStatus.RUNNING && state.gpsStatus == RunGpsStatus.GOOD
            )
        }.orEmpty() else emptyList()
        val selectedRouteKey = selectedRoute.selection.runId?.let { "run:$it" }
            ?: selectedRoute.selection.plannedRouteId?.let { "gpx:$it" }
        val routePosition = state.activeRun?.let { run ->
            plannedRouteProgressTracker.update(
                activeRunId = run.id,
                selectedRouteKey = selectedRouteKey,
                point = state.activePoints.lastOrNull { !it.isFiltered }
                    .takeIf { run.status == RunStatus.RUNNING && state.gpsStatus == RunGpsStatus.GOOD },
                route = selectedRoute.points,
                recordedPoints = state.activePoints
            )
        } ?: run {
            plannedRouteProgressTracker.reset()
            null
        }
        val turnGuidances = state.activeRun?.takeIf { it.status == RunStatus.RUNNING }?.let { run ->
            upcomingRunTurnGuidances(
                selectedRoute.turns,
                routePosition?.progressMeters ?: run.distanceMeters,
                routeData.turnCueLimit
            )
        }.orEmpty()
        state.copy(
            plannedRouteRunId = selectedRoute.selection.runId,
            plannedRouteId = selectedRoute.selection.plannedRouteId,
            plannedRoutePoints = selectedRoute.points,
            plannedRoutePosition = routePosition,
            localSegments = localSegments,
            nextTurnGuidance = turnGuidances.firstOrNull(),
            upcomingTurnGuidances = turnGuidances,
            isOffPlannedRoute = isOffRunRoutePosition(routePosition)
        )
    }
    .combine(plannedRouteGuidance) { state, guidanceState ->
        val run = state.activeRun
        val route = guidanceState.route
        val position = if (run != null && route != null && state.plannedRouteKey != null) {
            plannedStreetGuidanceProgressTracker.update(
                activeRunId = run.id,
                selectedRouteKey = "guidance:${state.plannedRouteKey}",
                point = state.activePoints.lastOrNull { !it.isFiltered },
                route = route.points
            )
        } else {
            plannedStreetGuidanceProgressTracker.reset()
            null
        }
        val guidance = route?.let { streetRoute ->
            position?.takeUnless(::isOffRunRoutePosition)?.let {
                nextRunStreetGuidance(streetRoute.instructions, it.progressMeters)
            }
        }
        state.copy(plannedRouteGuidance = guidanceState.copy(nextGuidance = guidance))
    }
    .combine(combine(activeRunOpen, runPreparationOpen) { isActive, isPreparing -> isActive to isPreparing }) { state, (isActive, isPreparing) ->
        state.copy(isActiveRunOpen = isActive, isRunPreparationOpen = isPreparing)
    }
    .combine(mapPoiController.state) { state, poiState -> state.copy(mapPoiState = poiState) }
    .combine(bestEffortRecords) { state, records -> state.copy(bestEffortRecords = records) }
    .combine(runGearRepository.gears) { state, gears -> state.copy(runGears = gears) }
    .combine(activeMapCameraSnapshot) { state, snapshot ->
        state.copy(activeMapCameraSnapshot = snapshot?.takeIf { it.runId == state.activeRun?.id })
    }
    .combine(privacyZones) { state, zones -> state.copy(privacyZones = zones) }
    .flowOn(Dispatchers.Default)
    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), RunUiState())


}

private data class SelectedRunRoute(val runId: Long?, val plannedRouteId: Long?)

private data class SelectedRunRouteData(
    val selection: SelectedRunRoute,
    val points: List<RoutePoint>,
    val turns: List<RunTurnInstruction>,
    val elevationProfile: List<RunElevationProfilePoint>
)

private data class RunRouteDisplayData(
    val selectedRoute: SelectedRunRouteData,
    val segmentData: RunLocalSegmentUiData,
    val liveSegmentsEnabled: Boolean,
    val turnCueLimit: Int
)

private const val WEEK_IN_MILLISECONDS = 7 * 24 * 60 * 60 * 1_000L
private const val HEATMAP_REFRESH_INTERVAL_MS = 60 * 60 * 1_000L
