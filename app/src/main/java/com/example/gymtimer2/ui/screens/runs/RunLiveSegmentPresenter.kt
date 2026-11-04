@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs

import com.example.gymtimer2.domain.model.LocalSegmentStart
import com.example.gymtimer2.domain.model.LocalSegmentStartApproach
import com.example.gymtimer2.domain.model.PlannedRouteModel
import com.example.gymtimer2.domain.model.RoutePoint
import com.example.gymtimer2.domain.model.RunElevationProfilePoint
import com.example.gymtimer2.domain.model.RunLocalSegmentApproach
import com.example.gymtimer2.domain.model.RunLocalSegmentEffort
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.findLocalSegmentApproach
import com.example.gymtimer2.domain.model.findLocalSegmentStartApproaches
import com.example.gymtimer2.domain.model.runRouteLengthMeters

/** Dados das rotas de segmentos que a tela usa para desenhar o progresso. */
internal data class RunLocalSegmentRouteData(
    val points: List<RoutePoint> = emptyList(),
    val elevationProfile: List<RunElevationProfilePoint> = emptyList()
)

/** Leituras e rotas locais usadas para apresentar segmentos durante a gravação. */
internal data class RunLocalSegmentUiData(
    val efforts: List<RunLocalSegmentEffort>,
    val routes: Map<Long, RunLocalSegmentRouteData>,
    val starts: List<LocalSegmentStart>,
    val completedEfforts: List<RunLocalSegmentEffort>
)

/** Constrói os segmentos próximos e as tentativas que aparecem durante uma corrida. */
internal object RunLiveSegmentPresenter {
    fun build(
        run: RunModel,
        selectedRoute: List<RoutePoint>,
        selectedElevationProfile: List<RunElevationProfilePoint>,
        completedRuns: List<RunModel>,
        plannedRoutes: List<PlannedRouteModel>,
        segmentData: RunLocalSegmentUiData,
        latestPoint: RunPointModel?,
        canShowApproach: Boolean
    ): List<RunLiveSegment> {
        val effortRouteIds = segmentData.efforts.mapTo(mutableSetOf(), RunLocalSegmentEffort::routeId)
        val segments = buildList {
            if (run.plannedRouteId !in effortRouteIds) {
                localSelectedRouteSegment(
                    run,
                    selectedRoute,
                    completedRuns,
                    plannedRoutes,
                    selectedElevationProfile,
                    latestPoint,
                    canShowApproach
                )?.let(::add)
            }

            if (canShowApproach) {
                findLocalSegmentStartApproaches(latestPoint, segmentData.starts)
                    .filterNot { it.start.routeId in effortRouteIds || it.start.routeId == run.plannedRouteId }
                    .forEach { approach ->
                        add(
                            approachingLocalSegment(
                                approach,
                                bestLocalSegmentDurationMs(
                                    approach.start.routeId,
                                    completedRuns,
                                    segmentData.completedEfforts
                                )
                            )
                        )
                    }
            }

            segmentData.efforts.mapNotNullTo(this) { effort ->
                val route = segmentData.routes[effort.routeId]
                val routeModel = plannedRoutes.firstOrNull { it.id == effort.routeId }
                val routeDistanceMeters = routeModel?.distanceMeters
                    ?: route?.points?.takeIf { it.isNotEmpty() }?.let(::runRouteLengthMeters)
                    ?: return@mapNotNullTo null
                localSegmentEffort(
                    run = run,
                    effort = effort,
                    routeDistanceMeters = routeDistanceMeters,
                    name = routeModel?.name,
                    bestDurationMs = bestLocalSegmentDurationMs(
                        effort.routeId,
                        completedRuns,
                        segmentData.completedEfforts
                    ),
                    elevationProfile = route?.elevationProfile.orEmpty()
                )
            }
        }
        return segments.sortedWith(
            compareBy<RunLiveSegment> { it.distanceToStartMeters != null }
                .thenBy { it.distanceToStartMeters ?: 0.0 }
        )
    }

