package com.example.gymtimer2.domain.model

import kotlin.math.cos
import kotlin.math.hypot

/** Calcula a distância mínima, em metros, entre a posição atual e os segmentos da rota. */
fun distanceFromRunRouteMeters(position: RoutePoint, route: List<RoutePoint>): Double? =
    distanceFromRunRouteMeters(position.latitude, position.longitude, route)

/** Calcula a distância mínima, em metros, a partir de coordenadas atuais. */
fun distanceFromRunRouteMeters(latitude: Double, longitude: Double, route: List<RoutePoint>): Double? {
    return runRoutePosition(latitude, longitude, route)?.distanceFromRouteMeters
}

/** Projeta uma posição na rota sem ligar trechos separados. */
fun runRoutePosition(
    latitude: Double,
    longitude: Double,
    route: List<RoutePoint>,
    minimumProgressMeters: Double? = null
): RunRoutePosition? {
    if (!hasValidCoordinates(latitude, longitude)) return null
    val candidates = collectRoutePositionCandidates(latitude, longitude, route, minimumProgressMeters)
    val nearestDistanceMeters = candidates.positions.minOfOrNull(RunRoutePosition::distanceFromRouteMeters) ?: return null
    val selected = selectRoutePosition(candidates.positions, nearestDistanceMeters, minimumProgressMeters) ?: return null
    return selected.copy(routeDistanceMeters = candidates.routeDistanceMeters)
}

private fun collectRoutePositionCandidates(
    latitude: Double,
    longitude: Double,
    route: List<RoutePoint>,
    minimumProgressMeters: Double?
): RoutePositionCandidates {
    var progressMeters = 0.0
    val candidates = mutableListOf<RunRoutePosition>()
    val points = route.filterNot(RoutePoint::isFiltered)
    for (index in 1 until points.size) {
        val start = points[index - 1]
        val end = points[index]
        if (start.segmentIndex == end.segmentIndex && start.hasValidCoordinates() && end.hasValidCoordinates()) {
            val projection = projectOntoRouteSegment(latitude, longitude, start, end)
            if (projection != null) {
                val segmentLengthMeters = distanceMetersBetweenCoordinates(
                    start.latitude,
                    start.longitude,
                    end.latitude,
                    end.longitude
                )
                val candidateProgressMeters = progressMeters + segmentLengthMeters * projection.ratio
                if (minimumProgressMeters == null ||
                    candidateProgressMeters >= minimumProgressMeters - ROUTE_PROGRESS_BACKTRACK_METERS
                ) {
                    candidates += RunRoutePosition(projection.distanceMeters, candidateProgressMeters, 0.0)
                }
                progressMeters += segmentLengthMeters
            }
        }
    }
    return RoutePositionCandidates(candidates, progressMeters)
}

private fun selectRoutePosition(
    candidates: List<RunRoutePosition>,
    nearestDistanceMeters: Double,
    minimumProgressMeters: Double?
): RunRoutePosition? = if (minimumProgressMeters == null) {
    candidates.minByOrNull(RunRoutePosition::distanceFromRouteMeters)
} else {
    candidates.filter {
        it.distanceFromRouteMeters <= nearestDistanceMeters + ROUTE_MATCH_TIE_METERS
    }.minByOrNull { kotlin.math.abs(it.progressMeters - minimumProgressMeters) }
}

/** Distância total do traçado, sem somar lacunas entre trechos. */
fun runRouteLengthMeters(route: List<RoutePoint>): Double {
    var lengthMeters = 0.0
    val points = route.filterNot(RoutePoint::isFiltered)
    for (index in 1 until points.size) {
        val start = points[index - 1]
        val end = points[index]
        if (start.segmentIndex == end.segmentIndex && start.hasValidCoordinates() && end.hasValidCoordinates()) {
            lengthMeters += distanceMetersBetweenCoordinates(
                start.latitude,
                start.longitude,
                end.latitude,
                end.longitude
            )
        }
    }
    return lengthMeters
}

/** Posição projetada e distância acumulada desde o início da rota. */
data class RunRoutePosition(
    val distanceFromRouteMeters: Double,
    val progressMeters: Double,
    val routeDistanceMeters: Double
)

/** Indica afastamento acima da margem definida para as leituras atuais do GPS. */
fun isOffRunRoute(position: RunPointModel?, route: List<RoutePoint>): Boolean {
    val currentPosition = position ?: return false
    if (currentPosition.isFiltered) return false
    return isOffRunRoute(currentPosition.latitude, currentPosition.longitude, route)
}

