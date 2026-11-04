package com.example.gymtimer2.ui.screens.runs.tracking

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import com.example.gymtimer2.domain.model.RunGpsReading

internal data class RunLocationUpdate(
    val runId: Long,
    val generation: Long,
    val location: Location,
    val activeDurationMs: Long
)

internal class RunLocationUpdatesController(
    private val locationManager: LocationManager,
    private val onGpsReading: (RunGpsReading?) -> Unit,
    private val onLocationUpdate: (RunLocationUpdate) -> Unit,
    private val onGpsSignalRestored: () -> Unit,
    private val onPermissionFailure: () -> Unit
) {
    private var listener: LocationListener? = null
    private var generation = 0L
    private var acceptingLocations = false

    fun isCurrentGeneration(value: Long): Boolean = value == generation

    fun isGpsProviderEnabled(): Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    fun invalidate() {
        generation++
    }

    fun start(runId: Long, activeDurationAt: (Long) -> Long) {
        val requestGeneration = ++generation
        acceptingLocations = true
        val requestStartedAtElapsedMs = SystemClock.elapsedRealtime()
        onGpsReading(
            RunGpsReading(
                runId = runId,
                receivedAtElapsedMs = null,
                accuracyMeters = null,
                providerEnabled = true,
                requestStartedAtElapsedMs = requestStartedAtElapsedMs
            )
        )

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!isCurrentRequest(requestGeneration)) return
                val elapsedRealtimeMs = location.elapsedRealtimeNanos
                    .takeIf { it > 0L }
                    ?.div(NANOS_PER_MILLISECOND)
                    ?: SystemClock.elapsedRealtime()
                onGpsReading(
                    RunGpsReading(
                        runId,
                        elapsedRealtimeMs,
                        location.accuracy.takeIf { location.hasAccuracy() },
                        true,
                        location.speedMetersPerSecondOrNull()
                    )
                )
                onGpsSignalRestored()
                onLocationUpdate(
                    RunLocationUpdate(
                        runId,
                        requestGeneration,
                        Location(location),
                        activeDurationAt(elapsedRealtimeMs)
                    )
                )
            }

            @Deprecated("Required by LocationListener on supported Android versions")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) {
                if (!isCurrentRequest(requestGeneration)) return
                onGpsReading(
                    RunGpsReading(
                        runId = runId,
                        receivedAtElapsedMs = null,
                        accuracyMeters = null,
                        providerEnabled = true,
                        requestStartedAtElapsedMs = requestStartedAtElapsedMs
                    )
                )
                onGpsSignalRestored()
            }

            override fun onProviderDisabled(provider: String) {
                if (isCurrentRequest(requestGeneration)) {
                    onGpsReading(RunGpsReading(runId, null, null, false))
                }
            }
        }
        listener = locationListener
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL_MS,
                MINIMUM_LOCATION_DISTANCE_METERS,
                locationListener,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            onPermissionFailure()
        }
    }

    fun stop() {
        acceptingLocations = false
        onGpsReading(null)
        listener?.let(locationManager::removeUpdates)
        listener = null
    }

    private fun isCurrentRequest(requestGeneration: Long): Boolean =
        acceptingLocations && isCurrentGeneration(requestGeneration)

    private companion object {
        const val LOCATION_INTERVAL_MS = 2_000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val MINIMUM_LOCATION_DISTANCE_METERS = 0f
    }
}
