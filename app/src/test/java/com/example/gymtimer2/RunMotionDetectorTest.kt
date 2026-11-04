package com.example.gymtimer2

import com.example.gymtimer2.domain.model.RunMotionDetector
import com.example.gymtimer2.domain.model.RunMotionTransition
import com.example.gymtimer2.domain.model.RunActivityType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunMotionDetectorTest {
    @Test
    fun accelerometerAutoPauseIsLimitedToRunActivityTypes() {
        assertTrue(RunActivityType.RUN.usesAccelerometerAutoPause)
        assertTrue(RunActivityType.TRAIL_RUN.usesAccelerometerAutoPause)
        assertFalse(RunActivityType.VIRTUAL_RUN.usesAccelerometerAutoPause)
        assertFalse(RunActivityType.WALK.usesAccelerometerAutoPause)
        assertFalse(RunActivityType.HIKE.usesAccelerometerAutoPause)
        assertFalse(RunActivityType.WHEELCHAIR.usesAccelerometerAutoPause)
    }

    @Test
    fun firstAccelerationChangeDoesNotAssumeTheRunnerIsMoving() {
        val detector = RunMotionDetector()
        detector.sample(0L, 0f, 0f, 0f)

        assertNull(detector.sample(50L, 10f, 0f, 0f))
    }

    @Test
    fun stationarySamplesDoNotPauseBeforeMovementIsDetected() {
        val detector = RunMotionDetector()
        detector.sample(0L, 0f, 0f, 9.8f)

        for (timeMs in 50L..2_000L step 50L) {
            assertNull(detector.sample(timeMs, 0f, 0f, 9.8f))
        }
    }

    @Test
    fun automaticResumeRestoresTheAlreadyMovingState() {
        val detector = RunMotionDetector()
        detector.reset(initiallyMoving = true)
        detector.sample(0L, 0f, 0f, 9.8f)

        val paused = (50L..2_000L step 50L).any { timeMs ->
            detector.sample(timeMs, 0f, 0f, 9.8f) == RunMotionTransition.PAUSE
        }

        assertTrue(paused)
    }

    @Test
    fun detectsMovementBeforePausingAndResumesWhenMovementReturns() {
        val detector = RunMotionDetector()
        detector.sample(0L, 0f, 0f, 9.8f)

        assertNull(detector.sample(50L, 0f, 0f, 9.8f))
        val movementStartMs = findTransition(detector, 100L, RunMotionTransition.RESUME)
        assertNotNull(movementStartMs)
        val movementStartedAt = movementStartMs ?: return

        var pauseAtMs: Long? = null
        for (timeMs in movementStartedAt + 50L..movementStartedAt + 5_000L step 50L) {
            if (detector.sample(timeMs, 30f, 0f, 9.8f) == RunMotionTransition.PAUSE) {
                pauseAtMs = timeMs
                break
            }
        }

        assertNotNull(pauseAtMs)
        val pausedAt = pauseAtMs ?: return
        assertNull(detector.sample(pausedAt + 50L, 30f, 0f, 9.8f))
        assertNotNull(findTransition(detector, pausedAt + 100L, RunMotionTransition.RESUME))
    }

    @Test
    fun gapsAndOutOfOrderSamplesDoNotCreateTransitions() {
        val detector = RunMotionDetector()
        detector.sample(0L, 0f, 0f, 9.8f)

        assertNull(detector.sample(250L, 0f, 0f, 9.8f))
        assertNull(detector.sample(300L, 0f, 0f, 9.8f))
        assertNull(detector.sample(275L, 0f, 0f, 9.8f))
        assertNull(detector.sample(325L, 0f, 0f, 9.8f))
    }

    private fun findTransition(
        detector: RunMotionDetector,
        startTimeMs: Long,
        expected: RunMotionTransition
    ): Long? {
        for (timeMs in startTimeMs..startTimeMs + 2_000L step 50L) {
            val acceleration = if ((timeMs / 50L) % 2L == 0L) 30f else 0f
            if (detector.sample(timeMs, acceleration, 0f, 9.8f) == expected) return timeMs
        }
        return null
    }
}
