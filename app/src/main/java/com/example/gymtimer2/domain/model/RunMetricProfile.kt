package com.example.gymtimer2.domain.model

private const val MAX_RUN_METRIC_PROFILE_POINTS = 256

/** Um valor de métrica associado à distância acumulada e ao trecho da corrida. */
interface RunMetricProfilePoint {
    val distanceMeters: Double
    val value: Double
    val segmentIndex: Int
}

/** Um intervalo GPS aceito entre dois pontos consecutivos da mesma corrida e trecho. */
internal data class RunGpsInterval(val distanceMeters: Double, val durationMs: Long)

internal fun acceptedRunGpsInterval(previous: RunPointModel, current: RunPointModel): RunGpsInterval? {
    if (previous.runId != current.runId || previous.segmentIndex != current.segmentIndex) return null

    val previousGps = previous.toGpsPoint()
    val currentGps = current.toGpsPoint()
    val durationMs = RunMetrics.elapsedIntervalMs(previousGps, currentGps).takeIf { it > 0L } ?: return null
    val distanceMeters = RunMetrics.acceptedDistanceMeters(previousGps, currentGps)
        ?.takeIf { it.isFinite() && it > 0.0 }
        ?: return null
    return RunGpsInterval(distanceMeters, durationMs)
}

internal fun <T : RunMetricProfilePoint> downsampleRunMetricProfile(profile: List<T>): List<T> {
    if (profile.size <= MAX_RUN_METRIC_PROFILE_POINTS) return profile

    val firstDistance = profile.first().distanceMeters
    val distanceRange = profile.last().distanceMeters - firstDistance
    if (distanceRange <= 0.0) return profile.take(MAX_RUN_METRIC_PROFILE_POINTS)

    val targetSpacingMeters = distanceRange / (MAX_RUN_METRIC_PROFILE_POINTS - 1)
    val sampled = mutableListOf<T>()
    var nextTargetDistance = firstDistance
    var previousSegmentIndex: Int? = null
    profile.forEach { point ->
        val segmentChanged = point.segmentIndex != previousSegmentIndex
        val hasRoom = sampled.size < MAX_RUN_METRIC_PROFILE_POINTS - 1
        val reachedDistance = point.distanceMeters >= nextTargetDistance
        if (hasRoom && (reachedDistance || segmentChanged)) {
            sampled += point
            nextTargetDistance = firstDistance + sampled.size * targetSpacingMeters
        }
        previousSegmentIndex = point.segmentIndex
    }
    if (sampled.lastOrNull() != profile.last()) sampled += profile.last()
    return sampled
}
