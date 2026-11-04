package com.example.gymtimer2.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gymtimer2.data.preferences.AppSettingsRepository
import com.example.gymtimer2.domain.model.AccentColor
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunTemperatureUnit
import com.example.gymtimer2.domain.model.RunStartEndHideDistance
import com.example.gymtimer2.domain.model.ThemeMode

class SettingsViewModel(private val repository: AppSettingsRepository) : ViewModel() {
    val settings = repository.settings

    fun selectTheme(mode: ThemeMode) = repository.setThemeMode(mode)

    fun selectAccent(color: AccentColor) = repository.setAccentColor(color)

    fun setRunDistanceUnit(unit: RunDistanceUnit) = repository.setRunDistanceUnit(unit)
    fun setRunTemperatureUnit(unit: RunTemperatureUnit) = repository.setRunTemperatureUnit(unit)
    fun setRunStartEndHideDistance(distance: RunStartEndHideDistance) =
        repository.setRunStartEndHideDistance(distance)

    fun setRunOffRouteAlertsEnabled(enabled: Boolean) = repository.setRunOffRouteAlertsEnabled(enabled)
    fun setRunOffRouteNotificationsEnabled(enabled: Boolean) =
        repository.setRunOffRouteNotificationsEnabled(enabled)
    fun setRunAutoPause(enabled: Boolean) = repository.setRunAutoPause(enabled)
    fun setRunKeepScreenAwake(enabled: Boolean) = repository.setRunKeepScreenAwake(enabled)
    fun setRunScreenDimTimeout(timeoutMs: Long) = repository.setRunScreenDimTimeout(timeoutMs)
    fun setRunShowControlsOnLockScreen(enabled: Boolean) = repository.setRunShowControlsOnLockScreen(enabled)
    fun setRunManualLapsEnabled(enabled: Boolean) = repository.setRunManualLapsEnabled(enabled)
    fun setRunStepRateSensorEnabled(enabled: Boolean) = repository.setRunStepRateSensorEnabled(enabled)
    fun setRunLiveSegmentsEnabled(enabled: Boolean) = repository.setRunLiveSegmentsEnabled(enabled)
    fun setRunDisplayAllTurnCues(enabled: Boolean) = repository.setRunDisplayAllTurnCues(enabled)
    fun setRunUpcomingTurnCueCount(count: Int) = repository.setRunUpcomingTurnCueCount(count)
    fun setRunBodyMassKg(bodyMassKg: Double?) = repository.setRunBodyMassKg(bodyMassKg)
    fun setRunWeatherEnabled(enabled: Boolean) = repository.setRunWeatherEnabled(enabled)

    companion object {
        fun factory(repository: AppSettingsRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { SettingsViewModel(repository) } }
    }
}
