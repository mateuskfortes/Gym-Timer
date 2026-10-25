package com.example.gymtimer2.ui.screens.runs.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import org.maplibre.android.geometry.LatLng

/** Busca a posição apenas enquanto o mapa está visível e ainda não há pontos da corrida. */
@Composable
internal fun rememberInitialMapLocation(enabled: Boolean): State<LatLng?> {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val source = remember(context) { RunMapLocationSource(context) }
    return produceState<LatLng?>(null, source, lifecycle, enabled) {
        if (!enabled) return@produceState
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            source.locations().collect { value = it }
        }
    }
}