/** Indica se as coordenadas atuais ultrapassam a margem da rota planejada. */
fun isOffRunRoute(latitude: Double, longitude: Double, route: List<RoutePoint>): Boolean {
    return isOffRunRoutePosition(runRoutePosition(latitude, longitude, route))
}

/** Indica se a posição projetada excede o limite de afastamento da rota. */
fun isOffRunRoutePosition(position: RunRoutePosition?): Boolean =
    position?.distanceFromRouteMeters?.let { it > RUN_ROUTE_DEVIATION_ALERT_METERS } == true

/** Reverte o percurso aceito para permitir que a pessoa retorne pelo caminho gravado. */
fun routeBackToRunStart(points: List<RoutePoint>): List<PlannedRoutePoint> {
    val acceptedPoints = points.filterNot(RoutePoint::isFiltered)
    if (!hasRecordedBacktrack(acceptedPoints)) return emptyList()
    return acceptedPoints.groupBy(RoutePoint::segmentIndex)
        .toSortedMap(compareByDescending { it })
        .values
        .filter { it.size >= MIN_BACKTRACK_POINTS }
        .flatMapIndexed { segmentIndex, segment ->
            segment.asReversed().map { point ->
                PlannedRoutePoint(point.latitude, point.longitude, segmentIndex)
            }
        }
}

/** Exige ao menos um trecho contínuo com duas posições para desenhar uma linha de retorno. */
fun hasRecordedBacktrack(points: List<RoutePoint>): Boolean {
    var previousSegmentIndex: Int? = null
    points.filterNot(RoutePoint::isFiltered).forEach { point ->
        if (point.segmentIndex == previousSegmentIndex) return true
        previousSegmentIndex = point.segmentIndex
    }
    return false
}

internal class RunRouteAlertState {
    private var isOffRoute = false
    var isMutedUntilReturn = false
        private set

    fun update(currentlyOffRoute: Boolean): Boolean {
        if (!currentlyOffRoute) isMutedUntilReturn = false
        val enteredOffRoute = currentlyOffRoute && !isOffRoute
        isOffRoute = currentlyOffRoute
        return enteredOffRoute && !isMutedUntilReturn
    }

    fun muteUntilRouteReturn() {
        if (isOffRoute) isMutedUntilReturn = true
    }

    fun resetForResume() {
        isOffRoute = false
    }

    fun reset() {
        isOffRoute = false
        isMutedUntilReturn = false
    }
}

private fun projectOntoRouteSegment(latitude: Double, longitude: Double, start: RoutePoint, end: RoutePoint): SegmentProjection? {
    if (!start.hasValidCoordinates() || !end.hasValidCoordinates()) return null
    val latitudeRadians = Math.toRadians(latitude)
    val startX = longitudeMeters(longitude, start.longitude, latitudeRadians)
    val startY = latitudeMeters(latitude, start.latitude)
    val endX = longitudeMeters(longitude, end.longitude, latitudeRadians)
    val endY = latitudeMeters(latitude, end.latitude)
    val deltaX = endX - startX
    val deltaY = endY - startY
    val lengthSquared = deltaX * deltaX + deltaY * deltaY
    val projection = if (lengthSquared == 0.0) 0.0 else {
        (-(startX * deltaX + startY * deltaY) / lengthSquared).coerceIn(0.0, 1.0)
    }
    return SegmentProjection(
        distanceMeters = hypot(startX + projection * deltaX, startY + projection * deltaY),
        ratio = projection
    )
}

private fun longitudeMeters(origin: Double, target: Double, latitudeRadians: Double): Double =
    Math.toRadians(target - origin) * EARTH_RADIUS_METERS * cos(latitudeRadians)

private fun latitudeMeters(origin: Double, target: Double): Double =
    Math.toRadians(target - origin) * EARTH_RADIUS_METERS

private fun RoutePoint.hasValidCoordinates(): Boolean =
    hasValidCoordinates(latitude, longitude)

private fun hasValidCoordinates(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0

private data class SegmentProjection(val distanceMeters: Double, val ratio: Double)

private data class RoutePositionCandidates(
    val positions: List<RunRoutePosition>,
    val routeDistanceMeters: Double
)

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val MIN_BACKTRACK_POINTS = 2
// shortcut: usa margem fixa de 50 m, revisar com histerese se leituras próximas ao limite oscilarem.
private const val RUN_ROUTE_DEVIATION_ALERT_METERS = 50.0
private const val ROUTE_PROGRESS_BACKTRACK_METERS = 30.0
private const val ROUTE_MATCH_TIE_METERS = 12.0
