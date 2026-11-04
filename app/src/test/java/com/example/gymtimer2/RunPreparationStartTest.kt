package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.ui.screens.runs.active.dispatchRunStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunPreparationStartTest {
    @Test
    fun startsImmediatelyWhenBatterySaverIsDisabled() {
        var startedActivity: RunActivityType? = null
        var warningShown = false

        dispatchRunStart(
            activityType = RunActivityType.TRAIL_RUN,
            isBatterySaverEnabled = false,
            onStart = { startedActivity = it },
            onShowBatterySaverWarning = { warningShown = true }
        )

        assertEquals(RunActivityType.TRAIL_RUN, startedActivity)
        assertFalse(warningShown)
    }

    @Test
    fun asksForConfirmationInsteadOfStartingWhenBatterySaverIsEnabled() {
        var startedActivity: RunActivityType? = null
        var warningShown = false

        dispatchRunStart(
            activityType = RunActivityType.TRAIL_RUN,
            isBatterySaverEnabled = true,
            onStart = { startedActivity = it },
            onShowBatterySaverWarning = { warningShown = true }
        )

        assertTrue(warningShown)
        assertEquals(null, startedActivity)
    }
}
