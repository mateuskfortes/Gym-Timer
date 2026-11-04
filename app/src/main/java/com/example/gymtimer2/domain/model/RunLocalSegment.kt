package com.example.gymtimer2.domain.model

internal const val MIN_LOCAL_SEGMENT_DISTANCE_METERS = 100.0
private const val MAX_DISTANCE_FROM_LOCAL_SEGMENT_METERS = 50.0
private const val MAX_LOCAL_SEGMENT_ACCURACY_METERS = 50f
private const val SEGMENT_ENDPOINT_RADIUS_METERS = 50.0
private const val MIN_PROGRESS_CHANGE_METERS = 1.0

/** Registra uma tentativa local no percurso que a pessoa escolheu antes da corrida. */
class RunLocalSegmentTracker(
    route: List<RoutePoint>,
    initialProgressMeters: Double = 0.0,
    initialStartedAtDurationMs: Long? = null,
    initialCompletedAtDurationMs: Long? = null
) {
    private val route = route.filterNot(RoutePoint::isFiltered)
    private val startPoint = route.firstOrNull(RoutePoint::hasValidSegmentCoordinates)
    private val endPoint = route.lastOrNull(RoutePoint::hasValidSegmentCoordinates)
    private val routeDistanceMeters = runRouteLengthMeters(route)
    private var progressMeters = initialProgressMeters.coerceIn(0.0, routeDistanceMeters)
    private var startedAtDurationMs = initialStartedAtDurationMs
    private var completedAtDurationMs = initialCompletedAtDurationMs

    /** Atualiza o progresso e devolve somente mudanças que precisam ser salvas. */
    fun update(point: GpsPoint, durationMs: Long): RunLocalSegmentUpdate? {
        if (completedAtDurationMs != null || !point.hasUsableSegmentAccuracy()) return null
        val routePosition = routePositionAt(point) ?: return null
        val start = startPoint ?: return null
        val end = endPoint ?: return null
        val startedAt = startedAtDurationMs ?: return startSegment(point, durationMs, start)
        return updateProgress(point, durationMs, startedAt, routePosition, end)
    }

    private fun routePositionAt(point: GpsPoint): RunRoutePosition? {
        if (routeDistanceMeters < MIN_LOCAL_SEGMENT_DISTANCE_METERS) return null
        return runRoutePosition(
            point.latitude,
            point.longitude,
            route,
            minimumProgressMeters = progressMeters.takeIf { startedAtDurationMs != null }
        )?.takeIf { it.distanceFromRouteMeters <= MAX_DISTANCE_FROM_LOCAL_SEGMENT_METERS }
    }

    private fun startSegment(point: GpsPoint, durationMs: Long, start: RoutePoint): RunLocalSegmentUpdate? {
        if (distanceMetersBetweenCoordinates(point.latitude, point.longitude, start.latitude, start.longitude) >
            SEGMENT_ENDPOINT_RADIUS_METERS
        ) return null
        startedAtDurationMs = durationMs
        progressMeters = 0.0
        return updateSnapshot()
    }

    private fun updateProgress(
        point: GpsPoint,
        durationMs: Long,
        startedAt: Long,
        routePosition: RunRoutePosition,
        end: RoutePoint
    ): RunLocalSegmentUpdate? {
        val updatedProgress = maxOf(progressMeters, routePosition.progressMeters)
        val progressChanged = updatedProgress - progressMeters >= MIN_PROGRESS_CHANGE_METERS
        if (progressChanged) progressMeters = updatedProgress.coerceAtMost(routeDistanceMeters)

        val reachedEnd = progressMeters >= routeDistanceMeters - SEGMENT_ENDPOINT_RADIUS_METERS &&
            distanceMetersBetweenCoordinates(point.latitude, point.longitude, end.latitude, end.longitude) <=
            SEGMENT_ENDPOINT_RADIUS_METERS && durationMs > startedAt
        if (reachedEnd) completedAtDurationMs = durationMs

        return if (progressChanged || reachedEnd) updateSnapshot() else null
    }

    private fun updateSnapshot() = RunLocalSegmentUpdate(
        progressMeters = progressMeters,
        startedAtDurationMs = startedAtDurationMs,
        completedAtDurationMs = completedAtDurationMs
    )

}

/** Dados do aviso de aproximação calculados somente quando o GPS está perto do início. */
internal data class RunLocalSegmentApproach(
    val distanceToStartMeters: Double,
    val routeDistanceMeters: Double
)

