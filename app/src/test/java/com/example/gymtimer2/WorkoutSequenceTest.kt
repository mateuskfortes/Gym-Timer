package com.example.gymtimer2

import com.example.gymtimer2.timer_overlay_service.components.float_timer.WorkoutSequence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSequenceTest {
    @Test
    fun advancesInSavedOrderThenFinishes() {
        val sequence = WorkoutSequence(listOf(9, 4, 12))

        assertEquals(9, sequence.currentId)
        assertEquals(4, sequence.nextId)
        assertTrue(sequence.hasNext)
        assertEquals(4, sequence.advance())
        assertEquals(12, sequence.advance())
        assertFalse(sequence.hasNext)
        assertEquals(null, sequence.advance())
    }

    @Test
    fun emptyScheduleHasNoExercise() {
        val sequence = WorkoutSequence(emptyList())

        assertEquals(null, sequence.currentId)
        assertFalse(sequence.hasNext)
    }
}