    private fun localSelectedRouteSegment(
        run: RunModel,
        route: List<RoutePoint>,
        completedRuns: List<RunModel>,
        plannedRoutes: List<PlannedRouteModel>,
        elevationProfile: List<RunElevationProfilePoint>,
        latestPoint: RunPointModel?,
        canShowApproach: Boolean
    ): RunLiveSegment? {
        val context = localSegmentContext(run, route)
        val approach = localSegmentApproach(run, context, latestPoint, route, canShowApproach)
        if (context == null && approach == null) return null
        val routeDistanceMeters = context?.routeDistanceMeters ?: approach?.routeDistanceMeters ?: return null
        val name = run.plannedRouteId?.let { id -> plannedRoutes.firstOrNull { it.id == id }?.name }
            ?: run.plannedRouteRunId?.let { id -> completedRuns.firstOrNull { it.id == id }?.title }
        val bestDurationMs = bestLocalSegmentDurationMs(run, completedRuns, routeDistanceMeters)
        val progressMeters = if (context == null) 0.0
        else run.localSegmentProgressMeters.coerceIn(0.0, routeDistanceMeters)
        return RunLiveSegment(
            name = name,
            progressMeters = progressMeters,
            distanceMeters = routeDistanceMeters,
            elapsedDurationMs = context?.elapsedDurationMs ?: 0L,
            bestDurationMs = bestDurationMs,
            differenceFromBestMs = localSegmentDifferenceFromBest(
                context,
                bestDurationMs,
                progressMeters,
                routeDistanceMeters
            ),
            isCompleted = context?.isCompleted == true,
            isNewRecord = isNewLocalSegmentRecord(context, bestDurationMs),
            distanceToStartMeters = approach?.distanceToStartMeters,
            elevationProfile = elevationProfile,
            routeId = run.plannedRouteId
        )
    }

    private fun approachingLocalSegment(
        approach: LocalSegmentStartApproach,
        bestDurationMs: Long?
    ) = RunLiveSegment(
        name = approach.start.name,
        progressMeters = 0.0,
        distanceMeters = approach.start.distanceMeters,
        elapsedDurationMs = 0L,
        bestDurationMs = bestDurationMs,
        differenceFromBestMs = null,
        isCompleted = false,
        isNewRecord = false,
        distanceToStartMeters = approach.distanceToStartMeters,
        routeId = approach.start.routeId
    )

    private fun localSegmentEffort(
        run: RunModel,
        effort: RunLocalSegmentEffort,
        routeDistanceMeters: Double,
        name: String?,
        bestDurationMs: Long?,
        elevationProfile: List<RunElevationProfilePoint>
    ): RunLiveSegment? {
        val startedAt = effort.startedAtDurationMs ?: return null
        val isCompleted = effort.completedAtDurationMs != null
        val elapsedDurationMs = ((effort.completedAtDurationMs ?: run.durationMs) - startedAt)
            .takeIf { it >= 0L } ?: return null
        val progressMeters = effort.progressMeters.coerceIn(0.0, routeDistanceMeters)
        val context = LocalSegmentContext(routeDistanceMeters, elapsedDurationMs, isCompleted)
        return RunLiveSegment(
            name = name,
            progressMeters = progressMeters,
            distanceMeters = routeDistanceMeters,
            elapsedDurationMs = elapsedDurationMs,
            bestDurationMs = bestDurationMs,
            differenceFromBestMs = localSegmentDifferenceFromBest(
                context,
                bestDurationMs,
                progressMeters,
                routeDistanceMeters
            ),
            isCompleted = isCompleted,
            isNewRecord = isNewLocalSegmentRecord(context, bestDurationMs),
            elevationProfile = elevationProfile,
            routeId = effort.routeId
        )
    }

    private fun bestLocalSegmentDurationMs(
        routeId: Long,
        completedRuns: List<RunModel>,
        completedEfforts: List<RunLocalSegmentEffort>
    ): Long? = sequenceOf(
        completedEfforts.asSequence()
            .filter { it.routeId == routeId }
            .mapNotNull(RunLocalSegmentEffort::durationMs),
        completedRuns.asSequence()
            .filter { it.plannedRouteId == routeId }
            .mapNotNull { it.localSegmentDuration() }
    ).flatten().minOrNull()

