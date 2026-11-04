package com.example.gymtimer2.domain.model

import kotlin.math.abs
import kotlin.math.max

private const val MAX_RUN_ELEVATION_PROFILE_POINTS = 128
private const val ELEVATION_GAIN_THRESHOLD_METERS = 8.0
private const val ELEVATION_RESET_JUMP_METERS = 10.0

/** Leitura de altitude com precisão vertical suficiente para orientar uma etapa. */
class RunElevationReading internal constructor(
    val altitudeMeters: Double,
    val accuracyMeters: Float
)

/** Constrói uma leitura utilizável pelos cálculos de elevação da corrida. */
fun runElevationReadingOrNull(altitudeMeters: Double?, verticalAccuracyMeters: Float?): RunElevationReading? {
    val altitude = altitudeMeters?.takeIf(Double::isFinite) ?: return null
    val accuracy = verticalAccuracyMeters?.takeIf { it.isFinite() && it in 0f..10f } ?: return null
    return RunElevationReading(altitude, accuracy)
}

/** Estima o ganho de elevação a partir das altitudes finitas registradas. */
fun estimatedElevationGainMeters(points: List<RunPointModel>): Double? {
    val elevation = ElevationGainAccumulator()
    points.forEach(elevation::add)
    return elevation.gain.takeIf { elevation.hasReadings }
}

/** Ponto do perfil, com distância acumulada e índice do trecho da rota. */
data class RunElevationProfilePoint(
    override val distanceMeters: Double,
    val elevationMeters: Double,
    override val segmentIndex: Int
) : RunMetricProfilePoint {
    override val value: Double get() = elevationMeters
}

/** Cria uma amostra limitada do perfil local e conserva as interrupções entre trechos. */
fun calculateRunElevationProfile(route: List<RoutePoint>): List<RunElevationProfilePoint> {
    val profile = mutableListOf<RunElevationProfilePoint>()
    var distanceMeters = 0.0
    var previousPoint: RoutePoint? = null

    route.forEach { point ->
        if (point.isFiltered) return@forEach
        val previous = previousPoint
        val previousRoutePoint = previous?.takeIf { hasContinuousElevationCoordinates(it, point) }
        if (previousRoutePoint != null) {
            distanceMeters += distanceMetersBetweenCoordinates(
                previousRoutePoint.latitude,
                previousRoutePoint.longitude,
                point.latitude,
                point.longitude
            )
        }
        previousPoint = point

        val elevation = point.altitudeMeters?.takeIf(Double::isFinite) ?: return@forEach
        if (!point.hasUsableProfileAccuracy()) return@forEach
        profile += RunElevationProfilePoint(distanceMeters, elevation, point.segmentIndex)
    }

    return downsampleElevationProfile(profile)
}

private fun downsampleElevationProfile(
    profile: List<RunElevationProfilePoint>
): List<RunElevationProfilePoint> {
    if (profile.size <= MAX_RUN_ELEVATION_PROFILE_POINTS) return profile

    val firstDistance = profile.first().distanceMeters
    val distanceRange = profile.last().distanceMeters - firstDistance
    if (distanceRange <= 0.0) return profile.take(MAX_RUN_ELEVATION_PROFILE_POINTS)

    val targetSpacingMeters = distanceRange / (MAX_RUN_ELEVATION_PROFILE_POINTS - 1)
    val sampled = mutableListOf<RunElevationProfilePoint>()
    var nextTargetDistance = firstDistance
    var previousSegmentIndex: Int? = null
    profile.forEach { point ->
        val segmentChanged = point.segmentIndex != previousSegmentIndex
        val isProfileSample = sampled.isEmpty() || point.distanceMeters >= nextTargetDistance || segmentChanged
        if (isProfileSample && sampled.size < MAX_RUN_ELEVATION_PROFILE_POINTS - 1) {
            sampled += point
            nextTargetDistance = firstDistance + sampled.size * targetSpacingMeters
        }
        previousSegmentIndex = point.segmentIndex
    }
    if (sampled.lastOrNull() != profile.last()) sampled += profile.last()
    return sampled
}

private fun RoutePoint.hasUsableProfileAccuracy(): Boolean =
    verticalAccuracyMeters?.let { it.isFinite() && it in 0f..10f } ?: true

private fun hasContinuousElevationCoordinates(previous: RoutePoint, current: RoutePoint): Boolean =
    previous.segmentIndex == current.segmentIndex &&
        previous.hasValidElevationCoordinates() && current.hasValidElevationCoordinates()

private fun RoutePoint.hasValidElevationCoordinates(): Boolean =
    latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0

private class ElevationGainAccumulator {
    var gain = 0.0
        private set
    var hasReadings = false
        private set
    private var climbStartMeters = 0.0
    private var climbPeakMeters = 0.0
    private var lastAltitudeMeters = 0.0

    fun add(point: RunPointModel) {
        val altitudeMeters = point.altitudeMeters?.takeIf(Double::isFinite) ?: return
        if (!hasReadings || abs(altitudeMeters - lastAltitudeMeters) > ELEVATION_RESET_JUMP_METERS) {
            resetWindow(altitudeMeters)
            hasReadings = true
            return
        }

        if (altitudeMeters > climbPeakMeters && altitudeMeters - climbStartMeters >= ELEVATION_GAIN_THRESHOLD_METERS) {
            val previousClimbWasCounted = climbPeakMeters - climbStartMeters >= ELEVATION_GAIN_THRESHOLD_METERS
            gain += if (previousClimbWasCounted) {
                altitudeMeters - max(lastAltitudeMeters, climbPeakMeters)
            } else {
                altitudeMeters - climbStartMeters
            }
            climbPeakMeters = altitudeMeters
        } else if (climbPeakMeters - altitudeMeters >= ELEVATION_GAIN_THRESHOLD_METERS || altitudeMeters < climbStartMeters) {
            climbStartMeters = altitudeMeters
            climbPeakMeters = altitudeMeters
        }

        lastAltitudeMeters = altitudeMeters
    }

    private fun resetWindow(altitudeMeters: Double) {
        climbStartMeters = altitudeMeters
        climbPeakMeters = altitudeMeters
        lastAltitudeMeters = altitudeMeters
    }
}
