package com.example.gymtimer2.domain.model

/** Trecho de uma corrida salva e os dados derivados ajustados ao novo intervalo. */
data class RunActivityCrop(
    val points: List<RunPointModel>,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val durationMs: Long,
    val elapsedDurationMs: Long,
    val distanceMeters: Double,
    val laps: List<RunLap>,
    val workoutSteps: List<RunWorkoutStep>,
    val localSegmentEfforts: List<RunLocalSegmentEffort>
)

/** Resultado de tentar aplicar um corte à corrida salva. */
enum class RunCropOutcome {
    SAVED,
    INVALID_SELECTION,
    RUN_NOT_COMPLETED,
    ACTIVE_ROUTE_IN_USE
}

/** Calcula métricas novas e reposiciona os registros que pertencem ao trecho escolhido. */
fun calculateRunActivityCrop(
    run: RunModel,
    points: List<RunPointModel>,
    startPointId: Long,
    endPointId: Long,
    laps: List<RunLap> = emptyList(),
    workoutSteps: List<RunWorkoutStep> = emptyList(),
    localSegmentEfforts: List<RunLocalSegmentEffort> = emptyList()
): RunActivityCrop? {
    val selection = selectCropPoints(run, points, startPointId, endPointId) ?: return null
    val window = createCropWindow(run, points, selection) ?: return null

    return RunActivityCrop(
        points = window.points,
        startedAtEpochMs = window.startedAtEpochMs,
        finishedAtEpochMs = window.finishedAtEpochMs,
        durationMs = window.durationMs,
        elapsedDurationMs = window.elapsedDurationMs,
        distanceMeters = window.distanceMeters,
        laps = cropLaps(
            laps,
            window.startDurationMs,
            window.endDurationMs,
            window.startDistanceMeters,
            window.endDistanceMeters,
            window.startElapsedDurationMs,
            window.endElapsedDurationMs
        ),
        workoutSteps = cropWorkoutSteps(
            workoutSteps,
            window.startDurationMs,
            window.endDurationMs,
            window.startDistanceMeters,
            window.endDistanceMeters
        ),
        localSegmentEfforts = cropSegmentEfforts(
            localSegmentEfforts,
            window.startDurationMs,
            window.endDurationMs
        )
    )
}

private data class CropSelection(val startIndex: Int, val endIndex: Int)

private data class CropWindow(
    val points: List<RunPointModel>,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val startDurationMs: Long,
    val endDurationMs: Long,
    val startElapsedDurationMs: Long,
    val endElapsedDurationMs: Long,
    val durationMs: Long,
    val elapsedDurationMs: Long,
    val startDistanceMeters: Double,
    val endDistanceMeters: Double,
    val distanceMeters: Double
)

private data class CropBoundary(
    val durationMs: Long,
    val distanceMeters: Double,
    val recordedAtEpochMs: Long
)

private fun selectCropPoints(
    run: RunModel,
    points: List<RunPointModel>,
    startPointId: Long,
    endPointId: Long
): CropSelection? {
    if (run.status != RunStatus.COMPLETED || points.size < MIN_CROP_POINT_COUNT) return null
    val startIndex = points.indexOfFirst { it.id == startPointId }
    val endIndex = points.indexOfFirst { it.id == endPointId }
    if (startIndex < 0 || endIndex <= startIndex) return null
    return CropSelection(startIndex, endIndex)
}

private fun createCropWindow(
    run: RunModel,
    points: List<RunPointModel>,
    selection: CropSelection
): CropWindow? {
    val (startIndex, endIndex) = selection
    val timeline = RunActivityTimeline(run, points)
    val startsAtBeginning = startIndex == 0
    val endsAtFinish = endIndex == points.lastIndex
    val selectedPoints = points.subList(startIndex, endIndex + 1)
    val start = cropStartBoundary(run, points, timeline, startIndex)
    val end = cropEndBoundary(run, points, timeline, endIndex)
    val durationMs = (end.durationMs - start.durationMs).coerceAtLeast(0L)
    val distanceMeters = (end.distanceMeters - start.distanceMeters).coerceAtLeast(0.0)
    if (durationMs <= 0L || distanceMeters <= 0.0) return null
    if (end.recordedAtEpochMs < start.recordedAtEpochMs) return null

    val elapsedDurationMs = cropElapsedDuration(
        run,
        selection,
        points.lastIndex,
        start.recordedAtEpochMs,
        end.recordedAtEpochMs,
        selectedPoints,
        durationMs
    )
    val startElapsedDurationMs = elapsedDurationAt(run, points, startIndex)
    val endElapsedDurationMs = if (endsAtFinish) {
        run.elapsedDurationMs ?: elapsedDurationAt(run, points, endIndex, atRunFinish = true)
    } else {
        elapsedDurationAt(run, points, endIndex)
    }.coerceAtLeast(startElapsedDurationMs)
    return CropWindow(
        points = selectedPoints,
        startedAtEpochMs = start.recordedAtEpochMs,
        finishedAtEpochMs = end.recordedAtEpochMs,
        startDurationMs = start.durationMs,
        endDurationMs = end.durationMs,
        startElapsedDurationMs = startElapsedDurationMs,
        endElapsedDurationMs = endElapsedDurationMs,
        durationMs = durationMs,
        elapsedDurationMs = elapsedDurationMs,
        startDistanceMeters = start.distanceMeters,
        endDistanceMeters = end.distanceMeters,
        distanceMeters = distanceMeters
    )
}

