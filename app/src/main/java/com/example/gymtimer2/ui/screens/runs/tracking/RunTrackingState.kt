package com.example.gymtimer2.ui.screens.runs.tracking

/** Identifica o serviço vivo neste processo para recuperar sessões interrompidas. */
internal object RunTrackingState {
    @Volatile
    var activeRunId: Long? = null
}
