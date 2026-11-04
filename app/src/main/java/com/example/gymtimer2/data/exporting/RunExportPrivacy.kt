package com.example.gymtimer2.data.exporting

import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunPointModel
import com.example.gymtimer2.domain.model.RunPrivacyZone
import com.example.gymtimer2.domain.model.distanceMetersBetweenCoordinates
import kotlin.math.cos
import kotlin.math.hypot

internal object RunExportPrivacy {
    fun visiblePoints(
        run: RunModel,
        points: List<RunPointModel>,
        privacyZones: List<RunPrivacyZone> = emptyList()
    ): List<RunPointModel> {
        val hiddenDistanceMeters = run.startEndHideDistance.meters.toDouble()
        if (hiddenDistanceMeters == 0.0 && privacyZones.isEmpty()) return points

        val routePoints = points.asSequence()
            .filter(RunPointModel::isExportable)
            .sortedBy(RunPointModel::recordedAtEpochMs)
            .toList()
        val startEndVisiblePoints = if (hiddenDistanceMeters == 0.0) {
            routePoints
        } else {
            hideStartAndEnd(routePoints, hiddenDistanceMeters)
        }
        return if (privacyZones.isEmpty()) startEndVisiblePoints else hidePrivacyZones(
            startEndVisiblePoints,
            privacyZones
        )
    }

    private fun hideStartAndEnd(
        points: List<RunPointModel>,
        hiddenDistanceMeters: Double
    ): List<RunPointModel> {
        if (points.size < 2) return emptyList()

        val distanceFromStart = DoubleArray(points.size)
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            distanceFromStart[index] = distanceFromStart[index - 1] +
                if (previous.segmentIndex == current.segmentIndex) {
                    distanceMetersBetweenCoordinates(
                        previous.latitude,
                        previous.longitude,
                        current.latitude,
                        current.longitude
                    )
                } else {
                    0.0
                }
        }

        val totalDistanceMeters = distanceFromStart.last()
        return points.filterIndexed { index, _ ->
            distanceFromStart[index] >= hiddenDistanceMeters &&
                totalDistanceMeters - distanceFromStart[index] >= hiddenDistanceMeters
        }
    }

    private fun hidePrivacyZones(
        points: List<RunPointModel>,
        privacyZones: List<RunPrivacyZone>
    ): List<RunPointModel> {
        val visiblePoints = mutableListOf<RunPointModel>()
        var previousPoint: RunPointModel? = null
        var previousRoutePoint: RunPointModel? = null
        var hasHiddenPoints = false
        var segmentIndex = -1

        points.forEach { point ->
            val hiddenByZone = privacyZones.any { zone -> point.isInside(zone) }
            val crossesZone = previousRoutePoint?.takeIf { it.segmentIndex == point.segmentIndex }
                ?.let { previous -> privacyZones.any { zone -> previous.segmentIntersects(zone, point) } }
                ?: false
            previousRoutePoint = point

            if (hiddenByZone) {
                hasHiddenPoints = true
                return@forEach
            }

            val startsNewSegment = previousPoint == null || hasHiddenPoints || crossesZone ||
                previousPoint?.segmentIndex != point.segmentIndex
            if (startsNewSegment) segmentIndex++
            visiblePoints += point.copy(segmentIndex = segmentIndex)
            previousPoint = point
            hasHiddenPoints = false
        }

        return visiblePoints
    }

    private fun RunPointModel.isInside(zone: RunPrivacyZone): Boolean =
        distanceMetersBetweenCoordinates(latitude, longitude, zone.latitude, zone.longitude) <= zone.radiusMeters

    private fun RunPointModel.segmentIntersects(zone: RunPrivacyZone, end: RunPointModel): Boolean {
        // shortcut: local projection suits zones up to 1.6 km; use geodesic geometry if that limit grows.
        val startX = longitudeOffsetMeters(longitude, zone.longitude, zone.latitude)
        val startY = latitudeOffsetMeters(latitude, zone.latitude)
        val endX = longitudeOffsetMeters(end.longitude, zone.longitude, zone.latitude)
        val endY = latitudeOffsetMeters(end.latitude, zone.latitude)
        val deltaX = endX - startX
        val deltaY = endY - startY
        val lengthSquared = deltaX * deltaX + deltaY * deltaY
        val fraction = if (lengthSquared == 0.0) 0.0 else {
            (-(startX * deltaX + startY * deltaY) / lengthSquared).coerceIn(0.0, 1.0)
        }
        return hypot(startX + fraction * deltaX, startY + fraction * deltaY) <= zone.radiusMeters
    }

    private fun longitudeOffsetMeters(longitude: Double, centerLongitude: Double, centerLatitude: Double): Double {
        val deltaDegrees = (longitude - centerLongitude + 540.0) % 360.0 - 180.0
        return Math.toRadians(deltaDegrees) * EARTH_RADIUS_METERS * cos(Math.toRadians(centerLatitude))
    }

    private fun latitudeOffsetMeters(latitude: Double, centerLatitude: Double): Double =
        Math.toRadians(latitude - centerLatitude) * EARTH_RADIUS_METERS

    private const val EARTH_RADIUS_METERS = 6_371_000.0
}
