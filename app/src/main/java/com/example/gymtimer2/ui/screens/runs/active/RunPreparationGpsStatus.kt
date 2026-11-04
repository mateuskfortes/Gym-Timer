package com.example.gymtimer2.ui.screens.runs.active

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.gymtimer2.domain.model.RunGpsReading
import com.example.gymtimer2.domain.model.RunGpsStatus
import com.example.gymtimer2.domain.model.runGpsStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Acompanha o sinal GPS enquanto a tela de preparação está visível. */
@Composable
internal fun rememberRunPreparationGpsStatus(): RunGpsStatus {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val source = remember(context) { RunPreparationGpsStatusSource(context) }
    return produceState(RunGpsStatus.SEARCHING, source, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            source.statuses().collect { value = it }
        }
    }.value
}

/** Lê posições GPS recentes e remove o listener quando a coleta termina. */
private class RunPreparationGpsStatusSource(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    fun statuses(): Flow<RunGpsStatus> = callbackFlow {
        if (!hasFineLocationPermission()) {
            trySend(RunGpsStatus.SEARCHING)
            close()
            return@callbackFlow
        }

        val session = RunPreparationGpsSession(locationManager) { trySend(it) }
        session.emitCurrentStatus()
        val statusUpdates = launch {
            while (isActive) {
                session.emitCurrentStatus()
                delay(GPS_STATUS_REFRESH_MS)
            }
        }
        awaitClose {
            statusUpdates.cancel()
            session.stopListening()
        }
    }

    private fun hasFineLocationPermission() =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}

private class RunPreparationGpsSession(
    private val locationManager: LocationManager,
    private val emit: (RunGpsStatus) -> Unit
) {
    private var latestReading: RunGpsReading? = null
    private var requestStartedAtElapsedMs: Long? = null
    private var isListening = false
    private val listener = createListener()

    fun emitCurrentStatus() {
        val providerEnabled = isGpsEnabled()
        if (providerEnabled) startListening() else onGpsDisabled()
        val reading = latestReading ?: RunGpsReading(
            runId = PREPARATION_RUN_ID,
            receivedAtElapsedMs = null,
            accuracyMeters = null,
            providerEnabled = providerEnabled,
            requestStartedAtElapsedMs = requestStartedAtElapsedMs
        )
        emit(
            runGpsStatus(
                reading.copy(providerEnabled = providerEnabled),
                PREPARATION_RUN_ID,
                SystemClock.elapsedRealtime()
            )
        )
    }

    fun stopListening() {
        if (!isListening) return
        try {
            locationManager.removeUpdates(listener)
        } catch (_: SecurityException) {
            // Permission can be revoked while preparation is open.
        }
        isListening = false
    }

    private fun startListening() {
        if (isListening) return
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                GPS_UPDATE_INTERVAL_MS,
                0f,
                listener,
                Looper.getMainLooper()
            )
            isListening = true
            requestStartedAtElapsedMs = SystemClock.elapsedRealtime()
        } catch (_: SecurityException) {
            latestReading = null
            requestStartedAtElapsedMs = null
        }
    }

    private fun onGpsDisabled() {
        latestReading = null
        requestStartedAtElapsedMs = null
        stopListening()
    }

    private fun createListener() = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            latestReading = RunGpsReading(
                PREPARATION_RUN_ID,
                location.receivedAtElapsedMs(),
                location.accuracy.takeIf { location.hasAccuracy() },
                isGpsEnabled()
            )
            emitCurrentStatus()
        }

        @Deprecated("Required by LocationListener on supported Android versions")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) {
            latestReading = null
            emitCurrentStatus()
        }

        override fun onProviderDisabled(provider: String) {
            latestReading = null
            emitCurrentStatus()
        }
    }

    private fun isGpsEnabled(): Boolean = runCatching {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }.getOrDefault(false)

    private fun Location.receivedAtElapsedMs(): Long = elapsedRealtimeNanos
        .takeIf { it > 0L }
        ?.div(NANOS_PER_MILLISECOND)
        ?: SystemClock.elapsedRealtime()
}

private const val PREPARATION_RUN_ID = 0L
private const val GPS_UPDATE_INTERVAL_MS = 1_000L
private const val GPS_STATUS_REFRESH_MS = 1_000L
private const val NANOS_PER_MILLISECOND = 1_000_000L
