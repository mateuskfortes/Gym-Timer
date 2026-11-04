package com.example.gymtimer2.ui.screens.runs.tracking

import android.location.Location
import android.os.Build
import com.example.gymtimer2.domain.model.GpsPoint
import com.example.gymtimer2.domain.model.RunMetrics

internal fun Location.toGpsPoint(
    nowElapsedMs: Long
): GpsPoint? {
    if (!hasValidCoordinates()) return null
    return GpsPoint(
        latitude = latitude,
        longitude = longitude,
        recordedAtEpochMs = time,
        elapsedRealtimeMs = elapsedRealtimeNanos
            .takeIf { it > 0L }
            ?.div(NANOS_PER_MILLISECOND)
            ?: nowElapsedMs,
        accuracyMeters = accuracy.takeIf { hasAccuracy() && it.isFinite() } ?: 0f,
        altitudeMeters = altitude.takeIf { hasAltitude() && it.isFinite() },
        verticalAccuracyMeters = verticalAccuracyMetersOrNull(),
        speedMetersPerSecond = speedMetersPerSecondOrNull(),
        speedAccuracyMetersPerSecond = speedAccuracyMetersPerSecondOrNull()
    )
}

private const val NANOS_PER_MILLISECOND = 1_000_000L

private fun Location.hasValidCoordinates(): Boolean =
    latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0

internal fun Location.speedMetersPerSecondOrNull(): Float? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !hasSpeed()) return null
    return speed.takeIf {
        it.isFinite() && it in RunMetrics.MIN_SPEED_METERS_PER_SECOND..RunMetrics.MAX_SPEED_METERS_PER_SECOND
    }
}

internal fun Location.speedAccuracyMetersPerSecondOrNull(): Float? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasSpeedAccuracy()) {
        speedAccuracyMetersPerSecond.takeIf { it.isFinite() && it >= 0f }
    } else null

private fun Location.verticalAccuracyMetersOrNull(): Float? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasVerticalAccuracy()) {
        verticalAccuracyMeters.takeIf { it.isFinite() && it >= 0f }
    } else null
