package com.example.gymtimer2.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gymtimer2.data.preferences.AppSettingsRepository
import com.example.gymtimer2.domain.model.AccentColor
import com.example.gymtimer2.domain.model.ThemeMode

class SettingsViewModel(private val repository: AppSettingsRepository) : ViewModel() {
    val settings = repository.settings

    fun selectTheme(mode: ThemeMode) = repository.setThemeMode(mode)

    fun selectAccent(color: AccentColor) = repository.setAccentColor(color)

    companion object {
        fun factory(repository: AppSettingsRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { SettingsViewModel(repository) } }
    }
}
