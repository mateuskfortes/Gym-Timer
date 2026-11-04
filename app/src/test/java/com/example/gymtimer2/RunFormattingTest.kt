package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunDistanceUnit
import com.example.gymtimer2.ui.screens.runs.format.RunActivityFormatting
import com.example.gymtimer2.ui.screens.runs.format.RunFormatting
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class RunFormattingTest {
    @Test
    fun everyActivityTypeHasALocalizedResourceForNotifications() {
        assertEquals(
            listOf(
                R.string.run_activity_type,
                R.string.run_activity_type_trail,
                R.string.run_activity_type_virtual,
                R.string.run_activity_type_walk,
                R.string.run_activity_type_hike,
                R.string.run_activity_type_wheelchair
            ),
            RunActivityType.entries.map(RunActivityFormatting::activityTypeResource)
        )
    }

    @Test
    fun workoutInputsUseReadablePrecisionWithoutChangingStoredUnits() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("0.2485", RunFormatting.workoutDistanceInputValue(400.0, RunDistanceUnit.IMPERIAL))
            assertEquals("0.4", RunFormatting.workoutDistanceInputValue(400.0, RunDistanceUnit.METRIC))
            assertEquals("32.8", RunFormatting.workoutElevationInputValue(10.0, RunDistanceUnit.IMPERIAL))
            assertEquals("10", RunFormatting.workoutElevationInputValue(10.0, RunDistanceUnit.METRIC))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
