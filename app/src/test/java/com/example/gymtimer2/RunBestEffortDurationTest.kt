package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunBestEffortDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifica a validação de tempos digitados para uma marca manual. */
class RunBestEffortDurationTest {
    @Test
    fun parsesMinuteAndHourFormats() {
        assertEquals(272_000L, RunBestEffortDuration.parse("4:32"))
        assertEquals(3_730_000L, RunBestEffortDuration.parse("1:02:10"))
    }

    @Test
    fun rejectsInvalidAndOutOfRangeTimes() {
        assertNull(RunBestEffortDuration.parse("4:75"))
        assertNull(RunBestEffortDuration.parse("1:60:00"))
        assertNull(RunBestEffortDuration.parse("48:00:01"))
        assertNull(RunBestEffortDuration.parse("0:00"))
    }
}
