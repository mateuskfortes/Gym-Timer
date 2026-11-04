package com.example.gymtimer2

import com.example.gymtimer2.ui.screens.runs.map.RunMapLayer
import com.example.gymtimer2.ui.screens.runs.map.isAvailable
import com.example.gymtimer2.ui.screens.runs.map.isSelected
import com.example.gymtimer2.ui.screens.runs.map.toggle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunMapLayerTest {
    @Test
    fun standardLayerIsAlwaysAvailable() {
        assertTrue(RunMapLayer.STANDARD.isAvailable(gradientAvailable = false, weeklyActivityAvailable = false))
    }

    @Test
    fun optionalLayersRequireTheirOwnData() {
        assertFalse(RunMapLayer.GRADIENT.isAvailable(gradientAvailable = false, weeklyActivityAvailable = true))
        assertFalse(RunMapLayer.WEEKLY_ACTIVITY.isAvailable(gradientAvailable = true, weeklyActivityAvailable = false))
        assertFalse(RunMapLayer.LOCAL_HISTORY.isAvailable(gradientAvailable = false, weeklyActivityAvailable = false))
        assertTrue(RunMapLayer.GRADIENT.isAvailable(gradientAvailable = true, weeklyActivityAvailable = false))
        assertTrue(RunMapLayer.WEEKLY_ACTIVITY.isAvailable(gradientAvailable = false, weeklyActivityAvailable = true))
        assertTrue(
            RunMapLayer.LOCAL_HISTORY.isAvailable(
                gradientAvailable = false,
                weeklyActivityAvailable = false,
                localHistoryAvailable = true
            )
        )
        assertFalse(RunMapLayer.OPENSTREETMAP_POI.isAvailable(gradientAvailable = false, weeklyActivityAvailable = false))
        assertTrue(
            RunMapLayer.OPENSTREETMAP_POI.isAvailable(
                gradientAvailable = false,
                weeklyActivityAvailable = false,
                poiLayerEnabled = true
            )
        )
    }

    @Test
    fun togglingOverlayKeepsOtherOverlaysActive() {
        val selected = emptySet<RunMapLayer>()
            .toggle(RunMapLayer.WEEKLY_ACTIVITY)
            .toggle(RunMapLayer.OPENSTREETMAP_POI)

        assertTrue(RunMapLayer.WEEKLY_ACTIVITY in selected)
        assertTrue(RunMapLayer.OPENSTREETMAP_POI in selected)
        assertTrue(selected.isSelected(RunMapLayer.WEEKLY_ACTIVITY))
        assertFalse(selected.isSelected(RunMapLayer.STANDARD))
    }

    @Test
    fun selectingStandardClearsEveryOverlay() {
        val selected = setOf(RunMapLayer.GRADIENT, RunMapLayer.OPENSTREETMAP_POI)
            .toggle(RunMapLayer.STANDARD)

        assertTrue(selected.isEmpty())
        assertTrue(selected.isSelected(RunMapLayer.STANDARD))
    }

    @Test
    fun togglingSelectedOverlayTurnsItOff() {
        val selected = setOf(RunMapLayer.WEEKLY_ACTIVITY).toggle(RunMapLayer.WEEKLY_ACTIVITY)

        assertTrue(selected.isEmpty())
    }
}
