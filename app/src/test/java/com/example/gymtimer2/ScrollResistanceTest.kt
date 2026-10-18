package com.example.gymtimer2

import com.example.gymtimer2.ui.screens.local_songs.dampScrollSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollResistanceTest {
    @Test
    fun speedsAtOrBelowThresholdRemainUnchanged() {
        assertEquals(0f, dampScrollSpeed(0f), 0.001f)
        assertEquals(600f, dampScrollSpeed(600f), 0.001f)
        assertEquals(1_200f, dampScrollSpeed(1_200f), 0.001f)
    }

    @Test
    fun fasterGesturesAreDampedGraduallyAndRemainMonotonic() {
        val speeds = listOf(1_200f, 1_500f, 2_300f, 4_000f, 10_000f)
        val adjusted = speeds.map(::dampScrollSpeed)

        assertTrue(adjusted.zipWithNext().all { (current, next) -> next > current })
        assertTrue(adjusted.drop(1).zip(speeds.drop(1)).all { (current, original) -> current < original })
        assertTrue(adjusted.last() < 3_400f)
    }

    @Test
    fun dampingPreservesScrollDirection() {
        assertEquals(-dampScrollSpeed(2_000f), dampScrollSpeed(-2_000f), 0.001f)
    }
}
