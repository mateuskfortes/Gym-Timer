package com.example.gymtimer2

import com.example.gymtimer2.util.secondsToMillisOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeInputTest {
    @Test
    fun convertsNonNegativeSeconds() {
        assertEquals(0L, secondsToMillisOrNull("0"))
        assertEquals(90000L, secondsToMillisOrNull(" 90 "))
    }

    @Test
    fun rejectsOverflowAndInvalidInput() {
        assertNull(secondsToMillisOrNull("-1"))
        assertNull(secondsToMillisOrNull("9223372036854776"))
        assertNull(secondsToMillisOrNull("invalid"))
    }
}