private fun cropStartBoundary(
    run: RunModel,
    points: List<RunPointModel>,
    timeline: RunActivityTimeline,
    index: Int
): CropBoundary {
    if (index == 0) return CropBoundary(0L, 0.0, run.startedAtEpochMs)
    val point = points[index]
    return CropBoundary(timeline.activeDurationAt(index), timeline.distanceAt(index), point.recordedAtEpochMs)
}

private fun cropEndBoundary(
    run: RunModel,
    points: List<RunPointModel>,
    timeline: RunActivityTimeline,
    index: Int
): CropBoundary {
    if (index == points.lastIndex) {
        return CropBoundary(
            run.durationMs,
            run.distanceMeters,
            run.finishedAtEpochMs ?: points[index].recordedAtEpochMs
        )
    }
    val point = points[index]
    return CropBoundary(timeline.activeDurationAt(index), timeline.distanceAt(index), point.recordedAtEpochMs)
}

private fun cropElapsedDuration(
    run: RunModel,
    selection: CropSelection,
    lastPointIndex: Int,
    startedAtEpochMs: Long,
    finishedAtEpochMs: Long,
    points: List<RunPointModel>,
    durationMs: Long
): Long {
    val startsAtBeginning = selection.startIndex == 0
    val endsAtFinish = selection.endIndex == lastPointIndex
    val elapsedDurationMs = if (startsAtBeginning && endsAtFinish) {
        run.elapsedDurationMs ?: (finishedAtEpochMs - startedAtEpochMs)
    } else {
        elapsedDurationBetween(
            run,
            startsAtBeginning,
            endsAtFinish,
            startedAtEpochMs,
            finishedAtEpochMs,
            points.first(),
            points.last()
        )
    }
    return elapsedDurationMs.coerceAtLeast(durationMs)
}

private fun cropLaps(
    laps: List<RunLap>,
    startDurationMs: Long,
    endDurationMs: Long,
    startDistanceMeters: Double,
    endDistanceMeters: Double,
    startElapsedDurationMs: Long,
    endElapsedDurationMs: Long
): List<RunLap> = laps.mapNotNull { lap ->
    val isWithinDuration = lap.durationMs in startDurationMs..endDurationMs
    val isWithinDistance = lap.distanceMeters.isFinite() &&
        lap.distanceMeters >= startDistanceMeters && lap.distanceMeters <= endDistanceMeters
    val isWithinElapsedDuration = lap.elapsedDurationMs?.let { it in startElapsedDurationMs..endElapsedDurationMs } ?: true
    if (!isWithinDuration || !isWithinDistance || !isWithinElapsedDuration) return@mapNotNull null
    lap.copy(
        durationMs = lap.durationMs - startDurationMs,
        distanceMeters = lap.distanceMeters - startDistanceMeters,
        elapsedDurationMs = lap.elapsedDurationMs?.minus(startElapsedDurationMs)?.coerceAtLeast(0L)
    )
}

private fun elapsedDurationAt(
    run: RunModel,
    points: List<RunPointModel>,
    index: Int,
    atRunFinish: Boolean = false
): Long {
    if (index == 0) return 0L
    val point = points[index]
    return elapsedDurationBetween(
        run = run,
        startsAtRunBeginning = true,
        endsAtRunFinish = atRunFinish,
        startEpochMs = run.startedAtEpochMs,
        endEpochMs = if (atRunFinish) run.finishedAtEpochMs ?: point.recordedAtEpochMs else point.recordedAtEpochMs,
        firstPoint = points.first(),
        lastPoint = point
    )
}

private fun cropWorkoutSteps(
    steps: List<RunWorkoutStep>,
    startDurationMs: Long,
    endDurationMs: Long,
    startDistanceMeters: Double,
    endDistanceMeters: Double
): List<RunWorkoutStep> = steps.mapNotNull { step ->
    val startedAt = step.startedAtDurationMs ?: return@mapNotNull null
    val completedAt = step.completedAtDurationMs
    if (startedAt >= endDurationMs || completedAt?.let { it < startDurationMs } == true) return@mapNotNull null
    val completedInsideCrop = completedAt?.let { it <= endDurationMs } == true
    val minimumElevationInsideCrop = step.minimumElevationAtDurationMs.isWithin(startDurationMs, endDurationMs)
    val maximumElevationInsideCrop = step.maximumElevationAtDurationMs.isWithin(startDurationMs, endDurationMs)
    step.copy(
        startedAtDurationMs = (startedAt - startDurationMs).coerceAtLeast(0L),
        startedAtDistanceMeters = step.startedAtDistanceMeters?.minus(startDistanceMeters)?.coerceAtLeast(0.0),
        completedAtDurationMs = completedAt?.takeIf { completedInsideCrop }?.minus(startDurationMs),
        completedAtDistanceMeters = step.completedAtDistanceMeters
            ?.takeIf { completedInsideCrop && it <= endDistanceMeters }
            ?.minus(startDistanceMeters)
            ?.coerceAtLeast(0.0),
        minimumElevationMeters = step.minimumElevationMeters.takeIf { minimumElevationInsideCrop },
        minimumElevationAtDurationMs = step.minimumElevationAtDurationMs
            ?.takeIf { minimumElevationInsideCrop }
            ?.minus(startDurationMs),
        maximumElevationMeters = step.maximumElevationMeters.takeIf { maximumElevationInsideCrop },
        maximumElevationAtDurationMs = step.maximumElevationAtDurationMs
            ?.takeIf { maximumElevationInsideCrop }
            ?.minus(startDurationMs)
    )
}