    private fun localSegmentApproach(
        run: RunModel,
        context: LocalSegmentContext?,
        latestPoint: RunPointModel?,
        route: List<RoutePoint>,
        canShowApproach: Boolean
    ): RunLocalSegmentApproach? {
        if (context != null || run.localSegmentStartedAtDurationMs != null || !canShowApproach) return null
        return findLocalSegmentApproach(latestPoint, route)
    }

    private fun localSegmentDifferenceFromBest(
        context: LocalSegmentContext?,
        bestDurationMs: Long?,
        progressMeters: Double,
        routeDistanceMeters: Double
    ): Long? {
        if (context == null || bestDurationMs == null) return null
        val expectedDurationMs = if (context.isCompleted) bestDurationMs
        else (bestDurationMs * progressMeters / routeDistanceMeters).toLong()
        return expectedDurationMs - context.elapsedDurationMs
    }

    private fun isNewLocalSegmentRecord(context: LocalSegmentContext?, bestDurationMs: Long?): Boolean {
        if (context?.isCompleted != true) return false
        return bestDurationMs == null || context.elapsedDurationMs < bestDurationMs
    }

    private fun localSegmentContext(run: RunModel, route: List<RoutePoint>): LocalSegmentContext? {
        val startedAtDurationMs = run.localSegmentStartedAtDurationMs ?: return null
        val routeDistanceMeters = runRouteLengthMeters(route)
        if (routeDistanceMeters <= 0.0) return null
        val completedAtDurationMs = run.localSegmentCompletedAtDurationMs
        val elapsedDurationMs = (completedAtDurationMs ?: run.durationMs) - startedAtDurationMs
        if (elapsedDurationMs < 0L) return null
        return LocalSegmentContext(routeDistanceMeters, elapsedDurationMs, completedAtDurationMs != null)
    }

    private fun bestLocalSegmentDurationMs(
        run: RunModel,
        completedRuns: List<RunModel>,
        routeDistanceMeters: Double
    ): Long? {
        val previousEfforts = completedRuns.mapNotNull { run.localSegmentDurationFor(it) }
        val sourceRunEffort = run.sourceRunEffort(completedRuns, routeDistanceMeters)
        return (previousEfforts + listOfNotNull(sourceRunEffort)).minOrNull()
    }

    private fun RunModel.sourceRunEffort(
        completedRuns: List<RunModel>,
        routeDistanceMeters: Double
    ): Long? {
        val sourceRunId = plannedRouteRunId ?: return null
        val sourceRun = completedRuns.firstOrNull { it.id == sourceRunId } ?: return null
        val distanceRatio = sourceRun.distanceMeters / routeDistanceMeters
        return sourceRun.durationMs.takeIf {
            it > 0L && distanceRatio in MIN_ROUTE_DISTANCE_RATIO..MAX_ROUTE_DISTANCE_RATIO
        }
    }

    private fun RunModel.localSegmentDurationFor(pastRun: RunModel): Long? {
        if (!usesSameLocalRoute(pastRun)) return null
        return pastRun.localSegmentDuration()
    }

    private fun RunModel.localSegmentDuration(): Long? {
        val start = localSegmentStartedAtDurationMs ?: return null
        val finish = localSegmentCompletedAtDurationMs ?: return null
        return (finish - start).takeIf { it > 0L }
    }

    private fun RunModel.usesSameLocalRoute(other: RunModel): Boolean = when {
        localSegmentRouteId != null -> other.localSegmentRouteId == localSegmentRouteId ||
            other.plannedRouteId == localSegmentRouteId
        plannedRouteRunId != null -> other.plannedRouteRunId == plannedRouteRunId
        plannedRouteId != null -> other.plannedRouteId == plannedRouteId || other.localSegmentRouteId == plannedRouteId
        else -> false
    }

    private data class LocalSegmentContext(
        val routeDistanceMeters: Double,
        val elapsedDurationMs: Long,
        val isCompleted: Boolean
    )

    private const val MIN_ROUTE_DISTANCE_RATIO = 0.8
    private const val MAX_ROUTE_DISTANCE_RATIO = 1.2
}
