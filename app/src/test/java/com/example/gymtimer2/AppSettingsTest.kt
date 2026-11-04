package com.example.gymtimer2

import com.example.gymtimer2.domain.model.AppSettings
import com.example.gymtimer2.domain.model.AccentColor
import com.example.gymtimer2.domain.model.DEFAULT_RUN_SCREEN_DIM_TIMEOUT_MS
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.domain.model.RunTemperatureUnit
import com.example.gymtimer2.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultAppearanceMatchesExistingLightPurpleTheme() {
        assertEquals(ThemeMode.LIGHT, AppSettings().themeMode)
        assertEquals(AccentColor.PURPLE, AppSettings().accentColor)
        assertEquals(RunDistanceUnit.METRIC, AppSettings().runDistanceUnit)
        assertEquals(RunTemperatureUnit.CELSIUS, AppSettings().runTemperatureUnit)
    }

    @Test
    fun distanceUnitConversionsKeepStoredValuesMetric() {
        assertEquals(1.0, RunDistanceUnit.IMPERIAL.distanceFromMeters(1_609.344), 0.0001)
        assertEquals(579.36, RunDistanceUnit.IMPERIAL.paceSecondsPerUnit(360.0), 0.01)
        assertEquals(360, RunDistanceUnit.IMPERIAL.paceSecondsPerKilometer(579))
        assertEquals(6.2137, RunDistanceUnit.IMPERIAL.speedFromKilometersPerHour(10.0), 0.0001)
        assertEquals(328.084, RunDistanceUnit.IMPERIAL.elevationFromMeters(100.0), 0.001)
        assertEquals(10.0, RunDistanceUnit.IMPERIAL.metersFromElevation(32.8084), 0.001)
        assertEquals(RunDistanceUnit.METRIC, RunDistanceUnit.fromStoredValue("UNKNOWN"))
    }

    @Test
    fun unknownStoredAccentFallsBackToPurple() {
        assertEquals(AccentColor.PURPLE, AccentColor.fromStoredValue(null))
        assertEquals(AccentColor.PURPLE, AccentColor.fromStoredValue("UNKNOWN"))
        assertEquals(AccentColor.GREEN, AccentColor.fromStoredValue("GREEN"))
    }

    @Test
    fun accentOptionsHaveDistinctColorsForEachTheme() {
        assertEquals(12, AccentColor.entries.size)
        assertEquals(12, AccentColor.entries.map { it.lightArgb }.toSet().size)
        assertEquals(12, AccentColor.entries.map { it.darkArgb }.toSet().size)
        assertTrue(AccentColor.entries.all { it.lightArgb != it.darkArgb })
        assertNotEquals(AccentColor.PURPLE.argb(false), AccentColor.PURPLE.argb(true))
    }

    @Test
    fun keepingScreenAwakeDuringRunsIsDisabledByDefault() {
        assertFalse(AppSettings().runKeepScreenAwake)
    }

    @Test
    fun screenDimTimeoutDefaultsToThirtySeconds() {
        assertEquals(DEFAULT_RUN_SCREEN_DIM_TIMEOUT_MS, AppSettings().runScreenDimTimeoutMs)
    }

    @Test
    fun lockScreenControlsAreDisabledByDefault() {
        assertFalse(AppSettings().runShowControlsOnLockScreen)
    }

    @Test
    fun runAppearsOverLockScreenOnlyWhenEnabledAndOpen() {
        assertTrue(shouldShowRunOnLockScreen(true, true, true))
        assertFalse(shouldShowRunOnLockScreen(false, true, true))
        assertFalse(shouldShowRunOnLockScreen(true, false, true))
        assertFalse(shouldShowRunOnLockScreen(true, true, false))
    }

    @Test
    fun manualLapButtonMatchesStravaDefault() {
        assertFalse(AppSettings().runManualLapsEnabled)
    }

    @Test
    fun runStepRateSensorPreservesExistingCollectionByDefault() {
        assertTrue(AppSettings().runStepRateSensorEnabled)
    }

    @Test
    fun localLiveSegmentMatchingPreservesExistingBehaviorByDefault() {
        assertTrue(AppSettings().runLiveSegmentsEnabled)
    }

    @Test
    fun routeCueSettingsKeepTheExistingSingleCueDisplayByDefault() {
        assertFalse(AppSettings().runDisplayAllTurnCues)
        assertEquals(1, AppSettings().runUpcomingTurnCueCount)
    }

    @Test
    fun runWeatherLookupIsDisabledByDefault() {
        assertFalse(AppSettings().runWeatherEnabled)
    }

    @Test
    fun temperatureUnitsConvertFromStoredCelsius() {
        assertEquals(32.0, RunTemperatureUnit.FAHRENHEIT.fromCelsius(0.0), 0.0)
        assertEquals(77.0, RunTemperatureUnit.FAHRENHEIT.fromCelsius(25.0), 0.0)
        assertEquals(25.0, RunTemperatureUnit.CELSIUS.fromCelsius(25.0), 0.0)
        assertEquals(RunTemperatureUnit.CELSIUS, RunTemperatureUnit.fromStoredValue("UNKNOWN"))
    }

    @Test
    fun routeNotificationsRequireBothRouteAlertsAndNotificationTypeEnabled() {
        assertTrue(AppSettings().runRouteNotificationsEnabled)
        assertTrue(
            AppSettings(runOffRouteAlertsEnabled = true, runOffRouteNotificationsEnabled = true)
                .runRouteNotificationsEnabled
        )
        assertFalse(
            AppSettings(runOffRouteAlertsEnabled = false, runOffRouteNotificationsEnabled = true)
                .runRouteNotificationsEnabled
        )
        assertFalse(
            AppSettings(runOffRouteAlertsEnabled = true, runOffRouteNotificationsEnabled = false)
                .runRouteNotificationsEnabled
        )
    }
}
