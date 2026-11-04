@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.tracking

import com.example.gymtimer2.data.repository.PlannedRouteRepository
import com.example.gymtimer2.data.repository.RunRepository
import com.example.gymtimer2.domain.model.AppSettings
import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.LocalSegmentStart
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import com.example.gymtimer2.domain.model.RunRouteAlertState
import com.example.gymtimer2.domain.model.RunLocalSegmentTracker
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.findLocalSegmentStarts
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.isOffRunRoute
import kotlinx.coroutines.flow.first

/** Coordena alertas de desvio e tentativas de segmentos locais. */
internal class RunRouteCueController(
    private val runRepository: RunRepository,
    private val plannedRouteRepository: PlannedRouteRepository,
    private val settings: () -> AppSettings,
    private val onMuteStateChanged: (Long, Boolean) -> Unit,
    private val onRouteAlertNotification: (Long) -> Unit,
    private val onRouteAlertNotificationCleared: () -> Unit
) {
    private val routeAlertState = RunRouteAlertState()
    private var activeRunId: Long? = null
    private var plannedRoutePoints: List<RoutePoint>? = null
    private var localSegmentTracker: RunLocalSegmentTracker? = null
    private var localSegmentTrackerRunId: Long? = null
    private var localSegmentStarts: List<LocalSegmentStart>? = null
    private var localSegmentEffortsRunId: Long? = null
    private val localSegmentEfforts = mutableMapOf<Long, RunLocalSegmentEffort>()
    private val localSegmentTrackers = mutableMapOf<Long, RunLocalSegmentTracker>()
    private val localSegmentRoutes = mutableMapOf<Long, List<RoutePoint>>()

    /** Limpa os dados da sessão anterior antes de começar uma corrida nova. */
    fun resetForNewRun(runId: Long) {
        activeRunId = runId
        plannedRoutePoints = null
        localSegmentTracker = null
        localSegmentTrackerRunId = null
        localSegmentStarts = null
        localSegmentEffortsRunId = null
        localSegmentEfforts.clear()
        localSegmentTrackers.clear()
        localSegmentRoutes.clear()
        routeAlertState.reset()
        onMuteStateChanged(runId, false)
        onRouteAlertNotificationCleared()
    }

    /** Inicializa a sessão recuperada e permite repetir o aviso ao sair da rota. */
    fun resumeRun(runId: Long) {
        if (activeRunId != runId) {
            resetForNewRun(runId)
        } else {
            routeAlertState.resetForResume()
        }
    }

    /** Silencia o aviso atual até que a posição volte à rota. */
    fun muteAlertsOnce(runId: Long) {
        if (activeRunId != runId) return
        routeAlertState.muteUntilRouteReturn()
        onMuteStateChanged(runId, routeAlertState.isMutedUntilReturn)
        onRouteAlertNotificationCleared()
    }

    /** Persiste o silenciamento dos avisos de desvio até o fim da corrida. */
    suspend fun muteAlertsForEntireRun(runId: Long) {
        if (activeRunId != runId) return
        runRepository.muteRouteAlerts(runId)
        onRouteAlertNotificationCleared()
    }

    /** Avalia os alertas ativos para uma posição aceita da corrida. */
    suspend fun update(runId: Long, run: RunModel, point: GpsPoint, durationMs: Long) {
        if (activeRunId != runId) return
        val preferences = settings()
        val route = loadPlannedRoute(run)
        if (preferences.runLiveSegmentsEnabled) {
            updatePlannedRouteSegmentEffort(run, point, durationMs, route)
            updateAutomaticLocalSegments(run, point, durationMs)
        }
        if (route.isEmpty()) {
            routeAlertState.update(false)
            onMuteStateChanged(runId, false)
            onRouteAlertNotificationCleared()
            return
        }
        val offRoute = isOffRunRoute(point.latitude, point.longitude, route)
        updateOffRouteAlert(
            runId = runId,
            preferences = preferences,
            offRoute = offRoute,
            mutedForRun = run.routeAlertsMuted
        )
    }

    private suspend fun updatePlannedRouteSegmentEffort(
        run: RunModel,
        point: GpsPoint,
        durationMs: Long,
        plannedRoute: List<RoutePoint>
    ) {
        if (run.status != RunStatus.RUNNING || plannedRoute.isEmpty()) return
        val selectedLocalSegment = run.plannedRouteId?.let { routeId ->
            loadLocalSegmentStarts().any { it.routeId == routeId }
        } == true
        if (selectedLocalSegment) return

        val tracker = if (localSegmentTrackerRunId == run.id) {
            localSegmentTracker
        } else {
            RunLocalSegmentTracker(
                route = plannedRoute,
                initialProgressMeters = run.localSegmentProgressMeters,
                initialStartedAtDurationMs = run.localSegmentStartedAtDurationMs,
                initialCompletedAtDurationMs = run.localSegmentCompletedAtDurationMs
            ).also {
                localSegmentTrackerRunId = run.id
                localSegmentTracker = it
            }
        } ?: return
        val update = tracker.update(point, durationMs) ?: return
        runRepository.updateLocalSegmentProgress(
            runId = run.id,
            progressMeters = update.progressMeters,
            startedAtDurationMs = update.startedAtDurationMs,
            completedAtDurationMs = update.completedAtDurationMs
        )
    }

    private suspend fun updateAutomaticLocalSegments(run: RunModel, point: GpsPoint, durationMs: Long) {
        if (run.status != RunStatus.RUNNING) return
        loadLocalSegmentEfforts(run.id)
        val starts = loadLocalSegmentStarts()
        findLocalSegmentStarts(point, starts)
            .filterNot { it.routeId in localSegmentEfforts }
            .forEach { start ->
                val route = loadLocalSegmentRoute(start.routeId)
                if (route.isNotEmpty()) {
                    localSegmentTrackers[start.routeId] = RunLocalSegmentTracker(route)
                    localSegmentEfforts[start.routeId] = RunLocalSegmentEffort(start.routeId, 0.0, null, null)
                }
            }

        localSegmentTrackers.toMap().forEach { (routeId, tracker) ->
            val previousEffort = localSegmentEfforts[routeId] ?: return@forEach
            if (previousEffort.completedAtDurationMs != null) return@forEach
            val update = tracker.update(point, durationMs) ?: return@forEach
            val effort = RunLocalSegmentEffort(
                routeId = routeId,
                progressMeters = update.progressMeters,
                startedAtDurationMs = update.startedAtDurationMs,
                completedAtDurationMs = update.completedAtDurationMs
            )
            runRepository.saveLocalSegmentEffort(run.id, effort)
            localSegmentEfforts[routeId] = effort
        }
    }

    private suspend fun loadLocalSegmentStarts(): List<LocalSegmentStart> {
        localSegmentStarts?.let { return it }
        return plannedRouteRepository.localSegmentStarts.first().also { localSegmentStarts = it }
    }

    private suspend fun loadLocalSegmentEfforts(runId: Long) {
        if (localSegmentEffortsRunId == runId) return
        localSegmentEfforts.clear()
        localSegmentTrackers.clear()
        runRepository.getLocalSegmentEfforts(runId).forEach { effort ->
            localSegmentEfforts[effort.routeId] = effort
            if (effort.completedAtDurationMs == null && effort.startedAtDurationMs != null) {
                val route = loadLocalSegmentRoute(effort.routeId)
                if (route.isNotEmpty()) {
                    localSegmentTrackers[effort.routeId] = RunLocalSegmentTracker(
                        route = route,
                        initialProgressMeters = effort.progressMeters,
                        initialStartedAtDurationMs = effort.startedAtDurationMs,
                        initialCompletedAtDurationMs = effort.completedAtDurationMs
                    )
                }
            }
        }
        localSegmentEffortsRunId = runId
    }

    private suspend fun loadLocalSegmentRoute(routeId: Long): List<RoutePoint> {
        localSegmentRoutes[routeId]?.let { return it }
        val points = plannedRouteRepository.observePoints(routeId).first()
        return points.also { localSegmentRoutes[routeId] = it }
    }

    /** Carrega uma vez por corrida a rota referenciada pela sessão. */
    private suspend fun loadPlannedRoute(run: RunModel): List<RoutePoint> {
        plannedRoutePoints?.let { return it }
        val route = when {
            run.plannedRouteRunId != null -> runRepository.observePoints(run.plannedRouteRunId).first()
            run.plannedRouteId != null -> plannedRouteRepository.observePoints(run.plannedRouteId).first()
            else -> emptyList()
        }
        return route.also { plannedRoutePoints = it }
    }

    /** Emite o aviso de afastamento uma vez até a pessoa voltar à rota. */
    private fun updateOffRouteAlert(
        runId: Long,
        preferences: AppSettings,
        offRoute: Boolean,
        mutedForRun: Boolean
    ) {
        val shouldNotify = routeAlertState.update(offRoute)
        onMuteStateChanged(runId, routeAlertState.isMutedUntilReturn)
        if (!offRoute) {
            onRouteAlertNotificationCleared()
        } else if (!preferences.runOffRouteAlertsEnabled || mutedForRun || routeAlertState.isMutedUntilReturn) {
            onRouteAlertNotificationCleared()
        } else if (shouldNotify && preferences.runRouteNotificationsEnabled) {
            onRouteAlertNotification(runId)
        }
    }
}
