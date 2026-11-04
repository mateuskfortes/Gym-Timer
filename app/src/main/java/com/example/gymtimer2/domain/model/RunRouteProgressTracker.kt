package com.example.gymtimer2.domain.model

/** Preserves route progress between GPS readings so overlapping route segments stay stable. */
internal class RunRouteProgressTracker {
    private var runId: Long? = null
    private var routeKey: String? = null
    private var lastProgressMeters: Double? = null

    fun update(
        activeRunId: Long,
        selectedRouteKey: String?,
        point: RunPointModel?,
        route: List<RoutePoint>,
        recordedPoints: List<RunPointModel> = emptyList()
    ): RunRoutePosition? {
        if (selectedRouteKey == null) {
            reset()
            return null
        }
        if (route.isEmpty()) return null
        if (activeRunId != runId || selectedRouteKey != routeKey) {
            runId = activeRunId
            routeKey = selectedRouteKey
            lastProgressMeters = null
            recordedPoints.filterNot(RunPointModel::isFiltered).forEach { recordedPoint ->
                updateProgress(recordedPoint, route)
            }
        }

        val currentPoint = point?.takeUnless { it.isFiltered } ?: return null
        return updateProgress(currentPoint, route)
    }

    private fun updateProgress(point: RunPointModel, route: List<RoutePoint>): RunRoutePosition? {
        val position = runRoutePosition(
            point.latitude,
            point.longitude,
            route,
            minimumProgressMeters = lastProgressMeters
        ) ?: runRoutePosition(point.latitude, point.longitude, route)

        if (position != null && !isOffRunRoutePosition(position)) {
            lastProgressMeters = position.progressMeters
        }
        return position
    }

    fun reset() {
        runId = null
        routeKey = null
        lastProgressMeters = null
    }
}
