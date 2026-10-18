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

    @Test
    fun skippingNextPostponesItUntilOtherExercisesHaveAdvanced() {
        val savedOrder = mutableListOf(9, 4, 12, 18)
        val sequence = WorkoutSequence(savedOrder)

        assertTrue(sequence.canSkipNext)
        assertTrue(sequence.skipNext())
        assertEquals(12, sequence.nextId)
        assertTrue(sequence.skipNext())
        assertEquals(18, sequence.nextId)
        assertFalse(sequence.canSkipNext)

        assertEquals(18, sequence.advance())
        assertEquals(4, sequence.nextId)
        assertEquals(4, sequence.advance())
        assertEquals(12, sequence.nextId)
        assertEquals(listOf(9, 4, 12, 18), savedOrder)
    }

    @Test
    fun cannotSkipWhenThereIsNoLaterExerciseToShow() {
        val sequence = WorkoutSequence(listOf(9, 4))

        assertFalse(sequence.canSkipNext)
        assertFalse(sequence.skipNext())
        assertEquals(4, sequence.nextId)
    }
}
