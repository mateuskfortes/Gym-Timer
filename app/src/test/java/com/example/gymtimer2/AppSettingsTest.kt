package com.example.gymtimer2

import com.example.gymtimer2.domain.model.AccentColor
import com.example.gymtimer2.domain.model.AppSettings
import com.example.gymtimer2.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultAppearanceMatchesExistingLightPurpleTheme() {
        assertEquals(ThemeMode.LIGHT, AppSettings().themeMode)
        assertEquals(AccentColor.PURPLE, AppSettings().accentColor)
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
}
