package com.example.gymtimer2.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.example.gymtimer2.domain.model.AccentColor
import com.example.gymtimer2.domain.model.AppSettings
import com.example.gymtimer2.domain.model.DEFAULT_RUN_SCREEN_DIM_TIMEOUT_MS
import com.example.gymtimer2.domain.model.RunMapCameraSnapshot
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunTemperatureUnit
import com.example.gymtimer2.domain.model.RunStartEndHideDistance
import com.example.gymtimer2.domain.model.ThemeMode
import com.example.gymtimer2.domain.model.isValidRunBodyMassKg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(
        AppSettings(
            themeMode = ThemeMode.entries.firstOrNull {
                it.name == preferences.getString(THEME_MODE_KEY, null)
            } ?: ThemeMode.LIGHT,
            accentColor = AccentColor.fromStoredValue(preferences.getString(ACCENT_COLOR_KEY, null)),
            runDistanceUnit = RunDistanceUnit.fromStoredValue(preferences.getString(RUN_DISTANCE_UNIT_KEY, null)),
            runOffRouteAlertsEnabled = preferences.getBoolean(RUN_OFF_ROUTE_ALERTS_ENABLED_KEY, true),
            runOffRouteNotificationsEnabled = preferences.getBoolean(RUN_OFF_ROUTE_NOTIFICATIONS_ENABLED_KEY, true),
            runAutoPause = preferences.getBoolean(RUN_AUTO_PAUSE_KEY, false),
            runKeepScreenAwake = preferences.getBoolean(RUN_KEEP_SCREEN_AWAKE_KEY, false),
            runScreenDimTimeoutMs = preferences.getLong(
                RUN_SCREEN_DIM_TIMEOUT_KEY,
                DEFAULT_RUN_SCREEN_DIM_TIMEOUT_MS
            ),
            runShowControlsOnLockScreen = preferences.getBoolean(RUN_LOCK_SCREEN_CONTROLS_KEY, false),
            runManualLapsEnabled = preferences.getBoolean(RUN_MANUAL_LAPS_KEY, false),
            runStepRateSensorEnabled = preferences.getBoolean(RUN_STEP_RATE_SENSOR_ENABLED_KEY, true),
            runLiveSegmentsEnabled = preferences.getBoolean(RUN_LIVE_SEGMENTS_ENABLED_KEY, true),
            runDisplayAllTurnCues = preferences.getBoolean(RUN_DISPLAY_ALL_TURN_CUES_KEY, false),
            runUpcomingTurnCueCount = preferences.getInt(RUN_UPCOMING_TURN_CUE_COUNT_KEY, 1).coerceAtLeast(1),
            runBodyMassKg = preferences.getString(RUN_BODY_MASS_KG_KEY, null)
                ?.toDoubleOrNull()
                ?.takeIf(::isValidRunBodyMassKg),
            runWeatherEnabled = preferences.getBoolean(RUN_WEATHER_ENABLED_KEY, false),
            runTemperatureUnit = RunTemperatureUnit.fromStoredValue(
                preferences.getString(RUN_TEMPERATURE_UNIT_KEY, null)
            ),
            runStartEndHideDistance = RunStartEndHideDistance.fromMeters(
                preferences.getInt(RUN_START_END_HIDE_DISTANCE_KEY, 0)
            )
        )
    )
    val settings = _settings.asStateFlow()
    private val _runMapCameraSnapshot = MutableStateFlow(readRunMapCameraSnapshot())
    val runMapCameraSnapshot = _runMapCameraSnapshot.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit { putString(THEME_MODE_KEY, mode.name) }
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setAccentColor(color: AccentColor) {
        preferences.edit { putString(ACCENT_COLOR_KEY, color.name) }
        _settings.value = _settings.value.copy(accentColor = color)
    }

    fun setRunMapCameraSnapshot(snapshot: RunMapCameraSnapshot) {
        require(snapshot.isValid())
        preferences.edit {
            putLong(RUN_MAP_CAMERA_RUN_ID_KEY, snapshot.runId)
            putString(RUN_MAP_CAMERA_LATITUDE_KEY, snapshot.latitude.toString())
            putString(RUN_MAP_CAMERA_LONGITUDE_KEY, snapshot.longitude.toString())
            putString(RUN_MAP_CAMERA_ZOOM_KEY, snapshot.zoom.toString())
            putString(RUN_MAP_CAMERA_TILT_KEY, snapshot.tilt.toString())
            putString(RUN_MAP_CAMERA_BEARING_KEY, snapshot.bearing.toString())
            putBoolean(RUN_MAP_CAMERA_FOLLOWS_USER_KEY, snapshot.followsUser)
        }
        _runMapCameraSnapshot.value = snapshot
    }

    fun clearRunMapCameraSnapshot() {
        preferences.edit {
            remove(RUN_MAP_CAMERA_RUN_ID_KEY)
            remove(RUN_MAP_CAMERA_LATITUDE_KEY)
            remove(RUN_MAP_CAMERA_LONGITUDE_KEY)
            remove(RUN_MAP_CAMERA_ZOOM_KEY)
            remove(RUN_MAP_CAMERA_TILT_KEY)
            remove(RUN_MAP_CAMERA_BEARING_KEY)
            remove(RUN_MAP_CAMERA_FOLLOWS_USER_KEY)
        }
        _runMapCameraSnapshot.value = null
    }

    private fun readRunMapCameraSnapshot(): RunMapCameraSnapshot? {
        if (!preferences.contains(RUN_MAP_CAMERA_RUN_ID_KEY)) return null
        val snapshot = RunMapCameraSnapshot(
            runId = preferences.getLong(RUN_MAP_CAMERA_RUN_ID_KEY, -1L),
            latitude = preferences.getString(RUN_MAP_CAMERA_LATITUDE_KEY, null)?.toDoubleOrNull() ?: return null,
            longitude = preferences.getString(RUN_MAP_CAMERA_LONGITUDE_KEY, null)?.toDoubleOrNull() ?: return null,
            zoom = preferences.getString(RUN_MAP_CAMERA_ZOOM_KEY, null)?.toDoubleOrNull() ?: return null,
            tilt = preferences.getString(RUN_MAP_CAMERA_TILT_KEY, null)?.toDoubleOrNull() ?: return null,
            bearing = preferences.getString(RUN_MAP_CAMERA_BEARING_KEY, null)?.toDoubleOrNull() ?: return null,
            followsUser = preferences.getBoolean(RUN_MAP_CAMERA_FOLLOWS_USER_KEY, true)
        )
        return snapshot.takeIf(RunMapCameraSnapshot::isValid)
    }

    fun setRunDistanceUnit(unit: RunDistanceUnit) {
        preferences.edit { putString(RUN_DISTANCE_UNIT_KEY, unit.name) }
        _settings.value = _settings.value.copy(runDistanceUnit = unit)
    }

    fun setRunOffRouteAlertsEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(RUN_OFF_ROUTE_ALERTS_ENABLED_KEY, enabled) }
        _settings.value = _settings.value.copy(runOffRouteAlertsEnabled = enabled)
    }

    fun setRunOffRouteNotificationsEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(RUN_OFF_ROUTE_NOTIFICATIONS_ENABLED_KEY, enabled) }
        _settings.value = _settings.value.copy(runOffRouteNotificationsEnabled = enabled)
    }

    fun setRunAutoPause(enabled: Boolean) {
        preferences.edit { putBoolean(RUN_AUTO_PAUSE_KEY, enabled) }
        _settings.value = _settings.value.copy(runAutoPause = enabled)
    }

    fun setRunKeepScreenAwake(enabled: Boolean) {
        preferences.edit { putBoolean(RUN_KEEP_SCREEN_AWAKE_KEY, enabled) }
        _settings.value = _settings.value.copy(runKeepScreenAwake = enabled)
    }

    fun setRunScreenDimTimeout(timeoutMs: Long) {
        preferences.edit { putLong(RUN_SCREEN_DIM_TIMEOUT_KEY, timeoutMs) }
        _settings.value = _settings.value.copy(runScreenDimTimeoutMs = timeoutMs)
    }

    fun setRunShowControlsOnLockScreen(enabled: Boolean) {
        preferences.edit { putBoolean(RUN_LOCK_SCREEN_CONTROLS_KEY, enabled) }
        _settings.value = _settings.value.copy(runShowControlsOnLockScreen = enabled)
    }

    fun setRunManualLapsEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(RUN_MANUAL_LAPS_KEY, enabled) }
        _settings.value = _settings.value.copy(runManualLapsEnabled = enabled)
    }

    fun setRunStepRateSensorEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(RUN_STEP_RATE_SENSOR_ENABLED_KEY, enabled) }
        _settings.value = _settings.value.copy(runStepRateSensorEnabled = enabled)
    }

    fun setRunLiveSegmentsEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(RUN_LIVE_SEGMENTS_ENABLED_KEY, enabled) }
        _settings.value = _settings.value.copy(runLiveSegmentsEnabled = enabled)
    }

    fun setRunDisplayAllTurnCues(enabled: Boolean) {
        preferences.edit { putBoolean(RUN_DISPLAY_ALL_TURN_CUES_KEY, enabled) }
        _settings.value = _settings.value.copy(runDisplayAllTurnCues = enabled)
    }

    fun setRunUpcomingTurnCueCount(count: Int) {
        require(count > 0)
        preferences.edit { putInt(RUN_UPCOMING_TURN_CUE_COUNT_KEY, count) }
        _settings.value = _settings.value.copy(runUpcomingTurnCueCount = count)
    }

    fun setRunBodyMassKg(bodyMassKg: Double?) {
        require(bodyMassKg == null || isValidRunBodyMassKg(bodyMassKg))
        preferences.edit {
            if (bodyMassKg == null) remove(RUN_BODY_MASS_KG_KEY)
            else putString(RUN_BODY_MASS_KG_KEY, bodyMassKg.toString())
        }
        _settings.value = _settings.value.copy(runBodyMassKg = bodyMassKg)
    }

    fun setRunWeatherEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(RUN_WEATHER_ENABLED_KEY, enabled) }
        _settings.value = _settings.value.copy(runWeatherEnabled = enabled)
    }

    fun setRunTemperatureUnit(unit: RunTemperatureUnit) {
        preferences.edit { putString(RUN_TEMPERATURE_UNIT_KEY, unit.name) }
        _settings.value = _settings.value.copy(runTemperatureUnit = unit)
    }

    fun setRunStartEndHideDistance(distance: RunStartEndHideDistance) {
        preferences.edit { putInt(RUN_START_END_HIDE_DISTANCE_KEY, distance.meters) }
        _settings.value = _settings.value.copy(runStartEndHideDistance = distance)
    }

    private companion object {
        const val PREFERENCES_NAME = "app_settings"
        const val THEME_MODE_KEY = "theme_mode"
        const val ACCENT_COLOR_KEY = "accent_color"
        const val RUN_DISTANCE_UNIT_KEY = "run_distance_unit"
        const val RUN_OFF_ROUTE_ALERTS_ENABLED_KEY = "run_off_route_alerts_enabled"
        const val RUN_OFF_ROUTE_NOTIFICATIONS_ENABLED_KEY = "run_off_route_notifications_enabled"
        const val RUN_AUTO_PAUSE_KEY = "run_auto_pause"
        const val RUN_KEEP_SCREEN_AWAKE_KEY = "run_keep_screen_awake"
        const val RUN_SCREEN_DIM_TIMEOUT_KEY = "run_screen_dim_timeout_ms"
        const val RUN_LOCK_SCREEN_CONTROLS_KEY = "run_lock_screen_controls"
        const val RUN_MANUAL_LAPS_KEY = "run_manual_laps"
        const val RUN_STEP_RATE_SENSOR_ENABLED_KEY = "run_step_rate_sensor_enabled"
        const val RUN_LIVE_SEGMENTS_ENABLED_KEY = "run_live_segments_enabled"
        const val RUN_DISPLAY_ALL_TURN_CUES_KEY = "run_display_all_turn_cues"
        const val RUN_UPCOMING_TURN_CUE_COUNT_KEY = "run_upcoming_turn_cue_count"
        const val RUN_BODY_MASS_KG_KEY = "run_body_mass_kg"
        const val RUN_WEATHER_ENABLED_KEY = "run_weather_enabled"
        const val RUN_TEMPERATURE_UNIT_KEY = "run_temperature_unit"
        const val RUN_START_END_HIDE_DISTANCE_KEY = "run_start_end_hide_distance_meters"
        const val RUN_MAP_CAMERA_RUN_ID_KEY = "run_map_camera_run_id"
        const val RUN_MAP_CAMERA_LATITUDE_KEY = "run_map_camera_latitude"
        const val RUN_MAP_CAMERA_LONGITUDE_KEY = "run_map_camera_longitude"
        const val RUN_MAP_CAMERA_ZOOM_KEY = "run_map_camera_zoom"
        const val RUN_MAP_CAMERA_TILT_KEY = "run_map_camera_tilt"
        const val RUN_MAP_CAMERA_BEARING_KEY = "run_map_camera_bearing"
        const val RUN_MAP_CAMERA_FOLLOWS_USER_KEY = "run_map_camera_follows_user"
    }
}
