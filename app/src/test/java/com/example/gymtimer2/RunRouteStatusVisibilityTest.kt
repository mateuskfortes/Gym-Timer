package com.example.gymtimer2

import com.example.gymtimer2.ui.screens.runs.active.shouldShowRunRouteStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunRouteStatusVisibilityTest {
    @Test
    fun routeStatusRespectsGpsAndOffRouteAlertPreference() {
        assertTrue(shouldShowRunRouteStatus(true, true, false, false))
        assertFalse(shouldShowRunRouteStatus(true, true, true, false))
        assertTrue(shouldShowRunRouteStatus(true, true, true, true))
        assertFalse(shouldShowRunRouteStatus(true, false, false, true))
        assertFalse(shouldShowRunRouteStatus(false, true, false, true))
    }
}
