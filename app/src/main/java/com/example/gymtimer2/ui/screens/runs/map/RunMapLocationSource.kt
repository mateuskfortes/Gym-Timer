@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2.ui.screens.runs.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.maplibre.android.geometry.LatLng

/** Obtém a posição inicial da câmera sem adicionar pontos ou distância à corrida. */
class RunMapLocationSource(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    /** Usa uma posição recente e pede uma leitura atual; cancelar a coleta cancela os pedidos. */
    fun locations(): Flow<LatLng> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }
        val cancellationSignals = mutableListOf<CancellationSignal>()
        try {
            val providers = locationManager.getProviders(true).filter {
                it == LocationManager.GPS_PROVIDER || it == LocationManager.NETWORK_PROVIDER
            }
            providers.mapNotNull(locationManager::getLastKnownLocation)
                .filter { it.isRecent() }
                .maxByOrNull { it.elapsedRealtimeNanos }
                ?.let { trySend(LatLng(it.latitude, it.longitude)) }

            providers.forEach { provider ->
                val signal = CancellationSignal()
                cancellationSignals.add(signal)
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(appContext)
                ) { location ->
                    location?.let { trySend(LatLng(it.latitude, it.longitude)) }
                }
            }
        } catch (_: SecurityException) {
            // A permissão pode ser revogada enquanto a tela está aberta.
            close()
        }
        awaitClose { cancellationSignals.forEach(CancellationSignal::cancel) }
    }

    /** Permite centralizar a câmera também com localização aproximada. */
    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Evita abrir o mapa numa posição antiga após uma viagem ou reinício do app. */
    private fun Location.isRecent(): Boolean {
        val ageNanos = SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos
        return ageNanos in 0..MAX_CACHED_LOCATION_AGE_NANOS
    }

    private companion object {
        const val MAX_CACHED_LOCATION_AGE_NANOS = 120_000_000_000L
    }
}
