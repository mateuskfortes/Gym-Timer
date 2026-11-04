package com.example.gymtimer2.domain.model

/** Estimativa de tempo para um quilômetro completo do percurso. */
data class RunSplit(val number: Int, val durationMs: Long)

/** Estima o pace atual usando o tempo e a distância que sobraram após as parciais completas. */
fun currentRunSplitPaceSecondsPerKm(
    run: RunModel,
    completedSplits: List<RunSplit>,
    distanceUnit: RunDistanceUnit = RunDistanceUnit.METRIC
): Double? {
    val completedDistanceMeters = completedSplits.size * distanceUnit.metersPerUnit
    val currentDistanceMeters = run.distanceMeters - completedDistanceMeters
    if (!currentDistanceMeters.isFinite()) return null
    val completedDurationMs = completedSplits.sumOf { it.durationMs }
    val currentDurationMs = run.durationMs - completedDurationMs
    if (currentDurationMs > MIN_SPLIT_PACE_TIME_MS) {
        RunMetrics.averagePaceSecondsPerKm(currentDurationMs, currentDistanceMeters)?.let { return it }
    }
    return completedSplits.asReversed().firstNotNullOfOrNull { split ->
        if (split.durationMs > MIN_SPLIT_PACE_TIME_MS) {
            RunMetrics.averagePaceSecondsPerKm(split.durationMs, distanceUnit.metersPerUnit)
        } else {
            null
        }
    }
}

/** Calcula parciais com os pontos aceitos, sem contar intervalos entre trechos. */
fun calculateRunSplits(
    points: List<RunPointModel>,
    distanceUnit: RunDistanceUnit = RunDistanceUnit.METRIC
): List<RunSplit> {
    val splits = mutableListOf<RunSplit>()
    var remainingMeters = distanceUnit.metersPerUnit
    var splitDurationMs = 0.0
    val acceptedPoints = points.filterNot(RunPointModel::isFiltered)
    for (index in 1 until acceptedPoints.size) {
        val previous = acceptedPoints[index - 1]
        val current = acceptedPoints[index]
        if (previous.segmentIndex == current.segmentIndex) {
            val previousGpsPoint = previous.toGpsPoint()
            val currentGpsPoint = current.toGpsPoint()
            val distance = RunMetrics.acceptedDistanceMeters(previousGpsPoint, currentGpsPoint)
            if (distance != null && distance > 0.0) {
                var availableMeters = distance
                val millisecondsPerMeter = RunMetrics.elapsedIntervalMs(previousGpsPoint, currentGpsPoint) / distance
                while (availableMeters >= remainingMeters) {
                    splitDurationMs += remainingMeters * millisecondsPerMeter
                    splits.add(RunSplit(splits.size + 1, splitDurationMs.toLong()))
                    availableMeters -= remainingMeters
                    remainingMeters = distanceUnit.metersPerUnit
                    splitDurationMs = 0.0
                }
                remainingMeters -= availableMeters
                splitDurationMs += availableMeters * millisecondsPerMeter
            }
        }
    }
    return splits
}

internal fun RunPointModel.toGpsPoint() = GpsPoint(
    latitude = latitude,
    longitude = longitude,
    recordedAtEpochMs = recordedAtEpochMs,
    accuracyMeters = accuracyMeters,
    isFiltered = isFiltered,
    elapsedRealtimeMs = elapsedRealtimeMs
)

// Strava waits five seconds before using a new split's pace.
private const val MIN_SPLIT_PACE_TIME_MS = 5_000L
