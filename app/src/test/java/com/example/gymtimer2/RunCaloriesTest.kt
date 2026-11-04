package com.example.gymtimer2

import com.example.gymtimer2.domain.model.estimateRunCaloriesKcal
import com.example.gymtimer2.domain.model.parseRunBodyMassKg
import com.example.gymtimer2.domain.model.RunActivityType
import com.example.gymtimer2.domain.model.RunModel
import com.example.gymtimer2.domain.model.RunStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunCaloriesTest {
    @Test
    fun estimatesCaloriesFromBodyMassAndDistance() {
        assertEquals(350, estimateRunCaloriesKcal(70.0, 5_000.0))
    }

    @Test
    fun rejectsMissingInvalidMassAndDistance() {
        assertNull(estimateRunCaloriesKcal(null, 5_000.0))
        assertNull(estimateRunCaloriesKcal(301.0, 5_000.0))
        assertNull(estimateRunCaloriesKcal(70.0, Double.NaN))
        assertNull(estimateRunCaloriesKcal(70.0, 0.0))
    }

    @Test
    fun acceptsLocalizedDecimalAndRejectsMassOutsideSupportedRange() {
        assertEquals(72.5, parseRunBodyMassKg(" 72,5 ")!!, 0.0)
        assertNull(parseRunBodyMassKg("19"))
        assertNull(parseRunBodyMassKg("NaN"))
    }

    @Test
    fun onlyRunActivitiesShowAnEstimate() {
        val run = RunModel(1L, 0L, 1L, 0L, 5_000.0, RunStatus.COMPLETED, 0,
            activityType = RunActivityType.RUN, bodyMassKgAtStart = 70.0)
        val walk = run.copy(activityType = RunActivityType.WALK)

        assertEquals(350, run.estimatedCaloriesKcal)
        assertNull(walk.estimatedCaloriesKcal)
    }
}
