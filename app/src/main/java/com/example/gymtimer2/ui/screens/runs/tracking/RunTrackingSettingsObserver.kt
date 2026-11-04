package com.example.gymtimer2.ui.screens.runs.tracking

import com.example.gymtimer2.domain.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class RunTrackingSettingsObserver(
    private val scope: CoroutineScope,
    private val settings: Flow<AppSettings>,
    private val onStepRateChanged: (Boolean) -> Unit,
    private val onAutoPauseChanged: suspend (Boolean) -> Unit
) {
    private var stepRatePreferenceEnabled: Boolean? = null

    fun start() {
        scope.launch {
            settings.collect { current ->
                if (current.runStepRateSensorEnabled != stepRatePreferenceEnabled) {
                    stepRatePreferenceEnabled = current.runStepRateSensorEnabled
                    onStepRateChanged(current.runStepRateSensorEnabled)
                }
                onAutoPauseChanged(current.runAutoPause)
            }
        }
    }
}
