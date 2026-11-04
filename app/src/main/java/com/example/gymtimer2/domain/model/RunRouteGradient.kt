package com.example.gymtimer2.domain.model

import kotlin.math.abs
import kotlin.math.max

internal enum class RunGradientDirection {
    DESCENT,
    LEVEL,
    ASCENT
}

internal data class RunGradientSegment(
    val start: RunPointModel,
    val end: RunPointModel,
    val direction: RunGradientDirection
)

internal fun runGradientSegments(points: List<RunPointModel>): List<RunGradientSegment> =
    points.asSequence()
        .filterNot(RunPointModel::isFiltered)
        .groupBy(RunPointModel::segmentIndex)
        .values
        .flatMap { segment ->
            segment.zipWithNext().mapNotNull { (start, end) -> createGradientSegment(start, end) }
        }

private fun createGradientSegment(
    start: RunPointModel,
    end: RunPointModel
): RunGradientSegment? {
    if (!start.hasValidGradientCoordinates() || !end.hasValidGradientCoordinates()) return null
    val startElevation = runElevationReadingOrNull(start.altitudeMeters, start.verticalAccuracyMeters) ?: return null
    val endElevation = runElevationReadingOrNull(end.altitudeMeters, end.verticalAccuracyMeters) ?: return null
    val distanceMeters = distanceMetersBetweenCoordinates(
        start.latitude,
        start.longitude,
        end.latitude,
        end.longitude
    )
    if (!distanceMeters.isFinite() || distanceMeters < MIN_GRADIENT_SEGMENT_DISTANCE_METERS) return null

    val elevationChangeMeters = endElevation.altitudeMeters - startElevation.altitudeMeters
    val measurementNoiseMeters = max(
        MIN_GRADIENT_ELEVATION_CHANGE_METERS,
        max(startElevation.accuracyMeters, endElevation.accuracyMeters).toDouble() * 2
    )
    val gradePercent = if (abs(elevationChangeMeters) < measurementNoiseMeters) {
        0.0
    } else {
        elevationChangeMeters / distanceMeters * 100
    }
    val direction = when {
        gradePercent <= -GRADE_DIRECTION_THRESHOLD_PERCENT -> RunGradientDirection.DESCENT
        gradePercent >= GRADE_DIRECTION_THRESHOLD_PERCENT -> RunGradientDirection.ASCENT
        else -> RunGradientDirection.LEVEL
    }
    return RunGradientSegment(start, end, direction)
}

private fun RunPointModel.hasValidGradientCoordinates(): Boolean =
    latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0

private const val MIN_GRADIENT_SEGMENT_DISTANCE_METERS = 5.0
private const val MIN_GRADIENT_ELEVATION_CHANGE_METERS = 3.0
private const val GRADE_DIRECTION_THRESHOLD_PERCENT = 3.0
