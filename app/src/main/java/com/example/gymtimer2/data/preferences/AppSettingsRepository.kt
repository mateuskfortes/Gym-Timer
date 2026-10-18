package com.example.gymtimer2.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.example.gymtimer2.domain.model.AccentColor
import com.example.gymtimer2.domain.model.AppSettings
import com.example.gymtimer2.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(
        AppSettings(
            themeMode = ThemeMode.entries.firstOrNull {
                it.name == preferences.getString(THEME_MODE_KEY, null)
            } ?: ThemeMode.LIGHT,
            accentColor = AccentColor.fromStoredValue(preferences.getString(ACCENT_COLOR_KEY, null))
        )
    )
    val settings = _settings.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit { putString(THEME_MODE_KEY, mode.name) }
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setAccentColor(color: AccentColor) {
        preferences.edit { putString(ACCENT_COLOR_KEY, color.name) }
        _settings.value = _settings.value.copy(accentColor = color)
    }

    private companion object {
        const val PREFERENCES_NAME = "app_settings"
        const val THEME_MODE_KEY = "theme_mode"
        const val ACCENT_COLOR_KEY = "accent_color"
    }
}