internal data class LocalSegmentStartApproach(
    val start: LocalSegmentStart,
    val distanceToStartMeters: Double
)

/** Mede a aproximação sem percorrer rotas longas quando a pessoa está longe delas. */
internal fun findLocalSegmentApproach(
    point: RunPointModel?,
    route: List<RoutePoint>
): RunLocalSegmentApproach? {
    if (point == null || !point.hasUsableSegmentAccuracy()) return null

    val start = route.firstOrNull { !it.isFiltered && it.hasValidSegmentCoordinates() } ?: return null
    val distanceMeters = distanceMetersBetweenCoordinates(
        point.latitude,
        point.longitude,
        start.latitude,
        start.longitude
    )
    if (distanceMeters <= SEGMENT_ENDPOINT_RADIUS_METERS ||
        distanceMeters > MAX_LOCAL_SEGMENT_APPROACH_METERS
    ) return null

    // shortcut: use a local 250 m approach radius until the APK's remote segment feed is available.
    val routeDistanceMeters = runRouteLengthMeters(route)
    if (routeDistanceMeters < MIN_LOCAL_SEGMENT_DISTANCE_METERS) return null
    return RunLocalSegmentApproach(distanceMeters, routeDistanceMeters)
}

internal fun findLocalSegmentStartApproach(
    point: RunPointModel?,
    starts: List<LocalSegmentStart>
): LocalSegmentStartApproach? = findLocalSegmentStartApproaches(point, starts).firstOrNull()

internal fun findLocalSegmentStartApproaches(
    point: RunPointModel?,
    starts: List<LocalSegmentStart>
): List<LocalSegmentStartApproach> {
    if (point == null || !point.hasUsableSegmentAccuracy()) return emptyList()
    return starts.asSequence()
        .filter { it.distanceMeters >= MIN_LOCAL_SEGMENT_DISTANCE_METERS }
        .mapNotNull { start ->
            val distance = distanceMetersBetweenCoordinates(
                point.latitude,
                point.longitude,
                start.latitude,
                start.longitude
            )
            start.takeIf {
                distance > SEGMENT_ENDPOINT_RADIUS_METERS && distance <= MAX_LOCAL_SEGMENT_APPROACH_METERS
            }?.let { LocalSegmentStartApproach(it, distance) }
        }
        .sortedBy(LocalSegmentStartApproach::distanceToStartMeters)
        .toList()
}

internal fun findLocalSegmentStart(
    point: GpsPoint,
    starts: List<LocalSegmentStart>
): LocalSegmentStart? = findLocalSegmentStarts(point, starts).firstOrNull()

internal fun findLocalSegmentStarts(
    point: GpsPoint,
    starts: List<LocalSegmentStart>
): List<LocalSegmentStart> {
    if (!point.hasUsableSegmentAccuracy()) return emptyList()
    return starts.asSequence()
        .filter { it.distanceMeters >= MIN_LOCAL_SEGMENT_DISTANCE_METERS }
        .mapNotNull { start ->
            val distance = distanceMetersBetweenCoordinates(
                point.latitude,
                point.longitude,
                start.latitude,
                start.longitude
            )
            start.takeIf { distance <= SEGMENT_ENDPOINT_RADIUS_METERS }?.let { it to distance }
        }
        .sortedBy { (_, distance) -> distance }
        .map { (start, _) -> start }
        .toList()
}

private fun GpsPoint.hasUsableSegmentAccuracy(): Boolean =
    hasUsableSegmentAccuracy(latitude, longitude, accuracyMeters, isFiltered)

private fun RunPointModel.hasUsableSegmentAccuracy(): Boolean =
    hasUsableSegmentAccuracy(latitude, longitude, accuracyMeters, isFiltered)

private fun hasUsableSegmentAccuracy(
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float,
    isFiltered: Boolean
): Boolean = !isFiltered && latitude.isFinite() && latitude in -90.0..90.0 &&
    longitude.isFinite() && longitude in -180.0..180.0 &&
    accuracyMeters.isFinite() && accuracyMeters in 0.1f..MAX_LOCAL_SEGMENT_ACCURACY_METERS

/** Progresso salvo da tentativa atual. */
data class RunLocalSegmentUpdate(
    val progressMeters: Double,
    val startedAtDurationMs: Long?,
    val completedAtDurationMs: Long?
)

private fun RoutePoint.hasValidSegmentCoordinates(): Boolean =
    latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0

private const val MAX_LOCAL_SEGMENT_APPROACH_METERS = 250.0
