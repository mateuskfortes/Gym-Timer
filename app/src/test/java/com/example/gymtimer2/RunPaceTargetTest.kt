package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunPaceGuidance
import com.example.gymtimer2.domain.model.RunPaceTarget
import com.example.gymtimer2.domain.model.RunDistanceUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunPaceTargetTest {
    @Test
    fun parsesOnlyAnOrderedMinutesAndSecondsRange() {
        assertEquals(RunPaceTarget(330, 360), RunPaceTarget.parse("5:30", "06:00"))
        assertNull(RunPaceTarget.parse("6:00", "5:00"))
        assertNull(RunPaceTarget.parse("5:60", "6:00"))
        assertNull(RunPaceTarget.parse("5:30", "5:30"))
        assertNull(RunPaceTarget.parse("", "6:00"))
    }

    @Test
    fun imperialPaceInputsAreStoredAsSecondsPerKilometer() {
        assertEquals(
            RunPaceTarget(149, 186),
            RunPaceTarget.parse("4:00", "5:00", RunDistanceUnit.IMPERIAL)
        )
        assertNull(RunPaceTarget.parse("0:01", "0:02", RunDistanceUnit.IMPERIAL))
    }

    @Test
    fun classifiesPaceAtAndOutsideTheTargetBoundaries() {
        val target = RunPaceTarget(330, 360)

        assertEquals(RunPaceGuidance.WAITING, target.classify(null))
        assertEquals(RunPaceGuidance.WAITING, target.classify(Double.NaN))
        assertEquals(RunPaceGuidance.FASTER, target.classify(329.0))
        assertEquals(RunPaceGuidance.ON_TARGET, target.classify(330.0))
        assertEquals(RunPaceGuidance.ON_TARGET, target.classify(360.0))
        assertEquals(RunPaceGuidance.SLOWER, target.classify(361.0))
    }
}
