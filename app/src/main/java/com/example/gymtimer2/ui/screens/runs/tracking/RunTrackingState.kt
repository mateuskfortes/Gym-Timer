package com.example.gymtimer2.ui.screens.runs.tracking

import com.example.gymtimer2.domain.model.RunGpsReading
import com.example.gymtimer2.domain.model.RunStepRateReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Identifica o serviço vivo neste processo para recuperar sessões interrompidas. */
internal object RunTrackingState {
    @Volatile
    var activeRunId: Long? = null
    @Volatile
    var crashRecoverableRunId: Long? = null
    private val mutableGpsReading = MutableStateFlow<RunGpsReading?>(null)
    val gpsReading = mutableGpsReading.asStateFlow()
    private val mutableStepRateReading = MutableStateFlow<RunStepRateReading?>(null)
    val stepRateReading = mutableStepRateReading.asStateFlow()
    private val mutableRouteAlertMute = MutableStateFlow<RunRouteAlertMute?>(null)
    val routeAlertMute = mutableRouteAlertMute.asStateFlow()

    fun updateGps(reading: RunGpsReading?) { mutableGpsReading.value = reading }

    /** Publica ou limpa a leitura temporária do sensor de passos. */
    fun updateStepRate(reading: RunStepRateReading?) { mutableStepRateReading.value = reading }

    /** Publica o silenciamento temporário do desvio para a tela da corrida. */
    fun updateRouteAlertMute(runId: Long, mutedUntilReturn: Boolean) {
        mutableRouteAlertMute.value = RunRouteAlertMute(runId, mutedUntilReturn)
    }
}

internal data class RunRouteAlertMute(val runId: Long, val mutedUntilReturn: Boolean)