private fun cropSegmentEfforts(
    efforts: List<RunLocalSegmentEffort>,
    startDurationMs: Long,
    endDurationMs: Long
): List<RunLocalSegmentEffort> = efforts.mapNotNull { effort ->
    val startedAt = effort.startedAtDurationMs ?: return@mapNotNull null
    val completedAt = effort.completedAtDurationMs
    if (startedAt < startDurationMs || startedAt >= endDurationMs || completedAt?.let { it > endDurationMs } == true) {
        return@mapNotNull null
    }
    effort.copy(
        startedAtDurationMs = startedAt - startDurationMs,
        completedAtDurationMs = completedAt?.minus(startDurationMs)
    )
}

private fun Long?.isWithin(start: Long, end: Long): Boolean = this?.let { it in start..end } == true

private class RunActivityTimeline(run: RunModel, points: List<RunPointModel>) {
    private val activeDurationsMs = LongArray(points.size)
    private val distancesMeters = DoubleArray(points.size)

    init {
        var activeDurationMs = points.first().activeDurationMs ?: if (points.first().segmentIndex == 0) {
            (points.first().recordedAtEpochMs - run.startedAtEpochMs).coerceAtLeast(0L)
        } else {
            0L
        }
        var distanceMeters = 0.0
        var previousValidPoint: RunPointModel? = null
        points.forEachIndexed { index, point ->
            if (index > 0) {
                val previousPoint = points[index - 1]
                activeDurationMs = point.activeDurationMs?.coerceAtLeast(activeDurationMs)
                    ?: if (previousPoint.segmentIndex == point.segmentIndex) {
                        activeDurationMs + durationBetween(previousPoint, point)
                    } else {
                        activeDurationMs
                    }
            }
            val gpsPoint = point.toGpsPoint()
            if (!point.isFiltered && gpsPoint.hasValidAccuracy()) {
                previousValidPoint?.takeIf { it.segmentIndex == point.segmentIndex }?.let { previous ->
                    distanceMeters += RunMetrics.acceptedDistanceMeters(previous.toGpsPoint(), gpsPoint) ?: 0.0
                }
                previousValidPoint = point
            }
            activeDurationsMs[index] = activeDurationMs
            distancesMeters[index] = distanceMeters
        }
    }

    fun activeDurationAt(index: Int): Long = activeDurationsMs[index]

    fun distanceAt(index: Int): Double = distancesMeters[index]
}

private fun durationBetween(first: RunPointModel, second: RunPointModel): Long {
    val elapsedRealtimeDelta = if (first.elapsedRealtimeMs != null && second.elapsedRealtimeMs != null) {
        second.elapsedRealtimeMs - first.elapsedRealtimeMs
    } else {
        second.recordedAtEpochMs - first.recordedAtEpochMs
    }
    return elapsedRealtimeDelta.coerceAtLeast(0L)
}

private fun elapsedDurationBetween(
    run: RunModel,
    startsAtRunBeginning: Boolean,
    endsAtRunFinish: Boolean,
    startEpochMs: Long,
    endEpochMs: Long,
    firstPoint: RunPointModel,
    lastPoint: RunPointModel
): Long {
    val startRealtimeMs = firstPoint.elapsedRealtimeMs?.let { elapsedRealtimeMs ->
        if (startsAtRunBeginning) {
            elapsedRealtimeMs - (firstPoint.recordedAtEpochMs - run.startedAtEpochMs).coerceAtLeast(0L)
        } else {
            elapsedRealtimeMs
        }
    }
    val endRealtimeMs = lastPoint.elapsedRealtimeMs?.let { elapsedRealtimeMs ->
        if (endsAtRunFinish) {
            elapsedRealtimeMs + ((run.finishedAtEpochMs ?: endEpochMs) - lastPoint.recordedAtEpochMs).coerceAtLeast(0L)
        } else {
            elapsedRealtimeMs
        }
    }
    val elapsedRealtimeDelta = if (startRealtimeMs != null && endRealtimeMs != null) {
        endRealtimeMs - startRealtimeMs
    } else {
        endEpochMs - startEpochMs
    }
    return elapsedRealtimeDelta.coerceAtLeast(0L)
}

private const val MIN_CROP_POINT_COUNT = 2
