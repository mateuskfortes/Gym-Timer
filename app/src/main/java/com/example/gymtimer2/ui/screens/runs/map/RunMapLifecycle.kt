package com.example.gymtimer2.ui.screens.runs.map

import androidx.lifecycle.Lifecycle
import org.maplibre.android.maps.MapView

/** Encaminha eventos de tela para o ciclo de vida do MapView. */
internal fun MapView.handleLifecycleEvent(event: Lifecycle.Event) {
    when (event) {
        Lifecycle.Event.ON_START -> onStart()
        Lifecycle.Event.ON_RESUME -> onResume()
        Lifecycle.Event.ON_PAUSE -> onPause()
        Lifecycle.Event.ON_STOP -> onStop()
        else -> Unit
    }
}
